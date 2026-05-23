package com.aerojudge.judge.dto;

public class FigureDTO {
    private int figNum;
    private int k_factor;
    private String description;
    private String spokenDescription;
    private String scoring;

    public FigureDTO(int figNum, int k_factor, String description, String scoring) {
        this.figNum = figNum;
        this.k_factor = k_factor;
        this.description = description.replaceAll("[^\\x00-\\x7F]", "");
        this.scoring = scoring;
    }

    public FigureDTO(int figNum, int k_factor, String description, String scoring, String spokenDescription) {
        this.figNum = figNum;
        this.k_factor = k_factor;
        this.description = description.replaceAll("[^\\x00-\\x7F]", "");
        this.spokenDescription = spokenDescription.replaceAll("[^\\x00-\\x7F]", "");
        this.scoring = scoring;
    }

    public int getFigNum() {
        return figNum;
    }

    public void setFigNum(int figNum) {
        this.figNum = figNum;
    }

    public int getK_factor() {
        return k_factor;
    }

    public void setK_factor(int k_factor) {
        this.k_factor = k_factor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpokenDescription() { return spokenDescription; }

    public void setSpokenDescription(String spokenDescription) { this.spokenDescription = spokenDescription; }

    public String getScoring() {
        return scoring;
    }

    public void setScoring(String scoring) {
        this.scoring = scoring;
    }

}
