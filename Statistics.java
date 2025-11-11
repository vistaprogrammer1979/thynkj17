/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import com.accumed.jpa.AccumedFacilitySchema;
import com.accumed.jpa.AccumedGroupCodes;
import com.accumed.jpa.FacilityCodeSchema;
import com.accumed.jpa.FacilityReceiverSchema;
import com.accumed.jpaSession.request.RequestFacadeLocal;
//import com.accumed.mb.linerPair;
//import com.accumed.mb.routedClaim;
//import com.accumed.model.Facility;
//import com.accumed.mb.Insurer;
//import com.accumed.mb.XCode;
import com.accumed.validation.ClaimHistory;
import com.accumed.validation.Utils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.drools.KnowledgeBase;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.rule.Rule;

/**
 *
 * @author smutlak
 */
public class Statistics {

    static {
        serviceInitialization = 1;
        serviceInitializationException = "";
        requestsCount = 0;
        claimsCount = 0;
        exceptionsList = new ArrayList<>();

        lastRequestTakenTime = 0L;
        totalRequestTakenTime = 0L;
        totalClaims = 0L;
        maxRequestTime = 0L;
        maxRequestTime_ClamsCount = 0;
    }
    private static Integer serviceInitialization;  //1 not initialized 2 in Initialzation 3 Errors While Initialzation 4 Initialized
    private static String serviceInitializationException;
    private static Integer requestsCount;
    private static Integer claimsCount;
    private static Date startTime;
    private static List<DatedException> exceptionsList;
    //validator pool
    private static Integer validatorCount;
    private static Integer validatorValidCount;
    private static Integer validatorLockedCount;
    private static Integer validatorUnlockedCount;
    //performance
    private static Long lastRequestTakenTime;
    private static Long totalRequestTakenTime;
    private static Long totalClaims;
    private static Long maxRequestTime;
    private static Integer maxRequestTime_ClamsCount;
    private static Integer maxRequestTime_id;
    public static List<AccumedRule> packagesRules;

    public static boolean logHistory = false;
    public static boolean disableDBRules = false;

