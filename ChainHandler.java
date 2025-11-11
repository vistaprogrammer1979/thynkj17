package com.accumed.webservices;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class ChainHandler implements SOAPHandler<SOAPMessageContext> {

    public static java.util.Queue<IOStringOutputStream> vals;
    private static boolean log_original_request;

    static {
        initialize();
    }

    protected static void initialize() {
        vals = new java.util.LinkedList<IOStringOutputStream>();
        log_original_request = System.getProperty("com.accumed.rules_log_request_enabled") == null ? false : System.getProperty("com.accumed.rules_log_request_enabled").compareToIgnoreCase("true") == 0;
    }

    @Override
    public synchronized boolean handleMessage(SOAPMessageContext context) {
        if (!log_original_request) {
            return true;
        }

        //val = new StringBuffer();
        SOAPMessage message = context.getMessage();
        try {
            Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            if (!outboundProperty) {
                vals.add(new IOStringOutputStream());
                message.writeTo(vals.element());

            }
            /*else {
                message.writeTo(new IOStringOutputStream());
            }*/

        } catch (SOAPException e) {
            // TODO Auto-generated catch block
            Logger.getLogger(ChainHandler.class.getName()).log(Level.SEVERE,
                    "exception caught", e);
            Statistics.addException(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Logger.getLogger(ChainHandler.class.getName()).log(Level.SEVERE,
                    "exception caught", e);
            Statistics.addException(e);
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        // TODO Auto-generated method stub

        return false;
    }

    @Override
    public void close(MessageContext context) {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<QName> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    class IOStringOutputStream extends java.io.OutputStream {

        public StringBuilder val = new StringBuilder();

        public void write(int character) throws java.io.IOException {
            val.append((char) character);
        }

        public String toString() {
            return this.val.toString();
        }

    }

}
