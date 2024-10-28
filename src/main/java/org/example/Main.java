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

    final static String SonarQubeCookie = "_xsrf=2|8e7c3eb6|37e1e16b4c5a6fe9c2a3ce8cce9f5be4|1728558468; username-localhost-8889=\"2|1:0|10:1728756948|23:username-localhost-8889|44:MjhkOTQ2NTVlZmE5NDUxMDkwMzU3YmRhNGRiZDBlMDY=|d10de9d63012c8e93c896321c9df10a450cdb0c7ee84710112b55a3d84a4afdb\"; username-localhost-8888=\"2|1:0|10:1728958240|23:username-localhost-8888|44:NWZhYjViMmJhNDJiNDE3Yjg3ZDlmYzhkMDkxZjc4NDY=|5f7ab6447eca7e1e99a1948cae14c7f02b93de040de6ddeb6368591d1aa11ed6\"; XSRF-TOKEN=q2ctg34va5kcjma86ur1frcq24; JWT-SESSION=eyJhbGciOiJIUzI1NiJ9.eyJsYXN0UmVmcmVzaFRpbWUiOjE3Mjg5NTkyNDMzMDUsInhzcmZUb2tlbiI6InEyY3RnMzR2YTVrY2ptYTg2dXIxZnJjcTI0IiwianRpIjoiNzQyNGNkZmYtYzM5ZS00Y2NkLWI3MjItNGRmMzFlOGEwNTE4Iiwic3ViIjoiYzE4ODZjNGItMmU3MS00OTlmLTgzMTYtZmE5MDU2OTE1N2VkIiwiaWF0IjoxNzI4OTU5MjQzLCJleHAiOjE3MjkyMTg0NDN9.ZunlqtYvd3Fed72r1xwHYqcwSDqxqQHymqosaNydi2k";
    final static String projectName = "logging-log4j2";
    final static String templatePath = "C:\\Users\\kust\\Desktop\\analyse_result.xlsx";
    final static String bugsJarPath = "C:\\progs\\bugs\\bugs-dot-jar";
    final static String PVSPath = "C:\\Users\\kust\\AppData\\Roaming\\PVS-Studio-Java\\7.33.85174\\pvs-studio.jar";
    final static String jreLibPath = "C:\\Program Files\\Java\\jre1.8.0_421\\lib";
    final static String SonarScannerPath = "C:\\sonarqube-10.6.0.92116\\sonar-scanner-6.2.0.4584-windows-x64\\bin\\sonar-scanner.bat";
    final static String analyseResultPath = "C:\\Users\\kust\\Desktop\\analyse_result_logging-log4j2.xlsx";

    public static void main(String[] args) throws IOException, InterruptedException {

        List<Branch> branches = GitUtils.getBranches(projectName);
        Iterator<Branch> iterator = branches.iterator();
        Integer analyseLine = 1;
        Workbook wb = new XSSFWorkbook(templatePath);
        Sheet sheet = wb.getSheetAt(0);

        while (iterator.hasNext()) {
            Branch branch = iterator.next();
            GitUtils.checkoutToBranch(branch.getBranchName(), projectName);
            String diff = getDiff(bugsJarPath+"\\"+projectName+"\\.bugs-dot-jar\\developer-patch.diff");
            for (String fileName : branch.getBugsFiles()) {

                String command = "java -jar "+PVSPath+" -s "+bugsJarPath+"\\"+projectName+"\\" + fileName + " -e \""+bugsJarPath+"\\"+projectName+"\" \""+jreLibPath+"\" -j4 -o report.txt -O text";

                System.out.println(new File(bugsJarPath+"\\"+projectName+"\\"+fileName).length());
                if(new File(bugsJarPath+"\\"+projectName+"\\"+fileName).length()>2500000){
                    continue;
                }
                BufferedReader reader = GitUtils.execCommandAndGetReader(SonarScannerPath+" -D\"sonar.sources=" + fileName + "\"", projectName);
                String s2 = null;
                while ((s2 = reader.readLine()) != null) {
                    System.out.println(s2);
                }
                System.out.println(command);
                BufferedReader bufferedReader = GitUtils.execCommandAndGetReader(command, projectName);
                String s = null;
                while ((s = bufferedReader.readLine()) != null) {
                    System.out.println(s);
                }
                File report = new File(bugsJarPath+"\\"+projectName+"\\report.txt");

                List<Object> issues = getIssue(fileName);

                if (report.length() > 0 || issues.size() > 0) {
                    if(report.length()>0){
                        System.out.println("PVS found something");
                    }
                    if(issues.size() > 0){
                        System.out.println("SonarQube found something");
                    }
                    String PVSBugsResult = getPVSReport(bugsJarPath+"\\"+projectName+"\\report.txt");
                    String result = getSonarQubeReport(issues);
                    Row row = sheet.createRow(analyseLine++);
                    saveResult(branch.branchName,fileName,PVSBugsResult,result,row,wb,diff);
                    System.out.println(branch.getBranchName());

                }

            }

        }


    }

    private static String getDiff(String diffPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(diffPath))) {
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

    private static String getPVSReport(String reportPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(reportPath))) {
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

    private static String getSonarQubeReport(List<Object> issues){

        String result = "";

        for (Object o : issues) {

            LinkedHashMap<String, Object> mappedIssue = (LinkedHashMap<String, Object>) o;
            System.out.println(mappedIssue.get("type"));
            if(!mappedIssue.get("type").equals("CODE_SMELL")){
                result += mappedIssue.get("component");
                result += " ";
                result += mappedIssue.get("textRange");
                result += " ";
                result += mappedIssue.get("message");
                result += "\n";
            }
        }

        return result;
    }

    private static List<Object> getIssue(String fileName) throws IOException, InterruptedException {
        int count =0;
        List<Object> issues = new ArrayList<>();
        while (issues.size()==0){
            count++;
            if(count==5){
                break;
            }
            TimeUnit.SECONDS.sleep(5);
            String request = "http://localhost:9000/api/issues/search?components=test:" + fileName;
            System.out.println(request);
            Content getResult = Request.Get(request)
                    .addHeader("cookie", SonarQubeCookie)
                    .execute().returnContent();

            Map<String, Object> map = new ObjectMapper().readValue(getResult.asString(), new TypeReference<HashMap<String, Object>>() {
            });
            issues = (List<Object>) map.get("issues");

            System.out.println(issues);
            System.out.println("Issue Count : " + issues.size());
        }

        return issues;
    }

    private static void saveResult(String branchName,String fileName,String PVSResult,String SonarQubeResult,Row row,Workbook wb,String diff) throws IOException {

        row.createCell(0).setCellValue(branchName);
        row.createCell(1).setCellValue(fileName);
        row.createCell(2).setCellValue(PVSResult);
        row.createCell(3).setCellValue(SonarQubeResult);
        if(diff.length()<32767) {
            row.createCell(4).setCellValue(diff);
        }        FileOutputStream fileOut = new FileOutputStream(analyseResultPath);
        wb.write(fileOut);
        fileOut.close();

    }

}