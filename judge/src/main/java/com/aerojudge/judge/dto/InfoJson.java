package com.aerojudge.judge.dto;

import java.util.List;

public class InfoJson {
    private String appName;
    private String appVersion;

    private List<InfoLine> info_lines;

    // Constructors, getters, setters
    public InfoJson() {
        this.appName = "AeroJudge Device";
        this.appVersion = "?";
        this.info_lines = null;
    }

    public InfoJson(String appName, String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.info_lines = null;
    }

    public InfoJson(String appName, String appVersion, List<InfoLine> info_lines) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.info_lines = info_lines;
    }

    public String getAppName() {
        return appName;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }
    public String getAppVersion() {
        return appVersion;
    }
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    public List<InfoLine> getInfo_lines() {
        return info_lines;
    }
    public void setInfo_lines(List<InfoLine> info_lines) {
        this.info_lines = info_lines;
    }

}
