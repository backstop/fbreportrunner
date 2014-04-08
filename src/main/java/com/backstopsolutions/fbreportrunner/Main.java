package com.backstopsolutions.fbreportrunner;

import com.backstopsolutions.backstopservice.ListSharedReports;
import com.backstopsolutions.backstopservice.ListSharedReportsResponse;
import com.backstopsolutions.backstopservice.LoginInfo;
import com.backstopsolutions.fbreportrunner.services.BackstopReportDirectoryService_1_0Stub;
import com.backstopsolutions.fundbutter.webservice.returnentity.InvocableReport;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: dsteeber
 * Date: 3/18/14
 * Time: 9:40 AM
 *
 */
public class Main {


    /*TODO
    figure out why "Current Month Closing Value" report fails on VM18, looks like oom, we should route std err to another file
     */

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

    static {
        //Don't log info from axis2
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }

    public static String getValidOption(final String optionName, final CommandLine cmd, String defaultValue)
        throws ParseException {
        String val = cmd.hasOption(optionName) ? cmd.getOptionValue(optionName) : defaultValue;
        if (StringUtils.isEmpty(val)) {
            throw new ParseException(optionName + " is required");
        }
        return val;
    }

    public static final void main(final String[] args)
        throws Exception {
        Options options = setupOptions();
        CommandLineParser parser = new BasicParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                throw new ParseException("");
            }
            SoapEnvironmentSettings settings = new SoapEnvironmentSettings();
            settings.setBaseURI(getValidOption("uri", cmd, null));
            settings.setUserName(getValidOption("user", cmd, null));
            settings.setPassword(getValidOption("password", cmd, null));
            settings.setReportPattern(getValidOption("report", cmd, "*"));
            settings.setIterationCount(Integer.parseInt(getValidOption("count", cmd, "1")));
            boolean listOnly = cmd.hasOption("list");


            try {
                System.out.println(getTimeStamp()  + "," + "MESSAGE=\"FBReportRunner Start\"");
                InvocableReport[] reports = fetchReports(settings);
                System.out.println(getTimeStamp()  + "," + "MESSAGE=\"Report Count = " + reports.length + '"');
                System.out.println(getTimeStamp()  + "," + "MESSAGE=\"Starting Report Run, iterations=" + settings.getIterationCount() + '"');
                for (InvocableReport report : reports)  {
                    if (listOnly) {
                        if (settings.acceptReport(report.getReportTitle())) {
                            System.out.println(getTimeStamp()  + ","  + "Report_TITLE=\"" + report.getReportTitle() + '"');
                        }
                    } else {
                        runReport(settings, report);
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            } finally {
                System.out.println(getTimeStamp()  + "," + "MESSAGE=\"FBReportRunner Complete\"");
            }

        } catch (ParseException exc) {
            String msg = exc.getMessage();
            System.out.println(msg);
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("fbreportrunner", options);
        }
    }

    private static Options setupOptions() {
        Options options = new Options();
        Option option;
        //http://vm18.backstopsolutions:8080 bsg341 rup3rt 1

        option = OptionBuilder.withDescription("Print this message").create("help");
        options.addOption(option);

        option = OptionBuilder.withArgName("uri").hasArg().withDescription("Host uri to hit ex. https://vm00.backstopsolutions").create("uri");
        options.addOption(option);

        option = OptionBuilder.withArgName("id").hasArg().withDescription("User login id").create("user");
        options.addOption(option);

        option = OptionBuilder.withArgName("password").hasArg().withDescription("User password").create("password");
        options.addOption(option);

        option = OptionBuilder.withArgName("n").hasArg().withDescription("Number of times to run the report").create("count");
        options.addOption(option);

        option = OptionBuilder.withArgName("report_name | regex | *").hasArg().withDescription("Regular Expression representing a set of reports.  Defaults to all (*)").create("report");
        options.addOption(option);

        option = OptionBuilder.withDescription("Do not execute the reports, just list them.  Respects pattern matching").create("list");
        options.addOption(option);

        return options;

    }

    private static InvocableReport[] fetchReports(SoapEnvironmentSettings settings) throws Exception {
        BackstopReportDirectoryService_1_0Stub stub = new BackstopReportDirectoryService_1_0Stub(settings.getBaseURI() + "/backstop/services/BackstopReportDirectoryService_1_0");
        LoginInfo loginInfo = settings.getLoginInfo();

        int timeout = 10*60*1000;
        ServiceClient sc = stub._getServiceClient();
        sc.getOptions().setTimeOutInMilliSeconds(timeout);
        sc.getOptions().setProperty(HTTPConstants.SO_TIMEOUT, new Integer(timeout));
        sc.getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(timeout));



        System.out.println(getTimeStamp()  + "," + "MESSAGE=\"Fetching reports from " + settings.getBaseURI() + '"');
        ListSharedReportsResponse sharedReportsResponse = stub.listSharedReports(new ListSharedReports(), loginInfo);
        InvocableReport[] reports = sharedReportsResponse.getOut().getInvocableReport();
        return reports;
    }

    private static String getTimeStamp() {
        String ts = "";
        try {
            ts = sdf.format(new Date());
        } catch (Exception exc) {

        }
        return  "[" + ts + "]";
    }

    private static void runReport(SoapEnvironmentSettings settings, InvocableReport report) {
        if (settings.acceptReport(report.getReportTitle())) {
            ReportRunner runner = new ReportRunner(settings, report);
            for (int inx =0; inx < settings.getIterationCount(); inx++) {
                runner.run();
                System.out.println(getTimeStamp()  + "," + runner.toString());
                if (runner.getRunStatus() == ReportRunner.RunStatus.FAILURE) {
                    try {
                        // on failures, sleep 1/2 second
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {

                    }
                    break;
                }
            }
        }
    }
}
