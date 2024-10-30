package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.Header;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnalyserComparator {

    static String SonarQubeCookie = "";
    static String projectName ;
    final static String templatePath = "analyse_result.xlsx";
    final static String bugsJarPath = "bugs-dot-jar";
    final static String PVSPath = "pvs-studio-java\\7.33.85174\\pvs-studio.jar";
    final static String SonarScannerPath = "sonarqube-10.6.0.92116\\sonar-scanner-6.2.0.4584-windows-x64\\bin\\sonar-scanner.bat";
    static String analyseResultPath ;
    static Integer analyseLine = 1;

    static Sheet sheet;
    static Workbook wb;

    private static void StartSonarQube() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "StartSonar.bat");
        processBuilder.directory(new File("sonarqube-10.6.0.92116\\sonarqube-10.6.0.92116\\bin\\windows-x86-64"));
        Process process = processBuilder.start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s = null;
        while ((s = bufferedReader.readLine()) != null) {
            System.out.println(s);
            if(s.contains("SonarQube is operational")){
                break;
            }
        }
        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        s = null;
        while ((s = bufferedReader2.readLine()) != null) {
            System.out.println(s);
        }
    }

    public static void SonarQubeAuth() throws IOException {
        boolean isAuth=false;
        try{
            String request = "http://localhost:9000/api/issues/search";
            System.out.println(request);
            Content getResult = Request.Get(request)
                    .addHeader("cookie", SonarQubeCookie)
                    .execute().returnContent();
            System.out.println(getResult);
            isAuth=true;
        }catch (HttpResponseException e){
            isAuth=false;
        }catch (HttpHostConnectException e){
            StartSonarQube();
        }
        if(!isAuth){
            String request = "http://localhost:9000/api/authentication/login";
            System.out.println(request);
            final Collection<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("login", "admin"));
            params.add(new BasicNameValuePair("password", "qweasdzxc123"));
            HttpResponse getResult = Request.Post(request)
                    .bodyForm(params, Charset.defaultCharset())
                    .execute().returnResponse();
            Header[] strings = getResult.getHeaders("Set-Cookie");
            String firstHeader = strings[0].getValue();
            firstHeader = firstHeader.substring(0,firstHeader.indexOf(";"));
            String secHeader = strings[0].getValue();
            secHeader = secHeader.substring(0,secHeader.indexOf(";"));
            AnalyserComparator.SonarQubeCookie = firstHeader+";"+secHeader;

        }

    }

    public static void compareAnalyzerOnProject(String projectName) throws IOException, InterruptedException {
        AnalyserComparator.projectName = projectName;
        AnalyserComparator.analyseResultPath = "analyse_result_"+projectName+".xlsx";

        List<Branch> branches = GitUtils.getBranches(projectName);
        Iterator<Branch> iterator = branches.iterator();

        wb = new XSSFWorkbook(templatePath);
        sheet = wb.getSheetAt(0);

        while (iterator.hasNext()) {

            Branch branch = iterator.next();
            analyseBranch(branch);
        }
    }

    public static void setStatic(String projectName) throws IOException {
        AnalyserComparator.projectName = projectName;
        AnalyserComparator.analyseResultPath = "analyse_result_"+projectName+".xlsx";
        System.out.println(AnalyserComparator.analyseResultPath);
        wb = new XSSFWorkbook(templatePath);
        sheet = wb.getSheetAt(0);
    }

    public static void analyseBranch(Branch branch) throws IOException, InterruptedException {
        GitUtils.checkoutToBranch(branch.getBranchName(), projectName);
        String diff = getDiff(bugsJarPath+"\\"+projectName+"\\.bugs-dot-jar\\developer-patch.diff");

        for (String fileName : branch.getBugsFiles()) {

            String command = "java -jar ../../"+PVSPath+" -s " + fileName + " -e / -j4 -o report.txt -O text";

            System.out.println(new File(bugsJarPath+"\\"+projectName+"\\"+fileName).length());
            if(new File(bugsJarPath+"\\"+projectName+"\\"+fileName).length()>2500000){
                continue;
            }
            String SonarQubeCommand = "call ..\\..\\"+SonarScannerPath+" -D\"sonar.sources=" + fileName + "\"";
            System.out.println(SonarQubeCommand);
            BufferedReader reader = GitUtils.execCommandAndGetReader(SonarQubeCommand, projectName);
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

            String result = getSonarQubeReport(issues);

            String PVSBugsResult = getPVSReport(bugsJarPath+"\\"+projectName+"\\report.txt");
            if (report.length() > 0 || result.length() > 0) {
                String PVSResult="";
                String SonarQubeResult="";
                if(report.length()>0){
                    System.out.println("PVS found something");
                    List<Bug> PVSBugs = getPVSBugs(PVSBugsResult);
                    PVSBugs = verifyPVSBugs(PVSBugs,diff);
                    PVSResult = bugListToString(PVSBugs);
                }

                if(result.length() > 0){
                    System.out.println("SonarQube found something");
                    List<Bug> SonarQubeBugs = getSonarQubeBugs(issues);
                    SonarQubeBugs = verifyPVSBugs(SonarQubeBugs,diff);
                    SonarQubeResult = bugListToString(SonarQubeBugs);
                }

                Row row = sheet.createRow(analyseLine++);

                saveResult(branch.branchName,fileName,PVSResult,SonarQubeResult,row,wb,diff);

                System.out.println(branch.getBranchName());

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

    private static String bugListToString(List<Bug> bugs){
        String result="";
        for (Bug bug:bugs) {
            result +=bug.getBugFile()+" ";
            result +="line: "+bug.getBugLine()+" ";
            result +="message: "+bug.getBugComment()+" ";
            result +='\n';
        }
        return result;
    }

    private static String getSonarQubeReport(List<Object> issues){

        String result = "";

        for (Object o : issues) {

            LinkedHashMap<String, Object> mappedIssue = (LinkedHashMap<String, Object>) o;
            System.out.println(mappedIssue.get("type"));
            //if(!mappedIssue.get("type").equals("CODE_SMELL")){
                result += mappedIssue.get("component");
                result += " ";
                result += mappedIssue.get("textRange");
                result += " ";
                result += mappedIssue.get("message");
                result += "\n";
            //}
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
    private static List<Bug> getPVSBugs(String PVSResult){
        List<String> PVSbugs = Arrays.asList( PVSResult.split("\n"));
        List<Bug> bugList = new ArrayList<>();
        String bugReport;
        String bugFile;
        int bugLine;
        for (String PVSbug:PVSbugs) {
            bugReport = PVSbug.substring(0,PVSbug.lastIndexOf("["));
            bugFile = PVSbug.substring(PVSbug.indexOf(projectName)+1+projectName.length(),PVSbug.lastIndexOf(":")).replace('\\','/');
            bugLine = Integer.parseInt(PVSbug.substring(PVSbug.lastIndexOf(":")+1,PVSbug.lastIndexOf("]")));
            bugList.add(new Bug(bugLine,bugReport,bugFile));
        }
        return bugList;
    }
    private static List<Bug> getSonarQubeBugs(List<Object> SonarQubeBugs){
        List<Bug> bugList = new ArrayList<>();
        String bugReport;
        String bugFile;
        int bugLine;
        for (Object o : SonarQubeBugs) {

            LinkedHashMap<String, Object> mappedIssue = (LinkedHashMap<String, Object>) o;
            //if(!mappedIssue.get("type").equals("CODE_SMELL")){
                bugFile= mappedIssue.get("component").toString();
                bugFile = bugFile.substring(bugFile.indexOf("test:")+6);
                Object range = mappedIssue.get("textRange");
                LinkedHashMap<String, Object> mappedRange = (LinkedHashMap<String, Object>) range;
                bugLine = (int)mappedRange.get("startLine");

                bugReport = mappedIssue.get("message").toString();
                bugList.add(new Bug(bugLine,bugReport,bugFile));
            //}
        }
        return bugList;
    }
    private static List<Bug> verifyPVSBugs (List<Bug> PVSbugs,String diff){
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
