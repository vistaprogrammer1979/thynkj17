/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.validation;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import org.drools.definition.rule.Rule;
import org.drools.runtime.rule.Activation;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.WorkingMemory;

/**
 *
 * @author smutlak
 */
public class ValidatorConsequenceException extends RuntimeException {
private final WorkingMemory workingMemory;
private final Activation activation;

public ValidatorConsequenceException(final Activation activation, final WorkingMemory workingMemory, final Throwable exception) {
    super(exception);
    this.activation = activation;
    this.workingMemory = workingMemory;
}

@Override
public String getMessage() {
    StringBuilder sb = new StringBuilder( "Exception executing consequence for " );
        Rule rule = null;
        
        if( activation != null && ( rule = activation.getRule() ) != null ){
            String packageName = rule.getPackageName();
            String ruleName = rule.getName();
            sb.append( "rule \"" ).append( ruleName ).append( "\" in " ).append( packageName );
        } else {
            sb.append( "rule, name unknown" );
        }
        sb.append( ": " ).append( super.getMessage() );
        return sb.toString();
}

 public void printFactDump(){
        printFactDump( System.err );
    }

    public void  printFactDump( PrintStream pStream ){
        List<? extends FactHandle> handles = activation.getFactHandles();
        for( FactHandle handle: handles ){
            Object object = workingMemory.getObject( handle );
            if( object != null ){
                pStream.println( "   Fact " + object.getClass().getSimpleName() +
                                 ": " + object.toString() );
            }
        }
    }

    @Override
    public String toString() {
        return getMessage();
    }
}