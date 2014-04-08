package com.backstopsolutions.fbreportrunner;

import com.backstopsolutions.backstopservice.LoginInfo;
import com.backstopsolutions.backstopservice.LoginInfoType;
import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created with IntelliJ IDEA.
 * User: dsteeber
 * Date: 3/21/14
 * Time: 8:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class SoapEnvironmentSettings {

    private static final Pattern MATCH_ALL = Pattern.compile(".*");

    private String baseURI;
    private String userName;
    private String password;
    private Calendar asOfDate;
    private int iterationCount = 1;
    private Pattern reportPattern = MATCH_ALL;

    public SoapEnvironmentSettings() {
        setDefaultAsOfDate();
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Calendar getAsOfDate() {
        return asOfDate;
    }

    private void setDefaultAsOfDate() {
        Calendar cal = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.add(1, Calendar.DATE);
        if (cal.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)) {
            // cal is not last day of month, so reset it to the prior month
            cal.set(Calendar.DATE,1);
            cal.add(Calendar.DATE, -1);

        }
        setAsOfDate(cal);
    }

    private Calendar resetTime(Calendar cal) {
        cal.set(Calendar.HOUR,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal;
    }

    public void setAsOfDate(String dateStr)  throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("mm/dd/yyyy");
        Date dt = sdf.parse(dateStr);
        setAsOfDate(dt);
    }

    public void setAsOfDate(Calendar cal) {
        setAsOfDate(cal.getTime());
    }

    public void setAsOfDate(Date dt) {
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        this.asOfDate = resetTime(c);
    }


    public LoginInfo getLoginInfo() {
        LoginInfo loginInfo = new LoginInfo();
        LoginInfoType loginInfoType = new LoginInfoType();
        loginInfoType.setUsername(getUserName());
        loginInfoType.setPassword(getPassword());
        loginInfo.setLoginInfo(loginInfoType);

        return loginInfo;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }
    public String getReportPattern() {
        return reportPattern.toString();
    }

    public void setReportPattern(String reportPattern) {
        if ("*".equals(reportPattern) || StringUtils.isEmpty(reportPattern)) {
            this.reportPattern = MATCH_ALL;
        } else {
            this.reportPattern = Pattern.compile(reportPattern);
        }
    }

    public boolean acceptReport(String reportName) {
        Matcher m = reportPattern.matcher(reportName);

        return m.matches();
    }


}
