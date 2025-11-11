/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import java.util.Date;

/**
 *
 * @author wfakhra
 */
public class RuleReference {
    private Integer id;
    private String ruleName;
     
    private String reference;
    
    private String createdBy;
    private Boolean deleted;
    private java.util.Date creationDate;
    private java.util.Date deleteDate;
    private String deletedBy;

    public RuleReference() {
    }

    public RuleReference(Integer id, String ruleName, Integer category_id, String reference, String createdBy, Boolean deleted, Date creationDate, Date deleteDate, String deletedBy) {
        this.id = id;
        this.ruleName = ruleName;
        
        this.reference = reference;
        this.createdBy = createdBy;
        this.deleted = deleted;
        this.creationDate = creationDate;
        this.deleteDate = deleteDate;
        this.deletedBy = deletedBy;
    }
    
    

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

   

    public String getCatName() {
        return reference;
    }

    public void setCatName(String reference) {
        this.reference = reference;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(Date deleteDate) {
        this.deleteDate = deleteDate;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }
    
    
}
