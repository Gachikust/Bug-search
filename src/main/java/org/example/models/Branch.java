package org.example.models;

import java.util.ArrayList;
import java.util.List;

public class Branch {
    String branchName;
    List<String> bugsFiles = new ArrayList<>();

    public Branch(String bramchName) {
        this.branchName = bramchName;
    }

    public List<String> getBugsFiles() {
        return new ArrayList<String>(bugsFiles);
    }

    public String getBranchName() {
        return branchName;
    }

    public void addBugFile(String file){
        bugsFiles.add(file);
    }

    @Override
    public String toString() {
        return "Branch{" +
                "bramchName='" + branchName + '\'' +
                ", bugsFiles=" + bugsFiles +
                '}';
    }
}
