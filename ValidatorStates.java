/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author smutlak
 */
public class ValidatorStates<T> extends CopyOnWriteArrayList<T> {

    public ValidatorStates() {
    }

    @Override
    public boolean add(T e) {
        if (this.size() > 0) {
            ValidatorState state = (ValidatorState) e;
            ValidatorState lastState = (ValidatorState) this.get(this.size() - 1);
            long diffInMillies = Math.abs(state.getTime().getTime() - lastState.getTime().getTime());
            long diff = TimeUnit.MINUTES.convert(diffInMillies, TimeUnit.MILLISECONDS);
            if (diff <= 1) {
                lastState.setRequestCount(lastState.getRequestCount() + state.getRequestCount());
                lastState.setSupervisorsCount(lastState.getSupervisorsCount() > state.getSupervisorsCount() ? lastState.getSupervisorsCount() : state.getSupervisorsCount());
                lastState.setSupervisorsActive(lastState.getSupervisorsActive() > state.getSupervisorsActive() ? lastState.getSupervisorsActive() : state.getSupervisorsActive());
                lastState.setSupervisorsIdle(lastState.getSupervisorsIdle() > state.getSupervisorsIdle() ? lastState.getSupervisorsIdle() : state.getSupervisorsIdle());
                return true;
            } else {
                return super.add(e); //To change body of generated methods, choose Tools | Templates.
            }
        }
        return super.add(e); //To change body of generated methods, choose Tools | Templates.
    }

}
