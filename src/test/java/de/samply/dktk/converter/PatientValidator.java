//package de.samply.dktk.converter;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.ExecutionException;
//
//import javax.xml.bind.JAXBElement;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import de.samply.common.mdrclient.MdrClient;
//import de.samply.common.mdrclient.MdrConnectionException;
//import de.samply.common.mdrclient.MdrInvalidResponseException;
//import de.samply.common.mdrclient.domain.PermissibleValue;
//import de.samply.common.mdrclient.domain.Validations;
//import de.samply.share.model.ccp.Attribute;
//import de.samply.share.model.ccp.Case;
//import de.samply.share.model.ccp.ObjectFactory;
//import de.samply.share.model.ccp.Patient;
//import de.samply.share.model.ccp.Sample;
//
//public class PatientValidator {
//
//       private static final Logger logger = LogManager.getLogger(PatientValidator.class);
//       private static final String languageCode = "de";
//
//       private static final String DATATYPE_ENUMERATED = "enumerated";
//       private static final String DATATYPE_STRING = "STRING";
//       private static final String DATATYPE_BOOLEAN = "BOOLEAN";
//
//       private static final String VALIDATIONTYPE_REGEX = "REGEX";
//
//       private static final String PREFIX_CASE_INSENSITIVE = "(?i)";
//
//       private MdrClient mdrClient;
//       private Path tempDirectory;
//
//       public PatientValidator(MdrClient mdrClient) {
//           this.mdrClient = mdrClient;
//           try {
//               tempDirectory = Files.createTempDirectory("tmpdir");
//           } catch (IOException e1) {
//               logger.debug("Could not create temp dir");
//           }
//       }
//
//       public PatientValidator(MdrClient mdrClient, Path tempDirectory) {
//           logger.debug("Creating PatientValidator with mdrClient " + mdrClient + " and tempDir " + tempDirectory);
//           this.mdrClient = mdrClient;
//           this.tempDirectory = tempDirectory;
//       }
//
//       /**
//        * @return the tempDirectory
//        */
//    public Path getTempDirectory() {
//        return tempDirectory;
//    }
//
//    /**
//     * @param tempDirectory the tempDirectory to set
//     */
//    public void setTempDirectory(Path tempDirectory) {
//        this.tempDirectory = tempDirectory;
//    }
//
//    public Patient fixOrRemoveWrongAttributes(Patient patient) throws MdrConnectionException, MdrInvalidResponseException, ExecutionException {
//        Patient newPatient = new Patient();
//        StringBuffer stringBuffer = new StringBuffer();
//
//        newPatient.setCentraxxId(patient.getCentraxxId());
//        newPatient.setDktkId(patient.getDktkId());
//        newPatient.setId(patient.getId());
//
//        for (Case _case : patient.getCase()) {
//            Case newCase = new Case();
//            newCase.setId(_case.getId());
//
//            for (Attribute attribute : _case.getAttribute()) {
//                Attribute fixedAttribute = fixOrNull(attribute, false);
//                if (fixedAttribute == null) {
//                    stringBuffer.append("\nInvalid in case " + _case.getId() + ": "+ attribute.getMdrKey() + " -> " + attribute.getValue().getValue());
//                } else {
//                    newCase.getAttribute().add(fixedAttribute);
//                }
//            }
//            newPatient.getCase().add(newCase);
//        }
//
//        for (Sample sample : patient.getSample()) {
//            Sample newSample = new Sample();
//            newSample.setId(sample.getId());
//
//            for (Attribute attribute : sample.getAttribute()) {
//                Attribute fixedAttribute = fixOrNull(attribute, false);
//                if (fixedAttribute == null) {
//                    stringBuffer.append("\nInvalid in sample " + sample.getId() + ": "+ attribute.getMdrKey() + " -> " + attribute.getValue().getValue());
//                } else {
//                    newSample.getAttribute().add(fixedAttribute);
//                }
//            }
//            newPatient.getSample().add(newSample);
//        }
//
//        if (stringBuffer.length() > 0) {
//            writeBufferToTempFile(patient, stringBuffer);
//        }
//
//        return newPatient;
//    }
//
//    public boolean isPatientValid(Patient patient) {
//        boolean isValid = false;
//        try {
//            isValid = getMdrKeysForErroneousEntries(patient).isEmpty();
//        } catch (MdrConnectionException | MdrInvalidResponseException | ExecutionException e) {
//            logger.error("Could not validate patient.");
//        }
//        return isValid;
//    }
//
//    public List<String> getMdrKeysForErroneousEntries(Patient patient)
//            throws MdrConnectionException, MdrInvalidResponseException, ExecutionException {
//        List<String> mdrKeys = new ArrayList<String>();
//
//        StringBuffer stringBuffer = new StringBuffer(patient.getId() + ": ");
//
//        for (Case _case : patient.getCase()) {
//            for (Attribute attribute : _case.getAttribute()) {
//                if (!isValid(attribute)) {
//                    mdrKeys.add(attribute.getMdrKey());
//                    stringBuffer.append("\nInvalid in case " + _case.getId() + ": "+ attribute.getMdrKey() + " -> " + attribute.getValue().getValue());
//                }
//            }
//        }
//
//        for (Sample sample : patient.getSample()) {
//            for (Attribute attribute : sample.getAttribute()) {
//                if (!isValid(attribute)) {
//                    mdrKeys.add(attribute.getMdrKey());
//                    stringBuffer.append("\nInvalid in sample " + sample.getId() + ": "+ attribute.getMdrKey() + " -> " + attribute.getValue().getValue());
//                }
//            }
//        }
//
//        if (mdrKeys.size() > 0) {
//            writeBufferToTempFile(patient, mdrKeys, stringBuffer);
//        }
//
//        return mdrKeys;
//    }
//
//    private void writeBufferToTempFile(Patient patient, List<String> mdrKeys, StringBuffer stringBuffer) {
////        try {
////            stringBuffer.append("\nTotal: " + mdrKeys.size() + "\n");
////            File tmpFile = new File(getTempDirectory() + File.separator + "removedkeys_pat_" + patient.getId());
////            BufferedWriter bwr = new BufferedWriter(new FileWriter(tmpFile));
////            bwr.write(stringBuffer.toString());
////            bwr.flush();
////            bwr.close();
////        } catch (IOException e) {
////            logger.error("Error writing to disk", e);
////        }
//    }
//
//    private void writeBufferToTempFile(Patient patient, StringBuffer stringBuffer) {
//        try {
//            File tmpFile = new File(getTempDirectory() + File.separator + "pat_" + patient.getId() + "_invalidAttrs");
//            BufferedWriter bwr = new BufferedWriter(new FileWriter(tmpFile));
//            bwr.write(stringBuffer.toString());
//            bwr.flush();
//            bwr.close();
//        } catch (IOException e) {
//            logger.error("Error writing to disk", e);
//        }
//    }
//
//
//    // TODO: Expand this to other than enumerated value domain
//    private Attribute fixOrNull(Attribute attribute, boolean caseSensitive) throws MdrConnectionException, MdrInvalidResponseException, ExecutionException {
//        Attribute fixedAttribute = null;
//        ObjectFactory objectFactory = new ObjectFactory();
//
//        Validations validations = mdrClient.getDataElementValidations(attribute.getMdrKey(), languageCode);
//
//        if (validations.getDatatype().equalsIgnoreCase(DATATYPE_ENUMERATED)) {
//            try {
//                for (PermissibleValue pv : validations.getPermissibleValues()) {
//                    if (caseSensitive) {
//                        if (pv.getValue().equals(attribute.getValue().getValue())) {
//                            fixedAttribute = attribute;
//                            break;
//                        }
//                    } else {
//                        if (pv.getValue().equalsIgnoreCase((attribute.getValue().getValue()))) {
//                            fixedAttribute = attribute;
//                            JAXBElement<String> newValue = objectFactory.createValue(pv.getValue());
//                            fixedAttribute.setValue(newValue);
//                            break;
//                        }
//                    }
//                }
//            } catch (NullPointerException npe) {
//                logger.warn("Null pointer exception caught when trying to validate: " + attribute.getMdrKey() + " -> " + attribute.getValue().getValue());
//            }
//        } else if (validations.getDatatype().equalsIgnoreCase(DATATYPE_STRING)) {
//            if (validations.getValidationType().equalsIgnoreCase(VALIDATIONTYPE_REGEX)) {
//                String regexPattern;
//                if (caseSensitive) {
//                    regexPattern = validations.getValidationData();
//                } else {
//                    regexPattern = PREFIX_CASE_INSENSITIVE + validations.getValidationData();
//                }
//
//                if (attribute.getValue().getValue().matches(regexPattern)) {
//                    fixedAttribute = attribute;
//                }
//            } else {
//                // TODO: for now return true for other strings. check max size later
//                fixedAttribute = attribute;
//            }
//
//        } else if (validations.getDatatype().equalsIgnoreCase(DATATYPE_BOOLEAN)) {
//            try {
//                String validationData = validations.getValidationData();
//                String[] validationsArray = validationData.substring(1, validationData.length()-1).split("\\|");
//                List<String> allowedValues = Arrays.asList(validationsArray);
//                if (allowedValues.contains(attribute.getValue().getValue())) {
//                    fixedAttribute = attribute;
//                }
//            } catch (NullPointerException npe) {
//                logger.debug("NPE caught while trying to validate boolean...returning value as is: " + attribute.getValue().getValue());
//            }
//        } else {
//            fixedAttribute = attribute;
//            // TODO: check other datatypes
//        }
//        return fixedAttribute;
//    }
//
//    // TODO: Expand this to other than enumerated value domain
//    private boolean isValid(Attribute attribute) throws MdrConnectionException, MdrInvalidResponseException, ExecutionException {
//        boolean isValid = false;
//
//        Validations validations = mdrClient.getDataElementValidations(attribute.getMdrKey(), languageCode);
//
//        if (validations.getDatatype().equalsIgnoreCase(DATATYPE_ENUMERATED)) {
//            try {
//                for (PermissibleValue pv : validations.getPermissibleValues()) {
//                    if (pv.getValue().equals(attribute.getValue().getValue())) {
//                        isValid = true;
//                        break;
//                    }
//                }
//            } catch (NullPointerException npe) {
//                logger.warn("Null pointer exception caught when trying to validate: " + attribute.getMdrKey() + " -> " + attribute.getValue().getValue());
//            }
//        } else if (validations.getDatatype().equalsIgnoreCase(DATATYPE_STRING)) {
//            if (validations.getValidationType().equalsIgnoreCase(VALIDATIONTYPE_REGEX)) {
//                // TODO: should this stay case sensitive?
//                // String regexPattern = PREFIX_CASE_INSENSITIVE + validations.getValidationData();
//                String regexPattern = validations.getValidationData();
//                isValid = attribute.getValue().getValue().matches(regexPattern);
//            } else {
//                // TODO: for now return true for other strings. check max size later
//                return true;
//            }
//
//        } else if (validations.getDatatype().equalsIgnoreCase(DATATYPE_BOOLEAN)) {
//            try {
//                String validationData = validations.getValidationData();
//                String[] validationsArray = validationData.substring(1, validationData.length()-1).split("\\|");
//                List<String> allowedValues = Arrays.asList(validationsArray);
//                isValid = allowedValues.contains(attribute.getValue().getValue());
//            } catch (NullPointerException npe) {
//                logger.debug("NPE caught while trying to validate boolean...returning true for: " + attribute.getValue().getValue());
//                isValid = true;
//            }
//        } else {
//            isValid = true;
//            // TODO: check other datatypes
//        }
//        return isValid;
//    }
//    */
//}
