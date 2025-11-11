/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author smutlak
 */
public class ValidatorState implements Serializable {

    private Date time;
    private Long requestCount;
    private Integer supervisorsCount;
    private Integer supervisorsActive;
    private Integer supervisorsIdle;

    public ValidatorState(Date time, Long requestCount, Integer supervisorsCount, Integer supervisorsActive, Integer supervisorsIdle) {
        this.time = time;
        this.requestCount = requestCount;
        this.supervisorsCount = supervisorsCount;
        this.supervisorsActive = supervisorsActive;
        this.supervisorsIdle = supervisorsIdle;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Integer getSupervisorsCount() {
        return supervisorsCount;
    }

    public void setSupervisorsCount(Integer supervisorsCount) {
        this.supervisorsCount = supervisorsCount;
    }

    public Integer getSupervisorsActive() {
        return supervisorsActive;
    }

    public void setSupervisorsActive(Integer supervisorsActive) {
        this.supervisorsActive = supervisorsActive;
    }

    public Integer getSupervisorsIdle() {
        return supervisorsIdle;
    }

    public void setSupervisorsIdle(Integer supervisorsIdle) {
        this.supervisorsIdle = supervisorsIdle;
    }

    public Calendar getCalendar(){
        Calendar ret =  Calendar.getInstance();
        ret.setTime(this.time);
        return ret;
    }
    
    
}