    /*public static BatchValidationFacadeLocal getFacade() {
     try {
     InitialContext ic = new InitialContext();
     return (BatchValidationFacadeLocal) ic.lookup("java:global/RulesEngine/BatchValidationFacade!com.accumed.webservices.BatchValidationFacadeLocal");
     } catch (NamingException ex) {
     Statistics.addException(ex);
     Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
     }
     return null;
     }*/
    public static RequestFacadeLocal getFacade() {
        try {
            InitialContext ic = new InitialContext();
            //return (BatchValidationFacadeLocal) ic.lookup("java:global/RulesEngine/BatchValidationFacade!com.accumed.webservices.BatchValidationFacadeLocal");
            return (RequestFacadeLocal) ic.lookup("java:global/RulesEngine/RequestFacade!com.accumed.jpaSession.request.RequestFacadeLocal");
            //comment
        } catch (NamingException ex) {
            Statistics.addException(ex);
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Returns an ArrayList of ArrayLists of Strings extracted from a ResultSet
     * retrieved from the database.
     *
     * @param resultSet ResultSet to extract Strings from
     * @return an ArrayList of ArrayLists of Strings
     * @throws SQLException if an SQL exception occurs
     */
    public static ArrayList<ArrayList<String>> extract(ResultSet resultSet)
            throws SQLException {
        ArrayList<ArrayList<String>> table;
        int columnCount = resultSet.getMetaData().getColumnCount();

        if (resultSet.getType() == ResultSet.TYPE_FORWARD_ONLY) {
            table = new ArrayList<>();
        } else {
            resultSet.last();
            table = new ArrayList<>(resultSet.getRow());
            resultSet.beforeFirst();
        }

        for (ArrayList<String> row; resultSet.next(); table.add(row)) {
            row = new ArrayList<>(columnCount);

            for (int c = 1; c <= columnCount; ++c) {
                if (resultSet != null && resultSet.getString(c) != null) {
                    row.add(resultSet.getString(c).intern());
                }
            }
        }

        return table;
    }

    private static ArrayList<ArrayList<String>> executeQuery(String sQuery) {
        java.sql.Connection connection = null;
        ResultSet rs = null;
        Statement stmt = null;
        int orginal = -1;
        try {
            connection = getFacade().getRulesDataSource().getConnection();
            orginal = connection.getTransactionIsolation();
            connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);

            stmt = connection.createStatement();
            rs = stmt.executeQuery(sQuery);
            return extract(rs);
        } catch (SQLException ex) {
            if (ex.getMessage().equalsIgnoreCase("The statement did not return a result set.")) {
                Logger.getLogger(Statistics.class.getName()).log(Level.WARNING, "The statement did not return a result set >>>>> " + sQuery);
            } else {
                Statistics.addException(ex);
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
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
                if (connection != null) {
                    try {
                        if (orginal > -1) {
                            connection.setTransactionIsolation(orginal);
                        }
                        connection.close();
                        connection = null;
                    } catch (SQLException ex) {
                        Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (SQLException ex) {
                Statistics.addException(ex);
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static ArrayList<ArrayList<String>> executeAccumedDBQuery(String sQuery) {
        java.sql.Connection connection = null;
        ResultSet rs = null;
        Statement stmt = null;
        int orginal = -1;
        try {
            connection = getFacade().getQueryDataSource().getConnection();
            orginal = connection.getTransactionIsolation();
            connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sQuery);
            return extract(rs);
        } catch (SQLException ex) {
            Statistics.addException(ex);
            Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            return null;
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
                if (connection != null) {
                    try {
                        if (orginal > -1) {
                            connection.setTransactionIsolation(orginal);
                        }
                        connection.close();
                        connection = null;
                    } catch (SQLException ex) {
                        Statistics.addException(ex);
                        Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (SQLException ex) {
                Statistics.addException(ex);
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

//    public static List<Facility> getFacilities(String prefix) {
//        List<Facility> facilities = new ArrayList<Facility>();
//        String sQuery = "SELECT  TOP 10 FACILITY_NAME,FACILITY_LICENSE,REGULATOR  FROM ACCUMED_FACILITY where FACILITY_LICENSE LIKE '%" + prefix.trim() + "%' or FACILITY_NAME like '%" + prefix.trim() + "%' ";
//        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
//        if (rs != null) {
//            int i = 0;
//            for (ArrayList<String> facil : rs) {
//                Facility item = new Facility();
//                item.setName(rs.get(i).get(0));
//                item.setLicense(rs.get(i).get(1));
//                item.setRegulator(Integer.parseInt(rs.get(i).get(2)));
//                facilities.add(item);
//                i++;
//            }
//        }
//
//        return facilities;
//    }
    public static Integer getAccumedGroupCodeID(int groupID, String from, String to) {
        String sQuery = "select TOP 1  ID   FROM ACCUMED_GROUP_CODES where "
                + "GROUP_ID='" + groupID + "' AND [FROM]='" + from + "'";
        if (to != null) {
            sQuery = sQuery + " AND  [TO]='" + to + "'";
        } else {
            sQuery = sQuery + " AND  [TO] is null ";
        }
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        AccumedGroupCodes gcode = new AccumedGroupCodes();
        if (rs != null) {

            return Integer.parseInt(rs.get(0).get(0));
        }
        return 0;
    }

    public static boolean isValidCode(String code, int type) {
        String select = "SELECT   TOP 1 1  FROM  ";
        String CPT = "   ACCUMED_CPT ";
        String SERVICE = "  ACCUMED_SERVICE";
        String HCPCS = "   ACCUMED_HCPCS";
        String DRUG = "   ACCUMED_DRUG";
        String DENTAL = "      ACCUMED_DENTAL";
        String ICD = "     ACCUMED_ICD";
        String sQuery = " where CODE='" + code + "'";
        switch (type) {
            case 3:
                sQuery = select + CPT + sQuery;
                break;
            case 5:
                sQuery = select + DRUG + sQuery;
                break;
            case 6:
                sQuery = select + DENTAL + sQuery;
                break;
            case 8:
                sQuery = select + SERVICE + sQuery;
                break;
            case 4:
                sQuery = select + HCPCS + sQuery;
                break;
            case 0:
                sQuery = select + ICD + sQuery;
                break;
            case 2:
                sQuery = select + ICD + sQuery;
                break;

        }
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            if (rs.size() == 0) {
                return false;
            } else if (Integer.parseInt(rs.get(0).get(0)) == 1) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidICD(String code, String type) {
        String select = "SELECT   TOP 1 1  FROM  ";

        String ICD = "     ACCUMED_ICD";
        String sQuery = " where CODE='" + code + "' AND type='ICD" + type + "'";

        sQuery = select + ICD + sQuery;

        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            if (rs.size() == 0) {
                return false;
            } else if (Integer.parseInt(rs.get(0).get(0)) == 1) {
                return true;
            }
        }
        return false;
    }

//    public static List<com.accumed.mb.XCode> getCodesRange(String from, String to, int type) {
//        List<com.accumed.mb.XCode> codes = new ArrayList<com.accumed.mb.XCode>();
//        String CPT = "SELECT       CODE,IsNull(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_CPT";
//        String SERVICE = "SELECT   CODE,IsNull(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_SERVICE";
//        String HCPCS = "SELECT     CODE,IsNull(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_HCPCS";
//        String DRUG = "SELECT      CODE,IsNull(Name, '-')               FROM     ACCUMED_DRUG";
//        String DENTAL = "SELECT    CODE,IsNull(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_DENTAL";
//        String ICD = "SELECT    CODE,IsNull(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_ICD";
//        String sQuery = " where  code  between '" + from + "' and  '" + to + "' ";
//        switch (type) {
//            case 3:
//                sQuery = CPT + sQuery;
//                break;
//            case 5:
//                sQuery = DRUG + sQuery;
//                break;
//            case 8:
//                sQuery = SERVICE + sQuery;
//                break;
//            case 4:
//                sQuery = HCPCS + sQuery;
//                break;
//            case 0:
//                sQuery = ICD + sQuery;
//                break;
//            case 2:
//                sQuery = ICD + sQuery;
//                break;
//            case 6:
//                sQuery = DENTAL + sQuery;
//                break;
//
//        }
//        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
//        String desc = "";
//        if (rs != null) {
//            int i = 0;
//            for (ArrayList<String> code : rs) {
//                com.accumed.mb.XCode xcode = new com.accumed.mb.XCode();
//                xcode.setCode(rs.get(i).get(0));
//                xcode.setShortDescription(rs.get(i).get(1));
//                codes.add(xcode);
//                i++;
//            }
//        }
//        return codes;
//    }
    public static boolean isGroupCodeExisted(String from, String to, Integer groupID) {
        String sQuery = "select  TOP 1  1  FROM ACCUMED_GROUP_CODES where GROUP_ID='" + groupID + "'"
                + " AND [FROM]='" + from + "' ";
        String and = "AND  [TO]='" + to + "'";
        if (to == null) {
            and = " AND  [TO] is  null";
        }
        sQuery = sQuery + and;
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            if (rs.size() == 0) {
                return false;
            } else if (Integer.parseInt(rs.get(0).get(0)) == 1) {
                return true;
            }
        }
        return false;

    }

    public static boolean isCodeGroupExisted(String groupName) {
        String sQuery = "select  TOP 1  1  FROM ACCUMED_CODE_GROUPS where  NAME ='" + groupName + "' ";
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            if (rs.size() == 0) {
                return false;
            } else if (Integer.parseInt(rs.get(0).get(0)) == 1) {
                return true;
            }
        }
        return false;

    }

    public static List<Integer> getGroupCodesList(Integer groupID) {

        List<Integer> codesId = new ArrayList<Integer>();
        String sQuery = " select  ID FROM    ACCUMED_GROUP_CODES where  GROUP_ID='" + groupID + "'";

        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    codesId.add(Integer.parseInt(rs.get(i).get(0)));
                } catch (Exception ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }
        }
        return codesId;
    }

    public static List<Integer> getGroupChildrenList(Integer parentID) {

        List<Integer> groupIds = new ArrayList<Integer>();
        String sQuery = " select  ID FROM    ACCUMED_CODE_GROUPS where PARENT_ID='" + parentID + "'";

        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    groupIds.add(Integer.parseInt(rs.get(i).get(0)));
                } catch (Exception ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }
        }
        return groupIds;
    }

//    public static List<XCode> getCodesAutoComplete(String prefix, int type) {
//        List<XCode> codes = new ArrayList<XCode>();
//        String CPT = "SELECT  TOP 10  CODE,ISNULL(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_CPT";
//        String SERVICE = "SELECT  TOP 10  CODE,ISNULL(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_SERVICE";
//        String HCPCS = "SELECT  TOP 10  CODE,ISNULL(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_HCPCS";
//        String DRUG = "SELECT  TOP 10  CODE,ISNULL(Name, '-')  FROM     ACCUMED_DRUG";
//        String DENTAL = "SELECT  TOP 10  CODE,ISNULL(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_DENTAL";
//        String ICD = "SELECT  TOP 10  CODE,ISNULL(SHORT_DESCRIPTION, '-')  FROM     ACCUMED_ICD";
//        String sQuery = "  where CODE LIKE '" + prefix + "%' ";
//        switch (type) {
//            case 3:
//                sQuery = CPT + sQuery;
//                break;
//            case 5:
//                sQuery = DRUG + sQuery;
//                break;
//            case 8:
//                sQuery = SERVICE + sQuery;
//                break;
//            case 4:
//                sQuery = HCPCS + sQuery;
//                break;
//            case 2:
//                sQuery = ICD + sQuery;
//                break;
//            case 0:
//                sQuery = ICD + sQuery;
//                break;
//            case 6:
//                sQuery = DENTAL + sQuery;
//                break;
//
//        }
//        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
//        if (rs != null) {
//            int i = 0;
//            for (ArrayList<String> code : rs) {
//                XCode xcode = new XCode();
//                xcode.setCode(rs.get(i).get(0));
//                xcode.setShortDescription(rs.get(i).get(1));
//                codes.add(xcode);
//                i++;
//            }
//        }
//        return codes;
//    }
    public static boolean check_Facility_Code_Schema_Duplication(String facilityLic, String recieverLic, java.util.Date effictiveDate, java.util.Date expiryDate, Integer type) {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "select  top 1 1  FROM BS_FACILITY_CODE_SCHEMA"
                + " where FACILITY_LICENSE='" + facilityLic + "' AND INSURANCE_LICENSE='" + recieverLic + "' "
                + "AND TYPE='" + type + "' AND IsDeleted=0 AND "
                + " (Convert(varchar(50),  '" + fmt.format(effictiveDate) + "', 103) BETWEEN EFFECTIVE_DATE AND EXPIRY_DATE OR "
                + "   (Convert(varchar(50),  '" + fmt.format(expiryDate) + "', 103) BETWEEN EFFECTIVE_DATE AND EXPIRY_DATE)) ";
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            if (rs.size() == 0) {
                return false;
            } else if (Integer.parseInt(rs.get(0).get(0)) == 1) {
                return true;
            }
        }
        return false;
    }

    public static List<FacilityCodeSchema> fetch_Facility_Code_Schema_Duplication(String facilityLic, String recieverLic, java.util.Date effictiveDate, java.util.Date expiryDate, Integer type) {
        List<FacilityCodeSchema> faciltiySchemaItems = new ArrayList<FacilityCodeSchema>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "SELECT  ID, FACILITY_LICENSE, INSURANCE_LICENSE,EFFECTIVE_DATE,EXPIRY_DATE,TYPE,REGULATOR_SCHEMA FROM BS_FACILITY_CODE_SCHEMA  where  IsDeleted=0 "
                + " and FACILITY_LICENSE='" + facilityLic + "' AND INSURANCE_LICENSE='" + recieverLic + "' "
                + "AND TYPE='" + type + "' AND IsDeleted=0 AND "
                + "(Convert(varchar(50),  '" + fmt.format(effictiveDate) + "', 103) BETWEEN EFFECTIVE_DATE AND EXPIRY_DATE "
                + " or (Convert(varchar(50),  '" + fmt.format(effictiveDate) + "', 103) < EFFECTIVE_DATE and "
                + " Convert(varchar(50),  '" + fmt.format(expiryDate) + "', 103) >= EFFECTIVE_DATE )"
                + "  ) ";
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    FacilityCodeSchema item = new FacilityCodeSchema();
                    item.setId(Integer.parseInt(rs.get(i).get(0)));
                    item.setFacilityLicense(rs.get(i).get(1));
                    item.setInsuranceLicense(rs.get(i).get(2));
                    item.setEffectiveDate(fmt.parse(rs.get(i).get(3)));
                    item.setExpiryDate(fmt.parse(rs.get(i).get(4)));
                    item.setType(Integer.parseInt(rs.get(i).get(5)));
                    item.setRegulatorSchema(Integer.parseInt(rs.get(i).get(6)));
                    faciltiySchemaItems.add(item);
                } catch (ParseException ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }

        }
        return faciltiySchemaItems;
    }

    public static List<FacilityReceiverSchema> fetch_Facility_Reciever_Schema_Duplication(String facilityLic, String recieverLic, java.util.Date effictiveDate, java.util.Date expiryDate) {
        List<FacilityReceiverSchema> faciltiySchemaItems = new ArrayList<FacilityReceiverSchema>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "SELECT  ID, FACILITY_LICENSE, INSURANCE_LIC,EFFECTIVE_DATE,EXPIRY_DATE, USED_INSURANCE_LICENSE FROM BS_FACILITY_RECEIVER_SCHEMA "
                + " where FACILITY_LICENSE='" + facilityLic + "'"
                + " AND INSURANCE_LIC='" + recieverLic + "' "
                + " AND IsDeleted=0 AND "
                + "(Convert(varchar(50),  '" + fmt.format(effictiveDate) + "', 103) BETWEEN EFFECTIVE_DATE AND EXPIRY_DATE "
                + " or (Convert(varchar(50),  '" + fmt.format(effictiveDate) + "', 103) < EFFECTIVE_DATE and "
                + " Convert(varchar(50),  '" + fmt.format(expiryDate) + "', 103) >= EFFECTIVE_DATE )"
                + "  ) ";
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    FacilityReceiverSchema item = new FacilityReceiverSchema();
                    item.setId(Integer.parseInt(rs.get(i).get(0)));
                    item.setFacilityLicense(rs.get(i).get(1));
                    item.setInsuranceLic(rs.get(i).get(2));
                    item.setEffectiveDate(fmt.parse(rs.get(i).get(3)));
                    item.setExpiryDate(fmt.parse(rs.get(i).get(4)));
                    item.setUsedInsuranceLicense(rs.get(i).get(5));
                    faciltiySchemaItems.add(item);
                } catch (ParseException ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }

        }
        return faciltiySchemaItems;
    }

