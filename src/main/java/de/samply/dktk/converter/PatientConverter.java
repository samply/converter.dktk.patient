package de.samply.dktk.converter;

import static de.samply.dktk.converter.Constants.CELL_VALUE_FIRST_PROGRESS_DATE;
import static de.samply.dktk.converter.Constants.CELL_VALUE_FIRST_TUMOUR_DIAGNOSIS_DATE;
import static de.samply.dktk.converter.Constants.CELL_VALUE_PATIENT_ID;
import static de.samply.dktk.converter.Constants.CELL_VALUE_SITE_ID;
import static de.samply.dktk.converter.Constants.COLUMN_DKTK_LOCAL_ID;
import static de.samply.dktk.converter.Constants.COLUMN_INDEX_FIRST_PROGRESS_DATE;
import static de.samply.dktk.converter.Constants.COLUMN_INDEX_FIRST_TUMOUR_DATE;
import static de.samply.dktk.converter.Constants.COLUMN_INDEX_FK;
import static de.samply.dktk.converter.Constants.COLUMN_INDEX_ID;
import static de.samply.dktk.converter.Constants.COLUMN_INDEX_PID;
import static de.samply.dktk.converter.Constants.DESIGNATION_DIAGNOSIS;
import static de.samply.dktk.converter.Constants.DESIGNATION_PATIENT;
import static de.samply.dktk.converter.Constants.DESIGNATION_SAMPLE;
import static de.samply.dktk.converter.Constants.DKTK_ID_SLOT_NAME;
import static de.samply.dktk.converter.Constants.DKTK_SUCHBAR_SLOT;
import static de.samply.dktk.converter.Constants.DKTK_SUCHBAR_SLOT_HIDDEN;
import static de.samply.dktk.converter.Constants.FILE_SUFFIX_XLSX;
import static de.samply.dktk.converter.Constants.HEADER_DIAGNOSIS_CODE;
import static de.samply.dktk.converter.Constants.HEADER_VALUE_ID;
import static de.samply.dktk.converter.Constants.MDR_LANGUAGE;
import static de.samply.dktk.converter.Constants.PROGRESS_FIRST_DATE;
import static de.samply.dktk.converter.Constants.REF_PARENT_PREFIX;
import static de.samply.dktk.converter.Constants.REF_SEPARATOR;
import static de.samply.dktk.converter.Constants.ROW_INDEX_DESTINATION;
import static de.samply.dktk.converter.Constants.ROW_INDEX_DKTK_ID;
import static de.samply.dktk.converter.Constants.ROW_INDEX_HEADER;
import static de.samply.dktk.converter.Constants.TUMOUR_FIRST_DATE;
import static de.samply.dktk.converter.Constants.URN_DKTK_DATAELEMENT_50;
import static java.time.format.ResolverStyle.STRICT;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeTraverser;
import de.dth.mdr.validator.MdrValidator;
import de.dth.mdr.validator.exception.ValidatorException;
import de.samply.common.mdrclient.MdrClient;
import de.samply.common.mdrclient.MdrConnectionException;
import de.samply.common.mdrclient.MdrInvalidResponseException;
import de.samply.common.mdrclient.domain.DataElement;
import de.samply.common.mdrclient.domain.Slot;
import de.samply.common.mdrclient.domain.Validations;
import de.samply.dktk.converter.sort.container.DktkSortRules;
import de.samply.dktk.converter.sort.container.SortRules;
import de.samply.dktk.converter.sort.mdr.GbaSortRules;
import de.samply.share.common.utils.MdrIdDatatype;
import de.samply.share.common.utils.PercentageLogger;
import de.samply.share.model.bbmri.BbmriResult;
import de.samply.share.model.ccp.Attribute;
import de.samply.share.model.ccp.Case;
import de.samply.share.model.ccp.Contact;
import de.samply.share.model.ccp.Container;
import de.samply.share.model.ccp.Inquiry;
import de.samply.share.model.ccp.ObjectFactory;
import de.samply.share.model.ccp.Patient;
import de.samply.share.model.ccp.QueryResult;
import de.samply.share.model.ccp.Ref;
import de.samply.share.model.ccp.Sample;
import de.samply.share.utils.Converter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Transform query results (list of patients) from the Centraxx based format to either excel or
 * central mds db format.
 */
public class PatientConverter {

  private static final Logger logger = LogManager.getLogger(PatientConverter.class);
  private static final int WORKBOOK_WINDOW = 30000000;

  private static CellStyle validationErrorCellStyle;

  private int workbookWindow;
  private MdrClient mdrClient;
  private String mdrLinkUrl;
  private String mdrLinkPath = "/detail.xhtml?urn=";
  private EnumValidationHandling validationHandling;
  private MdrValidator validator;
  private List<MdrIdDatatype> blacklist;
  private Object queryTyp;
  //runningEntityIds allows to generate readable ids for each entity in the generated excel export
  // file
  private Map<String, Integer> runningEntityIds = new HashMap<>();
  private int runningPatientId;

