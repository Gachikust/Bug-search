package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PrintData {
    private List<String> analyzersBugs;
    private String branchName;
    private String fileName;
    private String diff;
}
