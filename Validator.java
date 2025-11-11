/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.pool;

import com.accumed.model.Context;
import com.accumed.model.scrubRequest.Activity;
import com.accumed.model.scrubRequest.Claim;
import com.accumed.model.scrubRequest.Header;
import com.accumed.model.scrubRequest.ScrubRequest;
import com.accumed.tracing.ManualTracing;
import com.accumed.tracing.OpenTelemetryConfig;
import com.accumed.validation.*;
import com.accumed.db.DB;
import com.accumed.model.ControlRank;
import com.accumed.re.agents.repo.CachedData;
import com.accumed.re.agents.repo.CachedRepository;
import com.accumed.webservices.AccumedValidatorWS;
import com.accumed.webservices.Statistics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.drools.KnowledgeBase;
import org.drools.definition.rule.Rule;
import org.drools.event.rule.ActivationCancelledEvent;
import org.drools.event.rule.ActivationCreatedEvent;
import org.drools.event.rule.AfterActivationFiredEvent;
import org.drools.event.rule.BeforeActivationFiredEvent;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

/**
 *
 * @author smutlak
 */
public class Validator {

    private String pkgFile;
    private ArrayList<Pair<Integer, Integer>> ranks;
    private Long pkgTimestamp;
    private String name;
    private KnowledgeBase knowledgeBase;
    private List<String> rulesNames;
    private StatefulKnowledgeSession session;

    java.util.List<FactHandle> fhContext;
    FactHandle fhDB;
    FactHandle fhHeader;
    FactHandle fhClaim;
    java.util.List<FactHandle> fhActivities;
    FactHandle fhControlRank;
    private static final Tracer tracer = OpenTelemetryConfig.getOpenTelemetry().getTracer("validator");
    public Validator(String name, String pkgFile, CachedRepository repo, String[] factSets) {
        this(name, pkgFile, repo, factSets, null);
    }

    public Validator(String name, String pkgFile, CachedRepository repo, String[] factSets,
                     ArrayList<Pair<Integer, Integer>> ranks) {
        this.name = name;
        this.pkgFile = pkgFile;
        this.ranks = ranks;
        if (this.loadPackage()) {
            this.session = knowledgeBase.newStatefulKnowledgeSession();
            this.insertRepo(repo, factSets);
        }
    }

    public final boolean insertRepo(CachedRepository repo, String[] factSets) {
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, "entering insertRepo({0})", name);
        long lBegin = System.nanoTime();
        if (session == null) {
            return false;
        }


        for (String factSet : factSets) {
            CachedData cachedData = repo.getCachedDB().get(factSet);
            if (cachedData != null) {
                Logger.getLogger(
                        Validator.class.getName()).log(Level.INFO,
                        "+ Validator'{'{0}'}' Insert {1} Into Working Memory, "
                                + "size={2}", new Object[]{this.pkgFile.substring(this.pkgFile.lastIndexOf("/") + 1),
                                cachedData.getLogicalName(),
                                cachedData.getData().size()});

                for (Object obj : cachedData.getData()) {
                    session.insert(obj);
                }
            } else {
                Logger.getLogger(Validator.class
                        .getName()).log(Level.SEVERE, "FactSet={0} was not not found !!!!", factSet);
            }

        }


