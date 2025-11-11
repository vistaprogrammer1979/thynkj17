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
public class AccessDeniedBean {

    private String message;

    public AccessDeniedBean() {
    }

    public AccessDeniedBean(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
