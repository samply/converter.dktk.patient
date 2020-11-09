package de.samply.dktk.converter;

import de.samply.share.common.utils.MdrIdDatatype;

/**
 * A collection of the constants used.
 */
public class Constants {

  public static final String DESIGNATION_PATIENT = "Patient";
  public static final String DESIGNATION_TUMOUR = "Tumour";
  public static final String DESIGNATION_HISTOLOGY = "Histology";
  public static final String DESIGNATION_TNM = "TNM";
  public static final String DESIGNATION_PROGRESS = "Progress";
  public static final String DESIGNATION_METASTASIS = "Metastasis";
  public static final String DESIGNATION_SAMPLE = "Sample";
  public static final String DESIGNATION_DIAGNOSIS = "Diagnosis";

  public static final String FILE_SUFFIX_XLSX = ".xlsx";

  public static final String MDR_LANGUAGE = "de";
  public static final String DATE_FORMAT_SLOT_NAME = "JAVA_DATE_FORMAT";
  public static final String DKTK_ID_SLOT_NAME = "DKTK_ID";
  public static final String MDS_PREFIX_K = "K-";
  public static final String MDS_PREFIX_B = "B-";
  public static final String MDS_PREFIX_A = "A-";

  public static final int ROW_INDEX_HEADER = 0;
  public static final int ROW_INDEX_DESTINATION = 1;
  public static final int ROW_INDEX_DKTK_ID = 2;
  public static final int COLUMN_INDEX_ID = 0;
  public static final int COLUMN_INDEX_FK = COLUMN_INDEX_ID + 1;
  public static final int COLUMN_INDEX_PID = COLUMN_INDEX_FK + 1;
  public static final int COLUMN_DKTK_LOCAL_ID = COLUMN_INDEX_ID + 1;
  public static final int COLUMN_INDEX_FIRST_PROGRESS_DATE = COLUMN_DKTK_LOCAL_ID + 1; // This will
  // only be used on the patient sheet, so the index is the same as the patient id on other sheets
  public static final int COLUMN_INDEX_FIRST_TUMOUR_DATE = COLUMN_INDEX_FIRST_PROGRESS_DATE + 1;

  public static final String HEADER_VALUE_ID = "ID";
  public static final String HEADER_DIAGNOSIS_CODE = "Diagnosecode";

  public static final String REF_PARENT_PREFIX = "PARENT";

  public static final char REF_SEPARATOR = ':';

  public static final String URN_DKTK_DATAELEMENT_43 = "urn:dktk:dataelement:43";
  public static final String URN_DKTK_DATAELEMENT_50 = "urn:dktk:dataelement:50";
  public static final String URN_ADT_DATAELEMENT_90 = "urn:adt:dataelement:90";
  public static final String URN_ADT_DATAELEMENT_93 = "urn:adt:dataelement:93";
  public static final String URN_ADT_DATAELEMENT_77 = "urn:adt:dataelement:77";
  public static final String URN_ADT_DATAELEMENT_78 = "urn:adt:dataelement:78";
  public static final String COMMENT_URN_DKTK_DATAELEMENT_50 = "Gibt an, ob Probe vorhanden ist";
  public static final String COMMENT_URN_DKTK_DATAELEMENT_43 = "Entspricht dem Datum, an welchem "
      + "dieses Ergebnis befundet wurde";

  public static final String CELL_VALUE_PATIENT_ID = "Patient_ID";
  public static final String CELL_VALUE_SITE_ID = "Standort ID";
  public static final String CELL_VALUE_FIRST_PROGRESS_DATE = "Datum 1. Verlauf";
  public static final String CELL_VALUE_FIRST_TUMOUR_DIAGNOSIS_DATE = "Tumor Diagnosedatum";

  public static final MdrIdDatatype PROGRESS_FIRST_DATE = new MdrIdDatatype(
      "urn:dktk:dataelement:25:*");
  public static final MdrIdDatatype TUMOUR_FIRST_DATE = new MdrIdDatatype(
      "urn:dktk:dataelement:83:*");
  public static final MdrIdDatatype DKTK_GLOBAL_ID = new MdrIdDatatype("urn:dktk:dataelement:54:*");
  public static final MdrIdDatatype DIAGNOSE = new MdrIdDatatype("urn:dktk:dataelement:29:*");

  public static final String MDR_DATATYPE_DATE = "Date";

  public static final String DKTK_SUCHBAR_SLOT = "DKTK_SUCHBAR";
  public static final String DKTK_SUCHBAR_SLOT_HIDDEN = "H";


}
