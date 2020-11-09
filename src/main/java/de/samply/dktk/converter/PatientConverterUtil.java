package de.samply.dktk.converter;

import static de.samply.dktk.converter.Constants.COMMENT_URN_DKTK_DATAELEMENT_43;
import static de.samply.dktk.converter.Constants.COMMENT_URN_DKTK_DATAELEMENT_50;
import static de.samply.dktk.converter.Constants.DATE_FORMAT_SLOT_NAME;
import static de.samply.dktk.converter.Constants.DESIGNATION_SAMPLE;
import static de.samply.dktk.converter.Constants.MDR_LANGUAGE;
import static de.samply.dktk.converter.Constants.ROW_INDEX_DESTINATION;
import static de.samply.dktk.converter.Constants.ROW_INDEX_HEADER;
import static de.samply.dktk.converter.Constants.URN_DKTK_DATAELEMENT_43;
import static de.samply.dktk.converter.Constants.URN_DKTK_DATAELEMENT_50;

import com.google.common.base.Joiner;
import com.google.common.collect.TreeTraverser;
import de.samply.common.mdrclient.MdrClient;
import de.samply.common.mdrclient.MdrConnectionException;
import de.samply.common.mdrclient.MdrInvalidResponseException;
import de.samply.common.mdrclient.domain.DataElement;
import de.samply.common.mdrclient.domain.Meaning;
import de.samply.common.mdrclient.domain.PermissibleValue;
import de.samply.common.mdrclient.domain.Slot;
import de.samply.common.mdrclient.domain.Validations;
import de.samply.dktk.converter.sort.mdr.SortRules;
import de.samply.share.common.utils.MdrIdDatatype;
import de.samply.share.model.ccp.Attribute;
import de.samply.share.model.ccp.Case;
import de.samply.share.model.ccp.Contact;
import de.samply.share.model.ccp.Container;
import de.samply.share.model.ccp.Inquiry;
import de.samply.share.model.ccp.Patient;
import de.samply.share.model.ccp.Sample;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;

/**
 * Utility methods for the PatientConverter.
 */
public class PatientConverterUtil {

  private static final Logger logger = LogManager.getLogger(PatientConverterUtil.class);

  /**
   * Gets a Map of permitted values and their designation of a dataelement.
   *
   * @param mdrKey           the dataelements mdr id
   * @param languageCode     the language code
   * @param includeIdentical if set to false, values are only added if value and designation differ
   * @return the list of values and designations or null if it's not an enumerated value domain
   */
  public static Map<String, String> getValuesAndDesignations(MdrClient mdrClient, String mdrKey,
      String languageCode, boolean includeIdentical) {
    try {
      DataElement dataElement = mdrClient.getDataElement(mdrKey, languageCode);
      Validations validations = dataElement.getValidation();
      String dataType = validations.getDatatype();
      if (dataType.equalsIgnoreCase("enumerated")) {
        Map<String, String> valueAndDesignationMap = new HashMap<>();
        List<PermissibleValue> permissibleValues = validations.getPermissibleValues();
        for (PermissibleValue pv : permissibleValues) {
          List<Meaning> meanings = pv.getMeanings();
          for (Meaning m : meanings) {
            if (m.getLanguage().equalsIgnoreCase(languageCode)) {
              // Add it to the map if either the value differs from the desigation or the include
              // switch is true
              if (includeIdentical || !pv.getValue().equals(m.getDesignation())) {
                valueAndDesignationMap.put(pv.getValue(), m.getDesignation());
              }
            }
          }
        }
        return valueAndDesignationMap;
      } else {
        return null;
      }
    } catch (MdrConnectionException | MdrInvalidResponseException | ExecutionException e) {
      return null;
    }
  }


