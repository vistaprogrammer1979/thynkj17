/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.pool;

import com.accumed.db.DB;
import com.accumed.model.Context;
import com.accumed.model.scrubRequest.ClaimType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.accumed.model.scrubRequest.ScrubRequest;
import com.accumed.tracing.ManualTracing;
import com.accumed.tracing.OpenTelemetryConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author smutlak
 */
public class Worker {
    private static final Tracer tracer = OpenTelemetryConfig.getOpenTelemetry().getTracer("worker-pool");
    private static String packagesDir = "NONE";
    private static final String PACKAGES[]
            = {"mnec_crosswalk", "billing_ge", "authorization", "billing_protocols","mnec_coding",
            "core_validation", "analyzing", "workflow"};

    private static final String[] WORKFLOW_FACTS
            = {"Clinicians", "Facilities", "Payers"};


    private static final String[] CORE_VALIDATION_FACTS = { "Clinicians", "ClinicianCategories", "InsurerPackages", "Facilities", "Payers",  "RecieverPayers", "FacilityCodeSchema", "FacilityReceiverSchema" ,"CodeGroups"
            ,"HaadCPTCodes",  "HaadDentalCodes", "HaadDRGCodes",  "HaadDrugCodes", "HaadHCPCSCodes",  "HaadICDCodes", "HaadServiceCodes", "DhaCPTCodes",  "DhaDentalCodes",  "DhaDrugCodes","DhaHCPCSCodes", "DhaICDCodes", "DhaServiceCodes","DhaDrugPrices" };



    private static final String[] BILLING_PROTOCOLS_FACTS = {
            "Clinicians", "ClinicianCategories", "Facilities", "MarketFacilities", "Payers", "CodeGroups",
            "FacilityNetworks", "ProfessionDiagnosis", "ActivityQuantityLimit",
            "HaadCPTCodes","HaadDentalCodes","HaadDRGCodes", "HaadDrugCodes", "HaadHCPCSCodes","HaadICDCodes","HaadServiceCodes","DhaCPTCodes","DhaDentalCodes","DhaDrugCodes","DhaHCPCSCodes",  "DhaICDCodes", "DhaServiceCodes","DhaDrugPrices","STTFacilityNetworkMap" };

    private static final String[] AUTHORIZATION_FACTS = {
            "Clinicians", "ClinicianCategories", "Facilities", "Payers", "CodeGroups" };

    private static final String[] BILLING_GE_FACTS = {
            "Clinicians", "ClinicianCategories", "Facilities", "Payers", "CodeGroups" };

    private static final String[] MNEC_CROSSWALK_FACTS = {
            "Clinicians", "ClinicianCategories", "Facilities", "Payers", "CodeGroups",
            "AAPC_CPT_AGE_GENDER_MATERNITY", "AAPC_CPT",  "CrossWalk_CPT" ,"CrosswalkExclusion"};
    //14/May/2019 smutlak   remove --> , "ICDExclusion"

    private static final String[] MNEC_CODING_FACTS = {
            "Clinicians", "ClinicianCategories", "Facilities", "Payers", "CodeGroups", "NaCptsDos","FacilityCodeSchema","AAPC_NCCI_CPTS_List","IncompatibleCodesList","ICDExclusionMaster","ICDContradictories","AddOnCodes" ,"ICDAcuteChronic","ICD10_AdditionalCodes"};
    private static final String[] ANALYZING_FACTS = {
            "Clinicians", "ClinicianCategories", "Facilities", "Payers", "CodeGroups", "NaCptsDos","STTFacilityNetworkMap" };

    private ArrayList<Validator> validators;
    private static com.accumed.re.agents.CachedRepositoryService cachedRepositoryService;
    private com.accumed.re.agents.repo.CachedRepository repo;
    private java.util.Date repoTimestamp;

    static final AtomicLong NEXT_ID = new AtomicLong(0);
    final long uniqueId = NEXT_ID.getAndIncrement();

    static {
        packagesDir = System.getProperty("com.accumed.rules_packages.dir");
        try {
            packagesDir = (String) (new InitialContext().lookup("java:comp/env/com.accumed.rules_packages.dir"));  // from Tomcat's server.xml
        } catch (NamingException ex) {
            packagesDir = "E:/SameerWork/rulesPackages/test";
        }
        packagesDir += '/';
        Logger.getLogger(Worker.class
                .getName()).log(Level.INFO, "com.accumed.rules_packages.dir={0}", packagesDir);
    }

    public static void setCachedRepositoryService(com.accumed.re.agents.CachedRepositoryService cachedRepositoryService) {
        Worker.cachedRepositoryService = cachedRepositoryService;
    }

