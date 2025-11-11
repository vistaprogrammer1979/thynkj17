/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.webservices;

/**
 *
 * @author wfakhra
 */
public class User {
    public String userName;
    public String password;
    static public  boolean authenticated;

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static void setAuthenticated(boolean authenticated) {
        User.authenticated = authenticated;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
   public  static boolean Authentcate(String username,String password){
       //if (authenticated){return authenticated;}      
       if (username!=null && password!=null && username.equalsIgnoreCase("admin") && password.equals("admin") ){
           authenticated=true;
       }
       else{ 
           authenticated=false;
       }
       return  authenticated;
   }
    
    
}
