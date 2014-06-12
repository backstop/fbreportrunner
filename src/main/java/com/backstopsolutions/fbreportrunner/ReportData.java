package com.backstopsolutions.fbreportrunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dsteeber
 * Date: 5/6/14
 * Time: 8:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class ReportData {
    List<Collection<Object>> rows = new ArrayList<>();
    List<String> titles;

    public ReportData(Collection<String> titles) {
        titles = new ArrayList<String>();
       titles.addAll(titles);
    }

    public void addRow(Collection<Object> row) {
        List<Object> rowList = new ArrayList<>();
        rowList.addAll(row);
        rows.add(rowList);
    }

    private Collection<String> getTitles() {
        return titles;
    }

    public Collection<Collection<Object>> getData() {
        return rows;
    }

}
