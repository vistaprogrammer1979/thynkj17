/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents;

import com.accumed.re.agents.repo.CachedData;
import com.accumed.re.agents.repo.CachedRepository;
import com.accumed.re.pool.WorkersFactory;
import com.accumed.tracing.ManualTracing;
import com.accumed.tracing.OpenTelemetryConfig;
import com.accumed.webservices.AccumedValidatorWS;
import com.accumed.webservices.BackgroundTaskManager;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author smutlak
 */
public class CachedRepositoryService implements Runnable {

//    @Resource(name = "pricingDB")
//    private Connection pricingDB;
    private CachedRepository repo;
    private Date repoDay;
    private static final Tracer tracer = OpenTelemetryConfig.getOpenTelemetry().getTracer("cached-repository-service");


    public CachedRepositoryService() {

    }


    @Override
    public void run() {
        // Use root span for background task
        try (com.accumed.tracing.ManualTracing.TracingSession tracing =
                     com.accumed.tracing.ManualTracing.startRootSpan("CachedRepositoryService.run")) {

            tracing.addAttribute("background.task", true);
            tracing.addAttribute("thread.name", Thread.currentThread().getName());
            tracing.addAttribute("task.type", "scheduled.repository.sync");

            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                    "CachedRepository task running at {0}", new Date());



            long operationStartTime = System.nanoTime();

            try {
                if (repo == null) {
                    tracing.addAttribute("operation", "initial.load");
                    logger.info("Initializing new repository...");

                    repo = new CachedRepository();
                    initialize(null);
                    populate();

                    tracing.addAttribute("repository.initialized", true);
                    logger.info("Repository initialized successfully");

                } else {
                    tracing.addAttribute("operation", "incremental.sync");

                    if (repo.isValid()
                            && AccumedValidatorWS.getWorkersPool().getNumActive() <= AccumedValidatorWS.getWorkersPool().getNumIdle()) {

                        int tablesChangedCount = checkSynchronization();
                        tracing.addAttribute("tables.checked", tablesChangedCount);

                        Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                                "Check Cached Repository returned {0} out of date tables.", tablesChangedCount);

                        if (repoDay == null) {
                            repoDay = RepoUtils.getMidnightYesterday();
                            tracing.addAttribute("repo.day.initialized", true);
                        } else if (repoDay.before(RepoUtils.getMidnightYesterday())) {
                            tracing.addAttribute("operation", "midnight.refresh");
                            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                                    "***** Midnight refresh --> Refreshing the whole repository ");

                            repoDay = RepoUtils.getMidnightYesterday();
                            repo = null;
                            repo = new CachedRepository();
                            initialize(null);
                            populate();

                            tracing.addAttribute("midnight.refresh.completed", true);
                        }
                    } else if (AccumedValidatorWS.getWorkersPool().getNumActive() <= AccumedValidatorWS.getWorkersPool().getNumIdle()) {
                        tracing.addAttribute("operation", "resynchronize");
                        Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                                "Cached Repository is invalid re-synchronize...");

                        int tablesChangedCount = reSynchronize();
                        tracing.addAttribute("tables.resynchronized", tablesChangedCount);

                        Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                                "reSynchronizing {0} tables.", tablesChangedCount);
                    }
                }

                long operationDurationMs = (System.nanoTime() - operationStartTime) / 1_000_000;
                tracing.addAttribute("operation.duration.ms", operationDurationMs);
                tracing.addAttribute("success", true);

                Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                        "CachedRepository task completed in {0}ms", operationDurationMs);

            } catch (Throwable e) {
                long operationDurationMs = (System.nanoTime() - operationStartTime) / 1_000_000;
                tracing.addAttribute("operation.duration.ms", operationDurationMs);
                tracing.setError(e);

                Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE,
                        "CachedRepository task failed after {0}ms: {1}",
                        new Object[]{operationDurationMs, e.toString()});
                throw e;
            }
        }
    }

    private Connection getAccumedDB() {
        DataSource ds = null;
        Connection con = null;
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            ds = (DataSource) envCtx.lookup("jdbc/accumedDS");
            //ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/accumedDS");
            con = ds.getConnection();
            if (con.getTransactionIsolation() != Connection.TRANSACTION_READ_UNCOMMITTED) {
                Logger.getLogger(CachedRepositoryService.class
                        .getName()).log(Level.SEVERE, "DB connection is NOT READ_UNCOMMITTED.");
            } else {
                Logger.getLogger(CachedRepositoryService.class
                        .getName()).log(Level.INFO, "DB connection is READ_UNCOMMITTED.");
            }
        } catch (NamingException ex) {
            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return con;
    }

    private void initialize(String logicalName) {
        Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.INFO, "CachedRepositoryService.initialize...");
        Connection dbConn = getAccumedDB();

        try {
            if (logicalName != null) {
                java.lang.reflect.Method method = RepoUtils.class.getMethod("get" + logicalName, Connection.class, String.class);
                repo.addCachedData(logicalName, (CachedData) method.invoke(null, dbConn, logicalName));
            } else {
                String[] data = new String[]{
                    "Clinicians","ClinicianCategories","Facilities","Payers", "CodeGroups",//"ICDExclusions",
                    "AAPC_CPT_AGE_GENDER_MATERNITY","IncompatibleCodesList","AAPC_CPT","AAPC_NCCI_CPTS_List",
                    "InsurerPackages", 
                    "MarketFacilities", "ProfessionDiagnosis","ActivityQuantityLimit",  
                    "FacilityNetworks", 
                    "NaCptsDos", "AddOnCodes", "RecieverPayers", "FacilityCodeSchema", "FacilityReceiverSchema",
                    "DhaDrugPrices", "HaadCPTCodes", "HaadDentalCodes",
                    "HaadDRGCodes", "HaadDrugCodes", "HaadHCPCSCodes", "HaadICDCodes",
                    "HaadServiceCodes", "DhaCPTCodes", "DhaDentalCodes", "DhaDrugCodes",
                    "DhaHCPCSCodes", "DhaICDCodes", "DhaServiceCodes","ICDContradictories", 
                    "CrossWalk_CPT","ICDExclusionMaster","ICDAcuteChronic","CrosswalkExclusion","STTFacilityNetworkMap"
                       ,"ICD10_AdditionalCodes" };


                for (String dataName : data) {
                    Logger.getLogger(DroolsUpdaterService.class.getName()).
                            log(Level.INFO, "Calling get{0}...", dataName);
                    java.lang.reflect.Method method = com.accumed.re.agents.RepoUtils.class.getMethod("get" + dataName, Connection.class, String.class);
                    repo.addCachedData(dataName, (CachedData) method.invoke(null, dbConn, dataName));
                }

            }
        } catch (Exception ex) {
            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
                }
                dbConn = null;
            }
        }
    }

    private void populate() {
        com.accumed.re.pool.Worker.setCachedRepositoryService(this);
        AccumedValidatorWS.setCachedRepositoryService(this);
        DroolsUpdaterService.setCachedRepositoryService(this);
        WorkersFactory.setCachedRepositoryService(this);
    }
    private static final Logger logger = Logger.getLogger(BackgroundTaskManager.class.getName());
    public int reSynchronize() {
        try (com.accumed.tracing.ManualTracing.TracingSession tracing =
                     com.accumed.tracing.ManualTracing.startRootSpan("CachedRepositoryService.reSynchronize")) {

            tracing.addAttribute("background.task", true);
            tracing.addAttribute("thread.name", Thread.currentThread().getName());

            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                    "CachedRepositoryService.reSynchronize...");

            long startTime = System.nanoTime();
            int ret = 0;
            Connection dbConn = null;

            try {
                dbConn = getAccumedDB();
                tracing.addAttribute("db.connection.obtained", true);

                int totalTables = repo.getCachedDB().size();
                tracing.addAttribute("total.tables", totalTables);

                for (Map.Entry<String, CachedData> entry : repo.getCachedDB().entrySet()) {
                    CachedData cachedData = entry.getValue();
                    if (Status.INVALID == cachedData.getStatus()) {
                        logger.info("Re-synchronizing table: " + entry.getKey());
                        this.initialize(entry.getKey());
                        ret++;
                        tracing.addAttribute("table." + entry.getKey() + ".synced", true);
                        Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                                "{0} reSynchronized.", entry.getKey());
                    }
                }

                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                tracing.addAttribute("tables.resynchronized", ret);
                tracing.addAttribute("duration.ms", durationMs);
                tracing.addAttribute("success", true);

                Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.INFO,
                        "Re-synchronization completed: {0} tables in {1}ms",
                        new Object[]{ret, durationMs});

                return ret;

            } catch (Exception ex) {
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                tracing.setError(ex);
                tracing.addAttribute("duration.ms", durationMs);

                Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE,
                        "Re-synchronization failed after {0}ms", durationMs);
                return ret;
            } finally {
                if (dbConn != null) {
                    try {
                        dbConn.close();
                        tracing.addAttribute("db.connection.closed", true);
                    } catch (SQLException ex) {
                        Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    private int checkSynchronization() {
        Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.INFO, "CachedRepositoryService.checkSynchronization...");
        Connection dbConn = null;
        try {
            dbConn = getAccumedDB();
            return repo.checkSynchronization(dbConn);
        } catch (Exception ex) {
            Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CachedRepositoryService.class.getName()).log(Level.SEVERE, null, ex);
                }
                dbConn = null;
            }
        }
        return 0;
    }

    public CachedRepository getRepo() {
        return repo;
    }
}