        Logger.getLogger(Validator.class.getName()).log(Level.INFO, "exiting insertRepo({0}){1}", new Object[]{name, WorkerUtils.getTakenTime(lBegin)});
        return true;
    }

    public boolean isNewPackageExisted() {
        File fFile = null;
        fFile = new java.io.File(this.pkgFile);
        if (fFile.exists()) {
            if (pkgTimestamp == null) {
                return true;
            }
            if (pkgTimestamp < fFile.lastModified()) {
                return true;
            }
        }
        return false;
    }

    protected final boolean loadPackage() {
        Logger.getLogger(Validator.class.getName()).info("entering loadPackage");
        if (session != null) {
            session.dispose();
            session = null;
        }
        java.io.File fFile;
        fFile = new java.io.File(this.pkgFile);
        if (fFile.exists()) {
            knowledgeBase = null;
            knowledgeBase = WorkerUtils.createKnowledgeBase(this.pkgFile);
            getPackageRules();
            pkgTimestamp = fFile.lastModified();
        } else {
            Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, "loadPackage '{'{0}'}' failed, file is not exists", this.pkgFile);
            return false;
        }
        Logger.getLogger(Validator.class.getName()).info("exiting loadPackage");
        return true;
    }

    public boolean isLoaded() {
        if (this.knowledgeBase != null && this.session != null) {
            return true;
        }
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0} is not loaded", name);
        return false;
    }

    public void reset() {
        cleanse();
    }

    public void cleanse() {
        long ltime = System.currentTimeMillis();
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0} cleanse", name);

        if (session != null) {
            if (fhControlRank != null) {
                session.retract(fhControlRank);
                fhControlRank = null;
            }

            if (fhActivities != null) {
                for (FactHandle tmp : fhActivities) {
                    session.retract(tmp);
                    //fhActivities.remove(tmp);
                }
                fhActivities = null;
            }

            if (fhClaim != null) {
                session.retract(fhClaim);
                fhClaim = null;
            }

            if (fhHeader != null) {
                session.retract(fhHeader);
                fhHeader = null;
            }

            if (fhDB != null) {
                session.retract(fhDB);
                fhDB = null;
            }

            if (fhContext != null) {
                for (FactHandle tmp : fhContext) {
                    session.retract(tmp);
                }
                fhContext = null;
            }
        }
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, name + " cleanse completed time ms:{0}", System.currentTimeMillis() - ltime);
    }

    public boolean isDirty() {
        long ltime = System.currentTimeMillis();
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0}::isDirty", name);
        if (session != null) {
            if (WorkerUtils.isFactExisted(session, ControlRank.class)) {
                return true;
            }
            if (WorkerUtils.isFactExisted(session, com.accumed.model.scrubRequest.Activity.class)) {
                return true;
            }
            if (WorkerUtils.isFactExisted(session, com.accumed.model.scrubRequest.Claim.class)) {
                return true;
            }
            if (WorkerUtils.isFactExisted(session, com.accumed.model.scrubRequest.Header.class)) {
                return true;
            }
            if (WorkerUtils.isFactExisted(session, com.accumed.db.DB.class)) {
                return true;
            }
            if (WorkerUtils.isFactExisted(session, com.accumed.model.Context.class)) {
                return true;
            }
        }
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, name + "::isDirty completed time ms:{0}", System.currentTimeMillis() - ltime);
        return false;
    }

    public boolean cleanInjectedData() {
        long ltime = System.currentTimeMillis();
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0} cleanInjectedData", name);
        if (session != null) {
            if (WorkerUtils.deleteFacts(session, ControlRank.class)) {
                return true;
            }
            if (WorkerUtils.deleteFacts(session, Activity.class)) {
                return true;
            }
            if (WorkerUtils.deleteFacts(session, Claim.class)) {
                return true;
            }
            if (WorkerUtils.deleteFacts(session, Header.class)) {
                return true;
            }
            if (WorkerUtils.deleteFacts(session, DB.class)) {
                return true;
            }
            if (WorkerUtils.deleteFacts(session, Context.class)) {
                return true;
            }
        }
        Logger.getLogger(Validator.class.getName()).log(Level.INFO, name + "cleanInjectedData completed time ms:{0}", System.currentTimeMillis() - ltime);
        return false;
    }

    //OLD
    public void dispose() {
        Logger.getLogger(Validator.class.getName()).info("In dispose");
        if (this.session != null) {
            Logger.getLogger(Validator.class.getName()).info("disposing session");
            this.session.dispose();
            this.session = null;
        }
    }