  /**
   * Extract the value to a key in a list of slots.
   *
   * @param slots the list of slots to check
   * @param key   the key to look for
   * @return the slot value or null if not found
   */
  public static String getValueFromSlots(List<Slot> slots, String key) {
    for (Slot slot : slots) {
      if (slot.getSlotName().trim().equalsIgnoreCase(key)) {
        return slot.getSlotValue();
      }
    }
    return null;
  }

  /**
   * Convert date.
   *
   * @param dateString       the date string
   * @param simpleDateFormat the simple date format
   * @return the date
   */
  public static Date convertDate(String dateString, SimpleDateFormat simpleDateFormat)
      throws ParseException {
    return simpleDateFormat.parse(dateString);
  }

  /**
   * Fix the first three rows on top.
   *
   * @param workBook Todo.
   * @return Todo.
   */
  public static Workbook freezeHeaderRows(Workbook workBook) {
    for (int i = 0; i < workBook.getNumberOfSheets(); i++) {
      Sheet sheet = workBook.getSheetAt(i);
      sheet.createFreezePane(0, 3);
    }
    return workBook;
  }

  /**
   * Traverse through a patient and get the oldest date for a given mdr id element.
   *
   * @param patient the patient container to look in
   * @param mdrId   the mdr id to look for
   * @return the string representation of the youngest entry
   */
  public static String getOldestDateString(MdrClient mdrClient,
      de.samply.share.model.common.Container patient, MdrIdDatatype mdrId)
      throws PatientConverterException {
    TreeTraverser<de.samply.share.model.common.Container> containerTraverser =
        new TreeTraverser<de.samply.share.model.common.Container>() {
      @Override
      public Iterable<de.samply.share.model.common.Container> children(
          de.samply.share.model.common.Container root) {
        return root.getContainer();
      }
    };
    SimpleDateFormat dateFormat = null;
    try {
      DataElement dataElement = mdrClient.getDataElement(mdrId.getLatestMdr(), "en");
      List<Slot> dataElementSlots = dataElement.getSlots();
      String dateFormatString = PatientConverterUtil
          .getValueFromSlots(dataElementSlots, DATE_FORMAT_SLOT_NAME);
      if (dateFormatString != null) {
        dateFormat = new SimpleDateFormat(dateFormatString);
      }
    } catch (MdrConnectionException | MdrInvalidResponseException | ExecutionException e) {
      logger.warn("Could not get dataelement slots for " + mdrId.toString());
    }

    if (dateFormat == null) {
      return null;
    }

    List<Date> dates = new ArrayList<>();

    for (de.samply.share.model.common.Container container : containerTraverser
        .preOrderTraversal(patient)) {
      for (de.samply.share.model.common.Attribute attribute : container.getAttribute()) {
        MdrIdDatatype attributeKey = new MdrIdDatatype(attribute.getMdrKey());
        if (mdrId.equalsIgnoreVersion(attributeKey)) {
          try {
            dates
                .add(PatientConverterUtil.convertDate(attribute.getValue().getValue(), dateFormat));
          } catch (ParseException e) {
            throw new PatientConverterException(e);
          }
        }
      }
    }

    if (dates.isEmpty()) {
      return null;
    } else {
      return dateFormat.format(Collections.min(dates));
    }
  }

