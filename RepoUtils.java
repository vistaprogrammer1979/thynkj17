/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents;

import com.accumed.model.*;
import com.accumed.re.agents.repo.CachedData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author smutlak
 */
public class RepoUtils {

    public static int HAAD = 1;
    public static int DHA = 2;

    //Utils
    public static CachedData getICD10_AdditionalCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        final java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        final List<ICD10_AdditionalCodes> list = new ArrayList<>();
        tables.put("RULES_ICD10_AdditionalCodes", getTableChecksum(db, "RULES_ICD10_AdditionalCodes"));
        final String Sql = "SELECT Single_Code,RangeFrom,RangeTo,Additional_Code,IsRange,RuleName from RULES_ICD10_AdditionalCodes";
        try (PreparedStatement ps = db.prepareStatement(Sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String singleCode = rs.getString("Single_Code");
                String rangeFrom = rs.getString("RangeFrom");
                String rangeTo = rs.getString("RangeTo");
                String additionalCode = rs.getString("Additional_Code");
                boolean isRange = rs.getBoolean("IsRange");
                String ruleName = rs.getString("RuleName");
                List<String> rowAdditionalCodes = new ArrayList<>(1);
                rowAdditionalCodes.add(additionalCode);
                list.add(new ICD10_AdditionalCodes(singleCode, rangeFrom, rangeTo, additionalCode, isRange, ruleName, rowAdditionalCodes));
            }
        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                Long.toString((System.nanoTime() - lBegin) / 1000000));
        return new CachedData(logicalName, tables, list);
    }

    private static Long getTableChecksum(Connection db, String sTableName) {
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, "getTableChecksum {0}", sTableName);
        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = db.createStatement();
            rs = stmt.executeQuery("SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM dbo." + sTableName + " WITH (NOLOCK)");
            if (rs != null) {
                if (rs.next()) {
                    Long checksum = rs.getLong(1);
                    Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, "Set Table ({0}) check sum.", sTableName);
                    return checksum;
                }

            }
        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public static Date getMidnightYesterday() {
        Calendar calStart = new GregorianCalendar();
        calStart.setTime(new Date());
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date midnightYesterday = calStart.getTime();
        return midnightYesterday;
    }

    //Get Data
    public static CachedData getLOINCS(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.LOINC> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_CPT_LOINC", getTableChecksum(db, "ACCUMED_CPT_LOINC"));
            String Sql = "SELECT cpt.CODE AS CPT_CODE,l.LIONC_NUM AS LOINC_CODE, cl.isPrimary "
                    + "FROM ACCUMED_CPT_LOINC cl "
                    + "INNER JOIN ACCUMED_HAAD_CPTS cpt "
                    + "ON cl.cpt_id = cpt.ID "
                    + "INNER JOIN ACCUMED_LIONC l "
                    + "ON cl.loinc_id = l.ID ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String CPT_CODE = rs.getString(1);
                    String LOINC_CODE = rs.getString(2);
                    boolean isPrimary = rs.getInt(3) == 1;
                    //long CPT_ID = rs.getLong(3);
                    //long ICD_ID = rs.getLong(4);
                    list.add(new com.accumed.model.LOINC(/*CPT_ID, ICD_ID,*/CPT_CODE, LOINC_CODE, isPrimary));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDeletedDrugs(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.DeletedDrug> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DRUG_PRICES", getTableChecksum(db, "ACCUMED_DRUG_PRICES"));
            String Sql = "SELECT code, effective_delete_date"
                    + " FROM ACCUMED_DRUG_PRICES where  Status = 'Deleted'";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String CODE = rs.getString(1);
                    java.util.Date deleteDate = rs.getDate(2);
                    list.add(new com.accumed.model.DeletedDrug(CODE, deleteDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getClinicians(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Clinician> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_CLINICIANS", getTableChecksum(db, "ACCUMED_CLINICIANS"));
            tables.put("BS_CLINICIAN_PROFESSION", getTableChecksum(db, "BS_CLINICIAN_PROFESSION"));
            tables.put("BS_PROFESSIONS", getTableChecksum(db, "BS_PROFESSIONS"));

            String Sql = "SELECT [Clinician_License],[Facility_License], Status,\n"
                    + "Convert(datetime, [Valid_From], 103) AS [Valid_FROM], \n"
                    + "Convert(datetime, [Valid_To], 103) AS [Valid_To], \n"
                    + "bp.Profession   \n"
                    + "from Accumed_Clinicians c \n"
                    + "inner JOIN BS_CLINICIAN_PROFESSION AS bcp ON bcp.CLINICIAN_id = c.Clinician_Id\n"
                    + "INNER JOIN BS_PROFESSIONS AS bp ON bp.id = bcp.PROFESSION_id\n"
                    + "where c.IsDeleted<>1 and bp.IsDeleted<>1 and bcp.IsDeleted<>1";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String license = rs.getString(1);
                    String facility_license = rs.getString(2);
                    String status = rs.getString(3);
                    java.util.Date validFrom = rs.getDate(4);
                    java.util.Date validTo = rs.getDate(5);
                    String profession = rs.getString(6);

                    list.add(new com.accumed.model.Clinician(license, facility_license, status, validFrom, validTo, profession));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getInsurerPackages(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.InsurerPackage> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_INSURERS", getTableChecksum(db, "ACCUMED_INSURERS"));
            tables.put("ACCUMED_INSURER_PACKAGES", getTableChecksum(db, "ACCUMED_INSURER_PACKAGES"));

            String Sql = "SELECT dbo.ACCUMED_INSURERS.AUTH_NO, PACKAGE_NAME, DT_FROM, DT_TO "
                    + " FROM dbo.ACCUMED_INSURERS, dbo.ACCUMED_INSURER_PACKAGES  "
                    + " where dbo.ACCUMED_INSURER_PACKAGES.INSURERS_ID = dbo.ACCUMED_INSURERS.INSURERS_ID "
                    + " AND (dbo.ACCUMED_INSURERS.CLASSIFICATION='Insurance' OR dbo.ACCUMED_INSURERS.CLASSIFICATION='TPA') "
                    + " union "
                    + " SELECT dbo.ACCUMED_INSURERS.AUTH_NO, PACKAGE_NAME, DT_FROM, DT_TO "
                    + " FROM dbo.ACCUMED_INSURERS, dbo.ACCUMED_INSURER_PACKAGES "
                    + " where dbo.ACCUMED_INSURER_PACKAGES.[TPA_ID] = dbo.ACCUMED_INSURERS.INSURERS_ID "
                    + " AND (dbo.ACCUMED_INSURERS.CLASSIFICATION='TPA')";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String insurer = rs.getString(1);
                    String packageName = rs.getString(2);
                    //System.out.println("debugggggg"+insurer+packageName+rs.getTimestamp(3));
                    java.util.Date DT_FROM = new java.util.Date(rs.getTimestamp(3).getTime());
                    java.util.Date DT_TO = new java.util.Date(rs.getTimestamp(4).getTime());

                    list.add(new com.accumed.model.InsurerPackage(insurer, packageName, DT_FROM, DT_TO));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getFacilities(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Facility> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_FACILITY", getTableChecksum(db, "ACCUMED_FACILITY"));

            String Sql = "select FACILITY_NAME,"
                    + " FACILITY_LICENSE,"
                    + " FACILITY_TYPE_ID,"
                    + " FACILITY_STATUS_ID,"
                    + " IS_ACTIVE,"
                    + " installed, "
                    + " regulator "
                    + " from DBO.[ACCUMED_FACILITY]";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    String license = rs.getString(2);
                    int type = rs.getInt(3);
                    int status = rs.getInt(4);
                    boolean active = rs.getBoolean(5);
                    boolean installed = rs.getBoolean(6);
                    int regulator = rs.getInt(7);

                    list.add(new com.accumed.model.Facility(name, license, type, status, active, installed, regulator));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getMarketFacilities(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.MarketFacility> list = new ArrayList<>();
        try {
            tables.put("MARKET_FACILITY", getTableChecksum(db, "MARKET_FACILITY"));

            String Sql = "SELECT [FACILITY_LICENSE]\n"
                    + "      ,[FACILITY_NAME]\n"
                    + "      ,[FACILITY_Type]\n"
                    + "  FROM [MARKET_FACILITY]";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String license = rs.getString(1);
                    String name = rs.getString(2);
                    String type = rs.getString(3);
                    list.add(new com.accumed.model.MarketFacility(name, license, type));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getPayers(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Payer> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_INSURERS", getTableChecksum(db, "ACCUMED_INSURERS"));

            String Sql = "select AUTH_NO,"
                    + " CLASSIFICATION,"
                    + " regulator,"
                    + " MemberID_Prefix "
                    + " from [ACCUMED_INSURERS]";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String PayerLicense = rs.getString(1);
                    String PayerCLASS = rs.getString(2);
                    int regulator = rs.getInt(3);
                    String memberIdPrefix = rs.getString(4);
                    list.add(new com.accumed.model.Payer(PayerLicense, PayerCLASS, regulator,memberIdPrefix));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadICDCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DIAGNOSIS_CODES", getTableChecksum(db, "ACCUMED_DIAGNOSIS_CODES"));

            String Sql = "SELECT CODE, EFFECTIVE_DATE, EXPIRY_DATE FROM ACCUMED_DIAGNOSIS_CODES";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    Date startDate = rs.getTimestamp("EFFECTIVE_DATE");
                    Date endDate = rs.getTimestamp("EXPIRY_DATE");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.ICD9, code, startDate, endDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadCPTCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_HAAD_CPTS", getTableChecksum(db, "ACCUMED_HAAD_CPTS"));

            String Sql = "SELECT CODE, EFFECTIVE_DATE, EXPIRY_DATE FROM ACCUMED_HAAD_CPTS";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    Date startDate = rs.getTimestamp("EFFECTIVE_DATE");
                    Date endDate = rs.getTimestamp("EXPIRY_DATE");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.CPT, code, startDate, endDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadHCPCSCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_HAAD_HCPCS", getTableChecksum(db, "ACCUMED_HAAD_HCPCS"));

            String Sql = "SELECT CODE, EFFECTIVE_DATE ,EXPIRY_DATE FROM ACCUMED_HAAD_HCPCS";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    Date startDate = rs.getTimestamp("EFFECTIVE_DATE");
                    Date endDate = rs.getTimestamp("EXPIRY_DATE");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.HCPCS, code, startDate, endDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadDrugCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DRUG_PRICES", getTableChecksum(db, "ACCUMED_DRUG_PRICES"));

            String Sql = "SELECT CODE, DATE_FROM, DATE_TO FROM ACCUMED_DRUG_PRICES";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    Date startDate = rs.getTimestamp("DATE_FROM");
                    Date endDate = rs.getTimestamp("DATE_TO");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.TRADE_DRUG, code, startDate, endDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadDentalCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DENTAL_CODES", getTableChecksum(db, "ACCUMED_DENTAL_CODES"));

            String Sql = "SELECT CODE, EFFECTIVE_DATE, EXPIRY_DATE FROM ACCUMED_DENTAL_CODES";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    Date startDate = rs.getTimestamp("EFFECTIVE_DATE");
                    Date endDate = rs.getTimestamp("EXPIRY_DATE");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.DENTAL, code, startDate, endDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadServiceCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_SERVICE_CODES", getTableChecksum(db, "ACCUMED_SERVICE_CODES"));

            String Sql = "SELECT CODE, EFFECTIVE_DATE, EXPIRY_DATE FROM ACCUMED_SERVICE_CODES";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    Date startDate = rs.getTimestamp("EFFECTIVE_DATE");
                    Date endDate = rs.getTimestamp("EXPIRY_DATE");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.SERVICE, code, startDate, endDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getHaadDRGCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DRG_CODES", getTableChecksum(db, "ACCUMED_DRG_CODES"));

            String Sql = "SELECT CODE FROM ACCUMED_DRG_CODES";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CODE");
                    list.add(new com.accumed.model.Code(HAAD, CodeType.IR_DRG, code));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaICDCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_ICD10", getTableChecksum(db, "ACCUMED_DHA_ICD10"));

            String Sql = "SELECT Code,[2018] FROM  ACCUMED_DHA_ICD10";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("Code");
                    boolean is2018=rs.getBoolean("2018");
                    list.add(new com.accumed.model.Code(DHA, CodeType.ICD10, code,is2018));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaCPTCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_CPT", getTableChecksum(db, "ACCUMED_DHA_CPT"));

            String Sql = "SELECT CPT_CODE ,[2018] FROM  ACCUMED_DHA_CPT";
            
            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("CPT_CODE");
                    boolean is2018=rs.getBoolean("2018");
                    list.add(new com.accumed.model.Code(DHA, CodeType.CPT, code,is2018));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaDrugCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_DRUG", getTableChecksum(db, "ACCUMED_DHA_DRUG"));

            String Sql = "SELECT DUBAI_DRUG_CODE, effective_date, expiry_date, source  FROM ACCUMED_DHA_DRUG";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("DUBAI_DRUG_CODE");
                    Date startDate = rs.getTimestamp("effective_date");
                    Date endDate = rs.getTimestamp("expiry_date");
                    String source = rs.getString("source");
                    list.add(new com.accumed.model.Code(DHA, CodeType.TRADE_DRUG, code, startDate, endDate, source));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaHCPCSCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_HCPCS", getTableChecksum(db, "ACCUMED_DHA_HCPCS"));

            String Sql = "SELECT HCPC ,[2018] FROM  ACCUMED_DHA_HCPCS";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("HCPC");
                    boolean is2018=rs.getBoolean("2018");
                    list.add(new com.accumed.model.Code(DHA, CodeType.HCPCS, code,is2018));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaDentalCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_DENTAL_CODE", getTableChecksum(db, "ACCUMED_DHA_DENTAL_CODE"));

            String Sql = "SELECT Code,[2018] FROM  ACCUMED_DHA_DENTAL_CODE";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("Code");
                    boolean is2018=rs.getBoolean("2018");
                    list.add(new com.accumed.model.Code(DHA, CodeType.DENTAL, code,is2018));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaServiceCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.Code> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_SERVICE_CODE", getTableChecksum(db, "ACCUMED_DHA_SERVICE_CODE"));

            String Sql = "SELECT Code FROM  ACCUMED_DHA_SERVICE_CODE";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("Code");
                    list.add(new com.accumed.model.Code(DHA, CodeType.SERVICE, code));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getFacilitySchemas(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.FacilitySchema> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_FACILITY_SCHEMA", getTableChecksum(db, "ACCUMED_FACILITY_SCHEMA"));

            String Sql = "SELECT  [ID] "
                    + ",[FACILITY_LICENSE] "
                    + ",[HAAD_INSURANCE_LIC] "
                    + ",[DHA_INSURANCE_LIC] "
                    + ",[EFFECTIVE_DATE] "
                    + ",[EXPIRY_DATE] "
                    + ",[TYPE] "
                    + ",[REGULATOR_SCHEMA] "
                    + ",[USED_INSURANCE_LICENSE] "
                    + "FROM [dbo].[ACCUMED_FACILITY_SCHEMA]";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    Long id = 0L;
                    id = rs.getLong(1);
                    String facilityLicense = rs.getString(2);
                    String InsuranceLicenseHAAD = rs.getString(3);
                    String InsuranceLicenseDHA = rs.getString(4);
                    java.util.Date effectiveDate = rs.getTimestamp(5);
                    java.util.Date expiryDate = rs.getTimestamp(6);
                    Integer type = rs.getInt(7);
                    Integer regulatorSchema = rs.getInt(8);
                    String usedInsuranceLicense = rs.getString(9);

                    list.add(new com.accumed.model.FacilitySchema(id, facilityLicense, InsuranceLicenseHAAD,
                            InsuranceLicenseDHA, effectiveDate, expiryDate, type, regulatorSchema,
                            usedInsuranceLicense));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getClinicianCategories(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ClinicianCategory> list = new ArrayList<>();
        try {
            tables.put("BS_CategoryFacility_CLINICIAN", getTableChecksum(db, "BS_CategoryFacility_CLINICIAN"));
            tables.put("BS_CATEGORY_FACILITY", getTableChecksum(db, "BS_CATEGORY_FACILITY"));
            tables.put("BS_CATEGORIES", getTableChecksum(db, "BS_CATEGORIES"));

            String Sql = "SELECT cat.CATEGORY, f.FACILITY_LICENSE,c.Clinician_License "
                    + " FROM BS_CategoryFacility_CLINICIAN cfc inner join BS_CATEGORY_FACILITY  cf"
                    + " on cfc.CATEGORY_FACILITY_id = cf.id INNER join BS_CATEGORIES cat ON"
                    + " cat.id = cf.CATEGORY_id "
                    + " inner join ACCUMED_CLINICIANS c on c.Clinician_Id = cfc.CLINICIAN_id"
                    + " inner join ACCUMED_FACILITY f on f.FACILITY_ID = cf.FACILITY_id "
                    + " where cfc.IsDeleted=0 ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String category = rs.getString(1);
                    String facilityLicense = rs.getString(2);
                    String clinician_license = rs.getString(3);

                    list.add(new com.accumed.model.ClinicianCategory(category, facilityLicense, clinician_license));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
 public static CachedData getCrossWalk_CPT(Connection db, String logicalName) {
       
         long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.CrossWalk_CPT> list = new ArrayList<com.accumed.model.CrossWalk_CPT>();
        com.accumed.model.CrossWalk_CPT crossWalk_CPT = null;
           PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            tables.put("Rules_CW_CPT", getTableChecksum(db, "Rules_CW_CPT"));
            tables.put("RULES_CW_ICD", getTableChecksum(db, "RULES_CW_ICD"));
            tables.put("RULES_CW_CPT_RECEIVERS", getTableChecksum(db, "RULES_CW_CPT_RECEIVERS"));

            String Sql = "select  I.cpt,I.ICD, I.icds_version, c.CPT_TYPE,c.shortMsg,c.longMsg ,rs.receiver_license ,c.severity\n"
                    + " from RULES_CW_ICD I   inner join Rules_CW_CPT c on I.CPT=c.CPT and i.icds_version=c.icds_version   left outer join RULES_CW_CPT_RECEIVERS  rs   \n"
                    + "              on rs.CPT=I.CPT  order by I.cpt,c.CPT_TYPE, I.icds_version,I.ICD    ";

             ps = db.prepareStatement(Sql);
             rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String cpt = rs.getString(1);
                    String icd = rs.getString(2);
                    String icdVersion = rs.getString(3);
                    String type = rs.getString(4);
                    String shortMsg = rs.getString(5);
                    String longMsg = rs.getString(6);
                    String reciverLicense = rs.getString(7);
                    String severity = rs.getString(8);
                     CodeType ttype = null;
                         if ("6".equalsIgnoreCase(type)) {
                            ttype = CodeType.DENTAL;
                        }
                        else
                        {
                         
                            ttype = CodeType.CPT;
                        }
                    if (crossWalk_CPT == null || !(crossWalk_CPT.getCpt().equals(cpt) && crossWalk_CPT.getType().equals(ttype))) {
                        crossWalk_CPT = new com.accumed.model.CrossWalk_CPT(cpt, shortMsg, longMsg, severity);                       
                        crossWalk_CPT.setType(ttype);
                        crossWalk_CPT.setIcds(null);
                        crossWalk_CPT.setRecieverLicense(null);
                        list.add(crossWalk_CPT);
                    }              
                    //  this  block  is  to  add recievr  license
                    if (crossWalk_CPT.getRecieverLicense() == null || crossWalk_CPT.getRecieverLicense().isEmpty()) {
                        if (reciverLicense != null && reciverLicense != "NULL") {
                            List<String> listOfRecievres = new ArrayList<>();
                            listOfRecievres.add(reciverLicense);
                            crossWalk_CPT.setRecieverLicense(listOfRecievres);
                        }
                    } else {
 
                        if ( reciverLicense != null && reciverLicense != "NULL") {
                            crossWalk_CPT.getRecieverLicense().add(reciverLicense);
                        }
                    }

                    // this  block  is  to  add Icds9
                    if (icdVersion.equals("9") ) {
                        if (crossWalk_CPT.getIcds9() == null || crossWalk_CPT.getIcds9().isEmpty()) {
                            List<String> listOfIcds9 = new ArrayList<>();
                            listOfIcds9.add(icd);
                            crossWalk_CPT.setIcds9(listOfIcds9);
                        } else {
                             crossWalk_CPT.getIcds9().add(icd);
                        }
                    }
                    // this  block  is  to  add Icds10
                    if (icdVersion.equals("10") ) {
                        if (crossWalk_CPT.getIcds10() == null || crossWalk_CPT.getIcds10().isEmpty()) {
                            List<String> listOfIcds10 = new ArrayList<>();
                            listOfIcds10.add(icd);
                            crossWalk_CPT.setIcds10(listOfIcds10);
                        } else {
                            crossWalk_CPT.getIcds10().add(icd);
 
                        }
                    }
                }
        
            }
           

        } catch (Exception ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {;
            }
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
public static CachedData getAAPC_CPT(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.AAPC_CPT> list = new ArrayList<>();
        try {
            tables.put("RULES_AAPC_CW_CPT_ICD", getTableChecksum(db, "RULES_AAPC_CW_CPT_ICD"));

            String Sql = " Select CPT , ICD from RULES_AAPC_CW_CPT_ICD where isnull(excluded,0) !=1 order  by CPT ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            AAPC_CPT aapc=null;
            if (rs != null) {
                while (rs.next()) {
                    String cpt = rs.getString(1);
                    String icd = rs.getString(2);
                
                    if (aapc == null || !(aapc.getCPT().equals(cpt))) {
                        aapc = new com.accumed.model.AAPC_CPT(cpt);                       
                        
                        aapc.setIcds(null);
                        
                         list.add(aapc);
                    }              
                    
                    // this  block  is  to  add Icd
                   
                        if (aapc.getIcds() == null || aapc.getIcds().isEmpty()) {
                            List<String> listOfIcd = new ArrayList<>();
                            listOfIcd.add(icd);
                            aapc.setIcds(listOfIcd);
                        } else {
                             aapc.getIcds().add(icd);
                        }
                }
               // System.out.println(list.size());
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
    public static CachedData getCodeGroups(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.CodeGroup> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_CODE_GROUPS", getTableChecksum(db, "ACCUMED_CODE_GROUPS"));
            tables.put("ACCUMED_GROUP_CODES", getTableChecksum(db, "ACCUMED_GROUP_CODES"));

            String Sql = "SELECT c.ID as id ,c.NAME as name,c.TYPE as type ,[VERSION] , g.[FROM] as f ,g.[TO] as t,"
                    + " IsNull((Select name + '|' AS [text()] from ACCUMED_CODE_GROUPS where PARENT_ID = c.id "
                    + " order by ID "
                    + " For XML PATH ('')), '') as childGroups "
                    + "FROM  "
                    + " ACCUMED_GROUP_CODES  g right  JOIN     ACCUMED_CODE_GROUPS  c"
                    + " ON g.GROUP_ID=c.ID"
                    + " ORDER   BY   c.ID";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    Integer id = rs.getInt("id");
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    String version = rs.getString("VERSION");
                    String from = rs.getString("f");
                    String to = rs.getString("t");
                    String childGroups = rs.getString("childGroups");
                    CodeType ttype = null;
                    com.accumed.model.CodeGroup codeGroup = new com.accumed.model.CodeGroup(CodeType.CPT, id, name, childGroups);
                    if ("CPT".equalsIgnoreCase(type)) {
                        ttype = CodeType.CPT;
                    }
                    if ("HCPCS".equalsIgnoreCase(type)) {
                        ttype = CodeType.HCPCS;
                    }
                    if ("DRUG".equalsIgnoreCase(type)) {
                        ttype = CodeType.TRADE_DRUG;
                    }
                    if ("DENTAL".equalsIgnoreCase(type)) {
                        ttype = CodeType.DENTAL;
                    }
                    if ("SERVICE".equalsIgnoreCase(type)) {
                        ttype = CodeType.SERVICE;
                    }
                    if ("IR-DRG".equalsIgnoreCase(type)) {
                        ttype = CodeType.IR_DRG;
                    }
                    if ("GENERIC_DRUG".equalsIgnoreCase(type)) {
                        ttype = CodeType.GENERIC_DRUG;
                    }
                    if ("Package".equalsIgnoreCase(type)) {
                        ttype = CodeType.Package;
                    }
                    if ("CustomCode".equalsIgnoreCase(type)) {
                        ttype = CodeType.CustomCode;
                    }
 
                    if ("ICD".equalsIgnoreCase(type)) {
                        if (version != null) {
                            if ("ICD-10".equalsIgnoreCase(version)) {
                                ttype = CodeType.ICD10;
                            }
                            if ("ICD-9".equalsIgnoreCase(version)) {
                                ttype = CodeType.ICD9;
                            }
                        }
                    }

                    if (ttype == null) {
                        continue;
                    }
                    codeGroup.setType(ttype);

                    GroupCodesRange groupCodesRange = new GroupCodesRange(from, to);
                    boolean alreadyExisted = false;
                    for (com.accumed.model.CodeGroup cg : list) {
                        if (cg.getName().equals(name) && cg.getType() == codeGroup.getType()) {
                            if (cg.getItems() != null) {
                                cg.getItems().add(groupCodesRange);

                            } else {
                                List<GroupCodesRange> groupCodesRangeList = new ArrayList<GroupCodesRange>();
                                groupCodesRangeList.add(groupCodesRange);
                                cg.setItems(groupCodesRangeList);
                            }
                            alreadyExisted = true;
                            break;
                        }
                    }
                    if (!alreadyExisted) {
                        List<GroupCodesRange> groupCodesRangeList = new ArrayList<GroupCodesRange>();
                        groupCodesRangeList.add(groupCodesRange);
                        codeGroup.setItems(groupCodesRangeList);
                        list.add(codeGroup);
                    }
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getFacilityNetworks(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.FacilityNetwork> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_NETWORKS_PROVIDERS", getTableChecksum(db, "ACCUMED_NETWORKS_PROVIDERS"));
            tables.put("ACCUMED_INSURER_NETWORKS", getTableChecksum(db, "ACCUMED_INSURER_NETWORKS"));

            String Sql = "SELECT f.FACILITY_LICENSE, n.NETWORK_NAME, r.AUTH_NO FROM ACCUMED_NETWORKS_PROVIDERS l\n"
                    + "inner join ACCUMED_FACILITY f on f.FACILITY_ID = l.FACILITY_ID\n"
                    + "inner join ACCUMED_INSURER_NETWORKS n on n.NETWORK_ID = l.NETWORK_ID\n"
                    + "inner join ACCUMED_INSURERS r on r.company_id = l.COMPANY_ID\n"
                    + " WHERE l.IsDeleted = 0 "
                    + "order by f.FACILITY_LICENSE, n.NETWORK_NAME ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String facilityLicense = rs.getString("FACILITY_LICENSE");
                    String network = rs.getString("NETWORK_NAME");
                    String insurerLicense = rs.getString("AUTH_NO");

                    com.accumed.model.FacilityNetwork facilityNetwork = new com.accumed.model.FacilityNetwork(facilityLicense, insurerLicense, network);
                    list.add(facilityNetwork);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDhaDrugPrices(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.DrugPrice> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_DRUG", getTableChecksum(db, "ACCUMED_DHA_DRUG"));

            String Sql = "SELECT   CODE,"
                    + "Package_Price_to_Public,"
                    + "Unit_Price_to_Public,"
                    + "DATE_FROM,"
                    + "DATE_TO "
                    + "FROM  DRUG_PRICES_VIEW "
                    + "where regulator=2 ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    Float Package_Price_to_Public = rs.getFloat(2);
                    Float Unit_Price_to_Public = rs.getFloat(3);
                    java.util.Date DATE_FROM = rs.getDate(4);
                    java.util.Date DATE_TO = rs.getDate(5);
                    list.add(new com.accumed.model.DrugPrice(code, Package_Price_to_Public, Unit_Price_to_Public,
                            DATE_FROM, DATE_TO));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getNaCptsDos(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.NA_CPTS_DOS> list = new ArrayList<>();
        try {
            tables.put("RULE_533_CPTS_NO_SAME_DOS", getTableChecksum(db, "RULE_533_CPTS_NO_SAME_DOS"));

            String Sql = "Select CPT1, CPT2 from RULE_533_CPTS_NO_SAME_DOS";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String CPT1 = rs.getString(1);
                    String CPT2 = rs.getString(2);
                    list.add(new com.accumed.model.NA_CPTS_DOS(CPT1, CPT2));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
    public static CachedData getSTTFacilityNetworkMap(Connection db, String logicalName) { 
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.STTFacilityNetworkMap> list = new ArrayList<>();
        try {
            tables.put("STT_FACILITY_NETWORK", getTableChecksum(db, "STT_FACILITY_NETWORK"));

            String Sql = "SELECT  network_name,network_code,STT_network_NAME,Facility_License,Receiver,payer,regulator_ID FROM  STT_FACILITY_NETWORK";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String networkName = rs.getString(1);
                    String networkCode = rs.getString(2);
                    String STTNetworkName = rs.getString(3);
                    String providerID = rs.getString(4);
                    String receiverID = rs.getString(5);
                    String payerID = rs.getString(6);
                    int regulator = rs.getInt(7);
                     list.add(new com.accumed.model.STTFacilityNetworkMap(networkName, networkCode,STTNetworkName,providerID,receiverID,payerID,regulator)); 
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
    ////Added By Rasha
     public static CachedData getAAPC_CW(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.AAPC_CW> list = new ArrayList<>();
        try {
            tables.put("RULES_AAPC_CW_CPT_ICD", getTableChecksum(db, "RULES_AAPC_CW_CPT_ICD"));

            String Sql = " Select CPT , ICD from RULES_AAPC_CW_CPT_ICD ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String CPT = rs.getString(1);
                    String ICD = rs.getString(2);
                    list.add(new com.accumed.model.AAPC_CW(CPT, ICD));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
     
    ////Added By Rasha
     public static CachedData getAAPC_NCCI_CPTS(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.AAPC_NCCI_CPTS> list = new ArrayList<>();
        try {
            tables.put("RULES_AAPC_NCCI_Edites", getTableChecksum(db, "RULES_AAPC_NCCI_Edites"));

            String Sql = "Select CPT1 , CPT2,Effective_Date from RULES_AAPC_NCCI_Edites where (Del_Date is  null  or Del_Date>'2018-12-31 00:00:00.000') ";
           

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String CPT1 = rs.getString(1);
                    String CPT2 = rs.getString(2);
                      Date effectiveDate=rs.getTimestamp(3);
                    list.add(new com.accumed.model.AAPC_NCCI_CPTS(CPT1, CPT2,effectiveDate));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
    ///End Rasha
   public static CachedData getAAPC_NCCI_CPTS_List (Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.AAPC_NCCI_CPTS_List> list = new ArrayList<>();
        try {
             tables.put("RULES_AAPC_NCCI_Edites", getTableChecksum(db, "RULES_AAPC_NCCI_Edites"));

            String Sql = "Select CPT1 , CPT2,Effective_Date from RULES_AAPC_NCCI_Edites where CPT2 is not null and (Del_Date is  null  or Del_Date>'2018-12-31 00:00:00.000') order by CPT1 ,CPT2 ";
           

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            AAPC_NCCI_CPTS_List firstCode=null;
            if (rs != null) {
                while (rs.next()) {
                    String code1 = rs.getString(1);
                    String code2 = rs.getString(2);
                     Date effectiveDate=rs.getTimestamp(3);
                
                    if (firstCode == null || !(firstCode.getCPT().equals(code1))) {
                        firstCode = new com.accumed.model.AAPC_NCCI_CPTS_List(code1);                       
                        firstCode.setEffectiveDate(effectiveDate);
                        firstCode.setCptList(null);
                        
                         list.add(firstCode);
                    }              
                    
                    // this  block  is  to  add Conflict List
                   
                        if (firstCode.getCptList() == null || firstCode.getCptList().isEmpty()) {
                            List<String> listOfconflicts = new ArrayList<>();
                            listOfconflicts.add(code2);
                            firstCode.setCptList (listOfconflicts);
                        } else {
                             firstCode.getCptList().add(code2);
                        }
                }
                System.out.println(list.size());
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }  
public static CachedData getAAPC_CPT_AGE_GENDER_MATERNITY(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.AAPC_CPT_AGE_GENDER_MATERNITY> list = new ArrayList<>();
        try {
            tables.put("RULE_1414_CPT_Age_Gender_Indicator", getTableChecksum(db, "RULE_1414_CPT_Age_Gender_Indicator"));

            String Sql = "Select CPT, AGE_RESTRICTION,BEGIN_AGE,END_AGE,GENDER ,maternity from RULE_1414_CPT_Age_Gender_Indicator ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String CPT = rs.getString(1);
                    String ageRestriction = rs.getString(2);
                    int beginAge = rs.getInt(3);
                    int endAge = rs.getInt(4);
                    String gender = rs.getString(5);
                    String maternity = rs.getString(6);     
                    list.add(new com.accumed.model.AAPC_CPT_AGE_GENDER_MATERNITY( CPT,  maternity,  beginAge,  endAge, ageRestriction, gender));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

public static CachedData getIncompatibleCodesList(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.IncompatibleCodesList> list = new ArrayList<>();
        try {
             tables.put("Rule_Incompatible_Codes", getTableChecksum(db, "Rule_Incompatible_Codes"));

            String Sql = "SELECT   CODE1 ,CODE2 ,TYPE  FROM Rule_Incompatible_Codes where CODE1 <> CODE2  order by CODE1 , TYPE, CODE2 ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            IncompatibleCodesList firstCode=null;
            if (rs != null) {
                while (rs.next()) {
                    String code1 = rs.getString(1);
                    String code2 = rs.getString(2);
                    String type  =rs.getString(3);
                
                    if (firstCode == null || !(firstCode.getCode().equals(code1))) {
                        firstCode = new com.accumed.model.IncompatibleCodesList(code1,type);                       
                        
                        firstCode.setCodeList(null);
                        
                         list.add(firstCode);
                    }              
                    
                    // this  block  is  to  add Conflict List
                   
                        if (firstCode.getCodeList() == null || firstCode.getCodeList().isEmpty()) {
                            List<String> listOfconflicts = new ArrayList<>();
                            listOfconflicts.add(code2);
                            firstCode.setCodeList (listOfconflicts);
                        } else {
                             firstCode.getCodeList().add(code2);
                        }
                }
                System.out.println(list.size());
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
public static CachedData getIncompatibleCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.IncompatibleCodes> list = new ArrayList<>();
        try {
            tables.put("Rule_Incompatible_Codes", getTableChecksum(db, "Rule_Incompatible_Codes"));

            String Sql = "SELECT   CODE1 ,CODE2 ,TYPE  FROM Rule_Incompatible_Codes where CODE1 <> CODE2  ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code1 = rs.getString(1);
                    String code2 = rs.getString(2);                     
                    String type = rs.getString(3);     
                    list.add(new com.accumed.model.IncompatibleCodes( code1,code2,type));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
public static CachedData getCrossWalk(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.CrossWalk> list = new ArrayList<>();
        try {
            tables.put("Rules_CW_CPT", getTableChecksum(db, "Rules_CW_CPT"));
            tables.put("Rules_CW_ICD", getTableChecksum(db, "Rules_CW_ICD"));
            tables.put("RULES_CW_CPT_RECEIVERS", getTableChecksum(db, "RULES_CW_CPT_RECEIVERS"));

            String Sql = "select   I.cpt,I.ICD, I.icds_version, c.CPT_TYPE,c.shortMsg,c.longMsg ,rs.receiver_license ,'' as documentation,'' as refLink,c.severity\n" +
" from RULES_CW_ICD I   inner join Rules_CW_CPT c on I.CPT=c.CPT and i.icds_version=c.icds_version   left outer join RULES_CW_CPT_RECEIVERS  rs   \n" +
"                                     on rs.CPT=I.CPT  ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String cpt = rs.getString(1);
                    String icd = rs.getString(2);
                    String icdVersion = rs.getString(3);
                    String cptType = rs.getString(4);
                    String shortMsg = rs.getString(5);
                    String longMsg = rs.getString(6);
                    String receiver = rs.getString(7);
                    String documentation = rs.getString(8);
                    String refLink = rs.getString(9);
                    String severity = rs.getString(10);
                    
                    list.add(new com.accumed.model.CrossWalk(   cpt,  icd,  cptType,  icdVersion,  shortMsg,  longMsg,  receiver,  documentation,  refLink,  severity ));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
    public static CachedData getAddOnCodes(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.AddOnCode> list = new ArrayList<>();
        try {
            tables.put("J644_AddON_MASTER", getTableChecksum(db, "J644_AddON_MASTER"));
            tables.put("J644_AddOn_Detail", getTableChecksum(db, "J644_AddOn_Detail"));

            String Sql = "Select m.code, d.[from], d.[to] \n"
                    + " from J644_AddON_MASTER m inner join \n"
                    + " J644_AddOn_Detail d on m.ID=d.master_id";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    String from = rs.getString("from");
                    String to = rs.getString("to");
                    com.accumed.model.AddOnCode addOnCode = new com.accumed.model.AddOnCode(code);

                    com.accumed.model.AddOnCodesRange addOnCodesRange = new com.accumed.model.AddOnCodesRange(from, to);
                    boolean alreadyExisted = false;
                    for (com.accumed.model.AddOnCode cg : list) {
                        if (cg.getCode().equals(code)) {
                            if (cg.getItems() != null) {
                                cg.getItems().add(addOnCodesRange);

                            } else {
                                List<com.accumed.model.AddOnCodesRange> groupCodesRangeList = new ArrayList<com.accumed.model.AddOnCodesRange>();
                                groupCodesRangeList.add(addOnCodesRange);
                                cg.setItems(groupCodesRangeList);
                            }
                            alreadyExisted = true;
                            break;
                        }
                    }
                    if (!alreadyExisted) {
                        List<com.accumed.model.AddOnCodesRange> groupCodesRangeList = new ArrayList<com.accumed.model.AddOnCodesRange>();
                        groupCodesRangeList.add(addOnCodesRange);
                        addOnCode.setItems(groupCodesRangeList);
                        list.add(addOnCode);
                    }
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getRecieverPayers(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.RecieverPayers> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_NETWORK_MAPPING", getTableChecksum(db, "ACCUMED_NETWORK_MAPPING"));
            tables.put("ACCUMED_INSURANCE_COMPANIES", getTableChecksum(db, "ACCUMED_INSURANCE_COMPANIES"));
            tables.put("ACCUMED_INSURERS", getTableChecksum(db, "ACCUMED_INSURERS"));

            String Sql = " select ins1.AUTH_NO,"
                    + " ins1.CLASSIFICATION,"
                    + " ins2.AUTH_NO,"
                    + " ins2.CLASSIFICATION "
                    + " from ACCUMED_NETWORK_MAPPING map inner JOIN \n"
                    + " ACCUMED_INSURANCE_COMPANIES comp on map.COMPANY_ID=comp.ID \n"
                    + " INNER  JOIN ACCUMED_INSURERS ins1 on  ins1.COMPANY_ID=comp.ID \n"
                    + " INNER  JOIN ACCUMED_INSURERS ins2 on  MAPPED_COMPANY_ID=ins2.company_id \n";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String RecieverLicense = rs.getString(1);
                    String RecieverClass = rs.getString(2);
                    String PayerLicense = rs.getString(3);
                    String PayerClass = rs.getString(4);
                    list.add(new com.accumed.model.RecieverPayers(RecieverLicense, RecieverClass, PayerLicense, PayerClass));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getFacilityCodeSchema(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.FacilityCodeSchema> list = new ArrayList<>();
        try {
            tables.put("BS_FACILITY_CODE_SCHEMA", getTableChecksum(db, "BS_FACILITY_CODE_SCHEMA"));

            String Sql = "select ID,FACILITY_LICENSE,INSURANCE_LICENSE,"
                    + "EFFECTIVE_DATE,EXPIRY_DATE,TYPE,"
                    + "REGULATOR_SCHEMA "
                    + "FROM BS_FACILITY_CODE_SCHEMA where IsDeleted=0 ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {

                    Integer id = rs.getInt("ID");
                    String facilityLicense = rs.getString("FACILITY_LICENSE");

                    String insuranceLicense = rs.getString("INSURANCE_LICENSE");

                    Date effectiveDate = rs.getDate("EFFECTIVE_DATE");

                    Date expiryDate = rs.getDate("EXPIRY_DATE");

                    Integer type = rs.getInt("TYPE");

                    Integer regulatorSchema = rs.getInt("REGULATOR_SCHEMA");

                    CodeType ttype = null;
                    com.accumed.model.FacilityCodeSchema facilityCodeSchema
                            = new com.accumed.model.FacilityCodeSchema(facilityLicense,
                                    insuranceLicense, effectiveDate, expiryDate, type, regulatorSchema);

                    list.add(facilityCodeSchema);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getFacilityReceiverSchema(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.FacilityReceiverSchema> list = new ArrayList<>();
        try {
            tables.put("BS_FACILITY_RECEIVER_SCHEMA", getTableChecksum(db, "BS_FACILITY_RECEIVER_SCHEMA"));

            String Sql = "select ID,FACILITY_LICENSE, INSURANCE_LIC,"
                    + "USED_INSURANCE_LICENSE,EFFECTIVE_DATE,EXPIRY_DATE "
                    + "FROM BS_FACILITY_RECEIVER_SCHEMA WHERE IsDeleted=0 ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {

                    Integer id = rs.getInt("ID");
                    String facilityLicense = rs.getString("FACILITY_LICENSE");

                    String insuranceLicense = rs.getString("INSURANCE_LIC");
                    String usedInsuranceLicense = rs.getString("USED_INSURANCE_LICENSE");

                    Date effectiveDate = rs.getDate("EFFECTIVE_DATE");

                    Date expiryDate = rs.getDate("EXPIRY_DATE");

                    CodeType ttype = null;
                    com.accumed.model.FacilityReceiverSchema facilityReceiverSchema
                            = new com.accumed.model.FacilityReceiverSchema(facilityLicense, insuranceLicense,
                                    effectiveDate, expiryDate, usedInsuranceLicense);

                    list.add(facilityReceiverSchema);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getACode_OValue(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ACode_OValue> list = new ArrayList<>();
        try {
            tables.put("RULE_921_CPTCode_ObservationValue", getTableChecksum(db, "RULE_921_CPTCode_ObservationValue"));

            String Sql = "Select Code, Alias,Type from RULE_921_CPTCode_ObservationValue ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    String alias = rs.getString(2);
                    Integer type = Integer.parseInt(rs.getString(3));
                    list.add(new com.accumed.model.ACode_OValue(code, alias, type));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
 public static CachedData  getCrosswalkExclusion(Connection db, String logicalName) {
     
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.CrossWalkExclusion> list = new ArrayList<>();
        try {
            tables.put("RULES_CW_EXCLUSION", getTableChecksum(db, "RULES_CW_EXCLUSION"));

            String Sql = "select RECEIVER,PROVIDER,ICD,CPT from  RULES_CW_EXCLUSION WHERE isDeleted IS  NULL OR   isDeleted<>1";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String reciever = rs.getString(1);
                    String provider =  rs.getString(2) ;
                    String icd=rs.getString(3) ;
                    String cpt=rs.getString(4);
                    list.add(new com.accumed.model.CrossWalkExclusion(cpt,icd,reciever, provider ));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    
 }
    public static CachedData getActivityQuantityLimit(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ActivityQuantityLimit> list = new ArrayList<>();
        try {
            tables.put("Rule_1061_CPT_BILLING_QUANTITY_LIMIT", getTableChecksum(db, "Rule_1061_CPT_BILLING_QUANTITY_LIMIT"));

            String Sql = "select Code,Quantity from  Rule_1061_CPT_BILLING_QUANTITY_LIMIT";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    float quantity = Float.parseFloat(rs.getString(2));
                    list.add(new com.accumed.model.ActivityQuantityLimit(code, quantity));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getProfessionDiagnosis(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ProfessionDiagnosis> list = new ArrayList<>();
        try {
            tables.put("RULES_Diagnosis_Profession", getTableChecksum(db, "RULES_Diagnosis_Profession"));
            tables.put("BS_Professions", getTableChecksum(db, "BS_Professions"));
            tables.put("Accumed_Code_Groups", getTableChecksum(db, "Accumed_Code_Groups"));
            tables.put("Accumed_Clinicians", getTableChecksum(db, "Accumed_Clinicians"));

            String Sql = "Select g.name, p.PROFESSION from RULES_Diagnosis_Profession d\n"
                    + "inner join ACCUMED_CODE_GROUPS g on g.ID=d.ICD_group_id\n"
                    + "inner join BS_PROFESSIONS p on p.id=d.Proffession_id";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String groupName = rs.getString(1);
                    String profession = rs.getString(2);
                    list.add(new com.accumed.model.ProfessionDiagnosis(profession, groupName));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    //Global Variables
    public static CachedData getDentalCodeRequireToothNo(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DENTAL_CODES", getTableChecksum(db, "ACCUMED_DENTAL_CODES"));

            String Sql = "SELECT Code FROM  ACCUMED_DENTAL_CODES "
                    + "Where ACCUMED_DENTAL_CODES.Tooth_NO_REQ = 1";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    list.add(code);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDentalCodeNotRequireToothNo(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DENTAL_CODES", getTableChecksum(db, "ACCUMED_DENTAL_CODES"));

            String Sql = "SELECT Code FROM  ACCUMED_DENTAL_CODES "
                    + "Where ACCUMED_DENTAL_CODES.Tooth_NO_REQ <> 1";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    list.add(code);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDHADentalCodeNotRequireToothNo(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DHA_DENTAL_CODE", getTableChecksum(db, "ACCUMED_DHA_DENTAL_CODE"));

            String Sql = "SELECT Code FROM  ACCUMED_DHA_DENTAL_CODE "
                    + "Where ACCUMED_DHA_DENTAL_CODE.Tooth_NO_REQ <> 1";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    list.add(code);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getDiagnosisGroupCodes(Connection db, String sGroupName, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_DIAGNOSES_GROUP_CODE", getTableChecksum(db, "ACCUMED_DIAGNOSES_GROUP_CODE"));

            String Sql = "Select A.Code from "
                    + "ACCUMED_DIAGNOSIS_CODES A, "
                    + "[ACCUMED_DIAGNOSES_GROUP_CODE] B, "
                    + "[ACCUMED_DIAGNOSES_GROUP] C "
                    + "WHERE C.NAME='" + sGroupName + "' AND B.GROUP_ID = C.ID "
                    + "AND A.DIAGNOSIS_ID = B.DIAGNOSIS_ID";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    list.add(code);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getLaboratories(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_CPTGROUP_CPTS", getTableChecksum(db, "ACCUMED_CPTGROUP_CPTS"));

            String Sql = "SELECT ACCUMED_HAAD_CPTS.CODE AS CPT_CODE  "
                    + " from dbo.ACCUMED_CPTGROUP_CPTS, dbo.ACCUMED_HAAD_CPTGROUP, dbo.ACCUMED_HAAD_CPTS  "
                    + " where dbo.ACCUMED_CPTGROUP_CPTS.ID = dbo.ACCUMED_HAAD_CPTS.ID AND "
                    + " dbo.ACCUMED_CPTGROUP_CPTS.HAAD_CPTGROUP_ID=dbo.ACCUMED_HAAD_CPTGROUP.HAAD_CPTGROUP_ID "
                    + " group by ACCUMED_HAAD_CPTS.CODE, ACCUMED_HAAD_CPTGROUP.HAAD_CPTGROUP_ID  "
                    + " having ACCUMED_HAAD_CPTGROUP.HAAD_CPTGROUP_ID = (SELECT TOP 1 [HAAD_CPTGROUP_ID] "
                    + " FROM [dbo].[ACCUMED_HAAD_CPTGROUP] where [NAME] ='Laboratory')";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    list.add(code);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static CachedData getRadiologies(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            tables.put("ACCUMED_CPTGROUP_CPTS", getTableChecksum(db, "ACCUMED_CPTGROUP_CPTS"));

            String Sql = "SELECT ACCUMED_HAAD_CPTS.CODE AS CPT_CODE  "
                    + " from dbo.ACCUMED_CPTGROUP_CPTS, dbo.ACCUMED_HAAD_CPTGROUP, dbo.ACCUMED_HAAD_CPTS  "
                    + " where dbo.ACCUMED_CPTGROUP_CPTS.ID = dbo.ACCUMED_HAAD_CPTS.ID AND "
                    + " dbo.ACCUMED_CPTGROUP_CPTS.HAAD_CPTGROUP_ID=dbo.ACCUMED_HAAD_CPTGROUP.HAAD_CPTGROUP_ID "
                    + " group by ACCUMED_HAAD_CPTS.CODE, ACCUMED_HAAD_CPTGROUP.HAAD_CPTGROUP_ID  "
                    + " having ACCUMED_HAAD_CPTGROUP.HAAD_CPTGROUP_ID = (SELECT TOP 1 [HAAD_CPTGROUP_ID] "
                    + " FROM [dbo].[ACCUMED_HAAD_CPTGROUP] where [NAME] ='RADIOLOGY')";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    list.add(code);
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        if (e.getMessage() != null) {
            sb.append(e.getMessage());
            sb.append("\n");
        }
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

//    public static CachedData getDamanBasicExclusions(Connection db, String logicalName) {
//        long lBegin = System.nanoTime();
//        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
//        List<String> list = new ArrayList<>();
//        try {
//            tables.put("ACCUMED_CODE_GROUPS", getTableChecksum(db, "ACCUMED_CODE_GROUPS"));
//            tables.put("ACCUMED_GROUP_CODES", getTableChecksum(db, "ACCUMED_GROUP_CODES"));
//
//            String Sql = " Select c.[FROM], g.TYPE FROM ACCUMED_CODE_GROUPS g, ACCUMED_Group_codes c "
//                    + " Where c.GROUP_ID = g.ID AND "
//                    + " g.NAME like 'Daman Basic Exclusions%'"
//                    + "GROUP BY g.TYPE, c.[FROM] ORDER BY g.TYPE";
//
//            PreparedStatement ps = db.prepareStatement(Sql);
//            ResultSet rs = ps.executeQuery();
//            if (rs != null) {
//                while (rs.next()) {
//                    String code = rs.getString(1);
//                    String type = rs.getString(2);
//                    java.util.List<String> group = list.get(type);
//                    if (group != null) {
//                        group.add(code);
//                    } else {
//                        java.util.List<String> new_group = new java.util.ArrayList<String>();
//                        new_group.add(code);
//                        list.put(type, new_group);
//                    }
//                }
//                rs.close();
//                rs = null;
//            }
//            ps.close();
//            ps = null;
//
//        } catch (SQLException ex) {
//            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
//                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
//        return new CachedData(logicalName, tables, list);
//    }
 /*   public static CachedData getICDExclusions(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ICDExclusion> list = new ArrayList<>();
        try {
            tables.put("RULES_ICDEXCLUSION_LINKS", getTableChecksum(db, "RULES_ICDEXCLUSION_LINKS"));
            tables.put("RULES_ICDEXCLUSION_SETS", getTableChecksum(db, "RULES_ICDEXCLUSION_SETS"));
            tables.put("RULES_ICDEXCLUSION_CODES", getTableChecksum(db, "RULES_ICDEXCLUSION_CODES"));

            String Sql = "SELECT l.id, c1.ICD_CODE as code1, c2.ICD_CODE as code2 "
                    + "FROM RULES_ICDEXCLUSION_LINKS l \n"
                    + "INNER JOIN RULES_ICDEXCLUSION_SETS s1 ON s1.ID = L.ID_FIRST_SETS\n"
                    + "INNER JOIN RULES_ICDEXCLUSION_SETS s2 ON s2.ID = L.ID_SECOND_SETS\n"
                    + "INNER JOIN RULES_ICDEXCLUSION_CODES c1 ON c1.SET_ID = s1.id\n"
                    + "INNER JOIN RULES_ICDEXCLUSION_CODES c2 ON c2.SET_ID = s2.id\n"
                    + "ORDER BY l.id";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Integer linkId = rs.getInt("id");
                    String code1 = rs.getString("code1");
                    String code2 = rs.getString("code2");

                    list.add(new com.accumed.model.ICDExclusion(linkId, code1, code2));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
   */ 
public static CachedData getICDExclusionMaster(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ICDExclusionMaster> list = new ArrayList<>();
        try {
            tables.put("RULES_ICDEXCLUSION_RANGE", getTableChecksum(db, "RULES_ICDEXCLUSION_RANGE"));
             

            String Sql = "SELECT  distinct [MASTER_ICD_FROM]      ,[MASTER_ICD_TO]      ,[EXCLUDE_ICD_FROM]"+
     " ,[EXCLUDE_ICD_TO]   FROM [RULES_ICDEXCLUSION_RANGE] "+
  " order by [MASTER_ICD_FROM],[MASTER_ICD_TO] ,[EXCLUDE_ICD_FROM]   ,[EXCLUDE_ICD_TO]";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                     
                    String master_ICD_from = rs.getString("Master_ICD_from");
                    String master_ICD_to = rs.getString("Master_ICD_to");
                    String exclude_ICD_from = rs.getString("Exclude_ICD_from");
                    String exclude_ICD_To = rs.getString("Exclude_ICD_To");
                    if (master_ICD_to==null){
                        master_ICD_to=master_ICD_from;
                    }
                    if (exclude_ICD_To==null){
                    exclude_ICD_To=exclude_ICD_from;
                    }
                    ICDExclusionMaster icdExclusionMaster=new ICDExclusionMaster(master_ICD_from,master_ICD_to);
                    ICDExclusionDetails icdExclusionDetails = new ICDExclusionDetails(exclude_ICD_from, exclude_ICD_To);
                    boolean alreadyExisted = false;
                    for (com.accumed.model.ICDExclusionMaster cg : list) {
                        if (cg.getCodeFrom().equals(master_ICD_from) && cg.getCodeTo().equals(icdExclusionMaster.getCodeTo())) {
                            if (cg.getItems() != null) {
                                cg.getItems().add(icdExclusionDetails);

                            } else {
                                List<ICDExclusionDetails> ICDExclusionDetailsList = new ArrayList<ICDExclusionDetails>();
                                ICDExclusionDetailsList.add(icdExclusionDetails);
                                cg.setItems(ICDExclusionDetailsList);
                            }
                            alreadyExisted = true;
                            break;
                        }
                    }
                    if (!alreadyExisted) {
                        List<ICDExclusionDetails> icdExclusionDetailsList = new ArrayList<ICDExclusionDetails>();
                        icdExclusionDetailsList.add(icdExclusionDetails);
                        icdExclusionMaster.setItems(icdExclusionDetailsList);
                        list.add(icdExclusionMaster);
                    }
                }
                list.size();
                for (ICDExclusionMaster icdExclusionMaster:list){
                if (icdExclusionMaster.containsCode("A15.1")){
                    icdExclusionMaster.itemContainsCode("A15.0");
                }
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
}
    public static CachedData getICDContradictories(Connection db, String logicalName) {
        long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ICDContradictory> list = new ArrayList<>();
        try {
            tables.put("RULES_ICDCONTRADICTORY_LINKS", getTableChecksum(db, "RULES_ICDCONTRADICTORY_LINKS"));
            tables.put("RULES_ICDCONTRADICTORY_SETS", getTableChecksum(db, "RULES_ICDCONTRADICTORY_SETS"));
            tables.put("RULES_ICDCONTRADICTORY_CODES", getTableChecksum(db, "RULES_ICDCONTRADICTORY_CODES"));

            String Sql = "SELECT l.id, c1.ICD_CODE as code1, c2.ICD_CODE as code2 "
                    + "FROM RULES_ICDCONTRADICTORY_LINKS l \n"
                    + "INNER JOIN RULES_ICDCONTRADICTORY_SETS s1 ON s1.ID = L.ID_FIRST_SETS\n"
                    + "INNER JOIN RULES_ICDCONTRADICTORY_SETS s2 ON s2.ID = L.ID_SECOND_SETS\n"
                    + "INNER JOIN RULES_ICDCONTRADICTORY_CODES c1 ON c1.SET_ID = s1.id\n"
                    + "INNER JOIN RULES_ICDCONTRADICTORY_CODES c2 ON c2.SET_ID = s2.id\n"
                    + "ORDER BY l.id";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Integer linkId = rs.getInt("id");
                    String code1 = rs.getString("code1");
                    String code2 = rs.getString("code2");

                    list.add(new com.accumed.model.ICDContradictory(linkId, code1, code2));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
 public static CachedData getICDAcuteChronic(Connection db, String logicalName) {
 long lBegin = System.nanoTime();
        java.util.HashMap<String, Long> tables = new java.util.HashMap<>();
        List<com.accumed.model.ICDAcuteChronic> list = new ArrayList<>();
        try {
            tables.put("RULE_ACUTE_CHRONIC_ICDS", getTableChecksum(db, "RULE_ACUTE_CHRONIC_ICDS"));
             String Sql = "select  ACUTE_ICD, chronic_ICD from  RULE_ACUTE_CHRONIC_ICDS ";

            PreparedStatement ps = db.prepareStatement(Sql);
            ResultSet rs = ps.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    
                    String code1 = rs.getString("ACUTE_ICD");
                    String code2 = rs.getString("chronic_ICD");

                    list.add(new com.accumed.model.ICDAcuteChronic(code1, code2));
                }
                rs.close();
                rs = null;
            }
            ps.close();
            ps = null;

        } catch (SQLException ex) {
            Logger.getLogger(RepoUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RepoUtils.class.getName()).log(Level.INFO, logicalName + " loading time is {0}ms.",
                (Long.valueOf((System.nanoTime() - lBegin) / 1000000)).toString());
        return new CachedData(logicalName, tables, list);
    }
}
