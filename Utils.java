/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.validation;

import com.accumed.model.Result;
import com.accumed.model.scrubRequest.ScrubRequest;
import com.accumed.model.Context;
import com.accumed.model.ClaimResponse;
import com.accumed.model.scrubRequest.ExtendedValidationType;
import com.accumed.model.scrubRequest.ScrubRequestOutcome;
import com.accumed.webservices.Exclusion;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.drools.FactHandle;
import javax.xml.bind.Marshaller;
import org.drools.runtime.rule.FactHandle;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import com.thoughtworks.xstream.XStream;
import com.accumed.webservices.Statistics;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.ObjectFilter;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.conf.ConsequenceExceptionHandlerOption;
import org.drools.runtime.rule.ConsequenceExceptionHandler;

/**
 *
 * @author smutlak
 */
public class Utils {

    private static Boolean restrictFacilities = null;
    private static String[] allowedLicense = null;

    public static KnowledgeBase createKnowledgeBase(String packageFilePath) {
        FileInputStream fis = null;
        System.out.print("Loading " + packageFilePath + " ...");

        try {
            File pkgFile = new File(packageFilePath);
            fis = new FileInputStream(pkgFile);
            KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            builder.add(ResourceFactory.newInputStreamResource(fis), ResourceType.PKG);
            if (builder.hasErrors()) {
                for (KnowledgeBuilderError error : builder.getErrors()) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE,
                            "{0}\n{1}", new Object[]{error.getMessage(), error.toString()});
                }
                return null;
            }
            //KnowledgeBase base = KnowledgeBaseFactory.newKnowledgeBase();
            KnowledgeBaseConfiguration kbaseConf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
            /*@SuppressWarnings("unchecked")
             Class ehClass = (Class)ValidatorConsequenceExceptionHandler.class;
             ConsequenceExceptionHandlerOption cehOption = ConsequenceExceptionHandlerOption.get( ehClass );
             kbaseConf.setOption( cehOption );
            
             Class<? extends ConsequenceExceptionHandler> handler = ValidatorConsequenceExceptionHandler.class;*/
            // setting the option using the type safe method
            Class<? extends ConsequenceExceptionHandler> handler;

            handler = (Class<? extends ConsequenceExceptionHandler>) ValidatorConsequenceExceptionHandler.class;

            // Class handler = (Class)ValidatorConsequenceExceptionHandler.class;
            kbaseConf.setOption(ConsequenceExceptionHandlerOption.get(handler));

            /*// checking the type safe getOption() method
             assertEquals( ConsequenceExceptionHandlerOption.get(handler),
             kbaseConf.getOption( ConsequenceExceptionHandlerOption.class ) );
             // checking the string based getProperty() method
             assertEquals( handler.getName(),
             kbaseConf.getProperty( ConsequenceExceptionHandlerOption.PROPERTY_NAME ) );*/
            // setting the options using the string based setProperty() method
            kbaseConf.setProperty(ConsequenceExceptionHandlerOption.PROPERTY_NAME,
                    handler.getName());

            // checking the type safe getOption() method
            /*assertEquals( handler.getName(),
             kbaseConf.getOption( ConsequenceExceptionHandlerOption.class ).getHandler().getName() );
             // checking the string based getProperty() method
             assertEquals( handler.getName(),
             kbaseConf.getProperty( ConsequenceExceptionHandlerOption.PROPERTY_NAME ) );*/
            //kbaseConf.setOption(MBeansOption.ENABLED);
            //kbaseConf.setOption( SequentialOption.YES );
            //kbaseConf.setOption( MultithreadEvaluationOption.YES );
            //kbaseConf.setOption( MaxThreadsOption.get(200) );
            //kbaseConf.setOption(IndexPrecedenceOption.PROPERTY_NAME);
            //kbaseConf.setOption(LRUnlinkingOption.ENABLED);
            KnowledgeBase base = KnowledgeBaseFactory.newKnowledgeBase(kbaseConf);
            base.addKnowledgePackages(builder.getKnowledgePackages());
            Logger.getLogger(
                    Utils.class.getName()).log(Level.INFO, "Done");
            // fis.close();
            //  fis = null;