  /**
   * Check if a given container is a leaf (has no more children).
   * Ignore Samples in this calculation since they will be moved to a separate area later.
   *
   * @param container the container to check
   * @return true if it is a leaf, false if it has children
   */
  static boolean isLeaf(Container container) {
    List<Container> children = container.getContainer();

    // if the list of children is null or empty...it's easy
    if (children == null || children.size() < 1) {
      return true;
    }
    // Otherwise check if there is at least one subcontainer that is not a sample
    for (Container c : children) {
      if (!c.getDesignation().equalsIgnoreCase(DESIGNATION_SAMPLE)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the index of the cell containing a string in the header row.
   *
   * @param header    the value to check for
   * @param headerRow the header row to check in
   * @return the position where the header row contains the key, or -1 if not found
   */
  static int getCellIndex(String header, Row headerRow) {
    for (Cell cell : headerRow) {
      // We can safely assume that the header row only contains Strings
      if (cell.getStringCellValue().contains(header)) {
        return cell.getColumnIndex();
      }
    }
    return -1;
  }


  /**
   * Re-arrange Columns according to the dktk mds id.
   *
   * @param workbook   the workbook containing the sheet
   * @param sheetIndex which sheet to get
   * @return the sheet with re-ordered columns
   */
  public static Sheet sortSheet(Workbook workbook, int sheetIndex, SortRules sortRules) {
    Sheet sheet = workbook.getSheetAt(sheetIndex);
    if (sheet == null) {
      return sheet;
    }
    Map<String, Integer> mdrIdColumnMap = new TreeMap<>();
    Row headerRow = sheet.getRow(ROW_INDEX_HEADER);

    // For entries without DKTK-ID, use a high incremented number to enable sorting
    Set<Integer> excludedColumns = new HashSet<>();

    for (int i = headerRow.getFirstCellNum(); i <= headerRow.getLastCellNum(); ++i) {
      Cell cell;
      if (i < 0) {
        cell = null;
      } else {
        cell = headerRow.getCell(i);
      }
      if (cell == null) {
        continue;
      }
      String cellValue = cell.getStringCellValue();
      if (isUrn(cellValue)) {
        mdrIdColumnMap.put(cellValue, i);
      } else {
        excludedColumns.add(i);
      }
    }

    List<String> mdrIdsList = new ArrayList<>();
    mdrIdsList.addAll(mdrIdColumnMap.keySet());

    if (mdrIdsList.size() > 0) {

      mdrIdsList = sortRules.sortMdrIds(mdrIdsList);

      int offset = headerRow.getFirstCellNum();

      for (int i = 0; i < mdrIdsList.size(); ++i) {

        String mdrId = mdrIdsList.get(i);
        int toGrab = mdrIdColumnMap.get(mdrId);

        while (excludedColumns.contains(i + offset)) {
          offset++;
        }

        int target = i + offset;

        if (toGrab != target) {
          swapColumns(sheet, toGrab, target);
          updateMdrIdColumnMap(mdrIdColumnMap, toGrab, target);
        }

      }

    }

    return sheet;
  }

  private static boolean isUrn(String element) {
    return element != null && element.startsWith("urn");
  }

  private static void updateMdrIdColumnMap(Map<String, Integer> mdrIdColumnMap,
      int changedColumn1, int changedColumn2) {

    for (Map.Entry<String, Integer> entry : mdrIdColumnMap.entrySet()) {

      if (entry.getValue() == changedColumn1) {
        entry.setValue(changedColumn2);
      } else if (entry.getValue() == changedColumn2) {
        entry.setValue(changedColumn1);
      }
    }

  }

  /**
   * Swap two columns in a given sheet.
   * Index starts with zero - i.e. Column "A" = 0, "B" = 1...
   *
   * @param sheet   the sheet to modify
   * @param columnA the first column to swap
   * @param columnB the second column to swap
   */
  private static void swapColumns(Sheet sheet, int columnA, int columnB) {
    if (sheet == null) {
      return;
    }
    if (columnA == columnB) {
      logger.trace("Both row indices are identical. No modifications needed.");
      return;
    }
    if (sheet == null || columnA < 0 || columnB < 0) {
      throw new IllegalArgumentException(
          "An excel sheet has to be provided and the row indices have to be positive integers");
    }
    Row headerRow = sheet.getRow(ROW_INDEX_HEADER);
    int maxCellNum = headerRow.getLastCellNum();
    if (maxCellNum < columnA || maxCellNum < columnB) {
      throw new IndexOutOfBoundsException(
          "One of the given columns exceeds the maximum. Max=" + maxCellNum + ";a=" + columnA
              + ";b=" + columnB);
    }

    CellStyle defaultCellstyle = sheet.getWorkbook().createCellStyle();

    for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); ++i) {
      Cell a = SheetUtil.getCellWithMerges(sheet, i, columnA);
      Cell b = SheetUtil.getCellWithMerges(sheet, i, columnB);

      if (a != null && b != null) {
        String tmp = b.getStringCellValue();
        b.setCellValue(a.getStringCellValue());
        b.setCellStyle(a.getCellStyle());
        a.setCellValue(tmp);
        CellStyle tmpStyle = b.getCellStyle();
        a.setCellStyle(tmpStyle);
      } else if (a == null && b != null) {
        Row row = sheet.getRow(i);
        a = row.createCell(columnA);
        a.setCellValue(b.getStringCellValue());
        a.setCellStyle(b.getCellStyle());
        b.setCellType(CellType.BLANK);
        b.setCellStyle(defaultCellstyle);
      } else if (a != null && b == null) {
        Row row = sheet.getRow(i);
        b = row.createCell(columnB);
        b.setCellValue(a.getStringCellValue());
        b.setCellStyle(a.getCellStyle());
        a.setCellType(CellType.BLANK);
        a.setCellStyle(defaultCellstyle);
      } else {
        continue;
      }

      // Swap hyperlinks as well
      if (i == ROW_INDEX_HEADER) {
        Hyperlink hyperlinkA = a.getHyperlink();
        Hyperlink hyperlinkB = b.getHyperlink();

        if (hyperlinkA != null && hyperlinkB != null) {
          a.setHyperlink(hyperlinkB);
          b.setHyperlink(hyperlinkA);
        } else if (hyperlinkA == null && hyperlinkB != null) {
          a.setHyperlink(hyperlinkB);
          b.removeHyperlink();
        } else if (hyperlinkA != null && hyperlinkB == null) {
          b.setHyperlink(hyperlinkA);
          a.removeHyperlink();
        }
      }

      // Swap comments as well
      if (i == ROW_INDEX_DESTINATION) {
        Comment commentA = a.getCellComment();
        Comment commentB = b.getCellComment();

        if (commentA != null && commentB != null) {
          a.setCellComment(commentB);
          b.setCellComment(commentA);
        } else if (commentA == null && commentB != null) {
          a.setCellComment(commentB);
          b.removeCellComment();
        } else if (commentA != null && commentB == null) {
          b.setCellComment(commentA);
          a.removeCellComment();
        }
      }
    }

  }

  /**
   * Add comments, containing the permitted values and their meaning, to Header Cells.
   *
   * @param mdrClient an instance of the mdr client
   * @param workbook  the excel workbook to modify
   */
  public static void addCellComments(MdrClient mdrClient, Workbook workbook) {
    // Cell comments are attached to the cells in the designation header row on each sheet

    Iterator<Sheet> sheetIterator = workbook.sheetIterator();
    while (sheetIterator.hasNext()) {
      Sheet sheet = sheetIterator.next();
      // Do not try to add comments on the info sheet
      if (sheet.getSheetName() == null || sheet.getSheetName().equalsIgnoreCase("info")) {
        continue;
      }
      Row keyRow = sheet.getRow(ROW_INDEX_HEADER);
      Row destinationRow = sheet.getRow(ROW_INDEX_DESTINATION);

      Iterator<Cell> cellIterator = destinationRow.cellIterator();
      while (cellIterator.hasNext()) {
        Cell cell = cellIterator.next();
        String mdrKey = keyRow.getCell(cell.getColumnIndex()).getStringCellValue();
        if (mdrKey == null) {
          continue;
        }
        // For two defined Columns, set custom comments. Those do not have a list of permitted
        // values. If this changes...rethink this!
        if (mdrKey.startsWith(URN_DKTK_DATAELEMENT_43)) {
          addCommentToCell(cell, COMMENT_URN_DKTK_DATAELEMENT_43);
        } else if (mdrKey.startsWith(URN_DKTK_DATAELEMENT_50)) {
          addCommentToCell(cell, COMMENT_URN_DKTK_DATAELEMENT_50);
        } else {
          addValueDesignationListCommentToCell(mdrClient, cell, mdrKey);
        }

      }
    }
  }

  /**
   * Add a list of permitted values and their designations to a cell.
   *
   * @param mdrClient an instance of the mdr client
   * @param cell      the cell to add the comment to
   * @param mdrKey    the mdr id of the dataelement whose values are to be added
   * @return the modified cell
   */
  public static Cell addValueDesignationListCommentToCell(MdrClient mdrClient, Cell cell,
      String mdrKey) {
    Row row = cell.getRow();
    Sheet sheet = row.getSheet();
    Workbook workBook = sheet.getWorkbook();
    // Cell Comment stuff ( https://poi.apache.org/spreadsheet/quick-guide.html#CellComments )

    // First, check if this is a dataelement with enumerated value domain.
    // Otherwise (if map is null or empty) just return the cell as is
    Map<String, String> valueDesignationMap = PatientConverterUtil
        .getValuesAndDesignations(mdrClient, mdrKey, MDR_LANGUAGE, false);
    if (valueDesignationMap == null || valueDesignationMap.isEmpty()) {
      return cell;
    } else {
      Joiner.MapJoiner mapJoiner = Joiner.on('\n').withKeyValueSeparator(" -> ");
      String commentString = mapJoiner.join(valueDesignationMap);
      int listLength = valueDesignationMap.size();

      CreationHelper factory = workBook.getCreationHelper();
      Drawing drawing = sheet.createDrawingPatriarch();
      ClientAnchor anchor = factory.createClientAnchor();
      anchor.setCol1(cell.getColumnIndex());
      anchor.setCol2(cell.getColumnIndex() + 3);
      anchor.setRow1(row.getRowNum());
      anchor.setRow2(row.getRowNum() + listLength);
      Comment comment = drawing.createCellComment(anchor);
      RichTextString str = factory.createRichTextString(commentString);
      comment.setAuthor("Samply.Share.Client");
      comment.setString(str);
      cell.setCellComment(comment);
      return cell;
    }
  }

  /**
   * Add a text comment to a cell.
   *
   * @param cell          the cell to add the comment to
   * @param commentString the comment content
   * @return the modified cell
   */
  public static Cell addCommentToCell(Cell cell, String commentString) {
    Row row = cell.getRow();
    Sheet sheet = row.getSheet();
    Workbook workBook = sheet.getWorkbook();
    CreationHelper factory = workBook.getCreationHelper();
    Drawing drawing = sheet.createDrawingPatriarch();
    ClientAnchor anchor = factory.createClientAnchor();
    anchor.setCol1(cell.getColumnIndex());
    anchor.setCol2(cell.getColumnIndex() + 3);
    anchor.setRow1(row.getRowNum());
    anchor.setRow2(row.getRowNum() + 2);
    Comment comment = drawing.createCellComment(anchor);
    RichTextString str = factory.createRichTextString(commentString);
    comment.setAuthor("Samply.Share.Client");
    comment.setString(str);
    cell.setCellComment(comment);
    return cell;
  }

  /**
   * Create a minimalistic inquiry object with only those attributes that will be read by the.
   * converter
   *
   * @param inquiryLabel       the "name" of the inquiry
   * @param inquiryDescription the description of the inquiry
   * @return a minimalistic inquiry object
   */
  public static Inquiry createInquiryObjectForInfoSheet(String inquiryLabel,
      String inquiryDescription) {
    Inquiry inquiry = new Inquiry();
    inquiry.setLabel(inquiryLabel);
    inquiry.setDescription(inquiryDescription);
    return inquiry;
  }

  /**
   * Create a minimalistic contact object with only those attributes that will be read by the
   * converter.
   *
   * @param contactTitle     the title or null
   * @param contactFirstname the first name of the inquirer
   * @param contactLastname  the last name of the inquirer
   * @return a minimalistic contact object
   */
  public static Contact createContactObjectForInfoSheet(String contactTitle,
      String contactFirstname, String contactLastname) {
    Contact contact = new Contact();
    contact.setTitle(contactTitle);
    contact.setFirstname(contactFirstname);
    contact.setLastname(contactLastname);
    return contact;
  }

  /**
   * Todo.
   * @param patient Todo.
   * @param attributeString Todo.
   * @return Todo.
   */
  public static Patient removeAttributeFromPatient(Patient patient, String attributeString) {

    PatientAttributeRemover patientAttributeRemover = new PatientAttributeRemover(patient,
        attributeString);
    return patientAttributeRemover.remove();

  }

  /**
   * Remove attributes with a given MdrKey from the Patients Cases.
   *
   * @param patient         the patient to remove the attribute from
   * @param attributeString the mdr key of the attribute to remove
   * @return the patient without any attributes with the given mdrkey
   */
  public static Patient removeAttributeFromCases(Patient patient, String attributeString) {
    for (Iterator<Case> caseIterator = patient.getCase().iterator(); caseIterator.hasNext(); ) {
      Case caseCcp = caseIterator.next();
      for (Iterator<Attribute> attributeIterator = caseCcp.getAttribute().iterator();
          attributeIterator.hasNext(); ) {
        Attribute attribute = attributeIterator.next();
        if (attribute.getMdrKey().equalsIgnoreCase(attributeString)) {
          attributeIterator.remove();
        }
      }
    }
    return patient;
  }

  /**
   * Remove attributes with a given MdrKey from the Patients Samples.
   *
   * @param patient         the patient to remove the attribute from
   * @param attributeString the mdr key of the attribute to remove
   * @return the patient without any attributes with the given mdrkey
   */
  public static Patient removeAttributeFromSamples(Patient patient, String attributeString) {
    for (Iterator<Sample> sampleIterator = patient.getSample().iterator();
        sampleIterator.hasNext(); ) {
      Sample sample = sampleIterator.next();
      for (Iterator<Attribute> attributeIterator = sample.getAttribute().iterator();
          attributeIterator.hasNext(); ) {
        Attribute attribute = attributeIterator.next();
        if (attribute.getMdrKey().equalsIgnoreCase(attributeString)) {
          attributeIterator.remove();
        }
      }
    }
    return patient;
  }

  private static int getColumnIndex(Sheet sheet, String dataElementUrn) {
    Row headerRow = sheet.getRow(ROW_INDEX_HEADER);

    for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); ++i) {
      Cell cell = headerRow.getCell(i);
      if (cell.getStringCellValue() != null && cell.getStringCellValue()
          .startsWith(dataElementUrn)) {
        return cell.getColumnIndex();
      }
    }

