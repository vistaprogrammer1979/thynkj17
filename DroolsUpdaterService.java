/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents;

import com.accumed.re.agents.repo.CachedRepository;
import com.accumed.re.pool.Worker;
import com.accumed.webservices.AccumedValidatorWS;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author smutlak
 */
public class DroolsUpdaterService implements Runnable {

    protected static CachedRepositoryService cachedRepositoryService = null;

    public DroolsUpdaterService() {

    }

    public static void setCachedRepositoryService(CachedRepositoryService cachedRepositoryService) {
        DroolsUpdaterService.cachedRepositoryService = cachedRepositoryService;
    }

    @Override
    public void run() {
        Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.INFO, "Running DroolsUpdaterService...");
        Worker worker = null;
        try {
            if (DroolsUpdaterService.cachedRepositoryService == null) {
                Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.SEVERE, "Cached Repository Service is not set...");
                return;
            }
            CachedRepository repo = DroolsUpdaterService.cachedRepositoryService.getRepo();
            if (repo == null || !repo.isValid()) {
                Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.SEVERE, "Cached Repository is not set or invalid...");
                return;
            }
            if (AccumedValidatorWS.getWorkersPool().getNumActive() <= AccumedValidatorWS.getWorkersPool().getNumIdle()) {
                Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.INFO, 
                        "Clearing workers pool & checking if packages files changed.");
//                AccumedValidatorWS.getWorkersPool().clear();
//                worker = (Worker) AccumedValidatorWS.getWorkersPool().borrowObject(60000);
//                if (worker.isNewPackageExisted()) {
//                    AccumedValidatorWS.getWorkersPool().invalidateObject(worker);
//                    worker = null;
//                } else {
//                    AccumedValidatorWS.getWorkersPool().returnObject(worker);
//                    worker = null;
//                }
            }

            Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.INFO, "Running DroolsUpdaterService...END");

        } catch (Throwable e) {
            Logger.getLogger(DroolsUpdaterService.class.getName()).log(Level.SEVERE, "Exception {0}{1}", new Object[]{e.toString(), RepoUtils.stackTraceToString(e)});
        } finally {
            if (worker != null) {
                try {
                    AccumedValidatorWS.getWorkersPool().invalidateObject(worker);
                } catch (Exception ex) {
                    Logger.getLogger(AccumedValidatorWS.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
