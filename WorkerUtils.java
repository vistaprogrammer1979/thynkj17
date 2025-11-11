/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.pool;

import com.accumed.model.Context;
import com.accumed.model.scrubRequest.ClaimType;
import com.accumed.model.scrubRequest.ExtendedValidationType;
import com.accumed.validation.ValidatorConsequenceExceptionHandler;
import com.accumed.webservices.Statistics;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.ObjectFilter;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.ConsequenceExceptionHandlerOption;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.rule.Rule;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.ConsequenceExceptionHandler;
import org.drools.runtime.rule.FactHandle;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author smutlak
 */
public class WorkerUtils {

    protected static String getTakenTime(long lBeginTime) {
        return (Long.valueOf((System.nanoTime() - lBeginTime) / 1000000)).toString();
    }

    public static KnowledgeBase createKnowledgeBase(String packageFilePath) {
        FileInputStream fis = null;
        Logger.getLogger(WorkerUtils.class.getName()).log(Level.INFO, "entering Loading {0} ...", packageFilePath);

        try {
            File pkgFile = new File(packageFilePath);
            fis = new FileInputStream(pkgFile);
            KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            builder.add(ResourceFactory.newInputStreamResource(fis), ResourceType.PKG);
            if (builder.hasErrors()) {
                for(KnowledgeBuilderError error: builder.getErrors()){
                    Logger.getLogger(WorkerUtils.class.getName()).log(Level.SEVERE, 
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
            Logger.getLogger(WorkerUtils.class.getName()).log(Level.INFO, "Done Loading {0} ...", packageFilePath);
            // fis.close();
            //  fis = null;

            return base;
        } catch (FileNotFoundException ex) {
            Statistics.addException(ex);
            Logger.getLogger(WorkerUtils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Statistics.addException(ex);
                Logger.getLogger(WorkerUtils.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    public static boolean isFactExisted(final StatefulKnowledgeSession session, final Class clss) {
        ObjectFilter filter = new ObjectFilter() {
            @Override
            public boolean accept(Object object) {
                return object.getClass().equals(clss) /*&& beanMatcher.matches(object,expectedProperties)*/;
            }
        };
        return session.getFactHandles(filter).size() > 0;
    }

    public static boolean deleteFacts(final StatefulKnowledgeSession session, final Class clss) {
        boolean ret = false;
        ObjectFilter filter = new ObjectFilter() {
            @Override
            public boolean accept(Object object) {
                return object.getClass().equals(clss) /*&& beanMatcher.matches(object,expectedProperties)*/;
            }
        };
        Collection<FactHandle> factHandles = session.getFactHandles(filter);
        for (FactHandle handle : factHandles) {
            session.retract(handle);
            ret = true;
        }
        return ret;
    }

    protected static java.util.List<FactHandle> insertContext(com.accumed.model.scrubRequest.ScrubRequest req, StatefulKnowledgeSession session) {

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

    public static boolean isAnalyzing(com.accumed.model.scrubRequest.ScrubRequest req) {
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

    public static boolean isCoding(com.accumed.model.scrubRequest.ScrubRequest req) {
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

    public static boolean isSameNode(String node1, String node2) {
        return node1.equalsIgnoreCase(node2);
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

    public static boolean isNextDirectNode(String node1, String node2) {
        return getNextNode(node1).equalsIgnoreCase(node2);
    }

    public static boolean isJustCoding(com.accumed.model.scrubRequest.ScrubRequest req) {
        boolean justCodingExist = false;
        if (req != null && req.getHeader() != null
                && req.getHeader().getExtendedValidationType() != null) {
            for (ExtendedValidationType vtype : req.getHeader().getExtendedValidationType()) {
                if (vtype.getType().equalsIgnoreCase(Context.Coding)) {
                    justCodingExist = true;
                }
            }
            for (ExtendedValidationType vtype : req.getHeader().getExtendedValidationType()) {
                if (vtype.getType().equalsIgnoreCase(Context.Auditing)
                        || vtype.getType().equalsIgnoreCase(Context.Billing)
                        || vtype.getType().equalsIgnoreCase(Context.Submission)) {
                    justCodingExist = false;
                }
            }
        }
        return justCodingExist;
    }

    public static boolean isRULEPLAT_95(com.accumed.model.scrubRequest.ScrubRequest req) {
        return req != null && req.getHeader() != null
                && req.getClaim() != null
                && req.getClaim().getEncounter() != null && req.getClaim().getEncounter().get(0) != null
                && req.getClaim().getEncounter().get(0).getType() != null
                && (req.getClaim().getEncounter().get(0).getType() < 3
                || req.getClaim().getEncounter().get(0).getType() > 6)
                && req.getClaim().getNet() <= 1500 && WorkerUtils.isJustCoding(req);
    }

    public static boolean isContextExists(com.accumed.model.scrubRequest.ScrubRequest req, String context) {
        if (req != null && req.getHeader() != null
                && req.getHeader().getExtendedValidationType() != null) {
            for (ExtendedValidationType vtype : req.getHeader().getExtendedValidationType()) {
                if (vtype.getType().equalsIgnoreCase(context)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean addContextIfNotExists(java.util.HashSet<String> added, com.accumed.model.scrubRequest.ScrubRequest req, String context) {
        if (req != null && req.getHeader() != null
                && req.getHeader().getExtendedValidationType() != null) {
            if (!isContextExists(req, context)) {
                req.getHeader().getExtendedValidationType().add(new ExtendedValidationType(context));
                added.add(context);
                return true;
            }
        }
        return false;
    }

    public static boolean isWorkflow(com.accumed.model.scrubRequest.ScrubRequest req) {
        return req != null && req.getHeader() != null && req.getHeader().getWorkflow() != null
                && req.getHeader().getWorkflow().getCurrentNode() != null;
    }

    public static void removeAddedContexts(java.util.HashSet<String> addedContexts, com.accumed.model.scrubRequest.ScrubRequest req) {
        java.util.Iterator<String> iter = addedContexts.iterator();
        while (iter.hasNext()) {
            String context = iter.next();
            for (int j = 0; j < req.getHeader().getExtendedValidationType().size(); j++) {
                com.accumed.model.scrubRequest.ExtendedValidationType extendedValidationType = req.getHeader().getExtendedValidationType().get(j);
                if (extendedValidationType.getType().equalsIgnoreCase(context)) {
                    req.getHeader().getExtendedValidationType().remove(j);
                    break;
                }
            }
        }
    }

    public static JSONArray getPackageRules(String package_fileName) {
        JSONArray array = new JSONArray();
        KnowledgeBase knowledgeBase = createKnowledgeBase(package_fileName);
        if (knowledgeBase != null) {
            for (KnowledgePackage accumedPackage : knowledgeBase.getKnowledgePackages()) {
                for (Rule rule : accumedPackage.getRules()) {
                    JSONObject obj = new JSONObject();
                    obj.put("package", rule.getPackageName());
                    obj.put("name", rule.getName());
                    obj.put("meta", rule.getMetaData());
                    array.put(obj);
                }
            }
        }
        return array;
    }
    public static boolean isEmptyClaimType(com.accumed.model.scrubRequest.ScrubRequest req) {
        if (req.getClaim() == null) {
            return false;
        }
        if (null == req.getClaim().getClaimType()  ) {
            return true;
        }  
        if (req.getClaim().getClaimType().isEmpty()) {
            return true;
        }
//        if (((ClaimType) req.getClaim().getClaimType().get(0)).getType().equals("")) {
//            return true;
//        }
        if(  null==((ClaimType) req.getClaim().getClaimType().get(0))){
            return true;
        }
        return false;
    }
     

}
