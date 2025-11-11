/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.validation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.drools.runtime.rule.Activation;
import org.drools.runtime.rule.ConsequenceExceptionHandler;
import org.drools.runtime.rule.WorkingMemory;

/**
 *
 * @author smutlak
 */
public class ValidatorConsequenceExceptionHandler implements ConsequenceExceptionHandler, Externalizable {

    @Override
    public void handleException(Activation actvtn, WorkingMemory wm, Exception excptn) {

        throw new ValidatorConsequenceException(actvtn, wm, excptn);

    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {}
       

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {}
}
