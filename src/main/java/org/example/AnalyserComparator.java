package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.Header;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.Analyzers.PVSStudioUtils;
import org.example.Analyzers.SonarQubeUtils;
import org.example.models.Branch;
import org.example.models.Bug;
import org.example.models.PrintData;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnalyserComparator {
    private String projectName="" ;
    final private String templatePath = "template/analyse_result_template.xlsx";
    final private String bugsJarPath = "bugs-dot-jar";
    private String analyseResultPath ;
    private Integer analyseLine = 1;
    static Sheet sheet;
    static Workbook wb;

    public AnalyserComparator() throws IOException {
        wb = new XSSFWorkbook(templatePath);
        sheet = wb.getSheetAt(0);
        SonarQubeUtils.SonarQubeAuth();
    }

    public void compareAnalyzerOnProject(String projectName) throws IOException, InterruptedException {
        this.projectName = projectName;
        analyseResultPath = "analyseResult/analyse_result_"+projectName+".xlsx";

        List<Branch> branches = GitUtils.getBranches(projectName);
        Iterator<Branch> iterator = branches.iterator();

        while (iterator.hasNext()) {
            Branch branch = iterator.next();
            List<PrintData> printDataList = analyseBranch(branch);
            if(!printDataList.isEmpty()){
                for (PrintData printData:printDataList) {
                    saveBugsToTable(printData);
                }
            }
        }
    }
    public void compareAnalyzerOnBranch(String projectName,String branchName) throws IOException, InterruptedException {
        this.projectName = projectName;
        analyseResultPath = "analyseResult/analyse_result_"+projectName+"_"+branchName.split("_")[branchName.split("_").length - 1]+".xlsx";

        List<Branch> branches = GitUtils.getBranches(projectName);

        for (Branch branch:branches) {
            if(branch.getBranchName().trim().equals(branchName)){
                List<PrintData> printDataList = analyseBranch(branch);
                if(!printDataList.isEmpty()){
                    for (PrintData printData:printDataList) {
                        saveBugsToTable(printData);
                    }
                }
            }
        }
    }

    private List<PrintData> analyseBranch(Branch branch) throws IOException, InterruptedException {
        System.out.println(branch.getBranchName());
        GitUtils.checkoutToBranch(branch.getBranchName(), projectName);
        String diff = readFile(bugsJarPath+"\\"+projectName+"\\.bugs-dot-jar\\developer-patch.diff");
        List<PrintData> printDataList = new ArrayList<>();
        for (String fileName : branch.getBugsFiles()) {
            List<String> analyzerReports = analyzeFile(fileName,diff);
            if (analyzerReports != null) {
                PrintData printData = new PrintData(analyzerReports,branch.getBranchName(),fileName,diff);
                printDataList.add(printData);
            }
        }
        return printDataList;
    }

    private List<String> analyzeFile(String fileName,String diff) throws IOException, InterruptedException {
        if(new File(bugsJarPath+"\\"+projectName+"\\"+fileName).length()<2500000){
            //System.out.println(new File(bugsJarPath+"\\"+projectName+"\\"+fileName).length());

            List<Bug> SonarQubeBugs = SonarQubeUtils.analyzeFile(fileName,projectName);
            List<Bug> PVSBugs = PVSStudioUtils.analyzeFile(fileName,projectName);

            if (PVSBugs.size() > 0 || SonarQubeBugs.size() > 0) {
                String PVSResult="";
                String SonarQubeResult="";
                if(PVSBugs.size() > 0){
                    System.out.println("PVS found something");
                    PVSBugs = verifyPVSBugs(PVSBugs,diff);
                    PVSResult = bugListToString(PVSBugs);
                }

                if(SonarQubeBugs.size() > 0){
                    System.out.println("SonarQube found something");
                    SonarQubeBugs = verifyPVSBugs(SonarQubeBugs,diff);
                    SonarQubeResult = bugListToString(SonarQubeBugs);
                }
                List<String> analyzerReports = new ArrayList<>();
                analyzerReports.add(PVSResult);
                analyzerReports.add(SonarQubeResult);

                return analyzerReports;
            }
        }

        return null;
    }

    private void saveBugsToTable(PrintData printData) throws IOException {
            Row row = sheet.createRow(analyseLine++);
            saveResult(printData.getBranchName(),
                    printData.getFileName(),
                    printData.getAnalyzersBugs(),
                    row,
                    printData.getDiff());


    }

    public static String readFile(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    private String bugListToString(List<Bug> bugs){
        String result="";
        for (Bug bug:bugs) {
            result +=bug.getBugFile()+" ";
            result +="line: "+bug.getBugLine()+" ";
            result +="message: "+bug.getBugComment()+" ";
            result +='\n';
        }
        return result;
    }

    private void saveResult(String branchName,String fileName,List<String> analyzerReports,Row row,String diff) throws IOException {
        int k =0;
        row.createCell(k++).setCellValue(branchName);
        row.createCell(k++).setCellValue(fileName);
        for (String reports:analyzerReports) {
            row.createCell(k++).setCellValue(reports);
        }
        if(diff.length()<32767) {
            row.createCell(k++).setCellValue(diff);
        }else {
            row.createCell(k++).setCellValue(diff.substring(0,32767));
        }
        FileOutputStream fileOut = new FileOutputStream(analyseResultPath);
        wb.write(fileOut);
        fileOut.close();

    }


    private List<Bug> verifyPVSBugs (List<Bug> PVSbugs,String diff){
        Iterator<Bug> bugIterator = PVSbugs.iterator();
        List<String> diffLines = Arrays.asList(diff.split("\n"));
        List<Bug> checkedBugs = new ArrayList<>();
        while (bugIterator.hasNext()){
            Bug currentBug = bugIterator.next();
            int k=0;
            for (int i = 0; i < diffLines.size(); i++) {

                String line = diffLines.get(i);
                if(line.contains(currentBug.getBugFile())){
                    k=i+1;
                    break;

                }
            }
            while ( diffLines.size()>k && !diffLines.get(k).contains("diff --git")){
                if(diffLines.get(k).contains("@@")){
                    String infoLine = diffLines.get(k);
                    int startLine = Integer.parseInt(infoLine.substring(infoLine.indexOf("-")+1,infoLine.indexOf(",")));
                    int linesCount = Integer.parseInt(infoLine.substring(infoLine.indexOf(",")+1,infoLine.indexOf("+")).trim());
                    int checkedLine =0;
                    while (checkedLine<linesCount){
                        k++;
                        if(startLine+checkedLine == currentBug.getBugLine() && diffLines.get(k).substring(0,1).equals("-")){
                            checkedBugs.add(currentBug);
                        }
                        if(!diffLines.get(k).substring(0,1).equals("+")){
                            checkedLine++;
                        }

                    }
                }
                k++;
            }
        }
        return checkedBugs;
    }
}
