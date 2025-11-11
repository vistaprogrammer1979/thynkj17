/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

/**
 *
 * @author smutlak
 */
import javax.xml.ws.WebFault;
 
@WebFault(name="AccessDeniedException")
public class AccessDenied extends Exception{
    private AccessDeniedBean faultBean;
 
    public AccessDenied(String message, AccessDeniedBean faultInfo){
        super(message);
        faultBean = faultInfo;
    }
 
    public AccessDenied(String message, AccessDeniedBean faultInfo, Throwable cause) {
        super(message, cause);
        faultBean = faultInfo;
    }
 
    public AccessDeniedBean getFaultInfo(){
        return faultBean;
    }
}