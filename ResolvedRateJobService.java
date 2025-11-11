/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents;

import com.accumed.webservices.AccumedValidatorWS;
import java.sql.CallableStatement;
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
public class ResolvedRateJobService implements Runnable {

    protected static CachedRepositoryService cachedRepositoryService = null;

    public ResolvedRateJobService() {

    }

    private static java.sql.Connection getRulesDB() {
        Logger.getLogger(AccumedValidatorWS.class
                .getName())
                .log(Level.INFO, "getRulesDB ....");
        DataSource ds = null;
        java.sql.Connection con = null;
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            ds = (DataSource) envCtx.lookup("jdbc/rulesLoggingDS");
            //ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/accumedDS");
            con = ds.getConnection();

            if (con.getTransactionIsolation() != java.sql.Connection.TRANSACTION_READ_UNCOMMITTED) {
                Logger.getLogger(CachedRepositoryService.class
                        .getName()).log(Level.SEVERE, "DB connection is NOT READ_UNCOMMITTED.");

            } else {
                Logger.getLogger(CachedRepositoryService.class
                        .getName()).log(Level.INFO, "DB connection is READ_UNCOMMITTED.");

            }
        } catch (NamingException | java.sql.SQLException ex) {
            Logger.getLogger(CachedRepositoryService.class
                    .getName()).log(Level.SEVERE, null, ex);

        }
        Logger.getLogger(AccumedValidatorWS.class
                .getName())
                .log(Level.INFO, "getRulesDB Done.");
        return con;

    }

    @Override
    public void run() {
        Logger.getLogger(ResolvedRateJobService.class.getName()).log(Level.INFO, "Running ResolvedRateJobService...");
        java.sql.Connection connection = null;
        CallableStatement cstmt = null;

//        try {
//            connection = getRulesDB();
//            if (connection != null) {
//                 cstmt = connection.getConnection().prepareCall(
//                "{call dbo.uspGetAverageProductWeightOUT(?)}");
//
//            }
//            Logger.getLogger(ResolvedRateJobService.class.getName()).log(Level.INFO, "Running ResolvedRateJobService...END");
//
//        } catch (java.sql.SQLException ex) {
//            Logger.getLogger(AccumedValidatorWS.class
//                    .getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            if (preparedStatement != null) {
//                try {
//                    preparedStatement.close();
//                } catch (java.sql.SQLException ex) {
//                    Logger.getLogger(AccumedValidatorWS.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                preparedStatement = null;
//            }
//            if (connection != null) {
//                try {
//                    connection.close();
//                    connection = null;
//                } catch (java.sql.SQLException ex) {
////                    Statistics.addException(ex);
////                    Logger
////                            .getLogger(AccumedValidatorWS.class
////                                    .getName()).log(Level.SEVERE, null, ex);
////                    Statistics.addException(ex);
//                }
//            }
//        }
    }
}