//    public ScrubRequest validate(ScrubRequest req,
//                                 DB query_db, boolean excludeDBRules, boolean debug)
//            throws Exception {
//        try (ManualTracing.TracingSession tracing = ManualTracing.startSpan("Validator.for Package: " + this.getName(),
//                ManualTracing.getCurrentContext())) {
//
//            tracing.addAttribute("validator.name", this.getName());
//            tracing.addAttribute("validator.package", this.pkgFile);
//            tracing.addAttribute("claim.id", req.getClaim().getIdCaller() != null ?
//                    req.getClaim().getIdCaller().toString() : "unknown");
//            tracing.addAttribute("exclude.db.rules", excludeDBRules);
//            tracing.addAttribute("debug.mode", debug);
//            tracing.addAttribute("thread.name", Thread.currentThread().getName());
//
//            // Add rule package metrics
//            addRulePackageMetrics(tracing);
//            Logger.getLogger(Validator.class.getName()).log(Level.INFO, "entering {0}.validate start", getName());
//
//            if (session == null) {
//                return req;  //package is not loaded
//            }
//
//            long lBegin = 0;
//            try {
//
//
//                fhContext = WorkerUtils.insertContext(req, session);
//                if (!excludeDBRules && false) {
//                    fhDB = session.insert(query_db);
//                }
//
//                fhHeader = session.insert(req.getHeader());
//
//                fhClaim = session.insert(req.getClaim());
//
//                fhActivities = new ArrayList<>();
//                for (com.accumed.model.scrubRequest.Activity act : req.getClaim().getActivity()) {
//                    fhActivities.add(session.insert(act));
//                }
//
//                if (debug) {
//                    for (int severityType = com.accumed.model.Severity.getiBLOCKER();
//                         severityType <= com.accumed.model.Severity.getiFINEST(); severityType++) {
//                        for (int rankcnt = 0; rankcnt < 60; rankcnt++) {
//                            Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0}. Starting Rank={1}, Type={2})", new Object[]{getName(), rankcnt, severityType,});
//                            lBegin = System.nanoTime();
//                            this.fhControlRank = session.insert(new ControlRank(severityType, rankcnt));
//                            session.fireAllRules();
    ////                        try {
    ////                            Thread.currentThread().sleep(50);
    ////                        } catch (InterruptedException e) {
    ////                        }
