package com.aerojudge.judge.dto;

public class InfoLine {
    private String label;
    private String value;
    private String name; //optional
    private String icon; // optional

    public InfoLine(String label, String value) {
        this.label = label;
        this.value = value;
        this.name = null;
        this.icon = null;
    }
    public InfoLine(String label, String name, String value) {
        this.label = label;
        this.name = name;
        this.value = value;
        this.icon = null;
    }
    public InfoLine(String label, String name, String value, String icon) {
        this.label = label;
        this.name = name;
        this.value = value;
        this.icon = icon;
    }
    
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getValue() {
        return value;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
}