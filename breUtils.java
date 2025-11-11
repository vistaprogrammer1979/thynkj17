/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import com.accumed.bre.dha.DxInfoType;
import com.accumed.bre.dha.FActivity;
import com.accumed.bre.dha.FDiagnosis;
import com.accumed.bre.dha.XActivity;
import com.accumed.bre.dha.XDiagnosis;
import com.accumed.model.CodeType;
import com.accumed.model.scrubRequest.Activity;
import com.accumed.model.scrubRequest.ScrubRequest;
import com.accumed.model.scrubRequest.xclaim.XEncounter;
import com.accumed.validation.Utils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 *
 * @author smutlak
 */
public class breUtils {

    private static final java.text.SimpleDateFormat FMT = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final AtomicInteger CLAIM_ID = new AtomicInteger(1);
    private static final AtomicInteger ENCOUNTER_ID = new AtomicInteger(1);
    private static final AtomicInteger DIAGNOSIS_ID = new AtomicInteger(1);
    private static final AtomicInteger ACTIVITY_ID = new AtomicInteger(1);
    private static final AtomicInteger OBSERVATION_ID = new AtomicInteger(1);

    public static com.accumed.model.scrubRequest.ScrubRequest convertPatientToRequest(com.accumed.bre.patient.Patient patient, String userName) {
        com.accumed.model.scrubRequest.ScrubRequest req = new com.accumed.model.scrubRequest.ScrubRequest();
        com.accumed.model.scrubRequest.Header header = new com.accumed.model.scrubRequest.Header();

        req.setSender(patient.getProviderLicense());
        req.setCallingServer("Unknown");
        req.setCallingApp("External");
        req.setCallingAppVersion("Unknown");
        req.setUserID(0);
        req.setUserName(userName);
        req.setExcludeDBRules(false);
        req.setTop20(0);

        {//Header start
            List<com.accumed.model.scrubRequest.ExtendedValidationType> types
                    = new ArrayList<>();
            types.add(new com.accumed.model.scrubRequest.ExtendedValidationType("DataEntry"));
            header.setExtendedValidationType(types);
            header.setId(0);
            header.setReceiverID(patient.getInsurance().getReceiverLicense()!=null?patient.getInsurance().getReceiverLicense():"");
            //header.setWorkflow(workflow);
            req.setHeader(header);
        }//Header end

        {//Claim start
            com.accumed.model.scrubRequest.Claim claim = new com.accumed.model.scrubRequest.Claim();

            claim.setId(CLAIM_ID.incrementAndGet());
            claim.setRootID(CLAIM_ID.get());
            claim.setIdCaller(claim.getId());
            claim.setIDPayer(patient.getProviderLicense() + "-" + patient.getEmiratesId());

            claim.setMemberID(patient.getInsurance().getMemberId());
            claim.setPayerID(patient.getInsurance().getPayerLicense());
            claim.setProviderID(patient.getProviderLicense());

            claim.setEmiratesIDNumber(patient.getEmiratesId());
            claim.setGross(0);
            claim.setPatientShare(0);
            claim.setNet(0);
            claim.setVAT(0);

            claim.setProviderInvoiceAmount(0);
            //claim.setDenialCode(userName);
            //claim.setPaymentReference(userName);
            //claim.setDateSettlement(dateSettlement);
            //claim.setPending(Boolean.TRUE);
            //claim.setImported(Boolean.TRUE);

            //Mandatory claim.setEncounter(encounter);
            //Mandatory claim.setClaimType(claimType);
            //claim.setDiagnosis(diagnosis);
            //claim.setActivity(activity);
            //claim.setResubmission(resubmission);
            {//Contract start
                com.accumed.model.scrubRequest.Contract cont = new com.accumed.model.scrubRequest.Contract();
                cont.setId(0);
                cont.setNetwork(patient.getInsurance().getNetwork());
                cont.setPackageName(patient.getInsurance().getPackageName());
                cont.setPolicy(patient.getInsurance().getPlan());
                //cont.setSubNetworkName();
                claim.setContract(cont);
            }//Contract end

            {//Patient start
                com.accumed.model.scrubRequest.Patient pat = new com.accumed.model.scrubRequest.Patient();
                pat.setDateOfBirth(patient.getBirthDate());
                //pat.setDrivingLicense();
                pat.setEmirateTypes(1);
                pat.setEmiratesId(patient.getEmiratesId());
                pat.setGenderId(patient.getGender());
                pat.setId(0);
                pat.setIdCaller(claim.getIdCaller());
                //pat.setMartialStatus(userName);
                //pat.setNationality();
                //pat.setPassportId(userName);
                //pat.setPatientName(userName);
                //pat.setPatientSurname(userName);
                {//PatientInsurance start
                    com.accumed.model.scrubRequest.PatientInsurance patIns = new com.accumed.model.scrubRequest.PatientInsurance();

                    patIns.setId(0);
                    patIns.setIdCaller(claim.getIdCaller());
                    patIns.setPatientInsuranceId(patient.getInsurance().getMemberId());
                    patIns.setRelationTo("Principal");
                    patIns.setPrincipleInsuranceId("0");
                    patIns.setInsuranceLisence(patient.getInsurance().getPayerLicense());
                    patIns.setPackageName(patient.getInsurance().getPackageName());
                    patIns.setNetworkName(patient.getInsurance().getNetwork());
                    patIns.setNetworkId(0);
                    patIns.setSubNetworkName("");
                    patIns.setSubNetworkId(0);
                    patIns.setPlanName(patient.getInsurance().getPlan());
                    patIns.setPlanId(0);

                    patIns.setStartDate(patient.getInsurance().getStartDate());
                    patIns.setEndDate(patient.getInsurance().getEndDate());

                    patIns.setGrossPremium(0);
                    patIns.setPolicyHolderTypeId("3");
                    patIns.setIsExpired(0);
                    patIns.setPolicyNumber("123456");
                    patIns.setVerified(1);
                    patIns.setAdmissionType(1);
                    patIns.setIsPending(0);
                    patIns.setMaternity(0);
                    patIns.setDental(0);
                    patIns.setOptical(0);

                    //patIns.setPatientID(0);
                    //patIns.setPrincipleInsuranceNumber(userName);
                    //patIns.setCoPayment(coPayment);
                    //patIns.setDeductible(deductible);
                    pat.setPatientInsurance(patIns);
                }//PatientInsurance end

                claim.setPatient(pat);
            }//Patient end
            //claim.setXclaim(xclaim);
            //claim.setRegulatorMemberInfo(regulatorMemberInfo);

            req.setClaim(claim);

        }//Claim end
        return req;
    }

