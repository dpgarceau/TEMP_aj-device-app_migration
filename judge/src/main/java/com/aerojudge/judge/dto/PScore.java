package com.aerojudge.judge.dto;

public class PScore {
    private int round;
    private int sequence;
    private float[] scores;
    private String type;
    
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public PScore(int round, int sequence, float[] scores, String type) {
        this.round = round;
        this.sequence = sequence;
        this.scores = scores;
        this.type = type;
    }
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
    public float[] getScores() {
        return scores;
    }
    public void setScores(float[] scores) {
        this.scores = scores;
    }
    
}
