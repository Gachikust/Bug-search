package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        //AnalyserComparator.compareAnalyzerOnProject(Projects.commonsMath.getName());
        AnalyserComparator.setStatic(Projects.accumulo.getName());
        Branch branch = new Branch("remotes/origin/bugs-dot-jar_ACCUMULO-2390_28294266");
        branch.addBugFile("src/trace/src/main/java/org/apache/accumulo/cloudtrace/instrument/TraceProxy.java");
        AnalyserComparator.analyseBranch(branch);
    }

}