//                            Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0}. Rank={1}, Type={2}, validate Done({3}ns)", new Object[]{getName(), rankcnt, severityType,
//                                    (Long.valueOf(System.nanoTime() - lBegin)).toString()});
//                            session.retract(this.fhControlRank);
//                        }
//                    }
//                } else {
//                    this.fhControlRank = session.insert(new ControlRank(-1, -1));
//                    session.fireAllRules();
//                }
//
//                Logger.getLogger(Validator.class.getName()).log(Level.INFO, "{0}.validate Done({1}ms)", new Object[]{getName(), (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString()});
//            } /*catch(java.lang.reflect.InvocationTargetException e){
//            Statistics.addException(e, req);
//            Logger.getLogger(MnecValidator.class.getName()).log(Level.SEVERE, e.getMessage(), e);
//            throw e;
//        }*/ catch (org.drools.runtime.rule.ConsequenceException e) {
//                tracing.setError(e);
//                Statistics.addException(e, req);
//                Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, e.getMessage(), e);
//                throw e;
//            } catch (org.drools.RuntimeDroolsException e) {
//                tracing.setError(e);
//                Statistics.addException(e, req);
//                Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, e.getMessage(), e);
//                throw e;
//            } catch (Exception e) {
//                tracing.setError(e);
//                Statistics.addException(e, req);
//                Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, e.getMessage(), e);
//                throw e;
//            } finally {
//
//            }
//            Utils.setReferenceLinks(req, AccumedValidatorWS.ruleRefrences);
//            Utils.addPackageNameToOutcome(req, Utils.getPackageTradeName(this.getName()), getPackageRules());
//            Logger.getLogger(Validator.class.getName()).log(Level.INFO, "entering {0}.validate Done2({1}ms)", new Object[]{getName(), (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString()});
//            long endTime = System.nanoTime();
//            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//
//
//            return req;
//        }
//    }
    public ScrubRequest validate(ScrubRequest req, DB query_db, boolean excludeDBRules, boolean debug) throws Exception {

        // Start child span for this validator
        try (ManualTracing.TracingSession tracing = ManualTracing.startSpan("Validator. For Package: " + this.getName(),
                ManualTracing.getCurrentContext())) {

            tracing.addAttribute("validator.name", this.getName());
            tracing.addAttribute("validator.package", this.pkgFile);
            tracing.addAttribute("claim.id", req.getClaim().getIdCaller() != null ?
                    req.getClaim().getIdCaller().toString() : "unknown");
            tracing.addAttribute("exclude.db.rules", excludeDBRules);
            tracing.addAttribute("debug.mode", debug);
            tracing.addAttribute("thread.name", Thread.currentThread().getName());

            // Add rule package metrics
            addRulePackageMetrics(tracing);

            Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                    "Validator '{'{0}'}' starting - Trace: {1}",
                    new Object[]{getName(), tracing.getTraceId()});

            if (session == null) {
                tracing.setError(new IllegalStateException("Drools session is null"));
                return req;  // package is not loaded
            }

            long validationStartTime = System.nanoTime();

            try {
                // Phase 1: Fact Insertion with detailed tracing
                insertFactsWithTracing(req, query_db, excludeDBRules, tracing);

                // Phase 2: Rule Execution with detailed tracing
                executeRulesWithTracing(debug, tracing);

                // Phase 3: Post-processing
                postProcessWithTracing(req, tracing);

                long totalDurationMs = (System.nanoTime() - validationStartTime) / 1_000_000;
                tracing.addAttribute("total.validation.duration.ms", totalDurationMs);
                tracing.addAttribute("validation.success", true);

                Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                        "Validator '{'{0}'}' completed in {1}ms - Trace: {2}",
                        new Object[]{getName(), totalDurationMs, tracing.getTraceId()});

                return req;

            } catch (Exception e) {
                long errorDurationMs = (System.nanoTime() - validationStartTime) / 1_000_000;
                tracing.addAttribute("total.validation.duration.ms", errorDurationMs);
                tracing.setError(e);
                Statistics.addException(e, req);
                Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                throw e;
            }
        }
    }


    private void executeRulesWithTracing(boolean debug, ManualTracing.TracingSession tracing) {

        long rulesExecutionStart = System.nanoTime();
        int totalRulesFired = 0;
        int totalRuleExecutions = 0;

        try {
            Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                    "{0} - Starting rule execution, debug: {1}",
                    new Object[]{getName(), debug});

            if (debug) {
                // Debug mode: trace each severity type and rank
                int severityTypes = 0;
                for (int severityType = com.accumed.model.Severity.getiBLOCKER();
                     severityType <= com.accumed.model.Severity.getiFINEST(); severityType++) {
                    severityTypes++;

                    for (int rankcnt = 0; rankcnt < 60; rankcnt++) {
                        Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                                "{0}. Starting Rank={1}, Type={2})",
                                new Object[]{getName(), rankcnt, severityType});

                        long ruleBatchStart = System.nanoTime();
                        this.fhControlRank = session.insert(new ControlRank(severityType, rankcnt));
                        session.fireAllRules();
                        session.retract(this.fhControlRank);

                        long ruleBatchDurationMs = (System.nanoTime() - ruleBatchStart) / 1_000_000;
                        totalRuleExecutions++;

                        tracing.addAttribute("rules.debug.rank." + rankcnt + ".type." + severityType + ".duration.ms",
                                ruleBatchDurationMs);

                        Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                                "{0}. Rank={1}, Type={2}, validate Done({3}ms)",
                                new Object[]{getName(), rankcnt, severityType, ruleBatchDurationMs});
                    }
                }

                tracing.addAttribute("rules.debug.severity.types", severityTypes);
                tracing.addAttribute("rules.debug.total.executions", totalRuleExecutions);
                tracing.addAttribute("rules.execution.mode", "debug");

            } else {
                // Normal mode: single execution
                long rulesStart = System.nanoTime();
                this.fhControlRank = session.insert(new ControlRank(-1, -1));
                session.fireAllRules();
                long rulesDurationMs = (System.nanoTime() - rulesStart) / 1_000_000;

                tracing.addAttribute("rules.execution.duration.ms", rulesDurationMs);
                tracing.addAttribute("rules.execution.mode", "normal");
              //  tracing.addAttribute("rules.total.fired", getRulesFiredCount(session)); // Estimated

                Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                        "{0}.validate rules executed in {1}ms",
                        new Object[]{getName(), rulesDurationMs});
            }

            long totalRulesDurationMs = (System.nanoTime() - rulesExecutionStart) / 1_000_000;
            tracing.addAttribute("rules.total.duration.ms", totalRulesDurationMs);

        } catch (Exception e) {
            long rulesDurationMs = (System.nanoTime() - rulesExecutionStart) / 1_000_000;
            tracing.addAttribute("rules.total.duration.ms", rulesDurationMs);
            tracing.addAttribute("rules.executions.before.error", totalRuleExecutions);
            throw e;
        }
    }

    private void insertFactsWithTracing(ScrubRequest req, DB query_db, boolean excludeDBRules,
                                        ManualTracing.TracingSession tracing) {

        long factInsertionStart = System.nanoTime();
        int totalFactsInserted = 0;

        try {
            Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                    "{0} - Starting fact insertion", getName());

            // Insert context facts
            fhContext = WorkerUtils.insertContext(req, session);
            if (fhContext != null) {
                totalFactsInserted += fhContext.size();
                tracing.addAttribute("facts.context.count", fhContext.size());
            }

            // Insert DB fact if needed
            if (!excludeDBRules && false) { // Your existing condition
                fhDB = session.insert(query_db);
                totalFactsInserted++;
                tracing.addAttribute("facts.db.inserted", true);
            }

            // Insert header fact
            fhHeader = session.insert(req.getHeader());
            totalFactsInserted++;
            tracing.addAttribute("facts.header.inserted", true);

            // Insert claim fact
            fhClaim = session.insert(req.getClaim());
            totalFactsInserted++;
            tracing.addAttribute("facts.claim.inserted", true);

            // Insert activities
            fhActivities = new ArrayList<>();
            if (req.getClaim().getActivity() != null) {
                for (com.accumed.model.scrubRequest.Activity act : req.getClaim().getActivity()) {
                    fhActivities.add(session.insert(act));
                    totalFactsInserted++;
                }
                tracing.addAttribute("facts.activities.count", fhActivities.size());
            }

            long factInsertionDurationMs = (System.nanoTime() - factInsertionStart) / 1_000_000;
            tracing.addAttribute("facts.total.inserted", totalFactsInserted);
            tracing.addAttribute("facts.insertion.duration.ms", factInsertionDurationMs);
            tracing.addAttribute("facts.insertion.rate",
                    factInsertionDurationMs > 0 ? totalFactsInserted / (factInsertionDurationMs / 1000.0) : 0);

            Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                    "{0} - Inserted {1} facts in {2}ms",
                    new Object[]{getName(), totalFactsInserted, factInsertionDurationMs});

        } catch (Exception e) {
            long factInsertionDurationMs = (System.nanoTime() - factInsertionStart) / 1_000_000;
            tracing.addAttribute("facts.insertion.duration.ms", factInsertionDurationMs);
            tracing.addAttribute("facts.inserted.before.error", totalFactsInserted);
            throw e;
        }
    }
    private void postProcessWithTracing(ScrubRequest req, ManualTracing.TracingSession tracing) {

        long postProcessStart = System.nanoTime();

        try {
            // Set reference links
            Utils.setReferenceLinks(req, AccumedValidatorWS.ruleRefrences);

            // Add package name to outcome
            List<String> packageRules = getPackageRules();
            Utils.addPackageNameToOutcome(req, Utils.getPackageTradeName(this.getName()), packageRules);

            // Record outcome metrics
            if (req.getClaim() != null && req.getClaim().getOutcome() != null) {
                int outcomeCount = req.getClaim().getOutcome().size();
                tracing.addAttribute("validation.outcomes.count", outcomeCount);
            }

            long postProcessDurationMs = (System.nanoTime() - postProcessStart) / 1_000_000;
            tracing.addAttribute("post.process.duration.ms", postProcessDurationMs);

        } catch (Exception e) {
            long postProcessDurationMs = (System.nanoTime() - postProcessStart) / 1_000_000;
            tracing.addAttribute("post.process.duration.ms", postProcessDurationMs);
            tracing.addAttribute("post.process.error", e.getMessage());
            // Don't rethrow - post-processing errors shouldn't fail the whole validation
            Logger.getLogger(Validator.class.getName()).log(Level.WARNING,
                    "Post-processing error in {0}: {1}",
                    new Object[]{getName(), e.getMessage()});
        }
    }

    /**
     * Add rule package metrics to tracing
     */
    private void addRulePackageMetrics(ManualTracing.TracingSession tracing) {
        try {
            if (knowledgeBase != null) {
                List<String> rules = getPackageRules();
                if (rules != null) {
                    tracing.addAttribute("rules.package.total", rules.size());
                    tracing.addAttribute("rules.package.name", this.getName());
                }

                // Add knowledge base metrics
                tracing.addAttribute("knowledge.base.loaded", true);
                tracing.addAttribute("package.file", this.pkgFile);

                if (pkgTimestamp != null) {
                    tracing.addAttribute("package.timestamp", pkgTimestamp);
                    tracing.addAttribute("package.age.hours",
                            (System.currentTimeMillis() - pkgTimestamp) / (1000 * 60 * 60));
                }
            } else {
                tracing.addAttribute("knowledge.base.loaded", false);
                tracing.addAttribute("rules.package.total", 0);
            }
        } catch (Exception e) {
            Logger.getLogger(Validator.class.getName()).log(Level.WARNING,
                    "Failed to collect rule package metrics: {0}", e.getMessage());
        }
    }


    private List<String> getPackageRules() {
        if (rulesNames == null || rulesNames.size() == 0) {
            if (knowledgeBase != null) {
                rulesNames = new ArrayList<String>();
                try {
                    List<Rule> rules = new ArrayList<Rule>(knowledgeBase.getKnowledgePackage(name).getRules());
                    for (Rule rule : rules) {
                        rulesNames.add(rule.getName());
                    }

                    Logger.getLogger(Validator.class.getName()).log(Level.INFO,
                            "{0} - Loaded {1} rules from package",
                            new Object[]{getName(), rulesNames.size()});

                } catch (Exception e) {
                    Logger.getLogger(Validator.class.getName()).log(Level.WARNING,
                            "Failed to load rules for package {0}: {1}",
                            new Object[]{name, e.getMessage()});
                }
            }
        }
        return rulesNames;
    }
    //    protected final void resetContexts() {