    return -1;
  }

  protected static void autosizeAllColumns(Workbook workBook) {
    for (int i = 0; i < workBook.getNumberOfSheets(); i++) {
      Sheet sheet = workBook.getSheetAt(i);
      Row headerRow = sheet.getRow(ROW_INDEX_HEADER);
      if (headerRow != null) {
        for (int j = 0; j <= headerRow.getLastCellNum(); j++) {
          sheet.autoSizeColumn(j);
        }
      }
    }
  }

  /**
   * Add an autofilter to a whole sheet.
   * This comes closest to the "format as table" feature from Excel.
   *
   * @param workbook   the workbook in which the filter should be set
   * @param sheetIndex the index of the sheet to be filtered
   */
  protected static void addAutoFilter(Workbook workbook, int sheetIndex) {
    Sheet sheet = workbook.getSheetAt(sheetIndex);
    int rowStartIndex = 2; // Starting with index 2 means starting at "X3"
    int rowEndIndex = sheet.getLastRowNum();

    int columnStartIndex = 0;
    int columnEndIndex = sheet.getRow(0).getLastCellNum() - 1;

    CellRangeAddress cra = new CellRangeAddress(rowStartIndex, rowEndIndex, columnStartIndex,
        columnEndIndex);
    sheet.setAutoFilter(cra);
  }

  /**
   * Check if an MdrId is in the blacklist.
   * The check will be done ignoring the version number. Thus the standard contains() method of a
   * collection is not used here.
   *
   * @param blacklist    the list of keys to remove
   * @param attributeKey the key to check for
   * @return true if the attributeKey is in the blacklist (in any version)
   */
  public static boolean isBlacklisted(List<MdrIdDatatype> blacklist, MdrIdDatatype attributeKey) {
    for (MdrIdDatatype entry : blacklist) {
      if (entry.equalsIgnoreVersion(attributeKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert a Patient object to a container object.
   * For easy tree traversal since all child nodes are containers anyways.
   *
   * @param patient the patient to convert
   * @return a container object
   */
  static de.samply.share.model.common.Container createPatientContainer(
      de.samply.share.model.common.Patient patient) {
    // Create a Patient "Container"
    de.samply.share.model.common.Container patientContainer =
        new de.samply.share.model.common.Container();
    patientContainer.getAttribute().addAll(patient.getAttribute());
    patientContainer.getContainer().addAll(patient.getContainer());
    return patientContainer;
  }

  /**
   * Get the first value from an attribute with the given mdr key.
   * Traverses breadth-first through the patient container.
   *
   * @param patient the patient to check in
   * @param key     the key to look for
   * @return the value found in the container or null if not found
   */
  public static String getFirstValueForKey(de.samply.share.model.common.Patient patient,
      MdrIdDatatype key) {
    TreeTraverser<de.samply.share.model.common.Container> containerTraverser =
        new TreeTraverser<de.samply.share.model.common.Container>() {
      @Override
      public Iterable<de.samply.share.model.common.Container> children(
          de.samply.share.model.common.Container root) {
        return root.getContainer();
      }
    };

    de.samply.share.model.common.Container patientContainer = createPatientContainer(patient);
    for (de.samply.share.model.common.Container container : containerTraverser
        .breadthFirstTraversal(patientContainer)) {
      for (de.samply.share.model.common.Attribute attribute : container.getAttribute()) {
        MdrIdDatatype attributeKey = new MdrIdDatatype(attribute.getMdrKey());
        if (attributeKey.equalsIgnoreVersion(key)) {
          return attribute.getValue().getValue();
        }
      }
    }
    return null;
  }
  /*
  /**
   * Remove attributes with a given MdrKey from the Patient
   *
   * @param patient         the patient to remove the attribute from
   * @param attributeString the mdr key of the attribute to remove
   * @return the patient without any attributes with the given mdrkey
   */
  /*
    public static Patient removeAttributeFromPatient(Patient patient, String attributeString) {
        patient = removeAttributeFromCases(patient, attributeString);
        patient = removeAttributeFromSamples(patient, attributeString);
        return patient;
    }
  */

  private static class PatientAttributeRemover extends PatientAttributeOperator {

    private String attributeToBeRemoved;
    private Patient patient;

    public PatientAttributeRemover(Patient patient, String attributeToBeRemoved) {
      this.patient = patient;
      this.attributeToBeRemoved = getMajorMdrId(attributeToBeRemoved);
    }

    private String getMajorMdrId(Attribute attribute) {

      String mdrKey = attribute.getMdrKey();
      mdrKey = getMajorMdrId(mdrKey);

      return mdrKey;
    }

    private String getMajorMdrId(String urn) {

      if (urn != null) {

        MdrIdDatatype mdrId = new MdrIdDatatype(urn);
        urn = mdrId.getMajor();

      }

      return urn;

    }

    @Override
    protected Attribute operateAttribute(Attribute attribute) {
      return (getMajorMdrId(attribute).equalsIgnoreCase(attributeToBeRemoved)) ? null : attribute;
    }

    public Patient remove() {
      return this.operateAttributes(this.patient);
    }

  }


}
