/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.accumed.re.agents.repo;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.accumed.re.agents.Status;

/**
 *
 * @author smutlak
 * @param <T>
 */
public class CachedData<T> {
    
    private String logicalName;
    private java.util.HashMap<String, Long> tables;
    private Date checksumTime;
    private java.util.Collection<T> data;
    private Status status;
    

    public CachedData() {
    }

    public CachedData(String logicalName, java.util.HashMap<String, Long> tables, Collection<T> data) {
        this.logicalName = logicalName;
        this.tables = tables;
        this.checksumTime = new java.util.Date();
        this.data = data;
        status = Status.VALID;
        
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public HashMap<String, Long> getTables() {
        return tables;
    }

    public void setTables(HashMap<String, Long> tables) {
        this.tables = tables;
    }

    

    public Date getChecksumTime() {
        return checksumTime;
    }

    public void setChecksumTime(Date checksumTime) {
        this.checksumTime = checksumTime;
        status = Status.VALID;
    }

    public Collection<T> getData() {
        return data;
    }

    public void setData(Collection<T> data) {
        this.data = data;
    }
    
    public void invalidCache(){
        status = Status.INVALID;
        Logger.getLogger(CachedData.class.getName()).log(Level.INFO, "InvalidCache {0}.",this.logicalName);
    }
    public void validCache(){
        status = Status.VALID;
    }

    public Status getStatus() {
        return status;
    }

//    public void setStatus(Status status) {
//        this.status = status;
//    }
    
    
    public boolean isInvalid(){
        return Status.VALID != getStatus();
    }
}
