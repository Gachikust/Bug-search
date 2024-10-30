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
        AnalyserComparator.SonarQubeAuth();
        //AnalyserComparator.compareAnalyzerOnProject(Projects.logging_log4j2.getName());
        AnalyserComparator.setStatic(Projects.accumulo.getName());
        Branch branch = new Branch("remotes/origin/bugs-dot-jar_ACCUMULO-2390_28294266");
        branch.addBugFile("src/trace/src/main/java/org/apache/accumulo/cloudtrace/instrument/TraceProxy.java");
        AnalyserComparator.analyseBranch(branch);

        AnalyserComparator.setStatic(Projects.commonsMath.getName());
        branch = new Branch("remotes/origin/bugs-dot-jar_MATH-1045_a4ffd393");
        branch.addBugFile("src/main/java/org/apache/commons/math3/linear/EigenDecomposition.java");
        AnalyserComparator.analyseBranch(branch);

        AnalyserComparator.setStatic(Projects.flink.getName());
        branch = new Branch("remotes/origin/bugs-dot-jar_FLINK-1458_91f9bfc7");
        branch.addBugFile("flink-java/src/main/java/org/apache/flink/api/java/typeutils/TypeExtractor.java");
        AnalyserComparator.analyseBranch(branch);
    }

}