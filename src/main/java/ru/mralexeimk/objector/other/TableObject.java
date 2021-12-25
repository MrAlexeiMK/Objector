package ru.mralexeimk.objector.other;

import java.util.List;

public class TableObject {
    private String id;
    private String configuration;
    private double lr;

    public TableObject(String id, String configuration, double lr) {
        this.id = id;
        this.configuration = configuration;
        this.lr = lr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public double getLr() {
        return lr;
    }

    public void setLr(double lr) {
        this.lr = lr;
    }
}