//        if (session != null) {
//            WorkerUtils.deleteFacts(session, com.accumed.model.Context.class);
//        }
//    }
//    protected final void resetInjectedData() {
//
//        if (session != null) {
//            WorkerUtils.deleteFacts(session, ControlRank.class);
//            WorkerUtils.deleteFacts(session, com.accumed.model.scrubRequest.Activity.class);
//            WorkerUtils.deleteFacts(session, com.accumed.model.scrubRequest.Claim.class);
//            WorkerUtils.deleteFacts(session, com.accumed.model.scrubRequest.Header.class);
//            WorkerUtils.deleteFacts(session, com.accumed.db.DB.class);
//            WorkerUtils.deleteFacts(session, com.accumed.model.Context.class);
//        }
//    }
    @Override
    protected void finalize() throws Throwable {
        Logger.getLogger(Validator.class.getName()).info("In finalize");
        dispose();
        super.finalize();
    }

    //    public boolean isValid() {
//        return valid;
//    }
//
//    public void setValid(boolean valid) {
//        this.valid = valid;
//    }
//
//    public boolean isPackageChanged() {
//        return packageChanged;
//    }
//    public void setPackageChanged(boolean packageChanged) {
//        this.packageChanged = packageChanged;
//    }
    protected class Thread_Reprepare extends Thread {

        protected DB query_db;
        protected Validator validator;

        public Thread_Reprepare(DB query_db, Validator validator) {
            this.query_db = query_db;
            this.validator = validator;
        }

//        @Override
//        public void run() {
//            validator.reprepare(this.query_db);
//        }
    }
    public static long startTime;
    public static long startTime2;

    private void addSessionListener() {

        // session.addEventListener( new DebugAgendaEventListener() );
        //  session.addEventListener( new DebugWorkingMemoryEventListener() );
        session.addEventListener(new org.drools.event.rule.DefaultAgendaEventListener() {

            /* @Override
             public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
            Logger.getLogger(Validator.class.getName()).log(
                Level.INFO,"afterRuleFlowGroupDeactivated="+event.getRuleFlowGroup().getName()+"="+System.nanoTime());
             super.afterRuleFlowGroupDeactivated(event); //To change body of generated methods, choose Tools | Templates.
             }

             @Override
             public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
            Logger.getLogger(Validator.class.getName()).log(
                Level.INFO,"beforeRuleFlowGroupDeactivated="+event.getRuleFlowGroup().getName()+"="+System.nanoTime());
             super.beforeRuleFlowGroupDeactivated(event); //To change body of generated methods, choose Tools | Templates.
             }

             @Override
             public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
             Logger.getLogger(Validator.class.getName()).log(
                Level.INFO,"afterRuleFlowGroupActivated="+event.getRuleFlowGroup().getName()+"="+System.nanoTime());
             super.afterRuleFlowGroupActivated(event); //To change body of generated methods, choose Tools | Templates.
             }

             @Override
             public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
             Logger.getLogger(Validator.class.getName()).log(
                Level.INFO,"beforeRuleFlowGroupActivated="+event.getRuleFlowGroup().getName()+"="+System.nanoTime());
             super.beforeRuleFlowGroupActivated(event); //To change body of generated methods, choose Tools | Templates.
             }
             */
            @Override
            public void beforeActivationFired(BeforeActivationFiredEvent event) {
                Logger.getLogger(Validator.class.getName()).info("beforeActivationFired=" + event.getActivation().getRule().getName() + "=" + System.nanoTime());
                super.beforeActivationFired(event); //To change body of generated methods, choose Tools | Templates.
            }

            /* @Override
             public void agendaGroupPushed(AgendaGroupPushedEvent event) {
             Logger.getLogger(Validator.class.getName()).log(
                Level.INFO,"agendaGroupPushed="+event.getAgendaGroup().getName()+"="+System.nanoTime());
             super.agendaGroupPushed(event); //To change body of generated methods, choose Tools | Templates.
             }

             @Override
             public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
             Logger.getLogger(Validator.class.getName()).log(
                Level.INFO,"agendaGroupPopped="+event.getAgendaGroup().getName()+"="+System.nanoTime());
             super.agendaGroupPopped(event); //To change body of generated methods, choose Tools | Templates.
             }
             */
            @Override
            public void afterActivationFired(AfterActivationFiredEvent event) {
                Logger.getLogger(Validator.class.getName()).log(Level.INFO, "afterActivationFired={0}={1}", new Object[]{event.getActivation().getRule().getName(), System.nanoTime()});
                super.afterActivationFired(event); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void activationCreated(ActivationCreatedEvent event) {
                Logger.getLogger(Validator.class.getName()).log(Level.INFO, "activationCreated={0}={1}", new Object[]{event.getActivation().getRule().getName(), System.nanoTime()});
                super.activationCreated(event); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void activationCancelled(ActivationCancelledEvent event) {
                Logger.getLogger(Validator.class.getName()).log(Level.INFO, "activationCancelled={0}={1}", new Object[]{event.getActivation().getRule().getName(), System.nanoTime()});
                super.activationCancelled(event); //To change body of generated methods, choose Tools | Templates.
            }

        });

    }

    public Boolean getIsLoaded() {
        return this.session != null;
    }

    public String getName() {
        return name;
    }

    public Long getPkgTimestamp() {
        return pkgTimestamp;
    }

}
