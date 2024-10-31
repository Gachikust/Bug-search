package org.example.Analyzers;

import org.example.AnalyserComparator;
import org.example.GitUtils;
import org.example.models.Bug;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PVSStudioUtils {
    final private static String PVSPath = "pvs-studio-java\\7.33.85174\\pvs-studio.jar";
    public static List<Bug> analyzeFile(String fileName,String projectName) throws IOException {
        String startPVSCommand = "java -jar ../../"+PVSPath+" -s " + fileName + " -e / -j4 -o report.txt -O text";
        System.out.println(startPVSCommand);

        BufferedReader bufferedReader = GitUtils.execCommandAndGetReader(startPVSCommand, projectName);
        String s = null;
        while ((s = bufferedReader.readLine()) != null) {
            System.out.println(s);
        }
        String PVSreport = AnalyserComparator.readFile("bugs-dot-jar"+"\\"+projectName+"\\report.txt");
        return getPVSBugs(PVSreport,projectName);
    }

    private static List<Bug> getPVSBugs(String PVSResult,String projectName){
        List<String> PVSbugs = Arrays.asList( PVSResult.split("\n"));
        List<Bug> bugList = new ArrayList<>();
        String bugReport;
        String bugFile;
        int bugLine;
        if(!PVSResult.substring(0,0).isEmpty()){
            for (String PVSbug:PVSbugs) {
                bugReport = PVSbug.substring(0,PVSbug.lastIndexOf("["));
                bugFile = PVSbug.substring(PVSbug.indexOf(projectName)+1+projectName.length(),PVSbug.lastIndexOf(":")).replace('\\','/');
                bugLine = Integer.parseInt(PVSbug.substring(PVSbug.lastIndexOf(":")+1,PVSbug.lastIndexOf("]")));
                bugList.add(new Bug(bugLine,bugReport,bugFile));
            }
        }
        return bugList;
    }
}
