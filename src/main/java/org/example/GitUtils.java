package org.example;

import org.example.models.Branch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GitUtils {

    public static List<Branch> getBranches(String projectName) throws IOException {
        List<Branch> branches = new ArrayList<>();
        fillBranchesList(branches,projectName);
        return branches;
    }

    private static void fillBranchesList(List<Branch> branches,String projectName) throws IOException {
            BufferedReader stdInput = execCommandAndGetReader("git branch -a",projectName);

            String branchName = null;
            while ((branchName = stdInput.readLine()) != null) {
                System.out.println(branchName);
                Branch branch = new Branch(branchName);

                if (branchName.contains("remotes/origin/bugs-dot-jar_")){


                        String noErrBranch =branchName.split("_")[branchName.split("_").length - 1];
                        String command = "git diff " + branchName + " " + noErrBranch + " "+ " --name-only";
                        BufferedReader fileNameReader = execCommandAndGetReader(command,projectName);
                        String s = null;
                        while ((s = fileNameReader.readLine()) != null) {
                            if(s.contains(".java")){
                                branch.addBugFile(s);
                            }
                        }

                    branches.add(branch);
                }

            }
    }

    public static BufferedReader execCommandAndGetReader(String command, String projectName) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        processBuilder.directory(new File("bugs-dot-jar\\"+projectName));
        Process process = processBuilder.start();
        //Process process = Runtime.getRuntime().exec(command,null,new File("bugs-dot-jar\\"+projectName));
        //System.out.println("Working directory:" + "bugs-dot-jar\\"+projectName);
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public static void checkoutToBranch(String branch,String projectName) throws IOException {
        String command = "git checkout -f " + branch.split("/")[branch.split("/").length-1];
        System.out.println( command);
        BufferedReader bufferedReader = execCommandAndGetReader(command,projectName);
        String s = null;
        while ((s = bufferedReader.readLine()) != null) {
            System.out.println(s);
        }

    }

}
