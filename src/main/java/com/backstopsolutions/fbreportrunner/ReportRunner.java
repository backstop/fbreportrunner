package com.backstopsolutions.fbreportrunner;

import com.backstopsolutions.backstopservice.RunPeopleOrgsReport;
import com.backstopsolutions.backstopservice.RunPeopleOrgsReportResponse;
import com.backstopsolutions.fbreportrunner.services.BackstopCrmQueryService_1_0Stub;
import com.backstopsolutions.fundbutter.webservice.returnentity.InvocableReport;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dsteeber
 * Date: 3/21/14
 * Time: 9:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class ReportRunner implements Runnable {

    private static final String BACKSTOP_SERVICES_PN = "com.backstopsolutions.backstopservice";
    private static final String BACKSTOP_SERVICES_STUB_PN = "com.backstopsolutions.fbreportrunner.services";

    public enum RunStatus {
        NOT_EXECUTED, SUCCESS, FAILURE
    }

    private InvocableReport report;
    private SoapEnvironmentSettings settings;
    private String serviceURI;
    private String serviceMethodName;
    private long executionTime = 0l;
    private long resultsCount = 0l;
    private int fieldCount = 0;
    private List<String> fieldNames = new ArrayList<>();


    private RunStatus runStatus = RunStatus.NOT_EXECUTED;

    public ReportRunner(SoapEnvironmentSettings settings, InvocableReport report) {
        this.report = report;
        this.settings = settings;
        String serviceName = report.getServiceName();
        String[] parts = serviceName.split("#");
        serviceURI = settings.getBaseURI() + parts[0];
        serviceMethodName = parts[1];
        try {
            ObjectMapper om = new ObjectMapper();
            HashMap<String, Object> fields = om.readValue(report.getQueryDefinition(), HashMap.class);
            fieldCount = fields.size();
            for (String key : fields.keySet()) {
                fieldNames.add(key);
            }
        } catch (Exception exc) {
            fieldCount = 0;
        }
    }

    public boolean isLargeReport() {
        return fieldCount > 50;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    private void setExecutionTime(long time) {
        this.executionTime = time;
    }

    private void setResultsCount(long count) {
        this.resultsCount = count;
    }

    public long getResultsCount() {
        return resultsCount;
    }

    public RunStatus getRunStatus() {
        return runStatus;
    }

    public String getTitle() {
        return report.getReportTitle();
    }

    public void run() {
        runStatus = RunStatus.NOT_EXECUTED;
        setExecutionTime(0l);
        setResultsCount(0l);
       try {
           doRun();
           runStatus = RunStatus.SUCCESS;
       } catch (Exception exc) {
           runStatus = RunStatus.FAILURE;
           System.err.println(report.getReportTitle());
           System.err.println(serviceURI);
           System.err.println(serviceMethodName);
           exc.printStackTrace();
       }
    }

    private Object getStub() throws Exception  {
        String parts[] = serviceURI.split("/");
        Class<?> stubClass = Class.forName(BACKSTOP_SERVICES_STUB_PN + "." + parts[parts.length - 1] + "Stub");
        Constructor stubClassConstructor = stubClass.getConstructor(String.class);
        Object stub = stubClassConstructor.newInstance(serviceURI);

        int timeout = 20 * 60 * 1000;
        Method getSC = stub.getClass().getMethod("_getServiceClient");
        ServiceClient sc = (ServiceClient)getSC.invoke(stub);
        sc.getOptions().setTimeOutInMilliSeconds(10*60*1000);
        sc.getOptions().setProperty(HTTPConstants.SO_TIMEOUT, new Integer(timeout));
        sc.getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(timeout));


        return stub;
    }

    private Object getReportObject() throws Exception {
        String className = BACKSTOP_SERVICES_PN + "." + StringUtils.capitalize(serviceMethodName);
        Class<?> reportClass = Class.forName(className);

        Object reportObject = reportClass.newInstance();

        Method setDefinition = reportClass.getMethod("setJsonDefinition", String.class);
        setDefinition.invoke(reportObject, report.getQueryDefinition());

        Method setRestriction = reportClass.getMethod("setRestrictionExpression", String.class);
        setRestriction.invoke(reportObject, report.getRestrictionExpression());

        Method setAsOf = reportClass.getMethod("setAsOf", Calendar.class);
        setAsOf.invoke(reportObject, settings.getAsOfDate());


        // fill in the json stuff here
        return reportObject;
    }

    //TODO add in error handling
    private void doRun() throws Exception {
        Object stub = getStub();
        try {
            Object reportObject = getReportObject();

            executeSoapCall(stub, reportObject);
        } finally {

            try {
                Method c = stub.getClass().getMethod("_getServiceClient");
                Object sc = c.invoke(stub);
                c = sc.getClass().getMethod("cleanup");
                c.invoke(sc);
            }catch (Exception exc2) {
                exc2.printStackTrace();
            }
        }
    }

    private void executeSoapCall(Object stub, Object reportObject) throws Exception {
        Object loginInfo = settings.getLoginInfo();
        String methodName = serviceMethodName;
        if ((!serviceMethodName.endsWith("Large")) && isLargeReport()) {
            methodName = methodName + "Large";
        }

        Method execMethod = stub.getClass().getMethod(methodName, reportObject.getClass(), loginInfo.getClass());

        long startTime = System.currentTimeMillis();
        Object returnData = execMethod.invoke(stub, reportObject, loginInfo);
        long endTime = System.currentTimeMillis();
        setExecutionTime(endTime-startTime);
        Object[] rows = getRows(returnData);
        setResultsCount((rows != null) ? rows.length : 0);


        // can flag this, but for now, list build the ReportData
        //buildReportData(rows);
    }

    private ReportData buildReportData(Object[] rows) {
        ReportData data = new ReportData(this.fieldNames);
        for (Object row : rows) {
            for (String fieldName : fieldNames) {
               //
            }
            //data.addRow();
        }
        return data;
    }



    private Object[] getRows(Object returnData) throws Exception {
        Method m = returnData.getClass().getMethod("getOut");
        Object out = m.invoke(returnData);

        if (isLargeReport() || out.getClass().toString().contains("Large")) {
            m = out.getClass().getMethod("getReportRowLarge");
        } else {
            m = out.getClass().getMethod("getReportRow");
        }
        Object rows = m.invoke(out);
        Object[] ary = (Object[])rows;

        return ary;
    }

    private String getReportHash() {
        String request = StringUtils.replaceChars(report.getQueryDefinition() + report.getRestrictionExpression(), "\t\n\r ", "");
        return DigestUtils.md5Hex(request);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("REPORT_TITLE=");
        sb.append('"');
        sb.append(this.getTitle());
        sb.append('"');
        sb.append(',');
        sb.append("URI=");
        sb.append(this.serviceURI);
        sb.append(',');
        sb.append("RESULT=");
        sb.append(getRunStatus());
        sb.append(',');
        sb.append("RESULTS_COUNT=");
        sb.append(getResultsCount());
        sb.append(',');
        sb.append("RUN_TIME=");
        sb.append(getExecutionTime());
        sb.append(',');
        sb.append("REPORT_HASH=");
        sb.append(getReportHash());
        sb.append(',');
        sb.append("FIELD_COUNT=");
        sb.append(fieldCount);

        return sb.toString();
    }
}
