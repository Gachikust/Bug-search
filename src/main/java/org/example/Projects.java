package org.example;

public enum Projects {
    accumulo("accumulo"),
    camel("camel"),
    commonsMath("commons-math"),
    flink("flink"),
    jackrabbit_oak("jackrabbit-oak"),
    logging_log4j2("logging-log4j2"),
    wicket("wicket");

    Projects(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;
}
