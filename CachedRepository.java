/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents.repo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import com.accumed.re.agents.Status;

/**
 *
 * @author smutlak
 */
public class CachedRepository {

    private ConcurrentHashMap<String, CachedData> cachedDB;

    public CachedRepository() {
    }

    public Date getTimeStamp() {
        Date ret = null;
        if (cachedDB!=null){
        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
            CachedData cachedData = entry.getValue();
            if (ret == null) {
                ret = cachedData.getChecksumTime();
            } else if (ret.before(cachedData.getChecksumTime())) {
                ret = cachedData.getChecksumTime();
            }
        }
        }
        return ret;
    }

    public void addCachedData(String logicalName, CachedData cachedData) {
        if (cachedDB == null) {
            cachedDB = new ConcurrentHashMap();
        }
        cachedDB.put(logicalName, cachedData);
    }

    public final HashMap<String, Long> getDistinctTableList() {
        HashMap<String, Long> ret;
        ret = new HashMap();
        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
            CachedData cachedData = entry.getValue();

            for (Map.Entry<String, Long> entry2
                    : ((HashMap<String, Long>) cachedData.getTables()).entrySet()) {
                if (!ret.containsKey(entry2.getKey())) {
                    ret.put(entry2.getKey(), entry2.getValue());
                }
            }
        }
        return ret;
    }

    public final List<String> getLoadedCustomeContracts() {
        List<String> ret;
        ret = new ArrayList();
        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
            CachedData cachedData = entry.getValue();
            String logicalName = cachedData.getLogicalName();
            if (logicalName.startsWith("PL_CUS_CON")) {
                String arr[] = logicalName.split(Pattern.quote("|"));
                ret.add(arr[1] + ", " + arr[2]);

            }

        }
        return ret;
    }

    private void InvalidateCachedData(List<String> changedTables) {
        for (String changedTableName : changedTables) {
            for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
                CachedData cachedData = entry.getValue();
                if (cachedData.getTables().get(changedTableName) != null) {
                    cachedData.invalidCache();
                }
            }
        }
    }

    public Boolean isValid() {
        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
            CachedData cachedData = entry.getValue();
            if (Status.INVALID == cachedData.getStatus()) {
                return false;
            }
        }
        return true;
    }

    public int checkSynchronization(Connection conn) {

        HashMap<String, Long> allTablesList = getDistinctTableList();
        List<String> changedTables = new ArrayList();

        boolean ret = false;
        Statement stmt = null;
        ResultSet rs = null;
        if (allTablesList.size() <= 0) {
            return 0;
        }
        String sQuery = "";
        Set<String> tableNames = allTablesList.keySet();

        for (String t : tableNames) {
            sQuery += "SELECT '" + t + "', CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM dbo." + t + " WITH (NOLOCK) union ";
        }
        //remove last union
        sQuery = sQuery.substring(0, sQuery.length() - 7);
//        Logger.getLogger(CachedRepository.class.getName()).log(Level.INFO, "**NOTE** CachedRepository checkSynchronization **NOTE**");
        //Logger.getLogger(CachedRepository.class.getName()).log(Level.INFO, "refreshTablesStatus Query=" + sQuery);
        //Logger.getLogger(CachedRepository.class.getName()).info(sQuery);
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sQuery);
            if (rs != null) {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    Long newchecksum = rs.getLong(2);

                    Long currentChecksum = allTablesList.get(tableName);

                    if (currentChecksum != null
                            && currentChecksum != newchecksum.longValue()) {
                        changedTables.add(tableName);
                        Logger.getLogger(CachedRepository.class.getName()).log(Level.INFO, "Mark {0} as changed.", tableName);
//                        Logger.getLogger(CachedRepository.class.getName()).log(Level.INFO, "Mark " + tableName + " as changed.");
                        ret = true;
                    }

                }
                rs.close();
                rs = null;
                stmt.close();
                stmt = null;
            }

            InvalidateCachedData(changedTables);
            if (!changedTables.isEmpty()) {
                return changedTables.size();
            }

        } catch (SQLException ex) {
            Logger.getLogger(CachedRepository.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
                if (stmt != null) {
                    stmt.close();
                    stmt = null;
                }
            } catch (SQLException ex) {
                //Statistics.addException(ex);
                Logger.getLogger(CachedRepository.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return 0;
    }

//    public int reSynchronize(Connection conn) {
//        int count = 0;
//        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
//            CachedData cachedData = entry.getValue();
//            if (Status.INVALID == cachedData.getStatus()) {
//
//                CachedData cd = RepoUtils.getMasterPriceLists(conn, cachedData.getLogicalName());
//                addCachedData(cachedData.getLogicalName(), cd);
//                Logger.getLogger(CachedRepository.class.getName()).log(Level.INFO, "cachedData.getLogicalName() reSynchronized.");
//                count++;
//            }
//        }
//        return count;
//    }
    public ConcurrentHashMap<String, CachedData> getCachedDB() {
        return cachedDB;
    }

    public boolean isCached(String logicalName) {
        return null != cachedDB.get(logicalName);
    }

    public CachedData get(String logicalName) {
        return cachedDB.get(logicalName);
    }

    public int getInvalidCachedDataCount() {
        int rett = 0;
        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
            CachedData cachedData = entry.getValue();
            if (Status.INVALID == cachedData.getStatus()) {
                rett++;
            }
        }
        return rett;
    }

//    public boolean expireTTL() {
//
//        Date curDate = new Date();
//        for (Map.Entry<String, CachedData> entry : cachedDB.entrySet()) {
//            CachedData cachedData = entry.getValue();
//            if ((curDate.getTime() - cachedData.getChecksumTime().getTime()) >= (cachedData.getTTL() * 60000)) {
//                cachedDB.remove(entry.getKey());
//                return true;
//            }
//        }
//        return false;
//    }
}