    public static com.accumed.bre.patient.Patient convertResponseToPatient(com.accumed.bre.patient.Patient patient, com.accumed.model.scrubRequest.ScrubRequest resp) {

        if (resp.getOutcome() != null && !resp.getOutcome().isEmpty()) {
            for (com.accumed.model.scrubRequest.ScrubRequestOutcome o : resp.getOutcome()) {
                patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
            }
        }

        if (resp.getHeader() != null) {
            if (resp.getHeader().getOutcome() != null && !resp.getHeader().getOutcome().isEmpty()) {
                for (com.accumed.model.scrubRequest.HeaderOutcome o : resp.getHeader().getOutcome()) {
                    patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                }
            }
            if (resp.getHeader().getWorkflow() != null) {
                if (resp.getHeader().getWorkflow().getOutcome() != null
                        && !resp.getHeader().getWorkflow().getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.WorkflowOutcome o : resp.getHeader().getWorkflow().getOutcome()) {
                        patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                    }
                }
            }

            if (resp.getHeader().getExtendedValidationType() != null) {
                for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : resp.getHeader().getExtendedValidationType()) {
                    if (extendedValidationType.getOutcome() != null && !extendedValidationType.getOutcome().isEmpty()) {
                        for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome o : extendedValidationType.getOutcome()) {
                            patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                        }
                    }
                }
            }
        }

        //Claim
        if (resp.getClaim() != null) {
            //for (com.accumed.model.request.Claim claim : getClaim()) {

//            com.accumed.model.batch.outcome.ResponseClaim responseClaim = new com.accumed.model.batch.outcome.ResponseClaim();
//            responseClaim.setID(claim.getRootID());
//            responseClaim.setIdCaller(claim.getIdCaller());
            if (resp.getClaim().getOutcome() != null && !resp.getClaim().getOutcome().isEmpty()) {
                for (com.accumed.model.scrubRequest.ClaimOutcome o : resp.getClaim().getOutcome()) {
                    patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                }
            }

            if (resp.getClaim().getEncounter() != null) {
                for (com.accumed.model.scrubRequest.Encounter encounter : resp.getClaim().getEncounter()) {
                    if (encounter.getOutcome() != null && !encounter.getOutcome().isEmpty()) {
                        for (com.accumed.model.scrubRequest.EncounterOutcome o : encounter.getOutcome()) {
                            patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                        }
                    }

                    if (encounter.getAuthorisation() != null) {
                        if (encounter.getAuthorisation().getOutcome() != null
                                && !encounter.getAuthorisation().getOutcome().isEmpty()) {
                            for (com.accumed.model.scrubRequest.AuthorisationOutcome o : encounter.getAuthorisation().getOutcome()) {
                                patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                            }
                        }
                    }
                }
            }

            if (resp.getClaim().getDiagnosis() != null) {
                for (com.accumed.model.scrubRequest.Diagnosis diagnosis : resp.getClaim().getDiagnosis()) {
                    if (diagnosis.getOutcome() != null && !diagnosis.getOutcome().isEmpty()) {
                        for (com.accumed.model.scrubRequest.DiagnosisOutcome o : diagnosis.getOutcome()) {
                            patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                        }
                    }
                }
            }

            if (resp.getClaim().getActivity() != null) {
                for (com.accumed.model.scrubRequest.Activity activity : resp.getClaim().getActivity()) {
                    if (activity.getOutcome() != null && !activity.getOutcome().isEmpty()) {
                        for (com.accumed.model.scrubRequest.ActivityOutcome o : activity.getOutcome()) {
                            patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                        }
                    }

                    if (activity.getObservation() != null) {
                        for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                            if (observation.getOutcome() != null && !observation.getOutcome().isEmpty()) {
                                for (com.accumed.model.scrubRequest.ObservationOutcome o : observation.getOutcome()) {
                                    patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                                }
                            }
                        }
                    }
                }
            }

            if (resp.getClaim().getResubmission() != null) {
                if (resp.getClaim().getResubmission().getOutcome() != null
                        && !resp.getClaim().getResubmission().getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.ResubmissionOutcome o : resp.getClaim().getResubmission().getOutcome()) {
                        patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                    }
                }
            }
            if (resp.getClaim().getContract() != null) {
                if (resp.getClaim().getContract().getOutcome() != null
                        && !resp.getClaim().getContract().getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.ContractOutcome o : resp.getClaim().getContract().getOutcome()) {
                        patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                    }
                }
            }
            if (resp.getClaim().getPatient() != null) {
                if (resp.getClaim().getPatient().getOutcome() != null && !resp.getClaim().getPatient().getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.PatientOutcome o : resp.getClaim().getPatient().getOutcome()) {
                        patient.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                    }
                }

                if (resp.getClaim().getPatient().getPatientInsurance() != null) {
                    if (resp.getClaim().getPatient().getPatientInsurance().getOutcome() != null
                            && !resp.getClaim().getPatient().getPatientInsurance().getOutcome().isEmpty()) {
                        for (com.accumed.model.scrubRequest.PatientInsuranceOutcome o : resp.getClaim().getPatient().getPatientInsurance().getOutcome()) {
                            patient.getInsurance().addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
                        }
                    }
                }
            }
        }
        return patient;
    }

    public static com.accumed.model.scrubRequest.ScrubRequest convertbreClaimToRequest(com.accumed.bre.patient.Patient patient,
            com.accumed.bre.dha.Claim claim, String process, String userName) throws ParseException {
        if (patient==null){
        patient =new com.accumed.bre.patient.Patient();
        }
        com.accumed.model.scrubRequest.ScrubRequest req = breUtils.convertPatientToRequest(patient, userName);

 
        req.setSender(patient.getProviderLicense());
        req.setCallingServer("Unknown");
        req.setCallingApp("External");
        req.setCallingAppVersion("Unknown");
        req.setUserID(0);
        req.setUserName(userName);
        req.setExcludeDBRules(false);
        req.setTop20(0);

        {//Header start
//            List<com.accumed.model.scrubRequest.ExtendedValidationType> types
//                    = new ArrayList<>();
//            switch (process) {
//                case "AppointmentDesk":
//                    types.add(new com.accumed.model.scrubRequest.ExtendedValidationType("DataEntry"));
//                    types.add(new com.accumed.model.scrubRequest.ExtendedValidationType("Coding"));
//                    break;
//            }
//            req.getHeader().setExtendedValidationType(types);
            req.getHeader().setId(0);
            req.getHeader().setReceiverID(patient.getInsurance()!=null?patient.getInsurance().getReceiverLicense():null);
            //header.setWorkflow(workflow);
            //req.setHeader(header);
        }//Header end

        {//Claim start
            if (req.getClaim().getId() == null) {
                req.getClaim().setId(CLAIM_ID.incrementAndGet());
            }

            if (req.getClaim().getRootID() == null) {
                req.getClaim().setRootID(req.getClaim().getId());
            }
            if (req.getClaim().getIdCaller() == null) {
                req.getClaim().setIdCaller(req.getClaim().getId());
            }

            req.getClaim().setIDPayer(patient.getProviderLicense() + "-" + patient.getEmiratesId());

            req.getClaim().setMemberID(patient.getInsurance()!=null?patient.getInsurance().getMemberId():null);
            req.getClaim().setPayerID(patient.getInsurance()!=null?patient.getInsurance().getPayerLicense():null);
            req.getClaim().setProviderID(patient.getProviderLicense());

            req.getClaim().setEmiratesIDNumber(patient.getEmiratesId());
            req.getClaim().setGross(claim.getGross());
            req.getClaim().setPatientShare(claim.getPatientShare());
            req.getClaim().setNet(claim.getNet());

            req.getClaim().setProviderInvoiceAmount(0);
            //claim.setDenialCode(userName);
            //claim.setPaymentReference(userName);
            //claim.setDateSettlement(dateSettlement);
            //claim.setPending(Boolean.TRUE);
            //claim.setImported(Boolean.TRUE);

            if (claim.getEncounter() != null) {//Encounter start --Mandatory claim.setEncounter(encounter);
                com.accumed.model.scrubRequest.Encounter encounter = null;
                if (req.getClaim().getEncounter() == null || req.getClaim().getEncounter().isEmpty()) {
                    encounter = new com.accumed.model.scrubRequest.Encounter();
                } else {
                    encounter = req.getClaim().getEncounter().get(0);
                }

                //encounter.setAuthorisation(authorisation);
                //encounter.setClaimID(claimID);
                if (claim.getEncounter().getEnd() != null) {
                    encounter.setEnd(FMT.parse(claim.getEncounter().getEnd()));
                }
                if (claim.getEncounter().getEndType() != null) {
                    encounter.setEndType(claim.getEncounter().getEndType().intValue());
                }
                encounter.setFacilityID(patient.getProviderLicense());
                encounter.setId(ENCOUNTER_ID.incrementAndGet());
                encounter.setIdCaller(req.getClaim().getId());
                if (claim.getEncounter().getPatientID() != null) {
                    encounter.setPatientID(claim.getEncounter().getPatientID());
                } else {
                    encounter.setPatientID("0");
                }
                if (claim.getEncounter().getStart() != null) {
                    encounter.setStart(FMT.parse(claim.getEncounter().getStart()));
                }
                if (claim.getEncounter().getStartType() != null) {
                    encounter.setStartType(claim.getEncounter().getStartType().intValue());
                }
                encounter.setTransferDestination(claim.getEncounter().getTransferDestination());
                encounter.setTransferSource(claim.getEncounter().getTransferSource());
                if (claim.getEncounter().getType() != null) {
                    encounter.setType(claim.getEncounter().getType().intValue());
                }

                if (req.getClaim().getEncounter() == null || req.getClaim().getEncounter().isEmpty()) {
                    List<com.accumed.model.scrubRequest.Encounter> encounters = new ArrayList();
                    encounters.add(encounter);
                    req.getClaim().setEncounter(encounters);
                }
            }//Encounter end

            //{//ClaimType start -- Mandatory 
//                List<ClaimType> claimTypeList=new ArrayList();
            req.getClaim().setClaimType(null);

            //}//ClaimType end
            {//Diagnosis start
                List<com.accumed.model.scrubRequest.Diagnosis> diags = null;
                if (req.getClaim().getDiagnosis() == null || req.getClaim().getDiagnosis().isEmpty()) {
                    diags = new ArrayList<>();
                } else {
                    diags = req.getClaim().getDiagnosis();
                }

                if (claim.getDiagnosis() != null && !claim.getDiagnosis().isEmpty()) {
                    for (com.accumed.bre.dha.Diagnosis src : claim.getDiagnosis()) {
                        com.accumed.model.scrubRequest.Diagnosis dest = new com.accumed.model.scrubRequest.Diagnosis();

                        //dest.setClaimID(claimID);
                        dest.setId(DIAGNOSIS_ID.incrementAndGet());
                        dest.setIdCaller(req.getClaim().getId());
                        //dest.setProviderCode(process);
                        //dest.setProviderType(process);
                        dest.setType(src.getType());
                        dest.setCode(src.getCode());

                        {//DxInfo start
                            List<com.accumed.model.scrubRequest.DxInfo> dxInfos = null;
                            if (src.getDxInfo() != null && !src.getDxInfo().isEmpty()) {
                                dxInfos = new ArrayList();
                                for (com.accumed.bre.dha.Diagnosis.DxInfo dxSrc : src.getDxInfo()) {
                                    com.accumed.model.scrubRequest.DxInfo dxDest = new com.accumed.model.scrubRequest.DxInfo();
                                    dxDest.setCode(dxSrc.getCode());
                                    //dxDest.setDiagnosisID(dest);
                                    //dxDest.setId(Integer.SIZE);
                                    dxDest.setType(com.accumed.model.scrubRequest.DxInfoType.fromValue(DxInfoType.POA.value()));

                                    dxInfos.add(dxDest);
                                }

                                if (dxInfos != null && !dxInfos.isEmpty()) {
                                    dest.setDxInfo(dxInfos);
                                }
                            }

                        }//DxInfo end

                        diags.add(dest);
                    }
                }
                if (req.getClaim().getDiagnosis() == null || req.getClaim().getDiagnosis().isEmpty()
                        && diags != null && !diags.isEmpty()) {
                    req.getClaim().setDiagnosis(diags);
                }
            }//Diagnosis end

            {//Activity start
                List<com.accumed.model.scrubRequest.Activity> acts = null;
                if (claim.getActivity() != null && !claim.getActivity().isEmpty()) {

                    acts = new ArrayList();
                    for (com.accumed.bre.dha.Activity src : claim.getActivity()) {
                        com.accumed.model.scrubRequest.Activity dest = new com.accumed.model.scrubRequest.Activity();

                        dest.setActivityID(src.getID());
                        Integer autoActId = ACTIVITY_ID.incrementAndGet();
                        dest.setId(autoActId);
                        dest.setIdCaller(req.getClaim().getId());

                        dest.setClinician(src.getClinician());
                        dest.setCode(src.getCode());

                        dest.setQuantity(src.getQuantity());
                        dest.setStart(FMT.parse(src.getStart()));
                        dest.setType(CodeType.from(src.getType().intValue()));
                        dest.setNet(src.getNet());
                        dest.setGross(src.getGross());
                        dest.setPatientShare(src.getPatientShare());
                        dest.setCopayment(src.getCopayment());
                        dest.setDeductible(src.getDeductible());

                        dest.setManualPrices(Boolean.TRUE);
                        dest.setOrderingClinician(src.getClinician());
                        dest.setPriorAuthorizationID(src.getPriorAuthorizationID());
                        dest.setDateOrdered(src.getDateOrdered());
                        dest.setVAT(src.getVAT());
                        dest.setVATPercent(src.getVATPercent());
                        {//Observation start
                            List<com.accumed.model.scrubRequest.Observation> obs = new ArrayList();
                            for (com.accumed.bre.dha.Observation obsSrc : src.getObservation()) {
                                com.accumed.model.scrubRequest.Observation obsDest = new com.accumed.model.scrubRequest.Observation();

                                obsDest.setActivityID(dest);
                                obsDest.setCode(obsSrc.getCode());
                                obsDest.setId(OBSERVATION_ID.incrementAndGet());
                                obsDest.setIdCaller(req.getClaim().getIdCaller());
                                obsDest.setType(obsSrc.getType());
                                obsDest.setValue(obsSrc.getValue());
                                obsDest.setValueType(obsSrc.getValueType());
                                obs.add(obsDest);

                            }
                            if (obs != null && !obs.isEmpty()) {
                                dest.setObservation(obs);
                            }
                        }//Observation end
                        acts.add(dest);
                    }

                }
                if (acts != null && !acts.isEmpty()) {
                    req.getClaim().setActivity(acts);
                }
            }//Activity end
            {//xclaim
                req = convertbreXclaimToRequestXclaim(claim.getXclaim(), req);
            }
            { //Fclaim
             //   req =convertbreFClaimToRequestFClaim(claim.getFclaim(), req);
            }
            {//Resubmission start

            }//Resubmission end

            {//Contract start
                com.accumed.model.scrubRequest.Contract cont = new com.accumed.model.scrubRequest.Contract();
                req.getClaim().getContract().setId(0);
                req.getClaim().getContract().setNetwork(patient.getInsurance().getNetwork());
                req.getClaim().getContract().setPackageName(patient.getInsurance().getPackageName());
                req.getClaim().getContract().setPolicy(patient.getInsurance().getPlan());
                //cont.setSubNetworkName();
                //claim.setContract(cont);
            }//Contract end

//            {//Patient start
//                com.accumed.model.scrubRequest.Patient pat = new com.accumed.model.scrubRequest.Patient();
//                pat.setDateOfBirth(patient.getBirthDate());
//                //pat.setDrivingLicense();
//                pat.setEmirateTypes(1);
//                pat.setEmiratesId(patient.getEmiratesId());
//                pat.setGenderId(patient.getGender());
//                pat.setId(0);
//                //pat.setIdCaller(Integer.MIN_VALUE);
//                //pat.setMartialStatus(userName);
//                //pat.setNationality();
//                //pat.setPassportId(userName);
//                //pat.setPatientName(userName);
//                //pat.setPatientSurname(userName);
//                {//PatientInsurance start
//                    com.accumed.model.scrubRequest.PatientInsurance patIns = new com.accumed.model.scrubRequest.PatientInsurance();
//
//                    patIns.setId(0);
//                    patIns.setIdCaller(0);
//                    patIns.setPatientInsuranceId("0");
//                    patIns.setRelationTo("Principal");
//                    patIns.setPrincipleInsuranceId("0");
//                    patIns.setInsuranceLisence(patient.getInsurance().getPayerLicense());
//                    patIns.setPackageName(patient.getInsurance().getPackageName());
//                    patIns.setNetworkName(patient.getInsurance().getNetwork());
//                    patIns.setNetworkId(0);
//                    patIns.setSubNetworkName("");
//                    patIns.setSubNetworkId(0);
//                    patIns.setPlanName(patient.getInsurance().getPlan());
//                    patIns.setPlanId(0);
//
//                    patIns.setStartDate(patient.getInsurance().getStartDate());
//                    patIns.setEndDate(patient.getInsurance().getEndDate());
//
//                    patIns.setGrossPremium(0);
//                    patIns.setPolicyHolderTypeId("3");
//                    patIns.setIsExpired(0);
//                    patIns.setPolicyNumber("123456");
//                    patIns.setVerified(1);
//                    patIns.setAdmissionType(1);
//                    patIns.setIsPending(0);
//                    patIns.setMaternity(0);
//                    patIns.setDental(0);
//                    patIns.setOptical(0);
//
//                    //patIns.setPatientID(0);
//                    //patIns.setPrincipleInsuranceNumber(userName);
//                    //patIns.setCoPayment(coPayment);
//                    //patIns.setDeductible(deductible);
//                    pat.setPatientInsurance(patIns);
//                }//PatientInsurance end
//
//                claim.setPatient(pat);
//            }//Patient end
            //claim.setXclaim(xclaim);
            //claim.setRegulatorMemberInfo(regulatorMemberInfo);
            //req.setClaim(claim);
        }//Claim end
        return req;
    }

    public static com.accumed.model.scrubRequest.ScrubRequest addExtendedValidationType(
            com.accumed.model.scrubRequest.ScrubRequest req, String extValidationType, Boolean reset) {
        if (reset) {
            List<com.accumed.model.scrubRequest.ExtendedValidationType> types = new ArrayList<>();
            types.add(new com.accumed.model.scrubRequest.ExtendedValidationType(extValidationType));
            req.getHeader().setExtendedValidationType(types);
        } else {
            req.getHeader().getExtendedValidationType().
                    add(new com.accumed.model.scrubRequest.ExtendedValidationType(extValidationType));
        }
        return req;

    }

    public static com.accumed.bre.dha.Claim convertResponseTobreClaim(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        claim = appendResponseOutcom(claim, resp);
        if (resp.getHeader() != null) {
            claim = appendHeaderOutcome(claim, resp);
        }
        //Claim
        if (resp.getClaim() != null) {
            claim = appendClaimOutcome(claim, resp);
            if (resp.getClaim().getEncounter() != null) {
                claim = appendEncounterOutcome(claim, resp);
            }
            if (resp.getClaim().getDiagnosis() != null) {
                claim = appendDiagnosisOutcome(claim, resp);
            }
            if (resp.getClaim().getActivity() != null) {
                claim = appendActivityOutcome(claim, resp);
            }
            if (resp.getClaim().getContract() != null) {
                claim = appendContractOutcome(claim, resp);
            }
            if (resp.getClaim().getPatient() != null) {
                claim = appendPatientOutcome(claim, resp);
            }
//            if (resp.getClaim().getXclaim() != null) {
//                claim = appendXClaimOutcome(claim, resp);
//            }

        }
        return claim;
    }

    public static String marshal(com.accumed.bre.patient.Patient patient) {
        try {
            JAXBContext contextA = JAXBContext.newInstance(com.accumed.bre.patient.Patient.class);
            StringWriter writer = new StringWriter();
            Marshaller marshaller = contextA.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(patient, writer);
            marshaller = null;

            return writer.toString();
        } catch (JAXBException e) {
            Statistics.addException(e);
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, e);
            return "Error Marshalling Patient.";
        }
    }

    public static String marshal(com.accumed.bre.dha.Claim claim) {
        try {
            JAXBContext contextA = JAXBContext.newInstance(com.accumed.bre.dha.Claim.class);
            StringWriter writer = new StringWriter();
            Marshaller marshaller = contextA.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(claim, writer);
            marshaller = null;

            return writer.toString();
        } catch (JAXBException e) {
            Statistics.addException(e);
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, e);
            return "Error Marshalling Claim.";
        }
    }

    public static boolean savePatient(com.accumed.bre.patient.Patient patient) {
        String sRequest = marshal(patient);
        try {
            File file = File.createTempFile("priceEngine_patient" /*+ patient.get.getIdCaller()*/ + "_" + Long.toString(System.nanoTime()), ".log");
            //File file = new File("/users/mkyong/filename.txt");

//        // if file doesnt exists, then create it
//        if (!file.exists()) {
//            file.createNewFile();
//        }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(sRequest);
            bw.close();

            Logger.getLogger(Utils.class.getName()).log(Level.INFO, "Done");

        } catch (IOException e) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }

        return true;
    }

    public static boolean saveClaim(com.accumed.bre.dha.Claim claim) {
        String sRequest = marshal(claim);
        try {
            File file = File.createTempFile("validate_claim" /*+ patient.get.getIdCaller()*/ + "_" + Long.toString(System.nanoTime()), ".log");
            //File file = new File("/users/mkyong/filename.txt");

//        // if file doesnt exists, then create it
//        if (!file.exists()) {
//            file.createNewFile();
//        }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(sRequest);
            bw.close();

            Logger.getLogger(Utils.class.getName()).log(Level.INFO, "Done");

        } catch (IOException e) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }

        return true;
    }

    public static com.accumed.bre.dha.Claim convertScrubTobreClaim(com.accumed.model.scrubRequest.ScrubRequest request, com.accumed.bre.dha.Claim claim) {

        com.accumed.bre.dha.Encounter encounter = new com.accumed.bre.dha.Encounter();
        encounter = convertScrubEncounterTobreEncounter(request.getClaim().getEncounter().get(0), encounter);
        claim.setEncounter(encounter);
        claim.setGross(request.getClaim().getGross());
        claim.setNet(request.getClaim().getNet());
        claim.setPatientShare(request.getClaim().getPatientShare());
        List< com.accumed.bre.dha.Diagnosis> breDiagnosisList = new ArrayList< com.accumed.bre.dha.Diagnosis>();
        for (com.accumed.model.scrubRequest.Diagnosis d : request.getClaim().getDiagnosis()) {
            com.accumed.bre.dha.Diagnosis breDiagnosis = new com.accumed.bre.dha.Diagnosis();
            breDiagnosis.setCode(d.getCode());
            breDiagnosis.setType(d.getType());
//           switch (d.getType()) {
//            case "Principal":
//               breDiagnosis.setType(DiagnosisType.PRINCIPAL);
//                break;
//            case "Secondary":
//              breDiagnosis.setType(DiagnosisType.SECONDARY);
//                break;
//            case "Admitting":
//              breDiagnosis.setType(DiagnosisType.ADMITTING);
//                break;
//        }

//            List<Diagnosis.DxInfo> DXinfoList = new ArrayList<>();
//            for (com.accumed.model.scrubRequest.DxInfo dxinfo : d.getDxInfo()) {
//                Diagnosis.DxInfo dxi = new Diagnosis.DxInfo();
//                dxi.setCode(dxinfo.getCode());
//                if (dxi.getType().equals(com.accumed.model.scrubRequest.DxInfoType.POA)) {
//                    dxi.setType(DxInfoType.POA);
//                } else if (dxi.getType().equals(com.accumed.model.scrubRequest.DxInfoType.YearOfOnset)) {
//                    dxi.setType(DxInfoType.YearOfOnset);
//                }
//                DXinfoList.add(dxi);
//        }
//        breDiagnosis.setDxInfo(DXinfoList);
            breDiagnosisList.add(breDiagnosis);
        }
        claim.setDiagnosis(breDiagnosisList);
        List< com.accumed.bre.dha.Activity> breActivityList = new ArrayList< com.accumed.bre.dha.Activity>();
        for (com.accumed.model.scrubRequest.Activity a : request.getClaim().getActivity()) {
            com.accumed.bre.dha.Activity breActivity = new com.accumed.bre.dha.Activity();
            breActivity.setID(a.getActivityID());
            breActivity.setCode(a.getCode());
            breActivity.setClinician(a.getClinician());
            breActivity.setNet(a.getNet());
            breActivity.setPriorAuthorizationID(a.getPriorAuthorizationID());
            breActivity.setQuantity(a.getQuantity());
            breActivity.setStart(FMT.format(a.getStart()));
            breActivity.setType(BigInteger.valueOf(a.getType().getValue()));
            List< com.accumed.bre.dha.Observation> breObservationList = new ArrayList< com.accumed.bre.dha.Observation>();
            if (a.getObservation() != null) {
                for (com.accumed.model.scrubRequest.Observation o : a.getObservation()) {
                    com.accumed.bre.dha.Observation breObservation = new com.accumed.bre.dha.Observation();
                    breObservation.setCode(o.getCode());
                    breObservation.setType(o.getType());
//                switch (o.getType()) {
//                    case "LOINC":
//                        breObservation.setType(ObservationType.LOINC);
//                        break;
//                    case "TEXT":
//                        breObservation.setType(ObservationType.TEXT);
//                        break;
//                    case "FILE":
//                        breObservation.setType(ObservationType.FILE);
//                        break;
//
//                    case "UNIVERSAL_DENTAL":
//                        breObservation.setType(ObservationType.UNIVERSAL_DENTAL);
//                        break;
//                    case "FINANCIAL":
//                        breObservation.setType(ObservationType.FINANCIAL);
//                        break;
//                    case "GROUPING":
//                        breObservation.setType(ObservationType.GROUPING);
//                        break;
//                    case "ERX":
//                        breObservation.setType(ObservationType.ERX);
//                        break;
//
//                    case "RESULT":
//                        breObservation.setType(ObservationType.RESULT);
//
//                        break;
//
//                    case "ROM":
//                        breObservation.setType(ObservationType.ROM);
//                        break;
//
//                    case "EXCLUDE_FROM_DRG":
//                        breObservation.setType(ObservationType.EXCLUDE_FROM_DRG);
//                        break;
//                }
                    breObservation.setValue(o.getValue());
                    breObservation.setValueType(o.getValueType());

                }
            }
            breActivityList.add(breActivity);

        }
        claim.setDiagnosis(breDiagnosisList);
        claim.setActivity(breActivityList);

//        List<Outcome> outcomeList = new ArrayList<Outcome>();
//       for (com.accumed.model.scrubRequest.ScrubRequestOutcome soc : request.getOutcome()) {
//          Outcome o = new Outcome();
//           outcomeList.add(convertScrubOutcomeTobreOutCome(soc, o));
//       }
//        claim.setOutcome(outcomeList);
        return claim;
    }

    public static com.accumed.bre.dha.Encounter convertScrubEncounterTobreEncounter(com.accumed.model.scrubRequest.Encounter reqEncounter, com.accumed.bre.dha.Encounter encounter) {

        com.accumed.bre.dha.Encounter enc = new com.accumed.bre.dha.Encounter();
        enc.setEnd(FMT.format(reqEncounter.getEnd()));
        enc.setEndType(BigInteger.valueOf(reqEncounter.getEndType()));
        enc.setPatientID(reqEncounter.getPatientID());
        enc.setStart(FMT.format(reqEncounter.getStart()));
        enc.setStartType(BigInteger.valueOf(reqEncounter.getStartType()));
        enc.setTransferDestination(reqEncounter.getTransferDestination());
        enc.setTransferSource(reqEncounter.getTransferSource());
        enc.setType(BigInteger.valueOf(reqEncounter.getType()));
        return enc;
    }

    public static com.accumed.bre.Outcome convertScrubOutcomeTobreOutCome(com.accumed.model.scrubRequest.ScrubRequestOutcome requestOutCome, com.accumed.bre.Outcome outcome) {
        outcome.setId(requestOutCome.getId());
        outcome.setLongMsg(requestOutCome.getLongMsg());
        outcome.setRuleID(requestOutCome.getRuleID());
        outcome.setRuleName(requestOutCome.getRuleName());
        outcome.setSeverity(requestOutCome.getSeverity());
        outcome.setShortMsg(requestOutCome.getShortMsg());
        return outcome;
    }

    public static com.accumed.bre.patient.Patient convertScrubPatientTobrePatient(com.accumed.model.scrubRequest.Patient scrubPatient, com.accumed.bre.patient.Patient brePatient, com.accumed.model.scrubRequest.Claim scrubClaim) {
        brePatient.setBirthDate(scrubPatient.getDateOfBirth());
        brePatient.setEmiratesId(scrubPatient.getEmiratesId());
        brePatient.setGender(scrubPatient.getGenderId());
        brePatient.setProviderLicense(scrubClaim.getProviderID());
        brePatient.setInsurance(convertScrubInsuranceTobreInsurance(scrubPatient, scrubClaim));
        return brePatient;
    }

    public static com.accumed.bre.patient.Insurance convertScrubInsuranceTobreInsurance(com.accumed.model.scrubRequest.Patient scrubPatient, com.accumed.model.scrubRequest.Claim scrubClaim) {
        com.accumed.bre.patient.Insurance breInsurance = new com.accumed.bre.patient.Insurance();
        breInsurance.setMemberId(scrubClaim.getMemberID());
        breInsurance.setEndDate(scrubPatient.getPatientInsurance().getEndDate());
        breInsurance.setNetwork(scrubPatient.getPatientInsurance().getNetworkName());
        breInsurance.setPackageName(scrubPatient.getPatientInsurance().getPackageName());
        breInsurance.setPayerLicense(scrubClaim.getPayerID());
        breInsurance.setReceiverLicense(scrubPatient.getPatientInsurance().getInsuranceLisence());
        breInsurance.setPlan(scrubPatient.getPatientInsurance().getPlanName());
        breInsurance.setStartDate(scrubPatient.getPatientInsurance().getStartDate());
        return breInsurance;
    }

    public static com.accumed.bre.dha.Claim appendResponseOutcom(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        if (resp.getOutcome() != null && !resp.getOutcome().isEmpty()) {
            for (com.accumed.model.scrubRequest.ScrubRequestOutcome o : resp.getOutcome()) {
                claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg()));
            }
        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendClaimOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        if (resp.getClaim().getOutcome() != null && !resp.getClaim().getOutcome().isEmpty()) {
            for (com.accumed.model.scrubRequest.ClaimOutcome o : resp.getClaim().getOutcome()) {
                claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
            }
        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendHeaderOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        if (resp.getHeader().getOutcome() != null && !resp.getHeader().getOutcome().isEmpty()) {
            for (com.accumed.model.scrubRequest.HeaderOutcome o : resp.getHeader().getOutcome()) {
                claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
            }
        }
        if (resp.getHeader().getWorkflow() != null) {
            if (resp.getHeader().getWorkflow().getOutcome() != null
                    && !resp.getHeader().getWorkflow().getOutcome().isEmpty()) {
                for (com.accumed.model.scrubRequest.WorkflowOutcome o : resp.getHeader().getWorkflow().getOutcome()) {
                    claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                }
            }
        }

        if (resp.getHeader().getExtendedValidationType() != null) {
            for (com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType : resp.getHeader().getExtendedValidationType()) {
                if (extendedValidationType.getOutcome() != null && !extendedValidationType.getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.ExtendedValidationTypeOutcome o : extendedValidationType.getOutcome()) {
                        claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                    }
                }
            }
        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendEncounterOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        for (com.accumed.model.scrubRequest.Encounter encounter : resp.getClaim().getEncounter()) {
            if (encounter.getOutcome() != null && !encounter.getOutcome().isEmpty()) {
                for (com.accumed.model.scrubRequest.EncounterOutcome o : encounter.getOutcome()) {
                    claim.getEncounter().addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                }
            }

            if (encounter.getAuthorisation() != null) {
                if (encounter.getAuthorisation().getOutcome() != null
                        && !encounter.getAuthorisation().getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.AuthorisationOutcome o : encounter.getAuthorisation().getOutcome()) {
                        claim.getEncounter().addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                    }
                }
            }
        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendDiagnosisOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        for (com.accumed.model.scrubRequest.Diagnosis diagnosis : resp.getClaim().getDiagnosis()) {
            if (diagnosis.getOutcome() != null && !diagnosis.getOutcome().isEmpty()) {

                for (com.accumed.bre.dha.Diagnosis dest : claim.getDiagnosis()) {

                    if (dest.getCode().equals(diagnosis.getCode())
                            && dest.getType().equals(diagnosis.getType())) {

                        for (com.accumed.model.scrubRequest.DiagnosisOutcome o : diagnosis.getOutcome()) {
                            dest.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                        }
                        break;
                    }

                }
            }

        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendActivityOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {

        for (com.accumed.model.scrubRequest.Activity activity : resp.getClaim().getActivity()) {

            com.accumed.bre.dha.Activity dest = null;
            for (com.accumed.bre.dha.Activity tmp : claim.getActivity()) {
                if (tmp.getCode().equals(activity.getCode())
                        && tmp.getType().intValue() == activity.getType().getValue()) {
                    dest = tmp;
                    break;
                }
            }

            if (dest != null) {
                if (activity.getOutcome() != null && !activity.getOutcome().isEmpty()) {
                    for (com.accumed.model.scrubRequest.ActivityOutcome o : activity.getOutcome()) {
                        dest.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                    }
                }

                if (activity.getObservation() != null) {
                    for (com.accumed.model.scrubRequest.Observation observation : activity.getObservation()) {
                        if (observation.getOutcome() != null && !observation.getOutcome().isEmpty()) {

                            for (com.accumed.bre.dha.Observation tmp : dest.getObservation()) {
                                if (tmp.getCode().equals(observation.getCode())
                                        && tmp.getType().equals(observation.getType())) {
                                    for (com.accumed.model.scrubRequest.ObservationOutcome o : observation.getOutcome()) {
                                        tmp.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                                    }
                                    break;
                                }
                            }
                        }

                    }
                }
            }
        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendContractOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        if (resp.getClaim().getContract().getOutcome() != null
                && !resp.getClaim().getContract().getOutcome().isEmpty()) {
            for (com.accumed.model.scrubRequest.ContractOutcome o : resp.getClaim().getContract().getOutcome()) {
                claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
            }
        }
        return claim;
    }

    public static com.accumed.bre.dha.Claim appendPatientOutcome(com.accumed.bre.dha.Claim claim, com.accumed.model.scrubRequest.ScrubRequest resp) {
        if (resp.getClaim().getPatient().getOutcome() != null && !resp.getClaim().getPatient().getOutcome().isEmpty()) {
            for (com.accumed.model.scrubRequest.PatientOutcome o : resp.getClaim().getPatient().getOutcome()) {
                claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
            }
        }

        if (resp.getClaim().getPatient().getPatientInsurance() != null) {
            if (resp.getClaim().getPatient().getPatientInsurance().getOutcome() != null
                    && !resp.getClaim().getPatient().getPatientInsurance().getOutcome().isEmpty()) {
                for (com.accumed.model.scrubRequest.PatientInsuranceOutcome o : resp.getClaim().getPatient().getPatientInsurance().getOutcome()) {
                    claim.addOutcome(new com.accumed.bre.Outcome(o.getSeverity(), o.getRuleName(), o.getShortMsg(), o.getLongMsg(), o.getCategories(), o.getPackageName()));
                }
            }
        }
        return claim;
    }

    private static void convertBreActvityToRequestActivity(com.accumed.bre.dha.Claim claim, ScrubRequest req, List<Activity> acts) throws ParseException {
        acts = new ArrayList();
        for (com.accumed.bre.dha.Activity src : claim.getActivity()) {
            com.accumed.model.scrubRequest.Activity dest = new com.accumed.model.scrubRequest.Activity();

            dest.setActivityID(src.getID());
            Integer autoActId = ACTIVITY_ID.incrementAndGet();
            dest.setId(autoActId);
            dest.setIdCaller(req.getClaim().getId());

            dest.setClinician(src.getClinician());
            dest.setCode(src.getCode());

            dest.setQuantity(src.getQuantity());
            dest.setStart(FMT.parse(src.getStart()));
            dest.setType(CodeType.from(src.getType().intValue()));
            dest.setNet(src.getNet());
            dest.setGross(src.getGross());
            dest.setPatientShare(src.getPatientShare());
            dest.setCopayment(src.getCopayment());
            dest.setDeductible(src.getDeductible());

            dest.setManualPrices(Boolean.TRUE);
            dest.setOrderingClinician(src.getClinician());
            dest.setPriorAuthorizationID(src.getPriorAuthorizationID());
            dest.setDateOrdered(src.getDateOrdered());
            dest.setVAT(src.getVAT());
            dest.setVATPercent(src.getVATPercent());
            {//Observation start
                List<com.accumed.model.scrubRequest.Observation> obs = new ArrayList();
                for (com.accumed.bre.dha.Observation obsSrc : src.getObservation()) {
                    com.accumed.model.scrubRequest.Observation obsDest = new com.accumed.model.scrubRequest.Observation();

                    obsDest.setActivityID(dest);
                    obsDest.setCode(obsSrc.getCode());
                    obsDest.setId(OBSERVATION_ID.incrementAndGet());
                    obsDest.setIdCaller(req.getClaim().getIdCaller());
                    obsDest.setType(obsSrc.getType());
                    obsDest.setValue(obsSrc.getValue());
                    obsDest.setValueType(obsSrc.getValueType());
                    obs.add(obsDest);

                }
                if (obs != null && !obs.isEmpty()) {
                    dest.setObservation(obs);
                }
            }//Observation end
            acts.add(dest);
        }

    }

    private static ScrubRequest convertbreXclaimToRequestXclaim(List<com.accumed.bre.dha.XClaim> xclaims, ScrubRequest req) throws ParseException {//XClaim
        System.out.println("start creating xcalim list");
        if (xclaims != null && !xclaims.isEmpty()) {
            System.out.println("xcalim list size:"+xclaims.size());
            for (com.accumed.bre.dha.XClaim src : xclaims) {
                com.accumed.model.scrubRequest.xclaim.XClaim dest = new com.accumed.model.scrubRequest.xclaim.XClaim();
                dest.setMemberID(src.getMemberID());
                dest.setProviderID(src.getProviderID());
                dest.setGross(src.getGross());
                dest.setNet(src.getNet());
                dest.setPatientShare(src.getPatientShare());
                //--------added  virtually
                dest.setReceiverID(req.getHeader().getReceiverID());
                dest.setPayerID(req.getHeader().getReceiverID());
                dest.setEmiratesIDNumber(req.getClaim().getEmiratesIDNumber());
                dest.setDateSettlement(req.getClaim().getDateSettlement());
                dest.setIdCaller(req.getClaim().getIdCaller());
                dest.setClaim(req.getClaim());               
                
                ////////////////////////////////////////
                System.out.println("xencounter  found" );
                com.accumed.model.scrubRequest.xclaim.XEncounter enc = new com.accumed.model.scrubRequest.xclaim.XEncounter();
                enc.setPatientID(src.getXEncounter().getPatientID());
                enc.setStart(FMT.parse(src.getXEncounter().getStart()));
                enc.setEnd(FMT.parse(src.getXEncounter().getEnd()));
                enc.setType(src.getXEncounter().getType());
                enc.setStartType(src.getXEncounter().getStartType());
                enc.setEndType(src.getXEncounter().getEndType());              
                
                //--------added  virtually
                enc.setFacilityID(src.getProviderID());
                 
                List<com.accumed.model.scrubRequest.xclaim.XEncounter> listOfXencounter = new ArrayList();
                listOfXencounter.add(enc);
               // printObjectProperties(enc);
                dest.setEncounter(listOfXencounter);             
                ////////////////////////////////////////
                System.out.println("xActivity list size:" + src.getXActivity().size());
                List<com.accumed.model.scrubRequest.xclaim.XActivity> xaList = new ArrayList<>();
                for (XActivity xa : src.getXActivity()) {                   
                    com.accumed.model.scrubRequest.xclaim.XActivity ac = new com.accumed.model.scrubRequest.xclaim.XActivity();
                    ac.setCode(xa.getCode());
                    ac.setClinician(xa.getClinician());
                    ac.setCopayment(xa.getCopayment());
                    ac.setGross(xa.getGross());
                    ac.setNet(xa.getNet());
                    ac.setQuantity(xa.getQuantity());
                    ac.setStart(FMT.parse(xa.getStart()));
                    ac.setType(xa.getType());                                      
                    
                    xaList.add(ac);
                    //printObjectProperties(ac);
                }
                dest.setActivity(xaList);
                ////////////////////////////////////////
                List<com.accumed.model.scrubRequest.xclaim.XDiagnosis> xdList = new ArrayList<>();
                 System.out.println("XDiagnosis found:"+src.getXDiagnosis().size());
                for (XDiagnosis xd : src.getXDiagnosis()) {
                    com.accumed.model.scrubRequest.xclaim.XDiagnosis ad = new com.accumed.model.scrubRequest.xclaim.XDiagnosis();
                    ad.setCode(xd.getCode());
                    ad.setType(xd.getType());
                    xdList.add(ad);
                    //printObjectProperties(ad);
                }
                dest.setDiagnosis(xdList);
                ////////////////////////////////////////
                if (req.getClaim().getXclaim() == null) {
                    List<com.accumed.model.scrubRequest.xclaim.XClaim> listOfXclaim = new ArrayList<>();
                    req.getClaim().setXclaim(listOfXclaim);
                }
                req.getClaim().getXclaim().add(dest);
               // printObjectProperties(dest);

            }
        }
        return req;
    }
     private static ScrubRequest convertbreFClaimToRequestFClaim(List<com.accumed.bre.dha.FClaim> fclaims, ScrubRequest req) throws ParseException {//FClaim
        System.out.println("start creating fcalim list");
        if (fclaims != null && !fclaims.isEmpty()) {
            System.out.println("fcalim list size:"+fclaims.size());
            for (com.accumed.bre.dha.FClaim src : fclaims) {
                com.accumed.model.scrubRequest.fclaim.FClaim dest = new com.accumed.model.scrubRequest.fclaim.FClaim();
                dest.setMemberID(src.getMemberId());
                dest.setProviderID(src.getProviderID());
                dest.setGross(src.getGross());
                dest.setNet(src.getNet());
                dest.setPatientShare(src.getPatientShare());
               // printObjectProperties(dest);
                ////////////////////////////////////////
                System.out.println("fencounter  found" );
                com.accumed.model.scrubRequest.fclaim.FEncounter enc = new com.accumed.model.scrubRequest.fclaim.FEncounter();
                enc.setPatientID(src.getFEncounter().getPatientID());
                enc.setStart(FMT.parse(src.getFEncounter().getStart()));
                enc.setEnd(FMT.parse(src.getFEncounter().getEnd()));
                enc.setType(src.getFEncounter().getType()); 
                List<com.accumed.model.scrubRequest.fclaim.FEncounter> listOfFencounter = new ArrayList();
                listOfFencounter.add(enc);
                //printObjectProperties(enc);
                dest.setEncounter(listOfFencounter);             
                ////////////////////////////////////////
                System.out.println("fActivity list size:" + src.getFActivity().size());
                List<com.accumed.model.scrubRequest.fclaim.FActivity> faList = new ArrayList<>();
                for (FActivity fa : src.getFActivity()) {                   
                    com.accumed.model.scrubRequest.fclaim.FActivity ac = new com.accumed.model.scrubRequest.fclaim.FActivity();
                    ac.setCode(fa.getCode());
                    ac.setClinician(fa.getClinician());
                    ac.setCopayment(fa.getCopayment());
                    ac.setGross(fa.getGross());
                    ac.setNet(fa.getNet());
                    ac.setQuantity(fa.getQuantity());
                    faList.add(ac);
                   // printObjectProperties(ac);
                }
                dest.setActivity(faList);
                ////////////////////////////////////////
                List<com.accumed.model.scrubRequest.fclaim.FDiagnosis> fdList = new ArrayList<>();
                 System.out.println("FDiagnosis found:"+src.getFDiagnosis().size());
                for (FDiagnosis fd : src.getFDiagnosis()) {
                    com.accumed.model.scrubRequest.fclaim.FDiagnosis ad = new com.accumed.model.scrubRequest.fclaim.FDiagnosis();
                    ad.setCode(fd.getCode());
                    ad.setType(fd.getType());
                    fdList.add(ad);
                   // printObjectProperties(ad);
                }
                dest.setDiagnosis(fdList);
                ////////////////////////////////////////
                if (req.getClaim().getFclaim() == null) {
                    List<com.accumed.model.scrubRequest.fclaim.FClaim> listOfFclaim = new ArrayList<>();
                    req.getClaim().setFclaim(listOfFclaim);
                }
                req.getClaim().getFclaim().add(dest);

            }
        }
        return req;
    
    }
//     public static void printObjectProperties(Object obj) {
//        Class<?> objClass = obj.getClass();
//        Field[] fields = objClass.getDeclaredFields();
//
//        for (Field field : fields) {
//            field.setAccessible(true);
//            try {
//                System.out.println(field.getName() + ": " + field.get(obj));
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
