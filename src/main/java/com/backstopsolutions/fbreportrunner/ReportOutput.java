package com.backstopsolutions.fbreportrunner;

/**
 * Created with IntelliJ IDEA.
 * User: dsteeber
 * Date: 5/6/14
 * Time: 7:54 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ReportOutput {

    public static ReportOutput NOOP = new ReportOutput() {
        public void sendToOutput(ReportData data) throws Exception  {

        }
    };


    public void sendToOutput(ReportData data) throws Exception ;
}
