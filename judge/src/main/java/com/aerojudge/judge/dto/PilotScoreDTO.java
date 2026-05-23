package com.aerojudge.judge.dto;

public class PilotScoreDTO {
    private String primary_id;
    private int round;
    private int sequence;
    private String type;
    private float[] scores;

    public int getRound() {
        return round;
    }
    public void setRound(int round) {
        this.round = round;
    }
    public int getSequence() {
        return sequence;
    }
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public float[] getScores() {
        return scores;
    }
    public void setScores(float[] scores) {
        this.scores = scores;
    }
    public String getPrimary_id() {
        return primary_id;
    }
    public void setPrimary_id(String primary_id) {
        this.primary_id = primary_id;
    }

}
