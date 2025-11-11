/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import java.util.Date;

/**
 *
 * @author smutlak
 */
public class Exclusion {
    
    private Integer ID;
    private String ruleID;
    private String providerLicense;
    private String receiverLicense;
    private String payerLicense;
    private String createdBy;
    private Boolean deleted;
    private java.util.Date creationDate;
    private java.util.Date deleteDate;
    private String deletedBy;

    public Exclusion() {
    }

    public Exclusion(Integer ID, String ruleID, String providerLicense, String receiverLicense,
            String payerLicense, String createdBy, Boolean deleted, Date creationDate,
            Date deleteDate, String deletedBy) {
        this.ID = ID;
        this.ruleID = ruleID;
        this.providerLicense = providerLicense;
        this.receiverLicense = receiverLicense;
        this.payerLicense = payerLicense;
        this.createdBy = createdBy;
        this.deleted = deleted;
        this.creationDate = creationDate;
        this.deleteDate = deleteDate;
        this.deletedBy = deletedBy;
    }

    public Integer getID() {
        return ID;
    }

    public String getRuleID() {
        return ruleID;
    }

    public String getProviderLicense() {
        return providerLicense;
    }

    public String getReceiverLicense() {
        return receiverLicense;
    }

    public String getPayerLicense() {
        return payerLicense;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getDeleteDate() {
        return deleteDate;
    }

    public String getDeletedBy() {
        return deletedBy;
    }
    
    
    
}