            return base;
        } catch (FileNotFoundException ex) {
            Statistics.addException(ex);
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            System.out.print("Failed");
            return null;
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Statistics.addException(ex);
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                System.out.print("Failed");
                return null;
            }
        }
    }

    protected static java.util.List<FactHandle> insertContext(ScrubRequest req, StatefulKnowledgeSession session) {

//        Utils.deleteFacts(session, com.accumed.model.Context.class);
        @SuppressWarnings("unchecked")
        java.util.List<FactHandle> ret = new java.util.ArrayList();
        if (session != null) {
            if (req.getHeader().getExtendedValidationType() != null
                    && req.getHeader().getExtendedValidationType().size() >= 1) //&& !req.getHeader().getAccumedValidationType().trim().isEmpty())
            {

                for (int cnt = 0; cnt < req.getHeader().getExtendedValidationType().size(); cnt++) {
                    String sValidationType = req.getHeader().getExtendedValidationType().get(cnt).getType();
                    if (sValidationType != null && !sValidationType.trim().isEmpty()) {

                        ret.add(session.insert(new Context(sValidationType)));
                    }
                }
            }
        }
        return ret;
    }

    /*public static XStream buildClaimRequestXStream() {
     XStream xstream = new XStream();
     //XStream xstream = new XStream(new DomDriver());

     //class aliasing
     //xstream.alias("EncounterRequest", EncounterRequest.class);
     xstream.alias("ValidationRequest", ValidationRequest.class);
     xstream.alias("Header", Header.class);

     xstream.alias("Claim", Claim.class);
     xstream.alias("Encounter", Encounter.class);
     xstream.alias("Authorisation", Authorisation.class);
     xstream.alias("Patient", Patient.class);
     xstream.alias("Diagnosis", Diagnosis.class);
     xstream.alias("Activity", Activity.class);
     xstream.alias("Observation", Observation.class);
     xstream.alias("Contract", Contract.class);
     xstream.alias("Resubmission", Resubmission.class);

     //ValidationRequest class aliasing
     xstream.aliasField("Header", ValidationRequest.class, "header");
     xstream.aliasField("Claim", ValidationRequest.class, "claim");
     xstream.addImplicitCollection(ValidationRequest.class, "claim");

     //Header class aliasing
     xstream.aliasField("Validation-type", Header.class, "validationType");
     xstream.aliasField("Accumed-validation-type", Header.class, "accumedValidationType");
     xstream.aliasField("Server", Header.class, "server");
     xstream.aliasField("CallingApplication", Header.class, "callingApplication");
     xstream.aliasField("UserID", Header.class, "userID");
     xstream.aliasField("UserName", Header.class, "userName");
     xstream.aliasField("RequestID", Header.class, "requestID");
     xstream.aliasField("SenderID", Header.class, "senderID");
     xstream.aliasField("ReceiverID", Header.class, "receiverID");
     xstream.aliasField("TransactionDate", Header.class, "transactionDate");
     xstream.aliasField("AccumedBatchStartDate", Header.class, "accumedBatchStartDate");
     xstream.aliasField("AccumedBatchEndDate", Header.class, "accumedBatchEndDate");
     xstream.aliasField("RecordCount", Header.class, "recordCount");
     xstream.aliasField("DispositionFlag", Header.class, "dispositionFlag");

     xstream.registerLocalConverter(Header.class, "transactionDate", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Header.class, "accumedBatchStartDate", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Header.class, "accumedBatchEndDate", new com.accumed.validation.converters.DateTimeConverter());

     xstream.registerLocalConverter(Claim.class, "id", new com.accumed.validation.converters.SubmissionIDConverter());

     //Claim class aliasing
     xstream.aliasField("ID", Claim.class, "id");
     xstream.aliasField("ClaimLineID", Claim.class, "claimLineID");
     xstream.aliasField("ProviderID", Claim.class, "providerID");
     xstream.aliasField("ReceiverID", Claim.class, "receiverId");
     xstream.aliasField("PayerID", Claim.class, "payerID");
     xstream.aliasField("PatientShare", Claim.class, "patientShare");
     xstream.aliasField("Net", Claim.class, "net");
     xstream.aliasField("ProviderInvoiceAmount", Claim.class, "providerInvoiceAmount");
     xstream.aliasField("IDPayer", Claim.class, "idPayer");
     xstream.aliasField("Gross", Claim.class, "gross");
     xstream.aliasField("AccumedID", Claim.class, "accumedID");
     xstream.aliasField("MemberID", Claim.class, "memberID");
     xstream.aliasField("AccumedPatientID", Claim.class, "accumedPatientID");
     xstream.aliasField("EmiratesIDNumber", Claim.class, "emiratesIDNumber");
     xstream.aliasField("PaymentReference", Claim.class, "paymentReference");
     xstream.aliasField("DenialCode", Claim.class, "denialCode");
     xstream.aliasField("DateSettlement", Claim.class, "dateSettlement");

     xstream.registerLocalConverter(Claim.class, "dateSettlement", new com.accumed.validation.converters.DateTimeConverter());

     xstream.aliasField("Diagnosises", Claim.class, "diagnosises");
     xstream.aliasField("Activities", Claim.class, "activities");
     xstream.aliasField("Patient", Claim.class, "patient");
     xstream.aliasField("Encounter", Claim.class, "encounter");
     xstream.aliasField("Resubmission", Claim.class, "resubmission");
     xstream.aliasField("Contract", Claim.class, "contract");

     xstream.addImplicitCollection(Claim.class, "encounter");
     xstream.addImplicitCollection(Claim.class, "diagnosis");
     xstream.addImplicitCollection(Claim.class, "activity");

     //Encounter class aliasing
     xstream.aliasField("AccumedID", Encounter.class, "accumedID");
     xstream.aliasField("FacilityID", Encounter.class, "facilityID");
     xstream.aliasField("Type", Encounter.class, "type");
     xstream.aliasField("PatientID", Encounter.class, "patientID");
     xstream.aliasField("Start", Encounter.class, "start");
     xstream.aliasField("End", Encounter.class, "end");
     xstream.aliasField("StartType", Encounter.class, "startType");
     xstream.aliasField("EndType", Encounter.class, "endType");
     xstream.aliasField("TransferSource", Encounter.class, "transferSource");
     xstream.aliasField("TransferDestination", Encounter.class, "transferDestination");
     xstream.aliasField("Authorisation", Encounter.class, "authorisation");

     xstream.registerLocalConverter(Encounter.class, "start", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Encounter.class, "end", new com.accumed.validation.converters.DateTimeConverter());

     //Patient class aliasing
     /*xstream.aliasField("Name", Patient.class, "patientName");
     xstream.aliasField("Surname", Patient.class, "patientSurname");
     xstream.aliasField("PackageName", Patient.class, "packageName");
     xstream.aliasField("MemberID", Patient.class, "memberID");
     xstream.aliasField("EmiratesIDNumber", Patient.class, "emiratesId");
     xstream.aliasField("DateOfBirth", Patient.class, "dateOfBirth");
     xstream.aliasField("Gender", Patient.class, "genderId");
     xstream.aliasField("PassportNumber", Patient.class, "passportId");
     xstream.aliasField("Nationality", Patient.class, "nationality");
     xstream.aliasField("City", Patient.class, "city");
     xstream.aliasField("ContactNumber", Patient.class, "contactNumber");**
     //xstream.omitField(Patient.class, "MemberID");
     //xstream.aliasField("MemberID", Patient.class, "receiverId");
     //Authorisation class aliasing
     xstream.aliasField("Start", Authorisation.class, "start");
     xstream.aliasField("End", Authorisation.class, "end");
     xstream.registerLocalConverter(Authorisation.class, "start", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Authorisation.class, "end", new com.accumed.validation.converters.DateTimeConverter());
     xstream.aliasField("Verbal", Authorisation.class, "verbal");

     //Diagnosis class aliasing
     xstream.aliasField("Type", Diagnosis.class, "type");
     xstream.aliasField("Code", Diagnosis.class, "code");

     //Activity class aliasing
     xstream.aliasField("AccumedID", Activity.class, "accumedID");
     xstream.aliasField("ID", Activity.class, "id");
     xstream.aliasField("Start", Activity.class, "start");
     xstream.aliasField("Type", Activity.class, "type");
     xstream.aliasField("Code", Activity.class, "code");
     xstream.aliasField("Quantity", Activity.class, "quantity");
     xstream.aliasField("Net", Activity.class, "net");
     xstream.aliasField("ProviderNet", Activity.class, "providerNet");
     xstream.aliasField("Clinician", Activity.class, "clinician");
     xstream.aliasField("PriorAuthorizationID", Activity.class, "priorAuthorizationID");
     xstream.aliasField("List", Activity.class, "list");
     xstream.aliasField("Gross", Activity.class, "gross");
     xstream.aliasField("PatientShare", Activity.class, "patientShare");
     xstream.aliasField("PaymentAmount", Activity.class, "paymentAmount");
     xstream.aliasField("DenialCode", Activity.class, "denialCode");
     xstream.aliasField("Observation", Activity.class, "observation");
     xstream.addImplicitCollection(Activity.class, "observation");

     xstream.registerLocalConverter(Activity.class, "start", new com.accumed.validation.converters.DateTimeConverter());

     //Observation class aliasing
     xstream.aliasField("Type", Observation.class, "type");
     xstream.aliasField("Code", Observation.class, "code");
     xstream.aliasField("Value", Observation.class, "value");
     xstream.aliasField("ValueType", Observation.class, "valueType");

     //Contract class aliasing
     xstream.aliasField("PackageName", Contract.class, "packageName");
     xstream.aliasField("Policy", Contract.class, "policy");

     //Resubmission class aliasing
     xstream.aliasField("Type", Resubmission.class, "type");
     xstream.aliasField("Comment", Resubmission.class, "comment");
     xstream.aliasField("Attachment", Resubmission.class, "attachment");

     return xstream;
     }*/
    public static XStream buildClaimReponseXStream() {
        XStream xstream = new XStream();
        xstream.alias("Claim", ClaimResponse.class);
        xstream.aliasField("ClaimID", ClaimResponse.class, "claimID");
        xstream.aliasField("ClaimLineID", ClaimResponse.class, "claimLineID");
        xstream.aliasField("ClaimResults", ClaimResponse.class, "results");
        //xstream.addImplicitCollection(ClaimResponse.class, "results");
        xstream.alias("Result", Result.class);
        /*        xstream.aliasField("ClaimID", Result.class, "claimID");
         xstream.aliasField("ClaimActualID", Result.class, "claimActualID");
         */
        xstream.aliasField("RunDateTime", Result.class, "runDateTime");
        xstream.aliasField("RuleName", Result.class, "ruleName");
        xstream.aliasField("RuleID", Result.class, "ruleID");
        xstream.aliasField("Severity", Result.class, "severity");
        xstream.aliasField("LongMsgDescription", Result.class, "longMsgDescription");
        xstream.aliasField("ShortMsgDescription", Result.class, "ShortMsgDescription");
        return xstream;
    }

    /*public static XStream buildValidationResponseXStream() {
     XStream xstream = new XStream();
     xstream.alias("ValidationResponse", ValidationResponse.class);
     xstream.omitField(ValidationResponse.class, "fullValidation");
     xstream.omitField(ValidationResponse.class, "maxBlocker");
     xstream.omitField(ValidationResponse.class, "maxCritical");
     xstream.omitField(ValidationResponse.class, "maxSevere");
     xstream.omitField(ValidationResponse.class, "maxWarning");
     xstream.aliasField("Header", ValidationResponse.class, "header");
     xstream.aliasField("ValidationResults", ValidationResponse.class, "results");
     xstream.aliasField("Claims", ValidationResponse.class, "claims");
     xstream.aliasField("Workflow-Next-Node", ValidationResponse.class, "workflowNextNode");
     xstream.omitField(ValidationResponse.class, "workflowFiredRule");
     //xstream.addImplicitCollection(ValidationResponse.class, "results");
     //xstream.addImplicitCollection(ValidationResponse.class, "claims");
     xstream.alias("Header", ResponseHeader.class);
     xstream.aliasField("Timestamp", ResponseHeader.class, "timestamp");

     xstream.alias("Claim", ClaimResponse.class);
     xstream.omitField(ClaimResponse.class, "countBlocker");
     xstream.omitField(ClaimResponse.class, "countCritical");
     xstream.omitField(ClaimResponse.class, "countSevere");
     xstream.omitField(ClaimResponse.class, "countWarning");

     xstream.aliasField("ClaimID", ClaimResponse.class, "claimID");
     xstream.aliasField("ClaimLineID", ClaimResponse.class, "claimLineID");
     xstream.aliasField("ClaimResults", ClaimResponse.class, "results");
     //xstream.addImplicitCollection(ClaimResponse.class, "results");
     xstream.alias("Result", Result.class);
     xstream.aliasField("RunDateTime", Result.class, "runDateTime");
     xstream.aliasField("RuleName", Result.class, "ruleName");
     xstream.aliasField("RuleID", Result.class, "ruleID");
     xstream.aliasField("Severity", Result.class, "severity");
     xstream.aliasField("LongMsgDescription", Result.class, "longMsgDescription");
     xstream.aliasField("ShortMsgDescription", Result.class, "ShortMsgDescription");

     return xstream;
     }*/

 /*public static XStream buildSubmissionXStream() {
     XStream xstream = new XStream();

     xstream.alias("Claim.Submission", Submission.class);
     xstream.alias("Header", Header.class);
     xstream.alias("Claim", Claim.class);
     xstream.alias("Encounter", Encounter.class);
     xstream.alias("Diagnosis", Diagnosis.class);
     xstream.alias("Activity", Activity.class);
     xstream.alias("Observation", Observation.class);
     xstream.alias("Contract", Contract.class);
     xstream.alias("Resubmission", Resubmission.class);

     xstream.alias("Patient", Patient.class);

     //Submission class aliasing
     xstream.aliasField("Header", Submission.class, "header");
     xstream.addImplicitCollection(Submission.class, "claim");
     //xstream.aliasField("Claim", Submission.class, "claim");

     //Header class aliasing
     xstream.aliasField("SenderID", Header.class, "senderID");
     xstream.aliasField("ReceiverID", Header.class, "receiverID");
     xstream.aliasField("TransactionDate", Header.class, "transactionDate");
     xstream.aliasField("RecordCount", Header.class, "recordCount");
     xstream.aliasField("DispositionFlag", Header.class, "dispositionFlag");
     xstream.aliasField("AccumedBatchStartDate", Header.class, "accumedBatchStartDate");
     xstream.aliasField("AccumedBatchEndDate", Header.class, "accumedBatchEndDate");

     xstream.registerLocalConverter(Header.class, "transactionDate", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Header.class, "transactionDate", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Header.class, "accumedBatchStartDate", new com.accumed.validation.converters.DateTimeConverter());

     xstream.registerLocalConverter(Claim.class, "id", new com.accumed.validation.converters.SubmissionIDConverter());
     //Claim class aliasing
     xstream.aliasField("ID", Claim.class, "id");
     xstream.aliasField("ProviderID", Claim.class, "providerID");
     xstream.aliasField("ReceiverID", Claim.class, "receiverID");
     xstream.aliasField("PayerID", Claim.class, "payerID");
     xstream.aliasField("PatientShare", Claim.class, "patientShare");
     xstream.aliasField("Net", Claim.class, "net");
     xstream.aliasField("ProviderInvoiceAmount", Claim.class, "providerInvoiceAmount");
     xstream.aliasField("IDPayer", Claim.class, "idPayer");
     xstream.aliasField("Gross", Claim.class, "gross");
     xstream.aliasField("MemberID", Claim.class, "memberID");
     xstream.aliasField("EmiratesIDNumber", Claim.class, "emiratesIDNumber");
     xstream.aliasField("Contract", Claim.class, "contract");
     xstream.aliasField("Resubmission", Claim.class, "resubmission");
     xstream.aliasField("AccumedPatientID", Claim.class, "accumedPatientID");

     // Remittance.Advice
     xstream.aliasField("DenialCode", Claim.class, "denialCode");
     xstream.aliasField("PaymentReference", Claim.class, "paymentReference");
     xstream.aliasField("DateSettlement", Claim.class, "dateSettlement");

     xstream.registerLocalConverter(Claim.class, "dateSettlement", new com.accumed.validation.converters.DateTimeConverter());
     //        /xstream.addImplicitCollection(Claim.class, "encounter");
     //        xstream.addImplicitCollection(Claim.class, "diagnosis");
     //        xstream.addImplicitCollection(Claim.class, "activity");

     xstream.addImplicitCollection(Claim.class, "encounter", Encounter.class);
     xstream.addImplicitCollection(Claim.class, "diagnosis", Diagnosis.class);
     xstream.addImplicitCollection(Claim.class, "activity", Activity.class);


     /* xstream.aliasField("Diagnosises", Claim.class, "diagnosises");
     xstream.aliasField("Activities", Claim.class, "activities");
     xstream.aliasField("Patient", Claim.class, "patient");**
     //xstream.aliasField("Encounter", Claim.class, "encounter");
     //Encounter class aliasing
     xstream.aliasField("FacilityID", Encounter.class, "facilityID");
     xstream.aliasField("Type", Encounter.class, "type");
     xstream.aliasField("PatientID", Encounter.class, "patientID");
     xstream.aliasField("Start", Encounter.class, "start");
     xstream.aliasField("End", Encounter.class, "end");
     xstream.aliasField("StartType", Encounter.class, "startType");
     xstream.aliasField("EndType", Encounter.class, "endType");
     xstream.aliasField("TransferSource", Encounter.class, "transferSource");
     xstream.aliasField("TransferDestination", Encounter.class, "transferDestination");
     xstream.aliasField("AccumedID", Encounter.class, "accumedID");

     xstream.registerLocalConverter(Encounter.class, "start", new com.accumed.validation.converters.DateTimeConverter());
     xstream.registerLocalConverter(Encounter.class, "end", new com.accumed.validation.converters.DateTimeConverter());

     //Diagnosis class aliasing
     xstream.aliasField("Type", Diagnosis.class, "type");
     xstream.aliasField("Code", Diagnosis.class, "code");

     //Activity class aliasing
     xstream.aliasField("ID", Activity.class, "id");
     xstream.aliasField("Start", Activity.class, "start");
     xstream.aliasField("Type", Activity.class, "type");
     xstream.aliasField("Code", Activity.class, "code");
     xstream.aliasField("Quantity", Activity.class, "quantity");
     xstream.aliasField("Net", Activity.class, "net");
     xstream.aliasField("ProviderNet", Activity.class, "providerNet");
     xstream.aliasField("Clinician", Activity.class, "clinician");
     xstream.aliasField("PriorAuthorizationID", Activity.class, "priorAuthorizationID");
     xstream.addImplicitCollection(Activity.class, "observation");
     xstream.aliasField("AccumedID", Activity.class, "accumedID");
     xstream.aliasField("List", Activity.class, "list");
     xstream.aliasField("Gross", Activity.class, "gross");
     xstream.aliasField("PatientShare", Activity.class, "patientShare");
     xstream.aliasField("PaymentAmount", Activity.class, "paymentAmount");

     xstream.registerLocalConverter(Activity.class, "start", new com.accumed.validation.converters.DateTimeConverter());

     //Observation class aliasing
     xstream.aliasField("Type", Observation.class, "type");
     xstream.aliasField("Code", Observation.class, "code");
     xstream.aliasField("ValueType", Observation.class, "valueType");
     xstream.aliasField("Value", Observation.class, "value");

     //Contract class aliasing
     xstream.aliasField("PackageName", Contract.class, "packageName");
     xstream.aliasField("Policy", Contract.class, "policy");

     //Resubmission class aliasing
     xstream.aliasField("Type", Resubmission.class, "type");
     xstream.aliasField("Comment", Resubmission.class, "comment");
     xstream.aliasField("Attachment", Resubmission.class, "attachment");

     /*        xstream.omitField(Activity.class, "diagnosis");
     xstream.omitField(Activity.class, "ID");
     xstream.aliasField("PatientShare", Activity.class, "patientShare");
     xstream.aliasField("PaymentAmount", Activity.class, "paymentAmount");
     xstream.aliasField("Net", Activity.class, "netAmount");
     xstream.aliasField("ProviderNet", Activity.class, "providerNet");
     xstream.aliasField("List", Activity.class, "activityList");
     xstream.aliasField("Gross", Activity.class, "grossAmount");
     xstream.aliasField("DenialCode", Activity.class, "denialCode");
     xstream.aliasField("Observations", Activity.class, "observations");
     */
