package com.aerojudge.judge.dto;

public class Pilot {
    private Boolean freestyle;

    private String comments;

    private String addr2;

    private String addr1;

    private Classes classes;

    private int index;

    private Boolean active;

    private int comp_id;

    private int frequency;

    private Boolean spread_spectrum;

    private String secondary_id;

    private String airplane;

    private String name;

    private Boolean missing_pilot_panel;

    private String primary_id;

    public Pilot(Boolean freestyle, String comments, String addr2, String addr1, String _class, int index,
            Boolean active, int comp_id, int frequency, Boolean spread_spectrum, String secondary_id, String airplane,
            String name, Boolean missing_pilot_panel, String primary_id) {
        this.freestyle = freestyle;
        this.comments = comments;
        this.addr2 = addr2;
        this.addr1 = addr1;
        this.classes = new Classes(_class);
        this.index = index;
        this.active = active;
        this.comp_id = comp_id;
        this.frequency = frequency;
        this.spread_spectrum = spread_spectrum;
        this.secondary_id = secondary_id;
        this.airplane = airplane;
        this.name = name;
        this.missing_pilot_panel = missing_pilot_panel;
        this.primary_id = primary_id;
    }

    public Boolean getFreestyle() {
        return freestyle;
    }

    public void setFreestyle(Boolean freestyle) {
        this.freestyle = freestyle;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getAddr2() {
        return addr2;
    }

    public void setAddr2(String addr2) {
        this.addr2 = addr2;
    }

    public String getAddr1() {
        return addr1;
    }

    public void setAddr1(String addr1) {
        this.addr1 = addr1;
    }

    public Classes getClasses() {
        return classes;
    }

    public void setClasses(Classes classes) {
        this.classes = classes;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public int getComp_id() {
        return comp_id;
    }

    public void setComp_id(int comp_id) {
        this.comp_id = comp_id;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public Boolean getSpread_spectrum() {
        return spread_spectrum;
    }

    public void setSpread_spectrum(Boolean spread_spectrum) {
        this.spread_spectrum = spread_spectrum;
    }

    public String getSecondary_id() {
        return secondary_id;
    }

    public void setSecondary_id(String secondary_id) {
        this.secondary_id = secondary_id;
    }

    public String getAirplane() {
        return airplane;
    }

    public void setAirplane(String airplane) {
        this.airplane = airplane;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getMissing_pilot_panel() {
        return missing_pilot_panel;
    }

    public void setMissing_pilot_panel(Boolean missing_pilot_panel) {
        this.missing_pilot_panel = missing_pilot_panel;
    }

    public String getPrimary_id() {
        return primary_id;
    }

    public void setPrimary_id(String primary_id) {
        this.primary_id = primary_id;
    }
public String getClassString(){
    return this.classes.get_Class();
}

}
