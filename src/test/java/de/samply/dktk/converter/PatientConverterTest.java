//package de.samply.dktk.converter;
//import com.google.common.base.Stopwatch;
//import de.dth.mdr.validator.MdrValidator;
//import de.dth.mdr.validator.MdrConnection;
//import de.samply.common.mdrclient.MdrConnectionException;
//import de.samply.common.mdrclient.MdrInvalidResponseException;
//import de.samply.dktk.converter.model.AgeDistribution;
//import de.samply.dktk.converter.model.GenderDistribution;
//import de.samply.share.common.utils.MdrIdDatatype;
//import de.samply.share.model.ccp.*;
//
//import com.google.common.io.Files;
//import de.samply.common.mdrclient.MdrClient;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.xml.bind.*;
//import javax.xml.transform.Source;
//import javax.xml.transform.stream.StreamSource;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
///**
// * Test the PatientConverter.
// *
// * Most important is to check the resulting Excel file...not the JUnit test results
// */
//public class PatientConverterTest {
//
//
//    private static final Logger logger = LoggerFactory.getLogger(PatientConverterTest.class);
//
//    private final static String infilePath = "src/test/resources/";
//    private final static String MDR_BASE_URL = "https://mdr.ccpit.dktk.dkfz.de/v3/api/mdr/";
//    private static final List<String> nameSpaces = new ArrayList<>(Arrays.asList("dktk", "adt"));
//    private final static String destFolder = "src/test/resources/";
//    private final static String destFolderPatients = destFolder + "patients/";
//    private final static String SITE_NAME = "testSite";
//    private final static MdrIdDatatype MDR_KEY_GENDER = new MdrIdDatatype("urn:dktk:dataelement:1");
//    private final static MdrIdDatatype MDR_KEY_AGE = new MdrIdDatatype("urn:dktk:dataelement:28");
//    public static final String INPUT_FILENAME_CENTRAXX_TO_EXCEL = "infile.xml";
//    public static final String OUTPUT_FILENAME_CENTRAXX_TO_EXCEL = "outfile.xlsx";
//    public static final String INPUT_FILENAME_CENTRAXX_TO_CENTRALSEARCH = "infilec2c.xml";
//    private static File tempFolder;
//
//    private PatientConverter patientConverter;
//    private PatientValidator patientValidator;
//    private MdrClient mdrClient;
//    private static MdrConnection mdrConnection;
//    private MdrValidator dthValidator;
//
//    private static Inquiry inquiry;
//    private static Contact contact;
//    private static String executionDateString;
//
//    @Before
//    public void setUp() throws Exception {
//        List<MdrIdDatatype> blacklist = new ArrayList<>();
//        blacklist.add(new MdrIdDatatype("urn:dktk:dataelement:54:1"));
//        blacklist.add(new MdrIdDatatype("urn:dktk:dataelement:89:1"));
//        blacklist.add(new MdrIdDatatype("urn:dktk:dataelement:10:3"));
//        blacklist.add(new MdrIdDatatype("urn:dktk:dataelement:3:*"));
//        blacklist.add(new MdrIdDatatype("urn:dktk:dataelement:100:latest"));
//
//        contact = PatientConverterUtil.createContactObjectForInfoSheet(null, "Max", "Mustermann");
//        inquiry = PatientConverterUtil.createInquiryObjectForInfoSheet("Test Inquiry", "Dies ist nur ein Test");
//        executionDateString = getCurrentDateString();
//        mdrConnection = new MdrConnection(MDR_BASE_URL, null, null, null, null, nameSpaces, true, null);
//        mdrClient = new MdrClient(MDR_BASE_URL);
//        Stopwatch stopwatch = Stopwatch.createStarted();
//        dthValidator = new MdrValidator(mdrConnection, true);
//        stopwatch.stop();
//        logger.info("Initializing DTH Validator took " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " miliseconds");
//        patientConverter = new PatientConverter(mdrClient, dthValidator, EnumValidationHandling.KEEP_INVALID_ENTRIES, blacklist);
//        patientValidator = new PatientValidator(mdrClient);
//        tempFolder = Files.createTempDir();
//        System.out.println("Created temp dir: " + tempFolder.getAbsolutePath());
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        tempFolder.delete();
//    }
//
//    @Test
//    public void testCentraxxQueryResultToExcel() throws JAXBException, IOException, PatientConverterException {
//        QueryResult queryResult = readResultFromDisk(INPUT_FILENAME_CENTRAXX_TO_EXCEL);
//        final Stopwatch stopwatch = Stopwatch.createStarted();
//        String filename = patientConverter.centraxxQueryResultToExcel(queryResult, inquiry, contact, SITE_NAME, executionDateString, tempFolder);
//        logger.debug("Converting the result took " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + " " + TimeUnit.MILLISECONDS.name());
//        File excelFile = new File(filename);
//        File destFile = new File(destFolder + OUTPUT_FILENAME_CENTRAXX_TO_EXCEL);
//        Files.move(excelFile, destFile);
//        logger.debug("Result moved and folder deleted. Done.");
//    }
//
//    @Test
//    public void testCentraxxToCentralSearch() throws JAXBException, IOException, PatientConverterException, ExecutionException, MdrConnectionException, MdrInvalidResponseException {
//        QueryResult queryResult = readResultFromDisk(INPUT_FILENAME_CENTRAXX_TO_CENTRALSEARCH);
//        for (Patient patient : queryResult.getPatient()) {
//            writePatientToDisk(patient, "centraxx");
//            patient = patientConverter.centraxxToCentralsearch(patient);
//            patient = patientValidator.fixOrRemoveWrongAttributes(patient);
//            writePatientToDisk(patient, "mds");
//            patient = PatientConverterUtil.removeAttributeFromCases(patient, "urn:dktk:dataelement:54:1");
//            writePatientToDisk(patient, "final");
//        }
//    }
//
//    @Test
//    public void testCount() throws JAXBException, FileNotFoundException {
//        AgeDistribution ageDistribution = new AgeDistribution();
//        GenderDistribution genderDistribution = new GenderDistribution();
//        QueryResult queryResult = readResultFromDisk(INPUT_FILENAME_CENTRAXX_TO_EXCEL);
//        for (Patient patient : queryResult.getPatient()) {
//            ageDistribution.incrementCountForAge(getAge(patient));
//            genderDistribution.increaseCountForGender(getGender(patient));
//        }
//        logger.debug(ageDistribution.toString());
//        logger.debug(genderDistribution.toString());
//    }
//
//    private static QueryResult readResultFromDisk(String infileName) throws JAXBException, FileNotFoundException {
//        File file = new File(infilePath + infileName);
//        if (!file.exists()) {
//            throw new FileNotFoundException(infilePath + infileName);
//        }
//        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
//        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//
//        Source fileSource = new StreamSource(file);
//        JAXBElement<QueryResult> qrElement = jaxbUnmarshaller.unmarshal(fileSource, QueryResult.class);
//        QueryResult qr = qrElement.getValue();
//        return qr;
//    }
//
//    private static String getCurrentDateString() {
//        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
//        return dateFormat.format(new Date());
//    }
//
//    private static void writePatientToDisk(Patient patient, String suffix) throws JAXBException {
//        final JAXBContext context = JAXBContext.newInstance(Patient.class);
//        File xmlFile = new File(destFolderPatients + "pat_" + patient.getId() + suffix + ".xml");
//        final Marshaller marshaller = context.createMarshaller();
//        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
//        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
//        try {
//            ObjectFactory objectFactory = new ObjectFactory();
//            marshaller.marshal(objectFactory.createPatient(patient), xmlFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private int getAge(Patient patient) {
//        String ageString = PatientConverterUtil.getFirstValueForKey(patient, MDR_KEY_AGE);
//        if (ageString == null) {
//            return -1;
//        } else {
//            try {
//                return Integer.parseInt(ageString);
//            } catch (NumberFormatException e) {
//                return -1;
//            }
//        }
//    }
//
//    private String getGender(Patient patient) {
//        String gender = PatientConverterUtil.getFirstValueForKey(patient, MDR_KEY_GENDER);
//        if (gender == null) {
//            return "";
//        } else {
//            return gender;
//        }
//    }
//
//}
