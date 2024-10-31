package org.example;

import org.example.enums.Projects;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        AnalyserComparator comparator = new AnalyserComparator();
        comparator.compareAnalyzerOnProject(Projects.logging_log4j2.getName());
//        comparator.compareAnalyzerOnBranch(Projects.logging_log4j2.getName(), "remotes/origin/bugs-dot-jar_LOG4J2-1025_a96b455c");
    }

}