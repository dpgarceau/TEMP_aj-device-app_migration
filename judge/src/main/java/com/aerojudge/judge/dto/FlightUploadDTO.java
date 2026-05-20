package com.aerojudge.judge.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


public class FlightUploadDTO {
    public String pilot_primary_id;
	public String type;
	public int round;
	public int sequence;
	public int judge;
	public boolean missing_pilot_panel;
	public FiguresUploadDTO figures;
    @JacksonXmlProperty(isAttribute = true, localName = "index")
	public int index = 0;
    // Duration in seconds for FREESTYLE flights (default 240 = 4 minutes)
    public Integer duration_seconds;

    public FlightUploadDTO(String pilot_primary_id, String type, int round, int sequence, int judge,
            boolean missing_pilot_panel, FiguresUploadDTO figures, int index, int line_number) {
        this.pilot_primary_id = pilot_primary_id;
        this.type = type;
        if (line_number > 1) {
            if (round < 10) {
                this.round = (line_number * 10)+round;
            }
            else {this.round = (line_number * 100)+round;
            }
        }
        else {
            if (round < 20) {
                this.round = round;
            }
            else {this.round = (line_number * 100)+round;
            }
        }
        this.sequence = sequence;
        this.judge = judge;
        this.missing_pilot_panel = missing_pilot_panel;
        this.figures = figures;
        this.index = index;
        // Set duration_seconds for FREESTYLE flights (default 240 seconds = 4 minutes)
        if ("FREESTYLE".equalsIgnoreCase(type)) {
            this.duration_seconds = 240;
        }
    }
    public String getPilot_primary_id() {
        return pilot_primary_id;
    }
    public void setPilot_primary_id(String pilot_primary_id) {
        this.pilot_primary_id = pilot_primary_id;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
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
    public int getJudge() {
        return judge;
    }
    public void setJudge(int judge) {
        this.judge = judge;
    }
    public boolean isMissing_pilot_panel() {
        return missing_pilot_panel;
    }
    public void setMissing_pilot_panel(boolean missing_pilot_panel) {
        this.missing_pilot_panel = missing_pilot_panel;
    }
    public FiguresUploadDTO getFigures() {
        return figures;
    }
    public void setFigures(FiguresUploadDTO figures) {
        this.figures = figures;
    }
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }

    public Integer getDuration_seconds() {
        return duration_seconds;
    }

    public void setDuration_seconds(Integer duration_seconds) {
        this.duration_seconds = duration_seconds;
    }
}