    public Worker() {
        Logger.getLogger(Worker.class.getName()).
                log(Level.INFO, "+++++++++++ Creating Worker'{'{0}'}'", this.getUniqueId());
        validators = new ArrayList();
        if (Worker.cachedRepositoryService == null) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "Important: Worker Constructor Exception-->Cached Repository Service is not set...");
            return;
        }
        repo = Worker.cachedRepositoryService.getRepo();
//        if (repo == null || !repo.isValid()) {
        if (repo == null) {
            //Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "Cached Repository is not set or invalid...");
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "Important: Worker Constructor Exception-->Cached Repository is not set...");
            return;
        }

        this.repoTimestamp = repo.getTimeStamp();

        try {
            for (String pkg : PACKAGES) {
                String sfile = packagesDir + pkg + ".pkg";
                Method method = Worker.class.getMethod("get" + pkg.toUpperCase() + "_FACTS");
                Logger.getLogger(
                        Worker.class.getName()).log(Level.INFO, "Creating '{'{0}'}' in '{'{1}'}'", new Object[]{pkg, sfile});
                validators.add(new Validator(pkg, sfile, repo, (String[]) method.invoke(null)));
                Logger.getLogger(
                        Worker.class.getName()).log(Level.INFO, "Done creating '{'{0}'}' in '{'{1}'}'", new Object[]{pkg, sfile});
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "Important: Worker Constructor Exception-->" + ex.getMessage());
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }

        Logger.getLogger(Worker.class.getName()).
                log(Level.INFO, "+++++++++++ Worker'{'{0}'}' Created", this.getUniqueId());
    }

    public void reset() {
        for (Validator validator : validators) {
            if (validator.isLoaded()) {
                validator.reset();
            }
        }
    }

    public void cleanse() {
        for (Validator validator : validators) {
            if (validator.isLoaded()) {
                validator.cleanse();
            }
        }
    }

    public Validator getValidator(String name) {
        Logger.getLogger(Worker.class.getName()).log(Level.INFO, "entering getValidator'{'{0}'}'", name);
        for (Validator validator : validators) {
            if (validator.getName().equals(name)) {
                Logger.getLogger(Worker.class.getName()).log(Level.INFO, "exiting getValidator'{'{0}'}'", name);
                return validator;
            }
        }
        String printValidators = "";
        for (Validator validator : validators) {
            printValidators += validator.getName() + ",";
        }
        Logger.getLogger(Worker.class.getName()).log(Level.SEVERE,
                "Unable to find '{'{0}'}' in the validators array, printing current available validators in the array'{'{1}'}'", new Object[]{name, printValidators});
        Logger.getLogger(Worker.class.getName()).log(Level.INFO, "exiting getValidator'{'{0}'}' with null", name);
        return null;
    }


    public ScrubRequest validate(
            ScrubRequest req, DB query_db, boolean excludeDBRules, boolean debug)
            throws Exception {
        Span span = tracer.spanBuilder("Worker.validate").startSpan();

        // Start child span (automatically uses parent context from SOAP call)
        try (ManualTracing.TracingSession tracing = ManualTracing.startSpan("Worker.validate",
                ManualTracing.getCurrentContext())) {

            tracing.addAttribute("worker.id", this.getUniqueId());
            tracing.addAttribute("claim.id", req.getClaim().getIdCaller() != null ?
                    req.getClaim().getIdCaller().toString() : "unknown");
            tracing.addAttribute("exclude.db.rules", excludeDBRules);
            tracing.addAttribute("debug.mode", debug);
            tracing.addAttribute("thread.name", Thread.currentThread().getName());

            Logger.getLogger(Worker.class.getName()).
                    log(Level.INFO, "Worker'{'{0}'}' is processing claim {1}", new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});

            //if analyzing
            if (WorkerUtils.isAnalyzing(req)) {
                Validator mnecValidator = getValidator("analyzing");
                if (mnecValidator.isLoaded()) {
                    req = mnecValidator.validate(req, query_db, excludeDBRules, debug);
                    if(WorkerUtils.isEmptyClaimType(req) ){
                        ClaimType ct=new ClaimType();
                        ct.setType(ClaimType.None);
                        List claimtypelist=  new ArrayList();
                        claimtypelist.add(ct);
                        if (req.getClaim()!=null)
                            req.getClaim().setClaimType(claimtypelist);
                    }
                    Logger.getLogger(Worker.class.getName()).
                            log(Level.INFO, "Worker'{'{0}'}' completed --analyzing-- processing claim {1} ["+req.getClaim().getClaimType().size()+"]", new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                    return req;
                }
            } //end if analyzing
            if(WorkerUtils.isEmptyClaimType(req) )
            {

                Validator mnecValidator = getValidator("analyzing");

                if (mnecValidator.isLoaded()) {
                    java.util.HashSet<String> addedExtendedValidationTypes = new java.util.HashSet<>();
                    WorkerUtils.addContextIfNotExists(addedExtendedValidationTypes, req, Context.Analyzing);
                    java.util.List<ClaimType>  ClaimTypeList=new ArrayList<>();
                    req.getClaim().setClaimType(ClaimTypeList);
                    req = mnecValidator.validate(req, query_db, excludeDBRules, debug);
                    if(WorkerUtils.isEmptyClaimType(req) ){
                        ClaimType ct=new ClaimType();
                        ct.setType(ClaimType.None);
                        List claimtypelist=  new ArrayList();
                        claimtypelist.add(ct);
                        if (req.getClaim()!=null)
                            req.getClaim().setClaimType(claimtypelist);
                    }
                    WorkerUtils.removeAddedContexts(addedExtendedValidationTypes, req);
                    Logger.getLogger(Worker.class.getName()).
                            log(Level.INFO, "Worker'{'{0}'}' first --analyzing-- processing claim {1} ["+req.getClaim().getClaimType().size()+"]", new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                }
            }
            //RULEPLAT-95
            java.util.HashSet<String> addedExtendedValidationTypes = new java.util.HashSet<>();
            if (WorkerUtils.isRULEPLAT_95(req)) {
                WorkerUtils.addContextIfNotExists(addedExtendedValidationTypes, req, Context.Auditing);
                WorkerUtils.addContextIfNotExists(addedExtendedValidationTypes, req, Context.Billing);
                WorkerUtils.addContextIfNotExists(addedExtendedValidationTypes, req, Context.Submission);
            }
            //END RULEPLAT-95

            //if workflow rerouting advice is required.
            if (WorkerUtils.isWorkflow(req)) {
                boolean routed = false;

                req.getHeader().getWorkflow().setWorkflowNextNode(req.getHeader().getWorkflow().getCurrentNode());
                Validator workflowValidator = getValidator("workflow");
                if (workflowValidator.isLoaded()) {
                    req = workflowValidator.validate(req, query_db, excludeDBRules, debug);
                    if (req.getHeader().getWorkflow().getWorkflowNextNode() != null
                            && !WorkerUtils.isSameNode(req.getHeader().getWorkflow().getCurrentNode(), req.getHeader().getWorkflow().getWorkflowNextNode())
                            && !WorkerUtils.isNextDirectNode(req.getHeader().getWorkflow().getCurrentNode(), req.getHeader().getWorkflow().getWorkflowNextNode())) {

                        java.util.HashSet<String> WorkflowAddedExtendedValidationTypes = new java.util.HashSet<>();
                        WorkerUtils.addContextIfNotExists(WorkflowAddedExtendedValidationTypes, req, Context.Auditing);
                        WorkerUtils.addContextIfNotExists(WorkflowAddedExtendedValidationTypes, req, Context.Billing);
                        WorkerUtils.addContextIfNotExists(WorkflowAddedExtendedValidationTypes, req, Context.Submission);

                        //validate in all packages
                        for (Validator validator : validators) {
                            if (!validator.getName().equalsIgnoreCase("workflow")) {
                                if (validator.isLoaded()) {
                                    req = validator.validate(req, query_db, excludeDBRules, debug);
                                }
                            }
                        }

                        if (req.getHeader().getWorkflow().getActionThreshold() != null
                                && !req.ContainsOrAbove(req.getHeader().getWorkflow().getActionThreshold())) {
                            //claim will rerouted
                            routed = true;
                        }
//		                  Logger.getLogger(Worker.class.getName()).
//                            log(Level.INFO, "Worker'{'{0}'}' before removeAddedContexts " + req.getClaim().getClaimType(), new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                        WorkerUtils.removeAddedContexts(WorkflowAddedExtendedValidationTypes, req);
//                    Logger.getLogger(Worker.class.getName()).
//                            log(Level.INFO, "Worker'{'{0}'}' after removeAddedContexts " + req.getClaim().getClaimType(), new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                    }
                }

                if (!routed) {
                    req.getHeader().getWorkflow().setRoutingRule(null);
                    //	Logger.getLogger(Worker.class.getName()).
                    //log(Level.INFO, "Worker'{'{0}'}' before removeAllOutcomes "+req.getClaim().getClaimType(), new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                    req.removeAllOutcomes();
                    //Logger.getLogger(Worker.class.getName()).
                    //        log(Level.INFO, "Worker'{'{0}'}' after removeAllOutcomes " + req.getClaim().getClaimType(), new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                    for (Validator validator : validators) {
                        if (validator.isLoaded()) {
                            validator.cleanse();
                        }
                    }
                } else {
                    Logger.getLogger(Worker.class.getName()).
                            log(Level.INFO, "Worker'{'{0}'}' completed --workflow rerouting-- processing claim {1}", new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});
                    return req;
                }
            }

            for (Validator validator : validators) {
                if (!validator.getName().equalsIgnoreCase("workflow") && !validator.getName().equalsIgnoreCase("analyzing") ) {
                    if(req.getRestrictPackagesList() ==null || req.getRestrictPackagesList().size()==0 )
                    {
                        if (validator.isLoaded()) {
                            req = validator.validate(req, query_db, excludeDBRules, debug);
                        }
                    }
                    else if(req.getRestrictPackagesList().contains(validator.getName()))
                    {
                        if (validator.isLoaded()) {
                            req = validator.validate(req, query_db, excludeDBRules, debug);
                        }
                    }
                }
            }

            if (req != null && req.getHeader() != null && req.getClaim() != null
                    && req.getHeader().getWorkflow() != null && req.getHeader().getWorkflow().getCurrentNode() != null) {
                if (req.ContainsOrAbove(req.getHeader().getWorkflow().getActionThreshold())) {
                    req.getHeader().getWorkflow().setWorkflowNextNode(req.getHeader().getWorkflow().getCurrentNode());
                } else {
                    req.getHeader().getWorkflow().setWorkflowNextNode(WorkerUtils.getNextNode(req.getHeader().getWorkflow().getCurrentNode()));
                }
                req.getHeader().getWorkflow().setRoutingRule(null);
            }

            Logger.getLogger(Worker.class.getName()).
                    log(Level.INFO, "Worker'{'{0}'}' completed processing claim {1}", new Object[]{this.getUniqueId(), req.getClaim().getIdCaller()});

            long endTime = System.nanoTime();
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();


            return req;
        } catch (Exception e) {
            // Error is automatically recorded in the TracingSession
            throw e;
        }
    }


    public java.util.Date getRepoTimeStamp() {
        return repoTimestamp;
    }

    public boolean isNewPackageExisted() {
        for (Validator validator : validators) {
            if (validator.isNewPackageExisted()) {
                return true;
            }
        }
        return false;
    }

    public boolean isDirty() {
        for (Validator validator : validators) {
            if (validator.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public boolean cleanInjectedData() {
        for (Validator validator : validators) {
            if (validator.cleanInjectedData()) {
                return true;
            }
        }
        return false;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public static String getPackagesDir() {
        return packagesDir;
    }

    public static String[] getWORKFLOW_FACTS() {
        return WORKFLOW_FACTS;
    }

//    public static String[] getACCUMED_PAYER_FACTS() {
//        return ACCUMED_PAYER_FACTS;
//    }

    public static String[] getCORE_VALIDATION_FACTS() {
        return CORE_VALIDATION_FACTS;
    }

//    public static String[] getACCUMED_GENERAL_FACTS() {
//        return ACCUMED_GENERAL_FACTS;
//    }

    public static String[] getBILLING_PROTOCOLS_FACTS() {
        return BILLING_PROTOCOLS_FACTS;
    }

    public static String[] getAUTHORIZATION_FACTS() {
        return AUTHORIZATION_FACTS;
    }

    public static String[] getBILLING_GE_FACTS() {
        return BILLING_GE_FACTS;
    }

    public static String[] getMNEC_CROSSWALK_FACTS() {
        return MNEC_CROSSWALK_FACTS;
    }
    public static String[] getMNEC_CODING_FACTS() {
        return MNEC_CODING_FACTS;
    }
    public static String[] getANALYZING_FACTS() {
        return ANALYZING_FACTS;
    }

    public static JSONArray getRules(String packageName) {
        if (packageName.trim().isEmpty()) {
            JSONArray array = new JSONArray();
            for (String pkg : PACKAGES) {
                JSONArray arr = WorkerUtils.getPackageRules(packagesDir + pkg + ".pkg");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject jsonObject = arr.getJSONObject(i);
                    array.put(jsonObject);
                }
            }
            return array;
        } else {
            return WorkerUtils.getPackageRules(packagesDir + packageName + ".pkg");
        }
    }

    public JSONArray GetPackages() {

        JSONArray array = new JSONArray();
        for (Validator validator : validators) {
            JSONObject obj = new JSONObject();
            obj.put("name", validator.getName());
            obj.put("isLoaded", validator.isLoaded());
            array.put(obj);
        }
        return array;
    }

    public java.util.Date getNewestValidatorTimestamp() {
        long ret = 0;
        for (Validator validator : validators) {
            if (validator != null) {
                if (validator.getIsLoaded()) {
                    if (ret == 0 || ret < validator.getPkgTimestamp()) {
                        ret = validator.getPkgTimestamp();
                    }
                }
            }
        }
        if (ret > 0) {
            return new java.util.Date(ret);
        } else {
            return new java.util.Date(0);
        }
    }
}