//        xstream.aliasField("ClaimID", Encounter.class, "claimId");
    //xstream.omitField(Encounter.class, "FacilityID");
//        xstream.aliasField("ID", Encounter.class, "haadEncounterId");
    /*
     //Patient class aliasing
     xstream.aliasField("Name", Patient.class, "patientName");
     xstream.aliasField("Surname", Patient.class, "patientSurname");
     xstream.aliasField("PackageName", Patient.class, "packageName");
     xstream.aliasField("MemberID", Patient.class, "memberID");
     xstream.aliasField("EmiratesIDNumber", Patient.class, "emiratesId");
     xstream.aliasField("DateOfBirth", Patient.class, "dateOfBirth");
     xstream.aliasField("Gender", Patient.class, "genderId");
     xstream.aliasField("PassportNumber", Patient.class, "passportId");
     xstream.aliasField("Nationality", Patient.class, "nationality");
     xstream.aliasField("City", Patient.class, "city");
     xstream.aliasField("ContactNumber", Patient.class, "contactNumber");
     //xstream.omitField(Patient.class, "MemberID");
     //xstream.aliasField("MemberID", Patient.class, "receiverId");
        
        
     **
     return xstream;
     }*/
    public static String getRequest(ScrubRequest req) {
        try {
            JAXBContext contextA = JAXBContext.newInstance(ScrubRequest.class);
            StringWriter writer = new StringWriter();
            Marshaller marshaller = contextA.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(req, writer);
            marshaller = null;

            return writer.toString();
        } catch (JAXBException e) {
            Statistics.addException(e);
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, e);
            return "Error Marshalling Request.";
        }

    }

    public static ScrubRequest removeExclusions(ScrubRequest request) {
        if (request != null) {

            if (request.getOutcome() != null) {
                Iterator<ScrubRequestOutcome> requestOutcomeIter
                        = request.getOutcome().iterator();
                while (requestOutcomeIter.hasNext()) {
                    ScrubRequestOutcome requestOutcome = requestOutcomeIter.next();
                    if (requestOutcome.getExcluded() != null && requestOutcome.getExcluded()) {
                        requestOutcomeIter.remove();
                    }
                }
            }

            if (request.getHeader() != null) {
                request.getHeader().setRequestID(request);
                if (request.getHeader().getOutcome() != null) {
                    Iterator<com.accumed.model.scrubRequest.HeaderOutcome> headerOutcomeIter
                            = request.getHeader().getOutcome().iterator();
                    while (headerOutcomeIter.hasNext()) {
                        com.accumed.model.scrubRequest.HeaderOutcome headerOutcome = headerOutcomeIter.next();
                        if (headerOutcome.getExcluded() != null && headerOutcome.getExcluded()) {
                            headerOutcomeIter.remove();
                        }
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {

                        Iterator<com.accumed.model.scrubRequest.WorkflowOutcome> workflowOutcomeIter
                                = request.getHeader().getWorkflow().getOutcome().iterator();
                        while (workflowOutcomeIter.hasNext()) {
                            com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome = workflowOutcomeIter.next();
                            if (workflowOutcome.getExcluded() != null && workflowOutcome.getExcluded()) {
                                workflowOutcomeIter.remove();
                            }
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            Iterator<com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome> extendedValidationTypeOutcomeIter
                                    = extendedValidationType.getOutcome().iterator();
                            while (extendedValidationTypeOutcomeIter.hasNext()) {
                                com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome = extendedValidationTypeOutcomeIter.next();
                                if (extendedValidationTypeOutcome.getExcluded() != null && extendedValidationTypeOutcome.getExcluded()) {
                                    extendedValidationTypeOutcomeIter.remove();
                                }
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    Iterator<com.accumed.model.scrubRequest.ClaimOutcome> claimOutcomeIter
                            = claim.getOutcome().iterator();
                    while (claimOutcomeIter.hasNext()) {
                        com.accumed.model.scrubRequest.ClaimOutcome claimOutcome = claimOutcomeIter.next();
                        if (claimOutcome.getExcluded() != null && claimOutcome.getExcluded()) {
                            claimOutcomeIter.remove();
                        }
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            Iterator<com.accumed.model.scrubRequest.EncounterOutcome> encounterOutcomeIter
                                    = encounter.getOutcome().iterator();
                            while (encounterOutcomeIter.hasNext()) {
                                com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome = encounterOutcomeIter.next();
                                if (encounterOutcome.getExcluded() != null && encounterOutcome.getExcluded()) {
                                    encounterOutcomeIter.remove();
                                }
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                Iterator<com.accumed.model.scrubRequest.AuthorisationOutcome> authorisationOutcomeIter
                                        = encounter.getAuthorisation().getOutcome().iterator();
                                while (authorisationOutcomeIter.hasNext()) {
                                    com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome = authorisationOutcomeIter.next();
                                    if (authorisationOutcome.getExcluded() != null && authorisationOutcome.getExcluded()) {
                                        authorisationOutcomeIter.remove();
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setExcluded(evaluteExclusion(request, currentExclusions.get(headerOutcome.getRuleName())));
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            Iterator<com.accumed.model.scrubRequest.DiagnosisOutcome> diagnosisOutcomeIter
                                    = diagnosis.getOutcome().iterator();
                            while (diagnosisOutcomeIter.hasNext()) {
                                com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome = diagnosisOutcomeIter.next();
                                if (diagnosisOutcome.getExcluded() != null && diagnosisOutcome.getExcluded()) {
                                    diagnosisOutcomeIter.remove();
                                }
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            Iterator<com.accumed.model.scrubRequest.ActivityOutcome> activityOutcomeIter
                                    = activity.getOutcome().iterator();
                            while (activityOutcomeIter.hasNext()) {
                                com.accumed.model.scrubRequest.ActivityOutcome activityOutcome = activityOutcomeIter.next();
                                if (activityOutcome.getExcluded() != null && activityOutcome.getExcluded()) {
                                    activityOutcomeIter.remove();
                                }
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    Iterator<com.accumed.model.scrubRequest.ObservationOutcome> observationOutcomeIter
                                            = observation.getOutcome().iterator();
                                    while (observationOutcomeIter.hasNext()) {
                                        com.accumed.model.scrubRequest.ObservationOutcome observationOutcome = observationOutcomeIter.next();
                                        if (observationOutcome.getExcluded() != null && observationOutcome.getExcluded()) {
                                            observationOutcomeIter.remove();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        Iterator<com.accumed.model.scrubRequest.ResubmissionOutcome> resubmissionOutcomeIter
                                = claim.getResubmission().getOutcome().iterator();
                        while (resubmissionOutcomeIter.hasNext()) {
                            com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome = resubmissionOutcomeIter.next();
                            if (resubmissionOutcome.getExcluded() != null && resubmissionOutcome.getExcluded()) {
                                resubmissionOutcomeIter.remove();
                            }
                        }
                    }
                }

                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        Iterator<com.accumed.model.scrubRequest.ContractOutcome> contractOutcomeIter
                                = claim.getContract().getOutcome().iterator();
                        while (contractOutcomeIter.hasNext()) {
                            com.accumed.model.scrubRequest.ContractOutcome contractOutcome = contractOutcomeIter.next();
                            if (contractOutcome.getExcluded() != null && contractOutcome.getExcluded()) {
                                contractOutcomeIter.remove();
                            }
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        Iterator<com.accumed.model.scrubRequest.PatientOutcome> patientOutcomeIter
                                = claim.getPatient().getOutcome().iterator();
                        while (patientOutcomeIter.hasNext()) {
                            com.accumed.model.scrubRequest.PatientOutcome patientOutcome = patientOutcomeIter.next();
                            if (patientOutcome.getExcluded() != null && patientOutcome.getExcluded()) {
                                patientOutcomeIter.remove();
                            }
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            Iterator<com.accumed.model.scrubRequest.PatientInsuranceOutcome> patientInsuranceOutcomeIter
                                    = claim.getPatient().getPatientInsurance().getOutcome().iterator();
                            while (patientInsuranceOutcomeIter.hasNext()) {
                                com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome = patientInsuranceOutcomeIter.next();
                                if (patientInsuranceOutcome.getExcluded() != null && patientInsuranceOutcome.getExcluded()) {
                                    patientInsuranceOutcomeIter.remove();
                                }
                            }
                        }

                    }
                }
            }
        }
        return request;
    }

    private static Boolean evaluteExclusion(ScrubRequest request, String ruleID,
            Collection<Exclusion> exclusions) {
        if (exclusions != null) {
            for (Exclusion exclusion : exclusions) {
                if (exclusion != null && !exclusion.getDeleted()
                        && exclusion.getRuleID().equalsIgnoreCase(ruleID)
                        && (exclusion.getProviderLicense() == null || exclusion.getProviderLicense().equals(request.getClaim().getProviderID()))
                        && (exclusion.getReceiverLicense() == null || exclusion.getReceiverLicense().equals(request.getHeader().getReceiverID()))
                        && (exclusion.getPayerLicense() == null || exclusion.getPayerLicense().equals(request.getClaim().getPayerID()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Boolean evaluteExclusion(ScrubRequest request,
            Exclusion excl) {
        return (excl != null
                && (excl.getProviderLicense() == null || excl.getProviderLicense().equals(request.getClaim().getProviderID()))
                && (excl.getReceiverLicense() == null || excl.getReceiverLicense().equals(request.getHeader().getReceiverID()))
                && (excl.getPayerLicense() == null || excl.getPayerLicense().equals(request.getClaim().getPayerID())));
    }

    public static ScrubRequest setExclusions(ScrubRequest request,
            ConcurrentHashMap<Integer, Exclusion> exclusions) {

//        //convert to new HashMap
//        ConcurrentHashMap<String, Exclusion> currentExclusions = new ConcurrentHashMap();
//        Iterator<java.util.Map.Entry<Integer, Exclusion>> it = exclusions.entrySet().iterator();
//        while (it.hasNext()) {
//            Exclusion exclusion = it.next().getValue();
//            if (!exclusion.getDeleted()) {
//                currentExclusions.put(exclusion.getRuleID(), exclusion);
//            }
//        }
        if (request != null) {

            if (request.getOutcome() != null) {
                for (ScrubRequestOutcome requestOutcome : request.getOutcome()) {
                    requestOutcome.setExcluded(evaluteExclusion(request, requestOutcome.getRuleName(), exclusions.values()));
                }
            }

            if (request.getHeader() != null) {

                request.getHeader().setRequestID(request);

                if (request.getHeader().getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.HeaderOutcome headerOutcome : request.getHeader().getOutcome()) {
                        headerOutcome.setExcluded(evaluteExclusion(request, headerOutcome.getRuleName(), exclusions.values()));
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome : request.getHeader().getWorkflow().getOutcome()) {
                            workflowOutcome.setExcluded(evaluteExclusion(request, workflowOutcome.getRuleName(), exclusions.values()));
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome : extendedValidationType.getOutcome()) {
                                extendedValidationTypeOutcome.setExcluded(evaluteExclusion(request, extendedValidationTypeOutcome.getRuleName(), exclusions.values()));
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.ClaimOutcome claimOutcome : claim.getOutcome()) {
                        claimOutcome.setExcluded(evaluteExclusion(request, claimOutcome.getRuleName(), exclusions.values()));
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome : encounter.getOutcome()) {
                                encounterOutcome.setExcluded(evaluteExclusion(request, encounterOutcome.getRuleName(), exclusions.values()));
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                for (com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome : encounter.getAuthorisation().getOutcome()) {
                                    authorisationOutcome.setExcluded(evaluteExclusion(request, authorisationOutcome.getRuleName(), exclusions.values()));
                                }
                            }
                        }
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setExcluded(currentExclusions.get(dxInfoOutcome.getRuleName()) != null);
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome : diagnosis.getOutcome()) {
                                diagnosisOutcome.setExcluded(evaluteExclusion(request, diagnosisOutcome.getRuleName(), exclusions.values()));
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ActivityOutcome activityOutcome : activity.getOutcome()) {
                                activityOutcome.setExcluded(evaluteExclusion(request, activityOutcome.getRuleName(), exclusions.values()));
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome observationOutcome : observation.getOutcome()) {
                                        observationOutcome.setExcluded(evaluteExclusion(request, observationOutcome.getRuleName(), exclusions.values()));
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome : claim.getResubmission().getOutcome()) {
                            resubmissionOutcome.setExcluded(evaluteExclusion(request, resubmissionOutcome.getRuleName(), exclusions.values()));
                        }
                    }
                }
                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ContractOutcome contractOutcome : claim.getContract().getOutcome()) {
                            contractOutcome.setExcluded(evaluteExclusion(request, contractOutcome.getRuleName(), exclusions.values()));
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.PatientOutcome patientOutcome : claim.getPatient().getOutcome()) {
                            patientOutcome.setExcluded(evaluteExclusion(request, patientOutcome.getRuleName(), exclusions.values()));
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome : claim.getPatient().getPatientInsurance().getOutcome()) {
                                patientInsuranceOutcome.setExcluded(evaluteExclusion(request, patientInsuranceOutcome.getRuleName(), exclusions.values()));
                            }
                        }
                    }
                }
                //}
            }
        }
        return request;
    }

    public static ScrubRequest setParents(ScrubRequest request) {
        if (request != null) {

            if (request.getOutcome() != null) {
                for (ScrubRequestOutcome requestOutcome : request.getOutcome()) {
                    requestOutcome.setRequest(request);
                }
            }

            if (request.getHeader() != null) {

                request.getHeader().setRequestID(request);

                if (request.getHeader().getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.HeaderOutcome headerOutcome : request.getHeader().getOutcome()) {
                        headerOutcome.setHeader(request.getHeader());
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome : request.getHeader().getWorkflow().getOutcome()) {
                            workflowOutcome.setWorkflow(request.getHeader().getWorkflow());
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome : extendedValidationType.getOutcome()) {
                                extendedValidationTypeOutcome.setExtendedValidationType(extendedValidationType);
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.ClaimOutcome claimOutcome : claim.getOutcome()) {
                        claimOutcome.setClaim(claim);
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome : encounter.getOutcome()) {
                                encounterOutcome.setEncounter(encounter);
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                for (com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome : encounter.getAuthorisation().getOutcome()) {
                                    authorisationOutcome.setAuthorisation(encounter.getAuthorisation());
                                }
                            }
                        }
                    }
                }

                if (claim.getClaimType() != null) {
                    for (com.accumed.model.scrubRequest.ClaimType claimType : claim.getClaimType()) {
                        claimType.setClaimID(claim);
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setDxInfo(dxInfo);
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome : diagnosis.getOutcome()) {
                                diagnosisOutcome.setDiagnosis(diagnosis);
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ActivityOutcome activityOutcome : activity.getOutcome()) {
                                activityOutcome.setActivity(activity);
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome observationOutcome : observation.getOutcome()) {
                                        observationOutcome.setObservation(observation);
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome : claim.getResubmission().getOutcome()) {
                            resubmissionOutcome.setResubmission(claim.getResubmission());
                        }
                    }
                }
                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ContractOutcome contractOutcome : claim.getContract().getOutcome()) {
                            contractOutcome.setContract(claim.getContract());
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.PatientOutcome patientOutcome : claim.getPatient().getOutcome()) {
                            patientOutcome.setPatient(claim.getPatient());
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getCoPayment() != null) {
                            for (com.accumed.model.scrubRequest.CoPayment coPayment : claim.getPatient().getPatientInsurance().getCoPayment()) {
                                coPayment.setPatientInsurance(claim.getPatient().getPatientInsurance());
                            }
                        }
                        if (claim.getPatient().getPatientInsurance().getDeductible() != null) {
                            for (com.accumed.model.scrubRequest.Deductible deductible : claim.getPatient().getPatientInsurance().getDeductible()) {
                                deductible.setPatientInsurance(claim.getPatient().getPatientInsurance());
                            }
                        }

                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome : claim.getPatient().getPatientInsurance().getOutcome()) {
                                patientInsuranceOutcome.setPatientInsurance(claim.getPatient().getPatientInsurance());
                            }
                        }

                    }
                }
                //}
            }
        }
        return request;
    }

    public static boolean isSameNode(String node1, String node2) {
        return node1.equalsIgnoreCase(node2);
    }

    public static boolean isNextDirectNode(String node1, String node2) {
        return getNextNode(node1).equalsIgnoreCase(node2);
    }

    public static String getNextNode(String node1) {
        if (node1.equalsIgnoreCase("DataEntry")) {
            return "Coding";

        } else if (node1.equalsIgnoreCase("Coding")) {
            return "Auditing";

        } else if (node1.equalsIgnoreCase("Auditing")) {
            return "Billing";
        }
        return "";
    }

    public static int resolveGroupType(String sType) {
        if (sType.compareToIgnoreCase("CPT") == 0) {
            return 3;
        } else if (sType.compareToIgnoreCase("DRUG") == 0) {
            return 5;
        } else if (sType.compareToIgnoreCase("HCPCS") == 0) {
            return 4;
        } else if (sType.compareToIgnoreCase("ICD") == 0) {
            return 2;
        } else if (sType.compareToIgnoreCase("SERVICE") == 0) {
            return 8;
        } else if (sType.compareToIgnoreCase("Dental") == 0) {
            return 6;
        }
        return 0;
    }

    public static void deleteFacts(final StatefulKnowledgeSession session, final Class clss) {
        ObjectFilter filter = new ObjectFilter() {
            @Override
            public boolean accept(Object object) {
                return object.getClass().equals(clss) /*&& beanMatcher.matches(object,expectedProperties)*/;
            }
        };
        Collection<FactHandle> factHandles = session.getFactHandles(filter);
        for (FactHandle handle : factHandles) {
            session.retract(handle);
        }
    }

    public static boolean isCoding(ScrubRequest req) {
        if (req != null && req.getHeader() != null
                && req.getHeader().getExtendedValidationType() != null) {
            for (ExtendedValidationType vtype : req.getHeader().getExtendedValidationType()) {
                if (vtype.getType().equalsIgnoreCase(Context.Coding)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAnalyzing(ScrubRequest req) {
        if (req != null && req.getHeader() != null
                && req.getHeader().getExtendedValidationType() != null) {
            for (ExtendedValidationType vtype : req.getHeader().getExtendedValidationType()) {
                if (vtype.getType().equalsIgnoreCase(Context.Analyzing)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ScrubRequest setCategories(
            ScrubRequest request,
            ConcurrentHashMap<String, String> ruleCats) {

        if (request != null) {

            if (request.getOutcome() != null) {
                for (ScrubRequestOutcome requestOutcome : request.getOutcome()) {
                     
                    requestOutcome.setCategories(ruleCats.get(requestOutcome.getRuleName()));
                }
            }

            if (request.getHeader() != null) {

                request.getHeader().setRequestID(request);

                if (request.getHeader().getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.HeaderOutcome headerOutcome : request.getHeader().getOutcome()) {
                    
                        headerOutcome.setCategories(ruleCats.get(headerOutcome.getRuleName()));
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome : request.getHeader().getWorkflow().getOutcome()) {
                               
                            workflowOutcome.setCategories(ruleCats.get(workflowOutcome.getRuleName()));
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome : extendedValidationType.getOutcome()) {
                                extendedValidationTypeOutcome.setCategories(ruleCats.get(extendedValidationTypeOutcome.getRuleName()));
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.ClaimOutcome claimOutcome : claim.getOutcome()) {
                        
                        claimOutcome.setCategories(ruleCats.get(claimOutcome.getRuleName()));
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome : encounter.getOutcome()) {
                                 
                                encounterOutcome.setCategories(ruleCats.get(encounterOutcome.getRuleName()));
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                 
                                for (com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome : encounter.getAuthorisation().getOutcome()) {
                                 
                                    authorisationOutcome.setCategories(ruleCats.get(authorisationOutcome.getRuleName()));
                                }
                            }
                        }
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setExcluded(currentExclusions.get(dxInfoOutcome.getRuleName()) != null);
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome : diagnosis.getOutcome()) {
                                
                                diagnosisOutcome.setCategories(ruleCats.get(diagnosisOutcome.getRuleName()));
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ActivityOutcome activityOutcome : activity.getOutcome()) {
                                
                                activityOutcome.setCategories(ruleCats.get(activityOutcome.getRuleName()));
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome observationOutcome : observation.getOutcome()) {
                                      
                                        observationOutcome.setCategories(ruleCats.get(observationOutcome.getRuleName()));
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome : claim.getResubmission().getOutcome()) {
                            
                            resubmissionOutcome.setCategories(ruleCats.get(resubmissionOutcome.getRuleName()));
                        }
                    }
                }
                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ContractOutcome contractOutcome : claim.getContract().getOutcome()) {
                               
                            contractOutcome.setCategories(ruleCats.get(contractOutcome.getRuleName()));
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.PatientOutcome patientOutcome : claim.getPatient().getOutcome()) {
                            patientOutcome.setCategories(ruleCats.get(patientOutcome.getRuleName()));
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome : claim.getPatient().getPatientInsurance().getOutcome()) {
                                patientInsuranceOutcome.setCategories(ruleCats.get(patientInsuranceOutcome.getRuleName()));
                            }
                        }

                    }
                }
                //}
            }
        }
        return request;
    }
    public static ScrubRequest handleEmptyLongMsg(
            ScrubRequest request ) {

        if (request != null) {

            if (request.getOutcome() != null) {
                for (ScrubRequestOutcome requestOutcome : request.getOutcome()) {
                    if(requestOutcome.getLongMsg()==null ||requestOutcome.getLongMsg().isEmpty() ||requestOutcome.getLongMsg().trim().equals("E") )
                    {
                        requestOutcome.setLongMsg(requestOutcome.getShortMsg());
                    }   
                   
                }
            }

            if (request.getHeader() != null) {

                request.getHeader().setRequestID(request);

                if (request.getHeader().getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.HeaderOutcome headerOutcome : request.getHeader().getOutcome()) {
                    if(headerOutcome.getLongMsg()==null ||headerOutcome.getLongMsg().isEmpty() ||headerOutcome.getLongMsg().trim().equals("E") )
                    {
                        headerOutcome.setLongMsg(headerOutcome.getShortMsg());
                    }   
                        
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome : request.getHeader().getWorkflow().getOutcome()) {
                             if(workflowOutcome.getLongMsg()==null ||workflowOutcome.getLongMsg().isEmpty() ||workflowOutcome.getLongMsg().trim().equals("E") )
                            {
                                workflowOutcome.setLongMsg(workflowOutcome.getShortMsg());
                            }   
                            
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome : extendedValidationType.getOutcome()) {
                             if(extendedValidationTypeOutcome.getLongMsg()==null ||extendedValidationTypeOutcome.getLongMsg().isEmpty() ||extendedValidationTypeOutcome.getLongMsg().trim().equals("E") )
                            {
                                extendedValidationTypeOutcome.setLongMsg(extendedValidationTypeOutcome.getShortMsg());
                            }   
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.ClaimOutcome claimOutcome : claim.getOutcome()) {
                         if(claimOutcome.getLongMsg()==null ||claimOutcome.getLongMsg().isEmpty() ||claimOutcome.getLongMsg().trim().equals("E") )
                            {
                                claimOutcome.setLongMsg(claimOutcome.getShortMsg());
                            }   
                       
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome : encounter.getOutcome()) {
                                 if(encounterOutcome.getLongMsg()==null ||encounterOutcome.getLongMsg().isEmpty() ||encounterOutcome.getLongMsg().trim().equals("E") )
                                    {
                                        encounterOutcome.setLongMsg(encounterOutcome.getShortMsg());
                                    }   
                                
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                 
                                for (com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome : encounter.getAuthorisation().getOutcome()) {
                                    if(authorisationOutcome.getLongMsg()==null ||authorisationOutcome.getLongMsg().isEmpty() ||authorisationOutcome.getLongMsg().trim().equals("E") )
                                {
                                    authorisationOutcome.setLongMsg(authorisationOutcome.getShortMsg());
                                }   
                                   
                                }
                            }
                        }
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setExcluded(currentExclusions.get(dxInfoOutcome.getRuleName()) != null);
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome : diagnosis.getOutcome()) {
                                if(diagnosisOutcome.getLongMsg()==null ||diagnosisOutcome.getLongMsg().isEmpty() ||diagnosisOutcome.getLongMsg().trim().equals("E") )
                                {
                                    diagnosisOutcome.setLongMsg(diagnosisOutcome.getShortMsg());
                                }   
                               
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ActivityOutcome activityOutcome : activity.getOutcome()) {                              
                                if(activityOutcome.getLongMsg()==null ||activityOutcome.getLongMsg().isEmpty() ||activityOutcome.getLongMsg().trim().equals("E") )
                                {                                      
                                    activityOutcome.setLongMsg(activityOutcome.getShortMsg());
                                }   
                              
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome observationOutcome : observation.getOutcome()) {
                                         if(observationOutcome.getLongMsg()==null ||observationOutcome.getLongMsg().isEmpty() ||observationOutcome.getLongMsg().trim().equals("E") )
                                        {
                                            observationOutcome.setLongMsg(observationOutcome.getShortMsg());
                                        }   
                                       
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome : claim.getResubmission().getOutcome()) {
                            if(resubmissionOutcome.getLongMsg()==null ||resubmissionOutcome.getLongMsg().isEmpty() ||resubmissionOutcome.getLongMsg().trim().equals("E") )
                                {
                                    resubmissionOutcome.setLongMsg(resubmissionOutcome.getShortMsg());
                                }   
                          
                        }
                    }
                }
                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ContractOutcome contractOutcome : claim.getContract().getOutcome()) {
                            if(contractOutcome.getLongMsg()==null ||contractOutcome.getLongMsg().isEmpty() ||contractOutcome.getLongMsg().trim().equals("E") )
                                {
                                    contractOutcome.setLongMsg(contractOutcome.getShortMsg());
                                }   
                            
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.PatientOutcome patientOutcome : claim.getPatient().getOutcome()) {
                           if(patientOutcome.getLongMsg()==null ||patientOutcome.getLongMsg().isEmpty() ||patientOutcome.getLongMsg().trim().equals("E") )
                                {
                                    patientOutcome.setLongMsg(patientOutcome.getShortMsg());
                                }   
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome : claim.getPatient().getPatientInsurance().getOutcome()) {
                                if(patientInsuranceOutcome.getLongMsg()==null ||patientInsuranceOutcome.getLongMsg().isEmpty() ||patientInsuranceOutcome.getLongMsg().trim().equals("E") )
                                {
                                    patientInsuranceOutcome.setLongMsg(patientInsuranceOutcome.getShortMsg());
                                }  
                            }
                        }

                    }
                }
                //}
            }
        }
        return request;
    }
     public static ScrubRequest setReferenceLinks( ScrubRequest request,ConcurrentHashMap<String, String> referenceLink ){

        if (request != null) {

            if (request.getOutcome() != null) {
                for (ScrubRequestOutcome requestOutcome : request.getOutcome()) {
                     
                    requestOutcome.setReferenceLink(referenceLink.get(requestOutcome.getRuleName()));
                }
            }

            if (request.getHeader() != null) {

                request.getHeader().setRequestID(request);

                if (request.getHeader().getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.HeaderOutcome headerOutcome : request.getHeader().getOutcome()) {
                    
                        headerOutcome.setReferenceLink(referenceLink.get(headerOutcome.getRuleName()));
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome : request.getHeader().getWorkflow().getOutcome()) {
                               
                            workflowOutcome.setReferenceLink(referenceLink.get(workflowOutcome.getRuleName()));
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome : extendedValidationType.getOutcome()) {
                                extendedValidationTypeOutcome.setReferenceLink(referenceLink.get(extendedValidationTypeOutcome.getRuleName()));
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.ClaimOutcome claimOutcome : claim.getOutcome()) {
                        
                        claimOutcome.setReferenceLink(referenceLink.get(claimOutcome.getRuleName()));
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome : encounter.getOutcome()) {
                                 
                                encounterOutcome.setReferenceLink(referenceLink.get(encounterOutcome.getRuleName()));
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                 
                                for (com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome : encounter.getAuthorisation().getOutcome()) {
                                 
                                    authorisationOutcome.setReferenceLink(referenceLink.get(authorisationOutcome.getRuleName()));
                                }
                            }
                        }
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setExcluded(currentExclusions.get(dxInfoOutcome.getRuleName()) != null);
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome : diagnosis.getOutcome()) {
                                
                                diagnosisOutcome.setReferenceLink(referenceLink.get(diagnosisOutcome.getRuleName()));
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ActivityOutcome activityOutcome : activity.getOutcome()) {
                                
                                activityOutcome.setReferenceLink(referenceLink.get(activityOutcome.getRuleName()));
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome observationOutcome : observation.getOutcome()) {
                                      
                                        observationOutcome.setReferenceLink(referenceLink.get(observationOutcome.getRuleName()));
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome : claim.getResubmission().getOutcome()) {
                            
                            resubmissionOutcome.setReferenceLink(referenceLink.get(resubmissionOutcome.getRuleName()));
                        }
                    }
                }
                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ContractOutcome contractOutcome : claim.getContract().getOutcome()) {
                               
                            contractOutcome.setReferenceLink(referenceLink.get(contractOutcome.getRuleName()));
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.PatientOutcome patientOutcome : claim.getPatient().getOutcome()) {
                            patientOutcome.setReferenceLink(referenceLink.get(patientOutcome.getRuleName()));
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome : claim.getPatient().getPatientInsurance().getOutcome()) {
                                patientInsuranceOutcome.setReferenceLink(referenceLink.get(patientInsuranceOutcome.getRuleName()));
                            }
                        }

                    }
                }
                //}
            }
        }
        return request;
    }
     public static ScrubRequest addPackageNameToOutcome(
            ScrubRequest request,String packageName, List<String> packageRules ) {
         if(packageName ==null || packageName.isEmpty() ||packageRules==null || packageRules.size()==0 ) return request;
        if (request != null) {

            if (request.getOutcome() != null) {
                for (ScrubRequestOutcome requestOutcome : request.getOutcome()) {
                    if(requestOutcome.getPackageName()==null ||requestOutcome.getPackageName().isEmpty() )
                    {
                        if(packageRules.contains(requestOutcome.getRuleName()))
                        requestOutcome.setPackageName(packageName);
                    }   
                   
                }
            }

            if (request.getHeader() != null) {

                request.getHeader().setRequestID(request);

                if (request.getHeader().getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.HeaderOutcome headerOutcome : request.getHeader().getOutcome()) {
                        if(headerOutcome.getPackageName()==null ||headerOutcome.getPackageName().isEmpty() )
                        {
                            if(packageRules.contains(headerOutcome.getRuleName()))
                            headerOutcome.setPackageName(packageName);
                        } 
                        
                    }
                }

                if (request.getHeader().getWorkflow() != null) {

                    request.getHeader().getWorkflow().setHeaderID(request.getHeader());

                    if (request.getHeader().getWorkflow().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.WorkflowOutcome workflowOutcome : request.getHeader().getWorkflow().getOutcome()) {
                             if(workflowOutcome.getPackageName()==null ||workflowOutcome.getPackageName().isEmpty() )
                                {
                                    if(packageRules.contains(workflowOutcome.getRuleName()))
                                    workflowOutcome.setPackageName(packageName);
                                } 
                            
                        }
                    }
                }

                if (request.getHeader().getExtendedValidationType() != null) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : request.getHeader().getExtendedValidationType()) {
                        extendedValidationType.setHeaderID(request.getHeader());

                        if (extendedValidationType.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome extendedValidationTypeOutcome : extendedValidationType.getOutcome()) {
                             if(extendedValidationTypeOutcome.getPackageName()==null ||extendedValidationTypeOutcome.getPackageName().isEmpty() )
                                {
                                    if(packageRules.contains(extendedValidationTypeOutcome.getRuleName()))
                                    extendedValidationTypeOutcome.setPackageName(packageName);
                                } 
                            }
                        }
                    }
                }
            }

            //Claim
            if (request.getClaim() != null) {
                //for (com.accumed.model.request.Claim claim : request.getClaim()) {
                com.accumed.model.scrubRequest.Claim claim = request.getClaim();
                claim.setRequest(request);

                if (claim.getOutcome() != null) {
                    for (com.accumed.model.scrubRequest.ClaimOutcome claimOutcome : claim.getOutcome()) {
                         if(claimOutcome.getPackageName()==null ||claimOutcome.getPackageName().isEmpty() )
                        {
                            if(packageRules.contains(claimOutcome.getRuleName()))
                            claimOutcome.setPackageName(packageName);
                        } 
                       
                    }
                }

                if (claim.getEncounter() != null) {
                    for (com.accumed.model.scrubRequest.Encounter encounter : claim.getEncounter()) {
                        encounter.setClaimID(claim);

                        if (encounter.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.EncounterOutcome encounterOutcome : encounter.getOutcome()) {
                                 if(encounterOutcome.getPackageName()==null ||encounterOutcome.getPackageName().isEmpty() )
                                {
                               
                                    if(packageRules.contains(encounterOutcome.getRuleName()))
                                    {
                               
                                        encounterOutcome.setPackageName(packageName);
                                    }
                                } 
                                
                            }
                        }

                        if (encounter.getAuthorisation() != null) {
                            encounter.getAuthorisation().setEncounterID(encounter);

                            if (encounter.getAuthorisation().getOutcome() != null) {
                                 
                                for (com.accumed.model.scrubRequest.AuthorisationOutcome authorisationOutcome : encounter.getAuthorisation().getOutcome()) {
                                   if(authorisationOutcome.getPackageName()==null ||authorisationOutcome.getPackageName().isEmpty() )
                                    {
                                        if(packageRules.contains(authorisationOutcome.getRuleName()))
                                        authorisationOutcome.setPackageName(packageName);
                                    } 
                                   
                                }
                            }
                        }
                    }
                }

                if (claim.getDiagnosis() != null) {
                    for (com.accumed.model.scrubRequest.Diagnosis diagnosis : claim.getDiagnosis()) {
                        diagnosis.setClaimID(claim);

                        if (diagnosis.getDxInfo() != null) {
                            for (com.accumed.model.scrubRequest.DxInfo dxInfo : diagnosis.getDxInfo()) {
                                dxInfo.setDiagnosisID(diagnosis);
//                                if (dxInfo.getOutcome() != null) {
//                                    for (com.accumed.model.scrubRequest.DxInfoOutcome dxInfoOutcome : dxInfo.getOutcome()) {
//                                        dxInfoOutcome.setExcluded(currentExclusions.get(dxInfoOutcome.getRuleName()) != null);
//                                    }
//                                }
                            }
                        }

                        if (diagnosis.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.DiagnosisOutcome diagnosisOutcome : diagnosis.getOutcome()) {
                                if(diagnosisOutcome.getPackageName()==null ||diagnosisOutcome.getPackageName().isEmpty() )
                                {
                                    if(packageRules.contains(diagnosisOutcome.getRuleName()))
                                    diagnosisOutcome.setPackageName(packageName);
                                }  
                               
                            }
                        }
                    }
                }

                if (claim.getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity activity : claim.getActivity()) {
                        activity.setClaimID(claim);

                        if (activity.getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.ActivityOutcome activityOutcome : activity.getOutcome()) {                              
                                if(activityOutcome.getPackageName()==null ||activityOutcome.getPackageName().isEmpty() )
                                {
                                 
                                    if(packageRules.contains(activityOutcome.getRuleName()))
                                    {
                                 
                                    activityOutcome.setPackageName(packageName);
                                    }
                                } 
                              
                            }
                        }

                        if (activity.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                                observation.setActivityID(activity);

                                if (observation.getOutcome() != null) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome observationOutcome : observation.getOutcome()) {
                                         if(observationOutcome.getPackageName()==null ||observationOutcome.getPackageName().isEmpty() )
                                        {
                                            if(packageRules.contains(observationOutcome.getRuleName()))
                                            observationOutcome.setPackageName(packageName);
                                        } 
                                       
                                    }
                                }
                            }
                        }
                    }
                }

                if (claim.getResubmission() != null) {
                    claim.getResubmission().setClaimID(claim);

                    if (claim.getResubmission().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ResubmissionOutcome resubmissionOutcome : claim.getResubmission().getOutcome()) {
                            if(resubmissionOutcome.getPackageName()==null ||resubmissionOutcome.getPackageName().isEmpty() )
                            {
                                if(packageRules.contains(resubmissionOutcome.getRuleName()))
                                resubmissionOutcome.setPackageName(packageName);
                            } 
                          
                        }
                    }
                }
                if (claim.getContract() != null) {
                    claim.getContract().setClaimID(claim);

                    if (claim.getContract().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.ContractOutcome contractOutcome : claim.getContract().getOutcome()) {
                            if(contractOutcome.getPackageName()==null ||contractOutcome.getPackageName().isEmpty() )
                            {
                                if(packageRules.contains(contractOutcome.getRuleName()))
                                contractOutcome.setPackageName(packageName);
                            }  
                            
                        }
                    }
                }
                if (claim.getPatient() != null) {
                    claim.getPatient().setClaimID(claim);

                    if (claim.getPatient().getOutcome() != null) {
                        for (com.accumed.model.scrubRequest.PatientOutcome patientOutcome : claim.getPatient().getOutcome()) {
                           if(patientOutcome.getPackageName()==null ||patientOutcome.getPackageName().isEmpty() )
                            {
                                if(packageRules.contains(patientOutcome.getRuleName()))
                                patientOutcome.setPackageName(packageName);
                            } 
                        }
                    }

                    if (claim.getPatient().getPatientInsurance() != null) {
                        claim.getPatient().getPatientInsurance().setPatientID(claim.getPatient());

                        //Sameer
                        if (claim.getPatient().getPatientInsurance().getOutcome() != null) {
                            for (com.accumed.model.scrubRequest.PatientInsuranceOutcome patientInsuranceOutcome : claim.getPatient().getPatientInsurance().getOutcome()) {
                                if(patientInsuranceOutcome.getPackageName()==null ||patientInsuranceOutcome.getPackageName().isEmpty() )
                                {
                                    if(packageRules.contains(patientInsuranceOutcome.getRuleName()))
                                    patientInsuranceOutcome.setPackageName(packageName);
                                } 
                            }
                        }

                    }
                }
                //}
            }
        }
        return request;
    }
    public static String getPackageTradeName(String packageName)
    {
        
        switch(packageName.toLowerCase()) {
        case "authorization":
          return "Authorization";
          
        case "billing_protocols":
         return "Billing";
        
        case "billing_ge":
         return "Exclusion";
         
         
         
        case "core_validateion":
         return "Core";
         
        case "mnec_crosswalk":
         return "MNEC Crosswalk";
         
        case "mnec_coding":
         return "MNEC Coding";
         
         // case "accumed_payer":
         //return "Payer";
         // case "accumed_general":
         //return "General";
         case "analysing":
         return "Analysing";
         
          case "workflow":
         return "Workflow";
         
        default:
          return packageName;
      }
    }
    public static boolean isFacilityAllowed(/*String senderLicense, */String providerLicense) {
        if ( restrictFacilities != null && Objects.equals(restrictFacilities, Boolean.FALSE)) {
            return true;
        }
        if(true) return true;

        java.io.File propFile = new java.io.File("./allowedFacilities.properties");
        InputStream input = null;
        if (propFile.exists()) {
            try {
                Properties prop = new Properties();
                input = new FileInputStream(propFile);
                prop.load(input);
                String sRestrictFacilities = prop.getProperty("restrictFacilities");

                if (sRestrictFacilities == null || sRestrictFacilities.isEmpty()) {
                    return false;
                } else {
                    restrictFacilities = Boolean.parseBoolean(sRestrictFacilities);
                    if (!restrictFacilities) {
                        return true;
                    }
                    String sAllowdFacilites;
                    sAllowdFacilites = prop.getProperty("allowedFacilities");
                    if (sAllowdFacilites != null && !sAllowdFacilites.isEmpty()) {
                        allowedLicense = sAllowdFacilites.split(Pattern.quote("|"));
                        for (String lice : allowedLicense) {
                            if (lice.equals(providerLicense)) {
                                return true;
                            }
                        }
                    } else {
                        return false;
                    }
                }
//                filereader = new java.io.FileReader(csvFile);
//                com.opencsv.CSVParser csvParser = new com.opencsv.CSVParserBuilder()
//                        .withSeparator('\t')
//                        .build();
//                com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(filereader)
//                        .withCSVParser(csvParser)
//                        .withSkipLines(1)
//                        .build();
//                java.util.List<String[]> allData = csvReader.readAll();
//
//                // print Data 
//                for (String[] row : allData) {
//                    String service = row[0].replaceAll("(^\\h*)|(\\h*$)", "").trim();
//                    String facilityLicense = row[1].replaceAll("(^\\h*)|(\\h*$)", "").trim();
//                    String userName = row[2].replaceAll("(^\\h*)|(\\h*$)", "").trim();
//                    String password = row[3].replaceAll("(^\\h*)|(\\h*$)", "").trim();
//                    String enabled = row[4].replaceAll("(^\\h*)|(\\h*$)", "").trim();
//                    System.out.println("account DATA: " + service + " " + facilityLicense + " " + userName + " " + password + " " + enabled);
//                    try {
//                        FACILITY_ACCOUNTS.add(new Account(facilityLicense, service, userName, password, Boolean.parseBoolean(enabled)));
//                    } catch (NumberFormatException e) {
//                        Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, e);
//                    }
//                }

            } catch (FileNotFoundException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }
        } else { //if the file is not exists --create it
            Logger.getLogger(Utils.class.getName()).severe("allowedFacilities.properties file was not found creating new one.");
            try {
                propFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public static ScrubRequest trimLongFields(ScrubRequest request) {
        if (request != null) {
            if (request.getClaim() != null) {
                if (request.getClaim().getActivity() != null) {
                    for (com.accumed.model.scrubRequest.Activity act : request.getClaim().getActivity()) {
                        if (act.getObservation() != null) {
                            for (com.accumed.model.scrubRequest.Observation obs : act.getObservation()) {
                                if (obs.getValue() != null) {
                                    if (obs.getValue().length() > 1000) {
                                        obs.setValue(obs.getValue().substring(0, 1000));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return request;
    }
}
