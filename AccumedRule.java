/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

/**
 *
 * @author wfakhra
 */
public class AccumedRule {
    String ruleName;
    String packageName;
    boolean enabled;
    boolean byPassable;
   public int status ;
      public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isByPassable() {
        return byPassable;
    }

    public void setByPassable(boolean byPassable) {
        this.byPassable = byPassable;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

   
    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    
}
