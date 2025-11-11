/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.pool;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 *
 * @author smutlak
 */
public class WorkersPool extends GenericObjectPool {

    public WorkersPool(PooledObjectFactory factory) {
        super(factory);
    }

    public WorkersPool(PooledObjectFactory factory, GenericObjectPoolConfig config) {
        super(factory, config);
    }

    public WorkersPool(PooledObjectFactory factory, GenericObjectPoolConfig config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }

    @Override
    public Set listAllObjects() {
        return super.listAllObjects(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getFactoryType() {
        return super.getFactoryType(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumWaiters() {
        return super.getNumWaiters(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void use(Object pooledObject) {
        super.use(pooledObject); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObject() throws Exception {
        super.addObject(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void preparePool() throws Exception {
        super.preparePool(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void evict() throws Exception {
        Logger.getLogger(WorkersPool.class.getName()).
                log(Level.INFO, "Evict");
        super.evict(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() {
        super.close(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumIdle() {
        return super.getNumIdle(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumActive() {
        return super.getNumActive(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        super.clear(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void invalidateObject(Object obj) throws Exception {
        super.invalidateObject(obj); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void returnObject(Object obj) {
        super.returnObject(obj); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object borrowObject(long borrowMaxWaitMillis) throws Exception {
        return super.borrowObject(borrowMaxWaitMillis); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object borrowObject() throws Exception {
        return super.borrowObject(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PooledObjectFactory getFactory() {
        return super.getFactory(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAbandonedConfig(AbandonedConfig abandonedConfig) throws IllegalArgumentException {
        super.setAbandonedConfig(abandonedConfig); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setConfig(GenericObjectPoolConfig conf) {
        super.setConfig(conf); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getRemoveAbandonedTimeout() {
        return super.getRemoveAbandonedTimeout(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getRemoveAbandonedOnMaintenance() {
        return super.getRemoveAbandonedOnMaintenance(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getRemoveAbandonedOnBorrow() {
        return super.getRemoveAbandonedOnBorrow(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getLogAbandoned() {
        return super.getLogAbandoned(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isAbandonedConfig() {
        return super.isAbandonedConfig(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMinIdle() {
        return super.getMinIdle(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMinIdle(int minIdle) {
        super.setMinIdle(minIdle); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMaxIdle(int maxIdle) {
        super.setMaxIdle(maxIdle); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMaxIdle() {
        return super.getMaxIdle(); //To change body of generated methods, choose Tools | Templates.
    }

    public int getCount() {
        return this.getNumActive() + this.getNumIdle();
    }

    public java.util.Date getRulesPackageTime() {
        java.util.Date ret;
        Worker worker = null;
        try {
            worker = (Worker) this.borrowObject();
            ret = worker.getNewestValidatorTimestamp();
            this.returnObject(worker);
            worker = null;
            return ret;
        } catch (Exception ex) {
            Logger.getLogger(WorkersPool.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (worker != null) {
                this.returnObject(worker);
            }
        }
        return null;
    }

}
