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
public class ClaimHistory {
  Integer requestID;
  Integer   apm;
  Integer   userID;
  String   userName;
  Date validationTime;
  
  
    public Date getValidationTime() {
        return validationTime;
    }

    public void setValidationTime(Date validationTime) {
        this.validationTime = validationTime;
    }

    public Integer getRequestID() {
        return requestID;
    }

    public void setRequestID(Integer requestID) {
        this.requestID = requestID;
    }

    public Integer getApm() {
        return apm;
    }

    public void setApm(Integer apm) {
        this.apm = apm;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
  
    
}