  /**
   * Create the Patientconverter with an instance of an mdr client.
   * Keep a variant without validator for backwards compatibility.
   *
   * @param mdrClient an already instantiated mdr client, linked to the correct metadata repository
   */
  public PatientConverter(MdrClient mdrClient) {
    this(mdrClient, new ArrayList<MdrIdDatatype>(), WORKBOOK_WINDOW);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client.
   * Keep a variant without validator for backwards compatibility.
   *
   * @param mdrClient an already instantiated mdr client, linked to the correct metadata repository
   * @param workbookWindow buffer size of workbook
   */
  public PatientConverter(MdrClient mdrClient, Integer workbookWindow) {
    this(mdrClient, new ArrayList<MdrIdDatatype>(), workbookWindow);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client.
   * Keep a variant without validator for backwards compatibility.
   *
   * @param mdrClient an already instantiated mdr client, linked to the correct metadata repository
   * @param blacklist a list of mdr ids to be left out of the export
   */
  public PatientConverter(MdrClient mdrClient, List<MdrIdDatatype> blacklist) {
    this(mdrClient, blacklist, WORKBOOK_WINDOW);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client.
   * Keep a variant without validator for backwards compatibility.
   *
   * @param mdrClient an already instantiated mdr client, linked to the correct metadata repository
   * @param blacklist a list of mdr ids to be left out of the export
   * @param workbookWindow size of workbook buffer
   */
  public PatientConverter(MdrClient mdrClient, List<MdrIdDatatype> blacklist,
      Integer workbookWindow) {
    this(mdrClient, null, EnumValidationHandling.NO_VALIDATION, blacklist, workbookWindow);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client and a validator.
   * Sets validation handling to REMOVE_INVALID_ENTRIES if omitted, as proposed in CCPIT-622.
   *
   * @param mdrClient an already instantiated mdr client, linked to the correct metadata repository
   * @param validator a pre-configured DTHValidator object
   */
  public PatientConverter(MdrClient mdrClient, MdrValidator validator) {
    this(mdrClient, validator, EnumValidationHandling.REMOVE_INVALID_ENTRIES);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client, a validator and a validation
   * handling option.
   *
   * @param mdrClient          an already instantiated mdr client, linked to the correct metadata
   *                           repository
   * @param validator          a pre-configured DTHValidator object
   * @param validationHandling how should the patient converter handle invalid attributes
   */
  public PatientConverter(MdrClient mdrClient, MdrValidator validator,
      EnumValidationHandling validationHandling) {
    this(mdrClient, validator, validationHandling, new ArrayList<>(), WORKBOOK_WINDOW);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client, a validator, a validation
   * handling option and a blacklist.
   *
   * @param mdrClient          an already instantiated mdr client, linked to the correct metadata
   *                           repository
   * @param validator          a pre-configured DTHValidator object
   * @param validationHandling how should the patient converter handle invalid attributes
   * @param blacklist          a list of mdr ids to be left out of the export
   */
  public PatientConverter(MdrClient mdrClient,
      MdrValidator validator,
      EnumValidationHandling validationHandling,
      List<MdrIdDatatype> blacklist) {
    this(mdrClient, validator, validationHandling, blacklist, WORKBOOK_WINDOW);
  }

  /**
   * Create the Patientconverter with an instance of an mdr client, a validator, a validation
   * handling option and a blacklist.
   *
   * @param mdrClient          an already instantiated mdr client, linked to the correct metadata
   *                           repository
   * @param validator          a pre-configured DTHValidator object
   * @param validationHandling how should the patient converter handle invalid attributes
   * @param blacklist          a list of mdr ids to be left out of the export
   * @param workbookWindow     buffer size of workbook
   */
  public PatientConverter(MdrClient mdrClient,
      MdrValidator validator,
      EnumValidationHandling validationHandling,
      List<MdrIdDatatype> blacklist,
      Integer workbookWindow) {
    this.mdrClient = mdrClient;
    this.validationHandling = validationHandling;
    this.validator = validator;
    this.blacklist = blacklist;
    this.workbookWindow = (workbookWindow != null) ? workbookWindow : WORKBOOK_WINDOW;
    URI mdrBaseUri = mdrClient.getBaseUri();
    mdrLinkUrl = mdrBaseUri.getScheme() + "://" + mdrBaseUri.getHost();
  }

  /**
   * Create the first page of the workbook with information about the export.
   *
   * @param workBook            the workbook to put the sheet to
   * @param queryResult         the query result to transform
   * @param inquiry             the inquiry that produced the query result
   * @param contact             the contact information of the inquirer
   * @param siteName            the local site name
   * @param executionDateString when was the result generated?
   * @param validationHandling  were the values validated against the mdr? if so, what happened to
   *                            non-compliant data?
   */
  private static void createInfoSheet(Workbook workBook, Object queryResult, Inquiry inquiry,
      Contact contact, String siteName, String executionDateString,
      EnumValidationHandling validationHandling) {
    CellStyle cs = workBook.createCellStyle();
    cs.setWrapText(true);

    // Create a new font and alter it.
    Font headerFont = workBook.createFont();
    headerFont.setFontHeightInPoints((short) 24);
    headerFont.setBold(true);
    CellStyle headerStyle = workBook.createCellStyle();
    headerStyle.setFont(headerFont);

    Sheet infoSheet = workBook.createSheet("Info");
    if (infoSheet instanceof SXSSFSheet) {
      ((SXSSFSheet)infoSheet).trackAllColumnsForAutoSizing();
    }

    Row headerRow = infoSheet.createRow(0);
    headerRow.setHeightInPoints(24);
    Cell headerCell = headerRow.createCell(0);
    headerCell.setCellStyle(headerStyle);
    headerCell.setCellValue("Daten-Export aus Brückenkopf");

    Row contentRow = infoSheet.createRow(1);
;
    contentRow.setHeightInPoints(14 * infoSheet.getDefaultRowHeightInPoints());

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(
        "Dies ist ein automatischer Datenexport aus dem DKTK-Brückenkopf am Standort " + siteName
            + ", der am " + executionDateString
            + " zur Beantwortung der folgenden Suchanfrage erzeugt wurde:\n\n");
    stringBuilder.append("Name der Anfrage: " + inquiry.getLabel() + "\n");
    if (queryResult instanceof QueryResult) {
      QueryResult queryResult1 = (QueryResult) queryResult;
      stringBuilder.append("Anzahl der Ergebnisse: " + queryResult1.getPatient().size() + "\n");
    } else if (queryResult instanceof BbmriResult) {
      BbmriResult queryResult1 = (BbmriResult) queryResult;
      stringBuilder.append("Anzahl der Ergebnisse: " + queryResult1.getDonors().size() + "\n");
    }
    stringBuilder.append("Kontaktperson: ");
    if (contact.getTitle() != null && contact.getTitle().length() > 0) {
      stringBuilder.append(contact.getTitle() + " ");
    }
    stringBuilder.append(contact.getFirstname() + " " + contact.getLastname() + "\n");
    stringBuilder.append("Beschreibung: " + inquiry.getDescription() + "\n");
    stringBuilder.append("Syntaktische Validierung: ");
    switch (validationHandling) {
      case KEEP_INVALID_ENTRIES:
        stringBuilder.append(
            "Werte, die nicht konform zu den im MDR hinterlegten Validierungsinformationen sind, "
                + "sind in diesem Dokument orange hinterlegt.");
        break;
      case REMOVE_INVALID_ENTRIES:
        stringBuilder.append(
            "Werte, die nicht konform zu den im MDR hinterlegten Validierungsinformationen sind,"
                + " wurden aus diesem Dokument entfernt.");
        break;
      case NO_VALIDATION:
      default:
        stringBuilder.append(
            "Es fand keine Validierung der Werte statt. Dieses Dokument kann deshalb Einträge "
                + "enthalten, die nicht konform zu den im MDR hinterlegten "
                + "Validierungsinformationen sind.");
        break;
    }

    stringBuilder.append("\n\nVor Verwendung der Datei beachten Sie unbedingt:\n");
    stringBuilder.append(
        "1. Bitte stellen Sie sicher, dass organisatorische Rahmenbedingungen für den Export "
            + "gegeben sind (insb. Zustimmung der Datenbesitzer und des Datenschutzes).\n");
    stringBuilder.append(
        "2. Es handelt sich um ein experimentelles, noch in der Erprobung befindliches Feature. "
            + "Technische Fehler sind nicht ausgeschlossen. Im Zweifel konsultieren Sie die "
            + "Quelldatenbank.\n");
    stringBuilder.append(
        "3. Die exportierten Daten haben eine sehr hohe Präzision. Es wurden keine Maßnahmen zur "
            + "Anonymisierung vorgenommen. Bitte prüfen Sie die Ausgabe und anonymisieren Sie die "
            + "Daten ggfls. selbst, um eine unerlaubte Reidentifikation von Patienten/Probanden "
            + "auszuschließen.\n");
    stringBuilder.append(
        "\nBei Fragen wenden Sie sich bitte an Ihren Standortvertreter der Arbeitsgruppe CCP-IT.");

    Cell contentCell = contentRow.createCell(0);
    contentCell.setCellValue(stringBuilder.toString());
    contentCell.setCellStyle(cs);

    // Create a border around the two cells (header and content)
    CellRangeAddress region = new CellRangeAddress(0, 1, 0, 0);
    RegionUtil.setBorderBottom(BorderStyle.THICK, region, infoSheet);
    RegionUtil.setBorderTop(BorderStyle.THICK, region, infoSheet);
    RegionUtil.setBorderLeft(BorderStyle.THICK, region, infoSheet);
    RegionUtil.setBorderRight(BorderStyle.THICK, region, infoSheet);
  }

  /**
   * Split a reference to a parent object.
   *
   * @param container the container to check
   * @return the reference, split in designation type and id, or null if none is found
   */
  private static ArrayList<String> splitParentRef(
      de.samply.share.model.common.Container container) {
    try {
      for (de.samply.share.model.common.Ref ref : container.getRef()) {
        for (String entityId : ref.getEntityId()) {
          ArrayList<String> splitRef = Lists
              .newArrayList(Splitter.on(REF_SEPARATOR).split(entityId));
          if (splitRef.get(0).equals(REF_PARENT_PREFIX)) {
            return splitRef;
          }
        }
      }
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      System.out.println("No parent reference found");
    }
    return null;
  }

  /**
   * Transform a query result to a list of containers.
   *
   * @param queryResult the query result to transform
   * @return the list of containers
   */
  private static List<de.samply.share.model.common.Container> transformQueryResultToContainerList(
      Object queryResult) {
    List<de.samply.share.model.common.Container> list = new ArrayList<>();
    de.samply.share.model.common.Container patientContainer = null;
    if (queryResult instanceof QueryResult) {
      QueryResult queryResult1 = (QueryResult) queryResult;
      for (Patient patient : queryResult1.getPatient()) {
        try {
          patientContainer = PatientConverterUtil
              .createPatientContainer(Converter.convertCcpPatientToCommonPatient(patient));
        } catch (JAXBException e) {
          e.printStackTrace();
        }
        patientContainer.setId(patient.getId());
        patientContainer.setDesignation("Patient");
        list.add(patientContainer);
      }
      return list;
    } else if (queryResult instanceof BbmriResult) {
      BbmriResult queryResult1 = (BbmriResult) queryResult;
      for (de.samply.share.model.osse.Patient patient : queryResult1.getDonors()) {
        try {
          patientContainer = PatientConverterUtil
              .createPatientContainer(Converter.convertOssePatientToCommonPatient(patient));
        } catch (JAXBException e) {
          e.printStackTrace();
        }
        patientContainer.setId(patient.getId());
        patientContainer.setDesignation("Patient");
        list.add(patientContainer);
      }
      return list;
    }
    return null;
  }

  private Integer getRunningEntityId(String entity) {

    Integer runningEntityId = runningEntityIds.get(entity);
    if (runningEntityId == null) {
      runningEntityId = 1;
      runningEntityIds.put(entity, runningEntityId);
    }

    return runningEntityId;

  }

  private void incrementRunningEntityId(String entity) {

    Integer runningEntityId = getRunningEntityId(entity);
    runningEntityId++;
    runningEntityIds.put(entity, runningEntityId);

  }

  //sets Entity name in the excel export file
  private String getRunningEntityIdName(String entity) {
    return entity.substring(0, 3);
  }

  public List<MdrIdDatatype> getBlacklist() {
    return blacklist;
  }

  public void setBlacklist(List<MdrIdDatatype> blacklist) {
    this.blacklist = blacklist;
  }

  /**
   * Transform one patient dataset from centraxx format to central mds format.
   *
   * @param centraxxPatient the patient as received from centraxx
   * @return the patient formatted for the central mds database
   */
  public Patient centraxxToCentralsearch(Patient centraxxPatient) {
    Container patientContainer = null;
    try {
      patientContainer = Converter.convertCommonContainerToCcpContainer(PatientConverterUtil
          .createPatientContainer(Converter.convertCcpPatientToCommonPatient(centraxxPatient)));
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    Patient centralSearchPatient = traversePatientContainer(patientContainer);

    centralSearchPatient.setId(centraxxPatient.getId());
    centralSearchPatient.setCentraxxId(centraxxPatient.getCentraxxId());
    centralSearchPatient.setDktkId(centraxxPatient.getDktkId());
    return centralSearchPatient;
  }

  /**
   * Traverse through the patient container and replace container elements with patient, samples and
   * cases.
   *
   * @param patientContainer the source patient container
   * @return the transformed patient with samples and cases instead of containers
   */
  private Patient traversePatientContainer(Container patientContainer) {
    TreeTraverser<Container> containerTraverser = new TreeTraverser<Container>() {
      @Override
      public Iterable<Container> children(Container root) {
        List<Container> children = new ArrayList<Container>();
        for (Container c : root.getContainer()) {
          // For diagnosis...add a reference to itself, so we can pass it down to the child elements
          if (c.getDesignation().equalsIgnoreCase(DESIGNATION_DIAGNOSIS)) {
            Ref selfRef = new Ref();
            selfRef.getEntityId().add(DESIGNATION_DIAGNOSIS + ":" + c.getId());
            c.getRef().add(selfRef);
          }
          // Don't pass attributes or refs to samples
          if (!c.getDesignation().equalsIgnoreCase(DESIGNATION_SAMPLE)) {
            c.getAttribute().addAll(root.getAttribute());
            c.getRef().addAll(root.getRef());
          }
          children.add(c);
        }
        return children;
      }
    };

    Patient patient = new Patient();
    for (Container container : containerTraverser.breadthFirstTraversal(patientContainer)) {
      if (PatientConverterUtil.isLeaf(container)) {
        if (container.getDesignation().equalsIgnoreCase(DESIGNATION_SAMPLE)) {
          Sample sample = new Sample();
          sample.setId(container.getId());
          sample.getAttribute().addAll(container.getAttribute());
          sample.getRef().addAll(container.getRef());
          patient.getSample().add(sample);
        } else {
          Case caseCcp = new Case();
          caseCcp.setId(container.getId());
          caseCcp.getAttribute().addAll(container.getAttribute());
          caseCcp.getRef().addAll(container.getRef());
          patient.getCase().add(caseCcp);
        }
      }
    }

    return linkRefs(patient);
  }

  /**
   * Change Ref(erence)s from sample->diagnosis to sample->case.
   *
   * @param sourcePatient the patient where the references shall be changed
   * @return the patient with changed references
   */
  private Patient linkRefs(Patient sourcePatient) {
    // Check each sample for Refs to diagnoses. Then Check for the corresponding Refs in the cases
    // and replace the refs in the sample.
    for (Sample sample : sourcePatient.getSample()) {
      if (sample.getRef().isEmpty()) {
        continue;
      }
      String designation = sample.getRef().get(0).getEntityId().get(
          0); // There should be only one reference, since a sample belongs to only one diagnosis
      if (designation == null || designation.contains(":")
          == false) { // If the entity id does not contain any colons, it's erroneous
        continue;
      }

      // Clear the old ref
      sample.getRef().clear();

      Ref newRef = new Ref();
      for (Case caseCcp : sourcePatient.getCase()) {
        // Check all Refs (although there should be only one)
        for (Ref ref : caseCcp.getRef()) {
          // Check all Entity Ids (although there should be only one)
          for (String entityId : ref.getEntityId()) {
            if (entityId.equalsIgnoreCase(designation)) {
              newRef.getEntityId().add("Case:" + caseCcp.getId());
            }
          }
        }
      }
      sample.getRef().add(newRef);
    }

    // Now clear all refs from cases.
    for (Case caseCcp : sourcePatient.getCase()) {
      caseCcp.getRef().clear();
    }

    return sourcePatient;
  }

  /**
   * Unmarshal an input stream to a query result.
   *
   * @param in the input stream
   * @return the querey result
   */
  public QueryResult unmarshalInputStream(InputStream in) throws JAXBException {
    try {
      Source source = new StreamSource(in);
      JAXBContext context = JAXBContext.newInstance(QueryResult.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      JAXBElement<QueryResult> qrElement = unmarshaller.unmarshal(source, QueryResult.class);
      return qrElement.getValue();
    } catch (ClassCastException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Remove a list of attributes from a patient.
   *
   * @param patient    the patient
   * @param attributes a list of attributes to remove
   * @return the patient without any of the attributes that should be removed
   */
  public Patient removeAttributes(Patient patient, List<String> attributes) {
    ObjectFactory objectFactory = new ObjectFactory();
    for (Iterator<Case> caseIterator = patient.getCase().iterator(); caseIterator.hasNext(); ) {
      Case caseCcp = caseIterator.next();
      for (Iterator<Attribute> attributeIterator = caseCcp.getAttribute().iterator();
          attributeIterator.hasNext(); ) {
        Attribute attribute = attributeIterator.next();
        if (attributes.contains(attribute.getMdrKey())) {
          // TODO: REMOVE THIS DIRTY HACK!!! When no lowercase problems occur any more
          if (attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:29:2")
              || attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:57:2")
              || attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:58:2")
              || attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:4:2")) {
            attribute
                .setValue(objectFactory.createValue(attribute.getValue().getValue().toUpperCase()));
          } else {
            // TODO: UNTIL HERE
            attributeIterator.remove();
          }
        }
      }
    }

    for (Iterator<Sample> sampleIterator = patient.getSample().iterator();
        sampleIterator.hasNext(); ) {
      Sample sample = sampleIterator.next();
      for (Iterator<Attribute> attributeIterator = sample.getAttribute().iterator();
          attributeIterator.hasNext(); ) {
        Attribute attribute = attributeIterator.next();
        if (attributes.contains(attribute.getMdrKey())) {
          // TODO: REMOVE THIS DIRTY HACK!!! When no lowercase problems occur any more
          if (attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:29:2")
              || attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:57:2")
              || attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:58:2")
              || attribute.getMdrKey().equalsIgnoreCase("urn:dktk:dataelement:4:2")) {
            attribute
                .setValue(objectFactory.createValue(attribute.getValue().getValue().toUpperCase()));
          } else {
            // TODO: UNTIL HERE
            attributeIterator.remove();
          }
        }
      }
    }

    return patient;
  }

  /**
   * Generate an excel file from a query result.
   *
   * @param queryResult         the query result to transform
   * @param inquiry             the inquiry that produced the query result
   * @param contact             the contact information of the inquirer
   * @param siteName            the local site name
   * @param executionDateString when was the result generated?
   * @param destinationFolder   where to store the generated files to
   * @return the filename
   */
  public String centraxxQueryResultToExcel(QueryResult queryResult,
      Inquiry inquiry,
      Contact contact,
      String siteName,
      String executionDateString,
      File destinationFolder) throws PatientConverterException {

    Workbook workBook = centraxxQueryResultToExcel(queryResult, inquiry, contact, siteName,
        executionDateString);

    try {
      String filename = destinationFolder + File.separator + queryResult.getId() + FILE_SUFFIX_XLSX;
      FileOutputStream fileOut = new FileOutputStream(filename);
      workBook.write(fileOut);
      fileOut.close();
      workBook.close();
      return filename;
    } catch (IOException e) {
      throw new PatientConverterException(e);
    }
  }

  /**
   * Generate an excel file from a query result.
   *
   * @param queryResult         the query result to transform
   * @param inquiry             the inquiry that produced the query result
   * @param contact             the contact information of the inquirer
   * @param siteName            the local site name
   * @param executionDateString when was the result generated?
   * @return the generated excel workbook
   */
  public Workbook centraxxQueryResultToExcel(QueryResult queryResult,
      Inquiry inquiry,
      Contact contact,
      String siteName,
      String executionDateString) throws PatientConverterException {
    // Reset the running patient number.
    runningPatientId = 1;
    // Reset the running entity Hash number.
    runningEntityIds = new HashMap<>();

    queryTyp = queryResult;

    // TODO: Reduce WORKBOOK_WINDOW in order to improve performance. Hint: First row is problematic
    Workbook workBook = new SXSSFWorkbook(workbookWindow);

    validationErrorCellStyle = workBook.createCellStyle();
    validationErrorCellStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
    validationErrorCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    createInfoSheet(workBook, queryResult, inquiry, contact, siteName, executionDateString,
        validationHandling);

    SortRules sortRules = new DktkSortRules(mdrClient);

    List<de.samply.share.model.common.Container> containerList =
        transformQueryResultToContainerList(queryResult);
    PercentageLogger percentageLogger = new PercentageLogger(logger, containerList.size(),
        "adding patients...");
    for (de.samply.share.model.common.Container patient : containerList) {
      percentageLogger.incrementCounter();
      addPatientDataToExcel(workBook, patient, sortRules);
    }

    PatientConverterUtil.autosizeAllColumns(workBook);

    // Add autofilters to all sheets and sort by mds id
    // Does not apply to the info sheet (id=0), so start the loop with id 1

    percentageLogger = new PercentageLogger(logger, workBook.getNumberOfSheets(),
        "adding sheets...");
    for (int i = 1; i < workBook.getNumberOfSheets(); i++) {

      percentageLogger.incrementCounter();
      PatientConverterUtil.addAutoFilter(workBook, i);
      filterValueOfDatalement(workBook, i, URN_DKTK_DATAELEMENT_50, "true");
      PatientConverterUtil.sortSheet(workBook, i,
          new de.samply.dktk.converter.sort.mdr.DktkSortRules(mdrClient));

    }
    PatientConverterUtil.freezeHeaderRows(workBook);

    PatientConverterUtil.addCellComments(mdrClient, workBook);
    return workBook;
  }

  private void filterValueOfDatalement(Workbook workbook, int sheetIndex, String mdrKey,
      String value) {

    Sheet sheet = workbook.getSheetAt(sheetIndex);
    if (sheet != null) {
      Row headerRow = sheet.getRow(ROW_INDEX_HEADER);
      int columnIndex = PatientConverterUtil.getCellIndex(mdrKey, headerRow);

      if (columnIndex >= 0) {
        filterValueInColumn(sheet, columnIndex, value);
      }
    }

  }

  private void filterValueInColumn(Sheet sheet, int columnIndex, String value) {

    if (sheet != null && sheet instanceof SXSSFSheet) {

      for (final Row row : sheet) {
        for (final Cell c : row) {
          if (c.getColumnIndex() == columnIndex && !c.getStringCellValue().equals(value)) {
            final SXSSFRow r1 = (SXSSFRow) c.getRow();
            if (r1.getRowNum() > 2 && r1.getRowStyle() != null) { // skip header
              r1.getRowStyle().setHidden(true);
            }
          }
        }
      }

    }

  }

  /**
   * Generate an excel file from a query result.
   *
   * @param queryResult         the query result to transform
   * @param inquiry             the inquiry that produced the query result
   * @param contact             the contact information of the inquirer
   * @param siteName            the local site name
   * @param executionDateString when was the result generated?
   * @return the generated excel workbook
   */
  public Workbook biobanksQueryResultToExcel(BbmriResult queryResult,
      Inquiry inquiry,
      Contact contact,
      String siteName,
      String executionDateString) throws PatientConverterException {
    // Reset the running patient number.
    runningPatientId = 1;
    queryTyp = queryResult;

    // TODO: Reduce WORKBOOK_WINDOW in order to improve performance. Hint: First row is problematic
    Workbook workBook = new SXSSFWorkbook(workbookWindow);

    validationErrorCellStyle = workBook.createCellStyle();
    validationErrorCellStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
    validationErrorCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    createInfoSheet(workBook, queryResult, inquiry, contact, siteName, executionDateString,
        validationHandling);

    List<de.samply.share.model.common.Container> containerList =
        transformQueryResultToContainerList(queryResult);
    for (de.samply.share.model.common.Container patient : containerList) {
      addPatientDataToExcel(workBook, patient);
    }

    PatientConverterUtil.autosizeAllColumns(workBook);

    // Add autofilters to all sheets and sort by mds id
    // Does not apply to the info sheet (id=0), so start the loop with id 1
    for (int i = 1; i < workBook.getNumberOfSheets(); i++) {
      PatientConverterUtil.addAutoFilter(workBook, i);
      PatientConverterUtil.sortSheet(workBook, i, new GbaSortRules(mdrClient));
    }
    PatientConverterUtil.freezeHeaderRows(workBook);

    PatientConverterUtil.addCellComments(mdrClient, workBook);
    return workBook;
  }

  private void addPatientDataToExcel(Workbook workBook,
      de.samply.share.model.common.Container patient) throws PatientConverterException {
    addPatientDataToExcel(workBook, patient, null);
  }

  /**
   * Add the data of one patient dataset to the excel workbook.
   *
   * @param workBook the workbook to add the data to
   * @param patient  the patient to add
   */
  private void addPatientDataToExcel(Workbook workBook,
      de.samply.share.model.common.Container patient, SortRules sortRules)
      throws PatientConverterException {

    String newPatId = getRunningEntityIdName(patient.getDesignation()) + "_" + getRunningEntityId(
        patient.getDesignation());
    String localDktkId = patient.getId();
    String firstProgressDate = PatientConverterUtil
        .getOldestDateString(mdrClient, patient, PROGRESS_FIRST_DATE);
    String firstTumourDate = PatientConverterUtil
        .getOldestDateString(mdrClient, patient, TUMOUR_FIRST_DATE);

    DiagnosisConverter diagnosisConverter = new DiagnosisConverter(patient);

    Map<String, String> idAssignment = new HashMap<>();

    List<de.samply.share.model.common.Container> containersOfPatient = getContainersOfPatient(
        patient, sortRules);

    for (de.samply.share.model.common.Container container : containersOfPatient) {
      // Generate a random uuid as new id for this container and store the mapping
      String oldId = container.getId();
      String newId = getAppropriateEntityId(container.getDesignation());

      idAssignment.put(oldId + "." + container.getDesignation(), newId);

      String safeName = WorkbookUtil.createSafeSheetName(container.getDesignation());
      Sheet sheet = workBook.getSheet(safeName);
      Row headerRow;
      Row headerDestinationRow;
      Row headerDktkIdRow;

      ArrayList<String> splitRef = splitParentRef(container);

      // Check if we already have that entity type as a sheet
      if (sheet == null) {
        // If not, create it and begin a header row with its own ID and the Parent ID (if available)
        // , as well as the patient id for deeper leaves
        sheet = workBook.createSheet(safeName);
        if (sheet instanceof SXSSFSheet) {
          ((SXSSFSheet)sheet).trackAllColumnsForAutoSizing();
        }

        headerRow = sheet.createRow(ROW_INDEX_HEADER);
        headerDestinationRow = sheet.createRow(ROW_INDEX_DESTINATION);
        headerDktkIdRow = sheet.createRow(ROW_INDEX_DKTK_ID);

        headerRow.createCell(COLUMN_INDEX_ID).setCellValue(HEADER_VALUE_ID);

        if (splitRef != null) {
          headerRow.createCell(COLUMN_INDEX_FK).setCellValue(splitRef.get(1) + "_ID");
          if (!splitRef.get(1).equalsIgnoreCase(DESIGNATION_PATIENT)) {
            headerRow.createCell(COLUMN_INDEX_PID).setCellValue(CELL_VALUE_PATIENT_ID);
          }
        } else { // Patient Sheet
          headerRow.createCell(COLUMN_DKTK_LOCAL_ID).setCellValue(CELL_VALUE_SITE_ID);
          if (queryTyp instanceof QueryResult) {
            headerRow.createCell(COLUMN_INDEX_FIRST_PROGRESS_DATE)
                .setCellValue(CELL_VALUE_FIRST_PROGRESS_DATE);
            headerRow.createCell(COLUMN_INDEX_FIRST_TUMOUR_DATE)
                .setCellValue(CELL_VALUE_FIRST_TUMOUR_DIAGNOSIS_DATE);
          }

        }
      } else {
        headerRow = sheet.getRow(ROW_INDEX_HEADER);
        headerDestinationRow = sheet.getRow(ROW_INDEX_DESTINATION);
        headerDktkIdRow = sheet.getRow(ROW_INDEX_DKTK_ID);
      }

      // At this place, we have a sheet for this entity (aka container), as well as the header row
      // with all keys that are already known
      // and the row for the designations of the mdr keys
      // Now we iterate through the attributes, check if there's already a column for it in the
      // header and create if if that's not the case.

      Row thisRow = sheet.createRow(sheet.getLastRowNum() + 1);

      // Set the ID
      thisRow.createCell(COLUMN_INDEX_ID).setCellValue(newId);
      // Set the FK / Parent ID if available
      if (splitRef != null) {
        String parentId = idAssignment.get(splitRef.get(2) + "." + splitRef.get(1));
        thisRow.createCell(COLUMN_INDEX_FK).setCellValue(parentId);
        if (!splitRef.get(1).equalsIgnoreCase("patient")) {
          thisRow.createCell(COLUMN_INDEX_PID).setCellValue(newPatId);
        }
      } else {
        thisRow.createCell(COLUMN_DKTK_LOCAL_ID).setCellValue(localDktkId);
        thisRow.createCell(COLUMN_INDEX_FIRST_PROGRESS_DATE).setCellValue(firstProgressDate);
        thisRow.createCell(COLUMN_INDEX_FIRST_TUMOUR_DATE).setCellValue(firstTumourDate);
      }

      addDiagnosisCell(headerRow, thisRow, diagnosisConverter, container);

      try {
        for (de.samply.share.model.common.Attribute attribute : container.getAttribute()) {
          createCell(headerRow, headerDestinationRow, headerDktkIdRow, thisRow, attribute);
        }
      } catch (MdrConnectionException | ExecutionException | MdrInvalidResponseException
          | ValidatorException e) {
        throw new PatientConverterException(e);
      }

    }
  }

  //applies readable relative identifier for each element in each entity
  private String getAppropriateEntityId(String entity) {
    String resultID = "";
    int id = getRunningEntityId(entity);
    resultID = getRunningEntityIdName(entity) + "_" + id;
    incrementRunningEntityId(entity);
    return resultID;
  }

  private List<de.samply.share.model.common.Container> getContainersOfPatient(
      de.samply.share.model.common.Container patient, SortRules sortRules) {

    TreeTraverser<de.samply.share.model.common.Container> containerTraverser =
        new TreeTraverser<de.samply.share.model.common.Container>() {
      @Override
      public Iterable<de.samply.share.model.common.Container> children(
          de.samply.share.model.common.Container root) {
        List<de.samply.share.model.common.Container> children = new ArrayList<>();
        for (de.samply.share.model.common.Container c : root.getContainer()) {
          // Add a reference to the childs parent, so that we can assign that correctly later
          de.samply.share.model.common.Ref parentRef = new de.samply.share.model.common.Ref();
          parentRef.getEntityId()
              .add(REF_PARENT_PREFIX + ":" + root.getDesignation() + ":" + root.getId());
          c.getRef().add(parentRef);
          children.add(c);
        }

        return children;
      }
    };

    List<de.samply.share.model.common.Container> containers = new ArrayList<>();
    containerTraverser.preOrderTraversal(patient).forEach(containers::add);
    if (sortRules != null) {
      containers = sortRules.sortContainers(containers);
    }

    return containers;

  }

  private void addDiagnosisCell(Row headerRow, Row row, DiagnosisConverter diagnosisConverter,
      de.samply.share.model.common.Container container) {

    String diagnosisCode = diagnosisConverter.getDiagnosisCode(container);

    if (diagnosisCode != null) {

      int diagnosisCodeColumn = getDiagnosisCodeColumn(headerRow);
      row.createCell(diagnosisCodeColumn).setCellValue(diagnosisCode);

    }

  }

  private int getDiagnosisCodeColumn(Row headerRow) {

    int cellIndex = PatientConverterUtil.getCellIndex(HEADER_DIAGNOSIS_CODE, headerRow);

    if (cellIndex < 0) {
      cellIndex = headerRow.getLastCellNum();
      headerRow.createCell(cellIndex).setCellValue(HEADER_DIAGNOSIS_CODE);
    }

    return cellIndex;

  }

  private void createCell(Row headerRow, Row headerDestinationRow, Row headerDktkIdRow, Row thisRow,
      de.samply.share.model.common.Attribute attribute)
      throws MdrConnectionException, MdrInvalidResponseException, ExecutionException,
      ValidatorException {
    createCell(headerRow, headerDestinationRow, headerDktkIdRow, thisRow, attribute,
        validationHandling);
  }

  /**
   * Create a new cell in a row.
   *
   * @param headerRow            the row with the urns
   * @param headerDestinationRow the row with the dataelement designations
   * @param headerDktkIdRow      the row with the dktk ids
   * @param thisRow              the row to create the cell in
   * @param attribute            the attribute (key+value) to put in the cell
   */
  private void createCell(Row headerRow, Row headerDestinationRow, Row headerDktkIdRow, Row thisRow,
      de.samply.share.model.common.Attribute attribute, EnumValidationHandling validationHandling)
      throws MdrConnectionException, MdrInvalidResponseException, ExecutionException,
      ValidatorException {
    MdrIdDatatype attributeKey = new MdrIdDatatype(attribute.getMdrKey());
    // Do not include blacklisted entries
    if (PatientConverterUtil.isBlacklisted(blacklist, attributeKey)) {
      return;
    }

    DataElement dataElement = mdrClient.getDataElement(attribute.getMdrKey(), MDR_LANGUAGE);

    if (isHidden(dataElement)) {
      //return; //TODO: nach dem MDR-Update auskommentieren
    }

    String designation = dataElement.getDesignations().get(0).getDesignation();
    String dktkId = PatientConverterUtil
        .getValueFromSlots(dataElement.getSlots(), DKTK_ID_SLOT_NAME);

    // Check if there's already a cell in the sheet with the given key in the header
    // If not - create it.
    int cellIndex = PatientConverterUtil.getCellIndex(attribute.getMdrKey(), headerRow);
    if (cellIndex < 0) {
      cellIndex = headerRow.getLastCellNum();
      Cell newHeaderCell = headerRow.createCell(cellIndex);
      newHeaderCell.setCellValue(attribute.getMdrKey());
      try {
        addHyperlinkToCell(newHeaderCell);
      } catch (UnsupportedEncodingException e) {
        System.out.println("Error while trying to add hyperlink");
      }

      Cell newDestinationCell = headerDestinationRow.createCell(cellIndex);
      newDestinationCell.setCellValue(designation);

      Cell newDktkIdCell = headerDktkIdRow.createCell(cellIndex);
      newDktkIdCell.setCellValue(dktkId);
    }

    String value = attribute.getValue().getValue();
    Cell thisCell;
    boolean isValid = true;

    if (validationHandling != EnumValidationHandling.NO_VALIDATION) {
      isValid = ((value == null) || value.isEmpty() || validator
          .validate(attribute.getMdrKey(), value));
    }

    if (!isValid) {
      isValid = isValidAccordingToJavaDateFormatSlot(attribute.getMdrKey(), value);
    }

    if (isValid) {
      thisCell = thisRow.createCell(cellIndex);
      thisCell.setCellValue(value);
    } else if (validationHandling == EnumValidationHandling.KEEP_INVALID_ENTRIES) {
      thisCell = thisRow.createCell(cellIndex);
      thisCell.setCellValue(value);
      thisCell.setCellStyle(validationErrorCellStyle);
    }
  }

  private boolean isValidAccordingToJavaDateFormatSlot(String mdrKey, String value)
      throws ExecutionException, MdrConnectionException, MdrInvalidResponseException,
      ValidatorException {
    if (value == null || value.toString().equals("")) {
      return true;
    }
    Validations validations = mdrClient.getDataElementValidations(mdrKey, "de");
    ArrayList<Slot> slots = mdrClient.getDataElementSlots(mdrKey);

    if (validations.getDatatype().equalsIgnoreCase("DATE")) {
      for (Slot slot : slots) {
        if (slot.getSlotName().equalsIgnoreCase("JAVA_DATE_FORMAT")) {
          String targetDateFormat = slot.getSlotValue();
          DateTimeFormatter fmt = DateTimeFormatter.ofPattern(targetDateFormat);
          DateTimeFormatter fmt2 = fmt.withResolverStyle(STRICT);
          try {
            fmt2.parse(value);
            if (!(Integer.parseInt(value) > 1900)) {
              return false;
            }
            return true;
          } catch (DateTimeParseException | NumberFormatException e) {
            //not valid
          }
        }
      }
    }
    return false;
  }

  private boolean isHidden(DataElement dataElement) {

    for (Slot slot : dataElement.getSlots()) {
      if (slot.getSlotName().equalsIgnoreCase(DKTK_SUCHBAR_SLOT) && slot.getSlotValue()
          .equalsIgnoreCase(DKTK_SUCHBAR_SLOT_HIDDEN)) {
        return true;
      }
    }

    return false;

  }

  /**
   * Add a hyperlink to the dataelement in the MDR to a cell.
   *
   * @param cell the cell to add the hyperlink to
   * @return the modified cell
   */
  private Cell addHyperlinkToCell(Cell cell) throws UnsupportedEncodingException {
    Workbook workBook = cell.getRow().getSheet().getWorkbook();
    CellStyle hlinkStyle = workBook.createCellStyle();
    Font hlinkFont = workBook.createFont();
    hlinkFont.setUnderline(Font.U_SINGLE);
    hlinkFont.setColor(IndexedColors.BLUE.getIndex());
    hlinkStyle.setFont(hlinkFont);

    CreationHelper createHelper = workBook.getCreationHelper();
    Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
    link.setAddress(mdrLinkUrl + mdrLinkPath + URLEncoder
        .encode(cell.getStringCellValue(), StandardCharsets.UTF_8.name()));
    cell.setHyperlink(link);
    cell.setCellStyle(hlinkStyle);

    return cell;
  }
}
