/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accumed.re.agents;

import java.util.ArrayList;

/**
 *
 * @author smutlak
 */
public class WorkLogger extends ArrayList<WorkLog> implements Runnable {

    public WorkLogger() {

    }

    @Override
    public boolean add(WorkLog e) {
        synchronized (this) {
            e.setTimestap(System.currentTimeMillis());
            removeObseleteLogs();
            return super.add(e); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private void removeObseleteLogs() {
        synchronized (this) {
            long lTime = System.currentTimeMillis();
            int removeIndex = -1;
            for (int cnt = size() - 1; cnt >= 0; cnt--) {
                if ((lTime - this.get(cnt).getTimestap()) > 1800000) {
                    removeIndex = cnt;
                    break;
                }
            }

            if (removeIndex > -1) {
                this.removeRange(0, removeIndex);
            }
        }
    }

    public Pair<Long, Long> getLastMinuteAverageRequestTime(int minutes) {
        synchronized (this) {
            long lTime = System.currentTimeMillis();
            int startIndex = -1;
            int reqTime = minutes * 1000 * 60;
            for (int cnt = size() - 1; cnt >= 0; cnt--) {
                if ((lTime - this.get(cnt).getTimestap()) < reqTime) {
                    startIndex = cnt;
                }else{
                    break;
                }
            }

            Long count = 0l;
            Long totalTime = 0l;
            if (startIndex > -1) {
                for (int cnt = startIndex; cnt < size(); cnt++) {
                    totalTime += this.get(cnt).getPeriodInMilli();
                    count++;
                }
            }
            return Pair.createPair(count, totalTime);
        }
    }

    @Override
    public void run() {
        removeObseleteLogs();
    }
}
