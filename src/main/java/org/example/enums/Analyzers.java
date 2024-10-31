package org.example.enums;

import javax.swing.*;

public enum Analyzers {
    PVS_studio("PVS-studio"),
    SonarQube("SonarQube");

    Analyzers(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
