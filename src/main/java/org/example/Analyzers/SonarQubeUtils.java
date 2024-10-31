package org.example.Analyzers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.example.GitUtils;
import org.example.models.Bug;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SonarQubeUtils {
    private static String SonarQubeCookie = "";
    final private static String SonarScannerPath = "sonarqube-10.6.0.92116\\sonar-scanner-6.2.0.4584-windows-x64\\bin\\sonar-scanner.bat";
    public static List<Bug> analyzeFile(String fileName,String projectName) throws IOException, InterruptedException {
        String startSonarQubeCommand = "call ..\\..\\"+SonarScannerPath+" -D\"sonar.sources=" + fileName + "\"";

        System.out.println(startSonarQubeCommand);

        BufferedReader reader = GitUtils.execCommandAndGetReader(startSonarQubeCommand, projectName);
        String s2 = null;
        while ((s2 = reader.readLine()) != null) {
            System.out.println(s2);
        }
        List<Object> rawBugs = getRawSonarQubeBugs(fileName,SonarQubeCookie);
        return getSonarQubeBugs(rawBugs);
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

    private static List<Object> getRawSonarQubeBugs(String fileName, String SonarQubeCookie) throws IOException, InterruptedException {
        int count =0;
        List<Object> issues = new ArrayList<>();
        while (issues.size()==0){
            if(count==5){
                break;
            }
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
            count++;
            TimeUnit.SECONDS.sleep(5);
        }

        return issues;
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
            SonarQubeCookie = firstHeader+";"+secHeader;
        }

    }

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

}