    public static boolean check_Facility_Reciever_Schema_Duplication(String facilityLic, String recieverLic, java.util.Date effictiveDate, java.util.Date expiryDate) {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "select  top 1 1  FROM BS_FACILITY_RECEIVER_SCHEMA "
                + " where FACILITY_LICENSE='" + facilityLic + "'"
                + " AND INSURANCE_LIC='" + recieverLic + "' "
                + " AND IsDeleted=0 AND "
                + " (Convert(varchar(50),  '" + fmt.format(effictiveDate) + "', 103) BETWEEN EFFECTIVE_DATE AND EXPIRY_DATE OR "
                + " (Convert(varchar(50),  '" + fmt.format(expiryDate) + "', 103)  BETWEEN EFFECTIVE_DATE AND EXPIRY_DATE)) ";
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            if (rs.size() == 0) {
                return false;
            } else if (Integer.parseInt(rs.get(0).get(0)) == 1) {
                return true;
            }
        }
        return false;
    }

//    public static List<Insurer> getInsuranceLicense(int regulator, String prefix) {
//        List<Insurer> LicList = new ArrayList<Insurer>();
//        String sQuery = "SELECT top 10 AUTH_NO,COMPANY_NAME FROM  ACCUMED_INSURERS where REGULATOR=" + regulator + " AND AUTH_NO LIKE '%" + prefix + "%' or COMPANY_NAME like '%" + prefix + "%'";
//        if (regulator == 0) {
//            sQuery = "SELECT top 10 AUTH_NO,COMPANY_NAME FROM  ACCUMED_INSURERS where  AUTH_NO LIKE '%" + prefix + "%' or COMPANY_NAME like '%" + prefix + "%'";
//        }
//        if (regulator == 3) {
//            sQuery = "SELECT  AUTH_NO,COMPANY_NAME FROM  ACCUMED_INSURERS where  AUTH_NO LIKE '%" + prefix + "%' or COMPANY_NAME like '%" + prefix + "%'";
//        }
//        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
//        if (rs != null) {
//            int i = 0;
//            for (ArrayList<String> facil : rs) {
//                Insurer item = new Insurer();
//                item.setLicense(rs.get(i).get(0));
//                item.setName(rs.get(i).get(1));
//
//                LicList.add(item);
//                i++;
//            }
//        }
//
//        return LicList;
//    }
    public static List<AccumedFacilitySchema> getFaciltiySchemaInsurersItems(String facilityLisence) {
        List<AccumedFacilitySchema> faciltiySchemaItems = new ArrayList<AccumedFacilitySchema>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "SELECT  ID, FACILITY_LICENSE,HAAD_INSURANCE_LIC,DHA_INSURANCE_LIC,EFFECTIVE_DATE,EXPIRY_DATE,TYPE,REGULATOR_SCHEMA,USED_INSURANCE_LICENSE  FROM ACCUMED_FACILITY_SCHEMA where FACILITY_LICENSE ='" + facilityLisence + "'";
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    AccumedFacilitySchema item = new AccumedFacilitySchema();
                    item.setId(Integer.parseInt(rs.get(i).get(0)));
                    item.setFacilityLicense(rs.get(i).get(1));
                    item.setHaadInsuranceLic(rs.get(i).get(2));
                    item.setDhaInsuranceLic(rs.get(i).get(3));
                    item.setEffectiveDate(fmt.parse(rs.get(i).get(4)));
                    item.setExpiryDate(fmt.parse(rs.get(i).get(5)));
                    item.setType(Integer.parseInt(rs.get(i).get(6)));
                    item.setRegulatorSchema(Integer.parseInt(rs.get(i).get(7)));
                    item.setUsedInsuranceLicense(rs.get(i).get(8));
                    boolean ok = true;
                    if (facilityLisence != null) {
                        for (AccumedFacilitySchema f : faciltiySchemaItems) {
                            if (f.getEffectiveDate().equals(item.getEffectiveDate())
                                    && f.getDhaInsuranceLic().equals(item.getDhaInsuranceLic())
                                    && f.getHaadInsuranceLic().equals(item.getHaadInsuranceLic())) {
                                ok = false;
                            }
                        }
                    }
                    if (ok) {
                        faciltiySchemaItems.add(item);
                    }
                } catch (ParseException ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }

        }
        return faciltiySchemaItems;
    }

    public static List<FacilityCodeSchema> getFaciltiyCodeSchema(String facilityLisence) {
        List<FacilityCodeSchema> faciltiySchemaItems = new ArrayList<FacilityCodeSchema>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "SELECT  ID, FACILITY_LICENSE, INSURANCE_LICENSE,EFFECTIVE_DATE,EXPIRY_DATE,TYPE,REGULATOR_SCHEMA FROM BS_FACILITY_CODE_SCHEMA  where  IsDeleted=0 ";
        if (facilityLisence != "") {
            sQuery = sQuery + "AND FACILITY_LICENSE='" + facilityLisence + "'";
        }
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    FacilityCodeSchema item = new FacilityCodeSchema();
                    item.setId(Integer.parseInt(rs.get(i).get(0)));
                    item.setFacilityLicense(rs.get(i).get(1));
                    item.setInsuranceLicense(rs.get(i).get(2));
                    item.setEffectiveDate(fmt.parse(rs.get(i).get(3)));
                    item.setExpiryDate(fmt.parse(rs.get(i).get(4)));
                    item.setType(Integer.parseInt(rs.get(i).get(5)));
                    item.setRegulatorSchema(Integer.parseInt(rs.get(i).get(6)));
                    faciltiySchemaItems.add(item);
                } catch (ParseException ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }

        }
        return faciltiySchemaItems;
    }

    public static List<FacilityReceiverSchema> getFaciltiyReceiverSchema(String facilityLisence) {
        List<FacilityReceiverSchema> faciltiySchemaItems = new ArrayList<FacilityReceiverSchema>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "SELECT  ID, FACILITY_LICENSE, INSURANCE_LIC,EFFECTIVE_DATE,EXPIRY_DATE, USED_INSURANCE_LICENSE FROM BS_FACILITY_RECEIVER_SCHEMA  where  IsDeleted=0 ";
        if (facilityLisence != "") {
            sQuery = sQuery + "AND FACILITY_LICENSE='" + facilityLisence + "'";
        }
        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
        if (rs != null) {
            int i = 0;
            for (ArrayList<String> Results : rs) {
                try {
                    FacilityReceiverSchema item = new FacilityReceiverSchema();
                    item.setId(Integer.parseInt(rs.get(i).get(0)));
                    item.setFacilityLicense(rs.get(i).get(1));
                    item.setInsuranceLic(rs.get(i).get(2));
                    item.setEffectiveDate(fmt.parse(rs.get(i).get(3)));
                    item.setExpiryDate(fmt.parse(rs.get(i).get(4)));
                    item.setUsedInsuranceLicense(rs.get(i).get(5));
                    faciltiySchemaItems.add(item);
                } catch (ParseException ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }

        }
        return faciltiySchemaItems;
    }

    public static Integer getServiceInitialization() {
        return serviceInitialization;
    }

    public static void setServiceInitialization(Integer serviceInitialization) {
        Statistics.serviceInitialization = serviceInitialization;
    }

    public static String getServiceInitializationException() {
        return serviceInitializationException;
    }

    public static void setServiceInitializationException(String serviceInitializationException) {
        Statistics.serviceInitializationException = serviceInitializationException;
    }

    public static Integer getRequestsCount() {
        return requestsCount;
    }

    public static void increaseRequestCount() {
        requestsCount++;
    }

    public static void setRequestsCount(Integer requestsCount) {
        Statistics.requestsCount = requestsCount;
    }

    public static Integer getClaimsCount() {
        return claimsCount;
    }

    public static void increaseClaimsCount(int claimsCount) {
        Statistics.claimsCount += claimsCount;
    }

    public static void setClaimsCount(Integer claimsCount) {
        Statistics.claimsCount = claimsCount;
    }

    public static Date getStartTime() {
        return startTime;
    }

    public static void setStartTime(Date startTime) {
        Statistics.startTime = startTime;
    }

    public static List<DatedException> getExceptionsList() {
        return exceptionsList;
    }

    public static void setExceptionsList(List<DatedException> exceptionsList) {
        Statistics.exceptionsList = exceptionsList;
    }

    /*
     * period = 0 since the start of the service
     * period = 1 in the last 24 hours
     * period = 2 in the last 12 hours
     * period = 3 in the last 6 hours
     * period = 4 in the last 3 hours
     * period = 5 in the last 1 hour
     * 
     */
    public static Double getAverageRequestPerMinute(int period) {

        // BatchValidationFacadeLocal local = getFacade();
        Integer local_requestsCount = Integer.valueOf(0);
        Date currDate = new Date();
        long millis = 0;
        int minutes = 0;
        String pre = "SELECT count(1)  FROM  ScrubRequest where requestTime> DATEADD(hour, -";
        String suf = ", getdate())";

        switch (period) {
            case 0: {
                /*if (startTime != null) {
                 millis = currDate.getTime() - startTime.getTime();
                 }*/
                ArrayList<ArrayList<String>> rs = executeQuery("SELECT count(1)  FROM  ScrubRequest where requestTime> DATEADD(minute, -1 , getdate())");
                if (rs != null) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = 1;
                //local_requestsCount = requestsCount;
            }
            break;
            case 1: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 24));
                if (rs != null) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (24 * 60);

            }
            break;
            case 2: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 12));
                if (rs != null) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (12 * 60);

            }
            break;
            case 3: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 6));
                if (rs != null) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (6 * 60);

            }
            break;
            case 4: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 3));
                if (rs != null) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (3 * 60);

            }
            break;
            case 5: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 1));
                if (rs != null) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (1 * 60);

            }
            break;
        }

        if (minutes > 0 && local_requestsCount != null) {
            Double d = Double.valueOf((double) local_requestsCount / (double) minutes);
            return d = Math.round(d * 100.0) / 100.0;
        }
        return Double.valueOf(0.0);

    }

    /*
     * period = 0 since the start of the service
     * period = 1 in the last 24 hours
     * period = 2 in the last 12 hours
     * period = 3 in the last 6 hours
     * period = 4 in the last 3 hours
     * period = 5 in the last 1 hour
     * 
     */
    public static Double getAverageClaimsPerMinute(int period) {
        Integer local_requestsCount = Integer.valueOf(0);
        Date currDate = new Date();
        long millis = 0;
        int minutes = 0;
        String pre = "SELECT  count(1) FROM  ScrubRequest where requestTime> DATEADD(hour, -";
        String suf = ", getdate())";

        switch (period) {
            case 0: {
                /*if (startTime != null) {
                 millis = currDate.getTime() - startTime.getTime();
                 }*/
                //minutes = (int)((float) millis / (float) (1000 * 60));
                //local_requestsCount = claimsCount;
                ArrayList<ArrayList<String>> rs = executeQuery("SELECT  count(1)  FROM  ScrubRequest where requestTime> DATEADD(minute, -1, getdate())");
                if (rs != null && !rs.isEmpty() && !rs.get(0).isEmpty()) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = 1;
            }
            break;
            case 1: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 24));
                if (rs != null && !rs.isEmpty() && !rs.get(0).isEmpty()) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (24 * 60);
            }
            break;
            case 2: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 12));
                if (rs != null && !rs.isEmpty() && !rs.get(0).isEmpty()) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (12 * 60);
            }
            break;
            case 3: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 6));
                if (rs != null && !rs.isEmpty() && !rs.get(0).isEmpty()) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (6 * 60);
            }
            break;
            case 4: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 3));
                if (rs != null && !rs.isEmpty() && !rs.get(0).isEmpty()) {
                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (3 * 60);
            }
            break;
            case 5: {
                ArrayList<ArrayList<String>> rs = executeQuery(buildQuery(pre, suf, 1));
                if (rs != null && !rs.isEmpty() && !rs.get(0).isEmpty()) {

                    local_requestsCount = Integer.valueOf(rs.get(0).get(0));
                }
                minutes = (1 * 60);
            }
            break;
        }

        if (minutes > 0 && local_requestsCount != null) {
            Double f = Double.valueOf((double) local_requestsCount / (double) minutes);
            return f = Math.round(f * 100.0) / 100.0;
        }
        return Double.valueOf(0.0);
    }

    public static String getInitializationStatus() {
        String initializationStatus = "";

        switch (getServiceInitialization()) {
            case 1:
                initializationStatus = "not initialized";

                break;
            case 2:
                initializationStatus = "Initializing ...";
                break;
            case 3:
                initializationStatus = "Errors While Initializing";

                break;
            case 4:
                initializationStatus = "Initialized";
                break;
        }
        return initializationStatus;
    }

    public static String buildQuery(String pre, String suf, int hours) {
        return "" + pre + hours + suf;
    }

    private static String stackTraceToString(Exception e) {
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

    public static void addException(Exception e) {
        addException(e, "");
    }

    public static void addException(Exception e, com.accumed.model.scrubRequest.ScrubRequest req) {
        addException(e, "");
    }

    public static void addException(Exception e, String sRequest) {
        DatedException de = new DatedException();
        de.setDate(new Date());
        de.setException(e);
        de.setExceptionStr(stackTraceToString(e));
        de.setRequest(sRequest);
        if (exceptionsList.size() >= 20) {
            /*for (int i = 0; i < 20; i++) {
             exceptionsList.set(i, exceptionsList.get(i + 1));
             }*/
            exceptionsList.remove(19);
        }
        exceptionsList.add(de);
    }

    public static void setValidatorPoolStatistics(Integer validatorCount,
            Integer validatorValidCount,
            Integer validatorLockedCount,
            Integer validatorUnlockedCount) {
        if (validatorCount != -1) {
            Statistics.validatorCount = validatorCount;
        }
        if (validatorValidCount != -1) {
            Statistics.validatorValidCount = validatorValidCount;
        }
        if (validatorLockedCount != -1) {
            Statistics.validatorLockedCount = validatorLockedCount;
        }
        if (validatorUnlockedCount != -1) {
            Statistics.validatorUnlockedCount = validatorUnlockedCount;
        }
    }

    public static Integer getValidatorCount() {
        return validatorCount;
    }

    public static void setValidatorCount(Integer validatorCount) {
        Statistics.validatorCount = validatorCount;
    }

    public static Integer getValidatorValidCount() {
        return validatorValidCount;
    }

    public static void setValidatorValidCount(Integer validatorValidCount) {
        Statistics.validatorValidCount = validatorValidCount;
    }

    public static Integer getValidatorLockedCount() {
        return validatorLockedCount;
    }

    public static void setValidatorLockedCount(Integer validatorLockedCount) {
        Statistics.validatorLockedCount = validatorLockedCount;
    }

    public static Integer getValidatorUnlockedCount() {
        return validatorUnlockedCount;
    }

    public static void setValidatorUnlockedCount(Integer validatorUnlockedCount) {
        Statistics.validatorUnlockedCount = validatorUnlockedCount;
    }

    /*public static List<UserRequest> getLastRequestInfo() {
     ArrayList<ArrayList<String>> rs = executeQuery("SELECT user_id, request, requestXML, responseXML FROM BatchValidation  where  user_id is not NULL"
     + " AND ID IN(SELECT max(ID) "
     + " FROM BatchValidation group BY user_id) order BY endDate desc");
     if (rs != null) {
     List<UserRequest> arrayofUserRequest = new ArrayList<UserRequest>();
     int i = 0;
     for (ArrayList<String> userResults : rs) {
     UserRequest userRequest = new UserRequest();
     userRequest.setUserID(rs.get(i).get(0));
     userRequest.setRequest(rs.get(i).get(1));
     //                userRequest.setRequestXML(rs.get(i).get(2));
     //               userRequest.setRequestXML(rs.get(i).get(3));
     i++;
     arrayofUserRequest.add(userRequest);
     }
     return arrayofUserRequest;
     }
     return null;
     }*/
    public static List<ClaimHistory> getClaimHistory(Integer claimHistory_ClaimID) {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sQuery = "SELECT TOP 100 r.ID, convert(varchar(255), r.requestTime, 121) as requestTime, "
                + "r.UserID, r.UserName "
                + " FROM ScrubRequest r INNER JOIN Claim c on c.request = r.ID "
                + " inner join Header h on h.requestID = r.ID "
                + "where c.idCaller = " + claimHistory_ClaimID + " "
                + "order by r.requestTime desc";
        /* ArrayList<ArrayList<String>> rs = executeQuery("SELECT user_id, request, requestXML, responseXML FROM BatchValidation  where  user_id is not NULL"
         + " AND ID IN(SELECT max(ID) "
         + " FROM BatchValidation group BY user_id) order BY endDate desc");*/
        ArrayList<ArrayList<String>> rs = executeQuery(sQuery);

        if (rs != null) {
            List<ClaimHistory> arrayofClaimHistory = new ArrayList<ClaimHistory>();
            int i = 0;
            for (ArrayList<String> claimResults : rs) {
                try {
                    ClaimHistory claimHistory = new ClaimHistory();
                    claimHistory.setRequestID(Integer.parseInt(rs.get(i).get(0)));
                    claimHistory.setValidationTime(fmt.parse(rs.get(i).get(1)));
                    claimHistory.setUserID(Integer.parseInt(rs.get(i).get(2)));
                    claimHistory.setUserName(rs.get(i).get(3));
                    arrayofClaimHistory.add(claimHistory);
                } catch (ParseException ex) {
                    Statistics.addException(ex);
                    Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
                }
                i++;
            }
            return arrayofClaimHistory;
        }
        return null;
    }

//    static boolean firstTime = true;
    public static void addRequestExecutingTime(long lTakenTime, int nClaims, int id) {
//        if (!firstTime) {
        if (requestsCount > 19) {
            Statistics.lastRequestTakenTime = lTakenTime;
            Statistics.totalRequestTakenTime += lTakenTime;
            Statistics.totalClaims += nClaims;
            if (lTakenTime > Statistics.maxRequestTime) {
                Statistics.maxRequestTime = lTakenTime;
                Statistics.maxRequestTime_ClamsCount = nClaims;
                Statistics.maxRequestTime_id = id;
            }

        }
//    else {
//            firstTime = false;
//        }
    }

    public static Long getLastRequestTakenTime() {
        return lastRequestTakenTime;
    }

    public static Long getTotalRequestTakenTime() {
        return totalRequestTakenTime;
    }

    public static Long getTotalClaims() {
        return totalClaims;
    }

    public static Double getAverageClaimValidationTime() {
        if (totalRequestTakenTime == 0) {
            return null;
        }
        Double ret;
        ret = Double.valueOf((double) totalRequestTakenTime / (double) totalClaims);
        return Math.round(ret * 100.0) / 100.0;
    }

//    public static List<linerPair> getRequestsPerMonth1() {
//        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM/yyyy");
//        List<linerPair> ret = new ArrayList<linerPair>();
//        /*Dynamicly Calculation */
////        ArrayList<ArrayList<String>> rs = executeQuery("Select Convert(varchar(50), dateadd(month, datediff(month, 0, startDate), 0), 103) As monthDate, count(ID) from ScrubRequest"
////                + "                 GROUP BY dateadd(month, datediff(month, 0, startDate), 0) "
////                + "                 ORDER BY dateadd(month, datediff(month, 0, startDate), 0) ");
//
//        executeQuery("exec UpdateRequestsPerMonth");
//        //  Staticly calculation
//        ArrayList<ArrayList<String>> rs = executeQuery("Select Convert(varchar(50),Month, 103) As monthDate, Requests_Count from  REQUEST_PER_MONTH order  by  Month  ");
//        for (int i = 0; i < rs.size(); i++) {
//            try {
//                ArrayList<String> s = rs.get(i);
//                if (s.size() > 0) {
//                    Date date = fmt.parse((String) s.get(0));
//                    Double d = Double.parseDouble(s.get(1));
//                    ret.add(new linerPair(date, d));
//                }
//            } catch (ParseException ex) {
//                Statistics.addException(ex);
//                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        //System.out.print(str);
//        return ret;
//    }
    public static String getRequestsPerMonth() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
        /*ArrayList<ArrayList<String>> rs = executeQuery("Select dateadd(month, datediff(month, 0, startDate), 0), count(1) from batchValidation "
         + "GROUP BY dateadd(month, datediff(month, 0, startDate), 0) "
         + "ORDER BY dateadd(month, datediff(month, 0, startDate), 0) ");
         */
        ArrayList<ArrayList<String>> rs = executeQuery("Select dateadd(month, datediff(month, 0, requestTime), 0) As monthDate, count(ID) from ScrubRequest "
                + "                 GROUP BY dateadd(month, datediff(month, 0, requestTime), 0) "
                + "                 ORDER BY dateadd(month, datediff(month, 0, requestTime), 0) ");
        String str = "";
        for (int i = 0; i < rs.size(); i++) {
            try {
                ArrayList s = rs.get(i);
                Date d = fmt.parse((String) s.get(0));
                str = str + " " + d.getTime() + "," + s.get(1);
                //str = str + " " + s.get(0).toString().split(" ")[0] + "," + s.get(1);
            } catch (ParseException ex) {
                Statistics.addException(ex);
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //System.out.print(str);
        return str;
    }

//    public static List<linerPair> getRequestsPerDay1() {
//        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM/yyyy");
//        List<linerPair> ret = new ArrayList<linerPair>();
//        ArrayList<ArrayList<String>> rs = executeQuery("SELECT  Convert(varchar(50),  requestTime, 103),count(ID) as dayDate from ScrubRequest "
//                + "                Where requestTime> dateadd(day, -30, getdate()) "
//                + "                group by Convert(varchar(50),  requestTime, 103)"
//                + "                ORDER by Convert(varchar(50),  requestTime, 103)");
//
//        for (int i = 0; i < rs.size(); i++) {
//            try {
//                ArrayList<String> s = rs.get(i);
//                Date date = fmt.parse((String) s.get(0));
//
//                Double d = Double.parseDouble(s.get(1));
//                ret.add(new linerPair(date, d));
//            } catch (ParseException ex) {
//                Statistics.addException(ex);
//                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        //System.out.print(str);
//        return ret;
//    }
    public static String getRequestsPerDay() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
        /*ArrayList<ArrayList<String>> rs = executeQuery("Select dateadd(DAY, datediff(DAY, 0, startDate), 0), count(1) from batchValidation \n"
         + "Where startDate> dateadd(day, -30, getdate()) "
         + "GROUP BY dateadd(DAY, datediff(DAY, 0, startDate), 0) "
         + "ORDER BY dateadd(DAY, datediff(DAY, 0, startDate), 0)  ");*/
        ArrayList<ArrayList<String>> rs = executeQuery("SELECT Convert(varchar(50),  requestTime, 103),count(ID) from ScrubRequest"
                + "                Where requestTime> dateadd(day, -30, getdate())"
                + "GROUP BY Convert(varchar(50),  requestTime, 103)");
        String str = "";
        for (int i = 0; i < rs.size(); i++) {
            try {
                ArrayList s = rs.get(i);
                Date d = fmt.parse((String) s.get(0));
                str = str + " " + d.getTime() + "," + s.get(1);
                // str = str + " " + s.get(0).toString().split(" ")[0] + "," + s.get(1);
            } catch (ParseException ex) {
                Statistics.addException(ex);
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //System.out.print(str);
        return str;
    }

    /*public static List<Map<String, String>> getAutoRoutedClaims() {
     ArrayList<ArrayList<String>> rs = executeQuery("SELECT CASE  WHEN C.claim_line_id IS  null THEN '' ELSE  C.claim_line_id END, C.claim_id, B.startDate from "+
     " BatchValidation B, ClaimValidation C "+
     " where C.batchID = B.ID "+
     " AND B.StartDate> dateadd(day, -30, getdate()) "+
     " AND B.StartDate = ( SELECT max(AR.startDate) from Rules_AutoRoutedClaimRequests AR " +
     "Where AR.claim_id = C.claim_id " +
     //                "and AR.StartDate> dateadd(day, -30, getdate()) " +
     "GROUP BY Ar.claim_id ) " +
     " ORDER BY B.startDate desc, C.claim_id desc "
     );
     if (rs != null) {
     List<Map<String, String>> arrayofMaps = new ArrayList<Map<String, String>>();
     int i = 0;
     for (ArrayList<String> userResults : rs) {
     Map<String, String> hmap = new HashMap<String, String>();
     hmap.put("claim_line_id", rs.get(i).get(0));               
     hmap.put("claim_id",rs.get(i).get(1));
     hmap.put("startDate", rs.get(i).get(2));                
     i++;
     arrayofMaps.add(hmap);
     }
     return arrayofMaps;
     }
     return null;
     }*/
    public static List<Map<String, String>> getAutoRoutedClaims() {
        /*ArrayList<ArrayList<String>> rs = executeQuery("SELECT CASE  WHEN C.claim_line_id IS  null THEN '' ELSE  C.claim_line_id END, C.claim_id, B.startDate from "+
         " BatchValidation B, ClaimValidation C "+
         " where B.StartDate> dateadd(day, -5, getdate()) AND C.batchID = B.ID "+
         " AND B.StartDate = (SELECT max(B1.startDate) from ClaimValidation C1, BatchValidation B1"+
         " Where C1.claim_id = C.claim_id "+
         " AND B1.ID = C1.batchID "+
         " and B1.StartDate> dateadd(day, -5, getdate()) "+                
         " AND B1.[responseXML].value('(/ValidationResponse/Workflow-Next-Node)[1]', 'varchar(max)') = 'Billing' "+                
         " GROUP BY C1.claim_id )" +
         " ORDER BY B.startDate desc, C.claim_id desc "
         );*/

        ArrayList<ArrayList<String>> rs = executeQuery(" Select TOP 20 claim_line_id , claim_id, max(requestTime) As maxDate "
                + " from routedRequests "
                + " Where requestTime> dateadd(day, -10, getdate())  "
                + " Group BY claim_id, claim_line_id ORDER BY maxDate "
        );
        if (rs != null) {
            List<Map<String, String>> arrayofMaps = new ArrayList<Map<String, String>>();
            int i = 0;
            for (ArrayList<String> userResults : rs) {
                Map<String, String> hmap = new HashMap<String, String>();
                hmap.put("claim_line_id", rs.get(i).get(0));
                hmap.put("claim_id", rs.get(i).get(1));
                hmap.put("requestTime", rs.get(i).get(2));
                i++;
                arrayofMaps.add(hmap);
            }
            return arrayofMaps;
        }
        return null;
    }

    /*public static String getAutoRoutedClaimsPerDay() {
     java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
     ArrayList<ArrayList<String>> rs = executeQuery("SELECT  dateadd(day, datediff(day, 0, B.StartDate), 0), count(C.claim_id)  "
     + " from BatchValidation B, ClaimValidation C"
     + " where C.batchID = B.ID AND "
     + " B.StartDate> dateadd(day, -30, getdate()) AND "
     + " B.StartDate = (" 
     + " SELECT max(AR.startDate) from Rules_AutoRoutedClaimRequests AR " 
     + " Where AR.claim_id = C.claim_id " 
     + " GROUP BY Ar.claim_id "
     +" ) "
     +" GROUP BY dateadd(day, datediff(day, 0, B.StartDate), 0)"
     +" ORDER BY dateadd(day, datediff(day, 0, B.StartDate), 0) desc ");
     String str = "";
     for (int i = 0; i < rs.size(); i++) {
     try {
     ArrayList s = rs.get(i);
     Date d = fmt.parse((String)s.get(0));
     str = str + " " + d.getTime()+ "," + s.get(1);
     } catch (ParseException ex) {
     Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
     }
     }
     System.out.print(str);
     return str;
     }*/
    public static String getAutoRoutedClaimsPerDay() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
        /*ArrayList<ArrayList<String>> rs = executeQuery("SELECT  dateadd(day, datediff(day, 0, B.StartDate), 0), count(C.claim_id)  "
         +" from BatchValidation B, ClaimValidation C"
         +" where B.StartDate> dateadd(day, -30, getdate()) AND C.batchID = B.ID AND B.StartDate = ("
         +" SELECT max(B1.startDate) from ClaimValidation C1, BatchValidation B1 "
         +" Where C1.claim_id = C.claim_id "
         +" AND B1.ID = C1.batchID "
         +" and B1.StartDate> dateadd(day, -30, getdate()) "
         +" AND B1.[responseXML].value('(/ValidationResponse/Workflow-Next-Node)[1]', 'varchar(max)') = 'Billing'"
         +" GROUP BY C1.claim_id )"
         +" GROUP BY dateadd(day, datediff(day, 0, B.StartDate), 0)"
         +" ORDER BY dateadd(day, datediff(day, 0, B.StartDate), 0) desc ");*/

        ArrayList<ArrayList<String>> rs = executeQuery(" SELECT  dateadd(day, datediff(day, 0, maxDate), 0), count(claim_id) from\n"
                + " ( "
                + " Select 0 as claim_line_id , claim_id, max(requestTime) As maxDate "
                + " from routedRequests "
                + " Where requestTime> dateadd(day, -30, getdate()) "
                + " Group BY claim_id "
                + ") A "
                + " Group BY dateadd(day, datediff(day, 0, maxDate), 0) "
                + " ORDER BY dateadd(day, datediff(day, 0, maxDate), 0) desc ");
        String str = "";
        for (int i = 0; i < rs.size(); i++) {
            try {
                ArrayList s = rs.get(i);
                Date d = fmt.parse((String) s.get(0));
                str = str + " " + d.getTime() + "," + s.get(1);
            } catch (ParseException ex) {
                Statistics.addException(ex);
                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //System.out.print(str);
        return str;
    }

    public static List<AccumedRule> getPackagesRules(boolean bRefresh) {
        if (packagesRules != null && bRefresh == false) {
            return packagesRules;
        }
        String sRulesSource = System.getProperty("com.accumed.rules_packages_path");
        Logger.getLogger(
                Statistics.class.getName()).log(Level.INFO,"com.accumed.rules_packages_path=" + sRulesSource);
        packagesRules = new ArrayList<AccumedRule>();
        String package_fileName = sRulesSource + "/accumed_general.pkg";
        if (IsFileExists(package_fileName)) {
            getPackageRules(package_fileName);
        }
        package_fileName = sRulesSource + "/accumed_payer.pkg";
        if (IsFileExists(package_fileName)) {
            getPackageRules(package_fileName);
        }
        package_fileName = sRulesSource + "/accumed_haad.pkg";
        if (IsFileExists(package_fileName)) {
            getPackageRules(package_fileName);
        }
        //package_fileName = sRulesSource + "/accumed_haad.pkg";
        //if(IsFileExists(package_fileName)) {getPackageRules(package_fileName);}
        package_fileName = sRulesSource + "/accumed_workflow.pkg";
        if (IsFileExists(package_fileName)) {
            getPackageRules(package_fileName);
        }
        return packagesRules;
    }

    private static boolean IsFileExists(String sFile) {
        java.io.File fFile = null;
        fFile = new java.io.File(sFile);
        if (fFile.exists()) {
            fFile = null;
            return true;
        }
        fFile = null;
        return false;
    }

    public static void getPackageRules(String package_fileName) {
        KnowledgeBase knowledgeBase = Utils.createKnowledgeBase(package_fileName);
        for (KnowledgePackage AccumedPackage : knowledgeBase.getKnowledgePackages()) {
            List<AccumedRule> listOfAccumedRules = new ArrayList<AccumedRule>();
            for (Rule rule : AccumedPackage.getRules()) {
                int index = package_fileName.lastIndexOf('/');
                String name = package_fileName.substring(index + 1);
                index = name.indexOf('.');
                name = name.substring(0, index);
                AccumedRule accumedRule = new AccumedRule();
                accumedRule.setRuleName(rule.getName());
                accumedRule.setEnabled(true);
                accumedRule.setPackageName(name);
                packagesRules.add(accumedRule);
            }

        }
    }

//    public static HashMap<String, List<linerPair>> getRoutedClaimsPerDay() {
//        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM/yyyy");
//        HashMap<String, List<linerPair>> ret = new HashMap<String, List<linerPair>>();
//        String sQuery = "SELECT\n"
//                + "\tCONVERT(varchar(50), DATEADD(DAY, DATEDIFF(DAY, 0, maxDate), 0), 103),\n"
//                + "\trouting_rule,\n"
//                + "\tCOUNT(rootID)\n"
//                + "FROM (SELECT\n"
//                + "\t0 AS idCaller,\n"
//                + "\trootID,\n"
//                + "\trouting_rule,\n"
//                + "\tMAX(requestTime) AS maxDate\n"
//                + "FROM routedClaims\n"
//                + " Where requestTime> dateadd(day, -30, getdate()) and routing_rule is not null \n"
//                + " Group BY rootID, routing_rule ) A\n"
//                + "GROUP BY\tDATEADD(DAY, DATEDIFF(DAY, 0, maxDate), 0),\n"
//                + "\t\t\trouting_rule\n"
//                + "ORDER BY DATEADD(DAY, DATEDIFF(DAY, 0, maxDate), 0) DESC";
//        ArrayList<ArrayList<String>> rs = executeQuery(sQuery);
//
//        for (int i = 0; i < rs.size(); i++) {
//            try {
//                ArrayList<String> s = rs.get(i);
//
//                Date date = fmt.parse((String) s.get(0));
//                String sRoutingRule = (String) s.get(1);
//                Double d = Double.parseDouble(s.get(2));
//                if (ret.get(sRoutingRule) != null) {
//                    ret.get(sRoutingRule).add(new linerPair(date, d));
//                } else {
//                    List<linerPair> temp = new ArrayList<linerPair>();
//                    temp.add(new linerPair(date, d));
//                    ret.put(sRoutingRule, temp);
//                }
//            } catch (ParseException ex) {
//                Statistics.addException(ex);
//                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        //System.out.print(str);
//        return ret;
//    }
//    public static List<routedClaim> getRoutedClaims() {
//        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//        //fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
//        List<routedClaim> ret = new ArrayList<routedClaim>();
//        String sQuery = "Select Convert(varchar(50), max(requestTime), 121) As routingDate, claimID, routing_rule \n"
//                + " From ( \n"
//                + " Select requestTime , Isnull(idCaller, rootID) As claimID, routing_rule \n"
//                + " from routedClaims\n"
//                + " where routing_rule is not null \n"
//                + " and requestTime> dateadd(day, -30, getdate()) \n"
//                + " ) A \n"
//                + " GROUP by claimID, routing_rule \n"
//                + " order by routingDate desc";
//        ArrayList<ArrayList<String>> rs = executeQuery(sQuery);
//
//        for (int i = 0; i < rs.size(); i++) {
//            try {
//                ArrayList<String> s = rs.get(i);
//
//                Date date = fmt.parse((String) s.get(0));
//                Long claim_line_id = Long.parseLong(s.get(1));
//                String sRoutingRule = (String) s.get(2);
//                ret.add(new routedClaim(date, "APM-" + claim_line_id, sRoutingRule));
//            } catch (ParseException ex) {
//                Statistics.addException(ex);
//                Logger.getLogger(Statistics.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        //System.out.print(str);
//        return ret;
//    }
    public static Long getMaxRequestTime() {
        return maxRequestTime;
    }

    public static Integer getMaxRequestTime_ClamsCount() {
        return maxRequestTime_ClamsCount;
    }

    public static ArrayList<ArrayList<String>> findCodeInTree(String search, String type, int parent_id) {
        if (search == null || search.isEmpty()) {
            return null;
        }
        if ((type == null || type.isEmpty()) && (parent_id == 0)) {
            return null;
        }
        String sep = ",";
        search += sep;

        String s = " WITH CTE ( pos, pos_begin, pos_end,code )\n"
                + "   AS (\n"
                + "       SELECT 0, 1, CHARINDEX( '" + sep + "','" + search + "'),CAST(RTRIM(LTRIM(SUBSTRING( '" + search + "', 1 , CHARINDEX( '" + sep + "','" + search + "' )-1 ))) AS varchar(100))  \n"
                + "        UNION ALL\n"
                + "       SELECT pos + 1, pos_end + 1, CHARINDEX( '" + sep + "', '" + search + "', pos_end + 1 ),CAST(RTRIM(LTRIM(SUBSTRING( '" + search + "', pos_end + 1 ,  CHARINDEX( '" + sep + "', '" + search + "', pos_end + 1 ) - pos_end -1))) AS varchar(100)) \n"
                + "         FROM CTE\n"
                + "        WHERE CHARINDEX( '" + sep + "', '" + search + "', pos_end + 1 ) > 0 \n"
                + "     ) \n";

        s += " SELECT c.ID as ID "
                + ", ct.code "
                + ", CASE WHEN P.Name IS NULL THEN g.NAME ELSE P.name +' &rarr; '+g.NAME END AS GROUP_NAME_PATH "
                + ",CASE WHEN P.ID IS NULL THEN CAST(g.ID AS varchar(10)) ELSE CAST(P.ID AS varchar(10))+'/'+CAST(g.ID AS varchar(10))  END  AS GRUP_ID_PATH "
                + ",g.TYPE "
                + " ,c.[FROM] "
                + ",c.[TO] "
                + " FROM  "
                + "  CTE ct inner join "
                + " ACCUMED_GROUP_CODES c on  ((c.[TO] IS NULL And c.[from]=ct.code) "
                + "OR ( c.[To] IS NOT null AND ct.code BETWEEN c.[from] AND c.[To]))"
                + "INNER Join Accumed_CODE_GROUPS g on g.ID = c.GROUP_ID "
                + " LEFT OUTER JOIN Accumed_CODE_GROUPS P ON g.PARENT_ID = p.ID "
                + " WHERE  ";
        if (type != null && !type.isEmpty()) {
            s += "  g.TYPE='" + type + "' ";
        }
        if (parent_id > 0) {
            s += " AND (g.ID =" + parent_id + " OR g.PARENT_ID =" + parent_id + ")";
        }
        s += " ORDER BY CODE,ID ";

        ArrayList<ArrayList<String>> rs = executeAccumedDBQuery(s);
        if (rs == null) {
            rs = new ArrayList<ArrayList<String>>();
        }
        return rs;

    }

    public static List<ArrayList<String>> findCWICD_CPTs(String icdType, String exactCodes, String startFrom, String startTo) {
        ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
        String sCTEQuery = "";

        if (exactCodes != null && !exactCodes.isEmpty()) {
            String sep = ",";
            exactCodes += sep;

            sCTEQuery = " WITH CTE ( pos, pos_begin, pos_end,code )\n"
                    + "   AS (\n"
                    + "       SELECT 0, 1, CHARINDEX( '" + sep + "','" + exactCodes + "'),CAST(RTRIM(LTRIM(SUBSTRING( '" + exactCodes + "', 1 , CHARINDEX( '" + sep + "','" + exactCodes + "' )-1 ))) AS varchar(100))  \n"
                    + "        UNION ALL\n"
                    + "       SELECT pos + 1, pos_end + 1, CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ),CAST(RTRIM(LTRIM(SUBSTRING( '" + exactCodes + "', pos_end + 1 ,  CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ) - pos_end -1))) AS varchar(100)) \n"
                    + "         FROM CTE\n"
                    + "        WHERE CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ) > 0 \n"
                    + "     ) \n";

        }
        String sQuery = sCTEQuery
                + "Select C.CPT, cpt.DESCRIPTION, IsNull(C.shortMsg, \'-\') as shortMsg, IsNull(C.longMsg, \'-\') as longMsg, \n"
                + "ISNULL(STUFF((Select \'|\' + CR.receiver_license FROM RULES_CW_CPT_RECEIVERS CR where CR.CPT = C.CPT FOR XML PATH(\'\')), 1, 1, \'\' ), \'-\') AS receivers, \n"
                + "ISNULL(C.severity, 0) as severity, IsNull(CC.ICD, '-') as ICD, IsNull(icd.DESCRIPTION, '-') as ICD_DESCRIPTION, IsNull(C.refLink, \'-\') as refLink, IsNull(C.documentation, \'-\') as documentation, 10 \n"
                + "FROM RULES_CW_CPT C \n";
        sQuery += "LEFT JOIN RULES_CW_ICD CC on CC.CPT = C.CPT and CC.icds_version = C.icds_version\n";
        if (exactCodes != null && !exactCodes.isEmpty()) {
            sQuery += " inner join CTE ct on (ct.code=CC.ICD )\n";
        }

        sQuery += "LEFT join (\t\t\n"
                + "\tSelect ACCUMED_DIAGNOSIS_CODES.CODE AS [CODE], Max(ACCUMED_DIAGNOSIS_CODES.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_DIAGNOSIS_CODES group BY ACCUMED_DIAGNOSIS_CODES.CODE\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DHA_ICD10.Code AS [CODE], Max(ACCUMED_DHA_ICD10.ShortDesc) AS [DESCRIPTION] from ACCUMED_DHA_ICD10\n"
                + "\tWhere ACCUMED_DHA_ICD10.Code not in(\n"
                + "\tSelect CODE from ACCUMED_DIAGNOSIS_CODES) group BY ACCUMED_DHA_ICD10.Code) as icd on(icd.CODE=CC.ICD) LEFT join (\t\n"
                + "\tSelect ACCUMED_HAAD_CPTS.CODE AS [CODE], Max(ACCUMED_HAAD_CPTS.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_HAAD_CPTS group BY ACCUMED_HAAD_CPTS.CODE\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DHA_CPT.CPT_CODE AS [CODE], Max(ACCUMED_DHA_CPT.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_DHA_CPT\n"
                + "\tWhere ACCUMED_DHA_CPT.CPT_CODE not in(\n"
                + "\tSelect CODE from ACCUMED_HAAD_CPTS) GROUP BY ACCUMED_DHA_CPT.CPT_CODE) as cpt ON ( cpt.CODE=C.CPT) ";

        String where = "";
        if (startFrom != null && !startFrom.isEmpty()) {
            where = " Where CC.ICD >= '" + startFrom + "' ";
        }
        if (startTo != null && !startTo.isEmpty()) {
            if (where.isEmpty()) {
                where = " Where ";
            } else {
                where += " AND ";
            }
            where += " CC.ICD  <= '" + startTo + "' ";
        }

        if (where.isEmpty()) {
            where = " WHERE C.icds_version=" + icdType + " AND C.icds_version=" + icdType + " ";
        } else {
            where += " AND C.icds_version=" + icdType + " AND C.icds_version=" + icdType + " ";
        }

        sQuery += where + " Order By C.CPT, CC.ICD  ";
        Logger.getLogger(
                Statistics.class.getName()).log(Level.INFO, "crossWalk ICDS Query={0}", sQuery);
        res = executeAccumedDBQuery(sQuery);

        if (res == null) {
            res = new ArrayList<ArrayList<String>>();
        }
        return res;
    }

    public static List<ArrayList<String>> findCWCPT_ICDs(String icdType, String exactCodes, String startFrom, String startTo) {
        ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
        String sCTEQuery = "";

        if (exactCodes != null && !exactCodes.isEmpty()) {
            String sep = ",";
            exactCodes += sep;

            sCTEQuery = " WITH CTE ( pos, pos_begin, pos_end,code )\n"
                    + "   AS (\n"
                    + "       SELECT 0, 1, CHARINDEX( '" + sep + "','" + exactCodes + "'),CAST(RTRIM(LTRIM(SUBSTRING( '" + exactCodes + "', 1 , CHARINDEX( '" + sep + "','" + exactCodes + "' )-1 ))) AS varchar(100))  \n"
                    + "        UNION ALL\n"
                    + "       SELECT pos + 1, pos_end + 1, CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ),CAST(RTRIM(LTRIM(SUBSTRING( '" + exactCodes + "', pos_end + 1 ,  CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ) - pos_end -1))) AS varchar(100)) \n"
                    + "         FROM CTE\n"
                    + "        WHERE CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ) > 0 \n"
                    + "     ) \n";

        }
        String sQuery = sCTEQuery
                + " \n Select C.CPT, IsNull(cpt.DESCRIPTION, \'-\'), IsNull(C.shortMsg, \'-\') as shortMsg, IsNull(C.longMsg, \'-\') as longMsg, \n"
                + " ISNULL(STUFF((Select \'|\' + CR.receiver_license FROM RULES_CW_CPT_RECEIVERS CR where CR.CPT = C.CPT and CR.icds_version = C.icds_version FOR XML PATH(\'\')), 1, 1, \'\' ), \'-\') AS receivers, \n"
                + " ISNULL(C.severity, 0) as severity, IsNull(CC.ICD, '-') as ICD, IsNull(icd.DESCRIPTION, '-') as ICD_DESCRIPTION,  IsNull(C.refLink, '-') as refLink,  IsNull(C.documentation, '-') as documentation, " + icdType + " \n"
                + " FROM RULES_CW_CPT C \n";

        if (exactCodes != null && !exactCodes.isEmpty()) {
            sQuery += " inner join CTE ct on (ct.code=C.CPT )\n";
        }

        sQuery += " LEFT JOIN RULES_CW_ICD CC on CC.CPT = C.CPT and CC.icds_version = C.icds_version\n";

        sQuery += " LEFT join (\t\t\n"
                + "\tSelect ACCUMED_DIAGNOSIS_CODES.CODE AS [CODE], Max(ACCUMED_DIAGNOSIS_CODES.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_DIAGNOSIS_CODES group BY ACCUMED_DIAGNOSIS_CODES.CODE\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DHA_ICD10.Code AS [CODE], Max(ACCUMED_DHA_ICD10.ShortDesc) AS [DESCRIPTION] from ACCUMED_DHA_ICD10\n"
                + "\tWhere ACCUMED_DHA_ICD10.Code not in(\n"
                + "\tSelect CODE from ACCUMED_DIAGNOSIS_CODES) group BY ACCUMED_DHA_ICD10.Code) as icd on(icd.CODE=CC.ICD) LEFT join (\t\n"
                + "\tSelect ACCUMED_HAAD_CPTS.CODE AS [CODE], ACCUMED_HAAD_CPTS.SHORT_DESCRIPTION AS [DESCRIPTION] from ACCUMED_HAAD_CPTS\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_HAAD_CPTS.CODE AS [CODE], Max(ACCUMED_HAAD_CPTS.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_HAAD_CPTS group BY ACCUMED_HAAD_CPTS.CODE\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DHA_CPT.CPT_CODE AS [CODE], Max(ACCUMED_DHA_CPT.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_DHA_CPT\n"
                + "\tWhere ACCUMED_DHA_CPT.CPT_CODE not in(\n"
                + "\tSelect CODE from ACCUMED_HAAD_CPTS) GROUP BY ACCUMED_DHA_CPT.CPT_CODE) as cpt ON ( cpt.CODE=C.CPT) \n";

        String where = "";
        if (startFrom != null && !startFrom.isEmpty()) {
            where = " Where C.CPT >= '" + startFrom + "' ";
        }
        if (startTo != null && !startTo.isEmpty()) {
            if (where.isEmpty()) {
                where = " Where ";
            } else {
                where += " AND ";
            }
            where += " C.CPT <= '" + startTo + "' ";
        }
        if (where.isEmpty()) {
            where = " WHERE C.icds_version=" + icdType + " AND C.icds_version=" + icdType + "    AND CPT_TYPE=3  ";
        } else {
            where += " AND C.icds_version=" + icdType + " AND C.icds_version=" + icdType + "    AND CPT_TYPE=3  ";
        }

        sQuery += where + " Order By C.CPT,CC.ICD ";
        ArrayList<ArrayList<String>> rs = executeAccumedDBQuery(sQuery);

        if (rs == null) {
            rs = new ArrayList<ArrayList<String>>();
        }

        return rs;
    }

    public static List<ArrayList<String>> findCWDental_ICDs(String icdType, String exactCodes, String startFrom, String startTo) {
        ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
        String sCTEQuery = "";
        if (exactCodes != null && !exactCodes.isEmpty()) {
            String sep = ",";
            exactCodes += sep;

            sCTEQuery = " WITH CTE ( pos, pos_begin, pos_end,code )\n"
                    + "   AS (\n"
                    + "       SELECT 0, 1, CHARINDEX( '" + sep + "','" + exactCodes + "'),CAST(RTRIM(LTRIM(SUBSTRING( '" + exactCodes + "', 1 , CHARINDEX( '" + sep + "','" + exactCodes + "' )-1 ))) AS varchar(100))  \n"
                    + "        UNION ALL\n"
                    + "       SELECT pos + 1, pos_end + 1, CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ),CAST(RTRIM(LTRIM(SUBSTRING( '" + exactCodes + "', pos_end + 1 ,  CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ) - pos_end -1))) AS varchar(100)) \n"
                    + "         FROM CTE\n"
                    + "        WHERE CHARINDEX( '" + sep + "', '" + exactCodes + "', pos_end + 1 ) > 0 \n"
                    + "     ) \n";

        }
        String sQuery = sCTEQuery
                + " \n Select C.CPT, IsNull( Dental.DESCRIPTION,'-') as DESCRIPTION, IsNull(C.shortMsg, '-') as shortMsg, IsNull(C.longMsg, '-') as longMsg, \n"
                + " ISNULL(STUFF((Select '|' + CR.receiver_license FROM RULES_CW_CPT_RECEIVERS CR where CR.CPT = C.CPT and CR.icds_version = C.icds_version FOR XML PATH('')), 1, 1, '' ), '-') AS receivers, \n"
                + " ISNULL(C.severity, 0) as severity, IsNull(CC.ICD, '-') as ICD, IsNull(icd.DESCRIPTION, '-') as ICD_DESCRIPTION,  IsNull(C.refLink, '-') as refLink,  IsNull(C.documentation, '-') as documentation, " + icdType + " \n"
                + " FROM RULES_CW_CPT C \n";

        if (exactCodes != null && !exactCodes.isEmpty()) {
            sQuery += " inner join CTE ct on (ct.code=C.CPT )\n";
        }

        sQuery += " LEFT JOIN RULES_CW_ICD CC on CC.CPT = C.CPT and CC.icds_version = C.icds_version \n";

        sQuery += " LEFT join (\t\t\n"
                + "\tSelect ACCUMED_DIAGNOSIS_CODES.CODE AS [CODE], Max(ACCUMED_DIAGNOSIS_CODES.SHORT_DESCRIPTION) AS [DESCRIPTION] from ACCUMED_DIAGNOSIS_CODES group BY ACCUMED_DIAGNOSIS_CODES.CODE\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DHA_ICD10.Code AS [CODE], Max(ACCUMED_DHA_ICD10.ShortDesc) AS [DESCRIPTION] from ACCUMED_DHA_ICD10\n"
                + "\tWhere ACCUMED_DHA_ICD10.Code not in(\n"
                + "\tSelect CODE from ACCUMED_DIAGNOSIS_CODES) group BY ACCUMED_DHA_ICD10.Code) as icd on(icd.CODE=CC.ICD) LEFT join (\t\n"
                + "\tSelect  ACCUMED_DENTAL_CODES.CODE AS [CODE], ACCUMED_DENTAL_CODES.DESCRIPTION AS [DESCRIPTION] from ACCUMED_DENTAL_CODES\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DENTAL_CODES.CODE AS [CODE], Max(ACCUMED_DENTAL_CODES.DESCRIPTION) AS [DESCRIPTION] from ACCUMED_DENTAL_CODES group BY ACCUMED_DENTAL_CODES.CODE\n"
                + "\tUNION\n"
                + "\tSelect ACCUMED_DHA_DENTAL_CODE.CODE AS [CODE], Max(ACCUMED_DHA_DENTAL_CODE.Description) AS [DESCRIPTION] from ACCUMED_DHA_DENTAL_CODE\n"
                + "\tWhere ACCUMED_DHA_DENTAL_CODE.Code not in(\n"
                + "\tSelect CODE from ACCUMED_DENTAL_CODES) GROUP BY ACCUMED_DHA_DENTAL_CODE.Code) as Dental ON ( Dental.CODE=C.CPT) \n";

        String where = "";
        if (startFrom != null && !startFrom.isEmpty()) {
            where = " Where C.CPT >= '" + startFrom + "' ";
        }
        if (startTo != null && !startTo.isEmpty()) {
            if (where.isEmpty()) {
                where = " Where ";
            } else {
                where += " AND ";
            }
            where += " C.CPT <= '" + startTo + "' ";
        }
        if (where.isEmpty()) {
            where = " WHERE C.icds_version=" + icdType + " AND C.icds_version=" + icdType + " AND CPT_TYPE=6 ";
        } else {
            where += " AND C.icds_version=" + icdType + " AND C.icds_version=" + icdType + " AND CPT_TYPE=6 ";
        }

        sQuery += where + " Order By C.CPT,CC.ICD ";
        Logger.getLogger(
                Statistics.class.getName()).log(Level.INFO, "crossWalk Query=" + sQuery);
        ArrayList<ArrayList<String>> rs = executeAccumedDBQuery(sQuery);

        if (rs == null) {
            rs = new ArrayList<ArrayList<String>>();
        }

        return rs;
    }

//    public static List<com.accumed.mb.Receiver> getReceivers() {
//        List<com.accumed.mb.Receiver> receiver = new ArrayList<com.accumed.mb.Receiver>();
//        String sQuery = "Select L.AUTH_NO,  C.COMPANY_NAME, IsNull(C.abbreviation, 'ZZZ') as abb from ACCUMED_INSURERS L \n"
//                + "INNER JOIN ACCUMED_INSURANCE_COMPANIES C on C.ID = L.company_id\n"
//                + "order by abb";
//        ArrayList<ArrayList<String>> rs = Statistics.executeAccumedDBQuery(sQuery);
//        if (rs != null) {
//            int i = 0;
//            for (ArrayList<String> row : rs) {
//                com.accumed.mb.Receiver item = new com.accumed.mb.Receiver();
//                item.setLicense(rs.get(i).get(0));
//                item.setName(rs.get(i).get(1));
//                item.setAbbreviation(rs.get(i).get(2).equalsIgnoreCase("ZZZ") ? null : rs.get(i).get(2));
//                receiver.add(item);
//                i++;
//            }
//        }
//
//        return receiver;
//    }
//    public static Integer getUserId() {
//
//        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
//        /*OMIT_CAS_03January_2017*/
//        org.jasig.cas.client.validation.Assertion assertion1
//                = (org.jasig.cas.client.validation.Assertion) request.getSession().getAttribute(org.jasig.cas.client.util.AbstractCasFilter.CONST_CAS_ASSERTION);
//        Map attributes = assertion1.getPrincipal().getAttributes();
//        if (!attributes.isEmpty()) {
//            return (Integer.valueOf(((String) attributes.get("USER_ID")).trim()));
//        }
//        return null;
//
//    }
    public static Integer getMaxRequestTime_id() {
        return maxRequestTime_id;
    }

    public static void setMaxRequestTime_id(Integer maxRequestTime_id) {
        Statistics.maxRequestTime_id = maxRequestTime_id;
    }

    public static boolean isLogHistory() {
        return logHistory;
    }

    public static void setLogHistory(boolean logHistory) {
        Statistics.logHistory = logHistory;
    }

    public static boolean isDisableDBRules() {
        return disableDBRules;
    }

    public static void setDisableDBRules(boolean disableDBRules) {
        Statistics.disableDBRules = disableDBRules;
    }

}
