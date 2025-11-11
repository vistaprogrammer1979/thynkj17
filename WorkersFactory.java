/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.pool;

import com.accumed.re.agents.CachedRepositoryService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 *
 * @author smutlak
 */
public class WorkersFactory extends BasePooledObjectFactory<Worker> {

    protected static CachedRepositoryService cachedRepositoryService;

    public static void setCachedRepositoryService(CachedRepositoryService cachedRepositoryService) {
        WorkersFactory.cachedRepositoryService = cachedRepositoryService;
    }

    @Override
    public Worker create() {
        return new Worker();
    }

    /**
     * Use the default PooledObject implementation.
     *
     * @param buffer
     * @return
     */
    @Override
    public PooledObject<Worker> wrap(Worker buffer) {
        return new DefaultPooledObject<>(buffer);
    }

    /**
     * When an object is returned to the pool, clear the buffer.
     *
     * @param pooledObject
     */
    @Override
    public void passivateObject(PooledObject<Worker> pooledObject) {
        pooledObject.getObject().reset();
    }

    // for all other methods, the no-op implementation
    // in BasePooledObjectFactory will suffice
    @Override
    public void activateObject(PooledObject<Worker> p) throws Exception {
        super.activateObject(p); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean validateObject(PooledObject<Worker> p) {
        boolean ret = true;
        Logger.getLogger(WorkersFactory.class.getName()).
                log(Level.INFO, "id:{0}::validateObject ....",
                        new Object[]{p.getObject().getUniqueId()});
        if (p.getObject().getRepoTimeStamp().before(cachedRepositoryService.getRepo().getTimeStamp())) {
            Logger.getLogger(WorkersFactory.class.getName()).
                    log(Level.INFO, "id'{'{0}'}'::worker invalid reason: change in DB.", p.getObject().getUniqueId());
            ret = false;
        }
        if (ret && p.getObject().isNewPackageExisted()) {
            Logger.getLogger(WorkersFactory.class.getName()).
                    log(Level.INFO, "id'{'{0}'}'::worker invalid reason: change in rules packages.", p.getObject().getUniqueId());
            ret = false;
        }
        if(ret && p.getObject().isDirty()){
            Logger.getLogger(WorkersFactory.class.getName()).
                    log(Level.INFO, "id'{'{0}'}'::worker invalid reason = dirty.", p.getObject().getUniqueId());
            ret = false;
        }
        if(ret){
            ret = super.validateObject(p); //To change body of generated methods, choose Tools | Templates.
        }
        Logger.getLogger(WorkersFactory.class.getName()).
                log(Level.INFO, "id:{0}::validateObject completed {1}",
                        new Object[]{p.getObject().getUniqueId(), ret?"---valid---":"---invalid---"});
        return ret;
    }

    @Override
    public void destroyObject(PooledObject<Worker> p) throws Exception {
        super.destroyObject(p); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PooledObject<Worker> makeObject() throws Exception {
        return super.makeObject(); //To change body of generated methods, choose Tools | Templates.
    }
    
    

}
