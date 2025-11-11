/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.accumed.webservices;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author smutlak
 */
public class claimHistory extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("RestartAllAgents") != null) {
//            com.accumed.pricing.cachedRepo.BackgroundTaskManager.restartAgents();
        } else if (req.getParameter("LoggingPatientHistory") != null) {
            Statistics.setLogHistory(!Statistics.isLogHistory());
        } else if (req.getParameter("EnableDBRules") != null) {
            Statistics.setDisableDBRules(!Statistics.isDisableDBRules());
        } else if (req.getParameter("getRequests") != null) {
            String apm = req.getParameter("apm");
            System.out.println(apm);
            resp.sendRedirect(req.getContextPath()+"/history.jsp");
            return;
        }
        resp.sendRedirect(req.getContextPath());
        //req.getRequestDispatcher("/index.jsp?").forward(req, resp);
    }

   
    
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
