/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author smutlak
 */
public enum Status {
    INVALID(1),
    VALID(2);
    
    private final int val;
    
    Status(int val)
    {
        this.val = val;
    }
    
    public int getValue() { return val; }
    
    // Mapping difficulty to difficulty id
    private static final Map<Integer, Status> _map = new HashMap<Integer, Status>();
    static
    {
        for (Status codeType : Status.values())
            _map.put(codeType.val, codeType);
    }
 
    /**
     * Get difficulty from value
     * @param value Value
     * @return Difficulty
     */
    public static Status from(int value)
    {
        return _map.get(value);
    }
    
}
