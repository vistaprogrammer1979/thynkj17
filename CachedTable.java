/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.accumed.validation;

import java.util.Date;

/**
 *
 * @author wfakhra
 */
public class CachedTable {
    String  tableName;
    java.util.Date modefiedDate;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Date getModefiedDate() {
        return modefiedDate;
    }

    public void setModefiedDate(Date modefiedDate) {
        this.modefiedDate = modefiedDate;
    }
    
    
}
