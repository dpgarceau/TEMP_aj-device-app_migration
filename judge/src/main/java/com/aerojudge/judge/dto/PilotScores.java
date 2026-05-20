package com.aerojudge.judge.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PilotScores {
    private String name;


    private String primary_id;

    private String _class;

    private Boolean isActive = true;

    // Per-type round tracking (2026 format)
    private Map<String, Integer> activeRoundByType = new HashMap<>(Map.of(
        "KNOWN", 1,
        "UNKNOWN", 1,
        "FREESTYLE", 1
    ));

    private Map<String, Integer> activeSequenceByType = new HashMap<>(Map.of(
        "KNOWN", 1,
        "UNKNOWN", 1,
        "FREESTYLE", 1
    ));
    private String activeRoundType = "KNOWN";
    private int judge_id;
    private List<PScore> scores = new ArrayList<>();

    public String getActiveRoundType() {
        return activeRoundType;
    }

    public void setActiveRoundType(String activeRoundType) {
        this.activeRoundType = activeRoundType;
    }

    public PilotScores(String name, String primary_id, String _class,int judge_id) {
        this.name = name;
        this.primary_id = primary_id;
        this._class = _class;
        this.judge_id = judge_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimary_id() {
        return primary_id;
    }

    public void setPrimary_id(String primary_id) {
        this.primary_id = primary_id;
    }

    public String get_class() {
        return _class;
    }

    public void set_class(String _class) {
        this._class = _class;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Get active round for a specific type.
     * @param roundType KNOWN, UNKNOWN, or FREESTYLE
     * @return Round number (defaults to 1 if type not found)
     */
    public int getActiveRound(String roundType) {
        if (roundType == null) {
            roundType = "KNOWN";
        }
        // Initialize map if null (handles deserialization of new format)
        if (activeRoundByType == null) {
            activeRoundByType = new HashMap<>(Map.of("KNOWN", 1, "UNKNOWN", 1, "FREESTYLE", 1));
        }
        return activeRoundByType.getOrDefault(roundType.toUpperCase(), 1);
    }

    /**
     * Get active round for current activeRoundType.
     */
    public int getActiveRound() {
        return getActiveRound(activeRoundType);
    }

    /**
     * Set active round for a specific type.
     * @param roundType KNOWN, UNKNOWN, or FREESTYLE
     * @param round Round number
     */
    public void setActiveRound(String roundType, int round) {
        if (roundType == null) {
            roundType = "KNOWN";
        }
        // Initialize map if null (handles deserialization of new format)
        if (activeRoundByType == null) {
            activeRoundByType = new HashMap<>(Map.of("KNOWN", 1, "UNKNOWN", 1, "FREESTYLE", 1));
        }
        activeRoundByType.put(roundType.toUpperCase(), round);
    }

    /**
     * Set active round for current activeRoundType.
     */
    public void setActiveRound(int round) {
        setActiveRound(activeRoundType, round);
    }

    /**
     * Increment active round for a specific type.
     * @param roundType KNOWN, UNKNOWN, or FREESTYLE
     */
    public void incrementActiveRound(String roundType) {
        int current = getActiveRound(roundType);
        setActiveRound(roundType, current + 1);
    }

    /**
     * Decrement active round for a specific type.
     * @param roundType KNOWN, UNKNOWN, or FREESTYLE
     */
    public void decrementActiveRound(String roundType) {
        int current = getActiveRound(roundType);
        setActiveRound(roundType, current - 1);
    }

    /**
     * Get the full activeRoundByType map (for JSON serialization).
     */
    public Map<String, Integer> getActiveRoundByType() {
        return activeRoundByType;
    }

    /**
     * Set the full activeRoundByType map (for JSON deserialization).
     */
    public void setActiveRoundByType(Map<String, Integer> activeRoundByType) {
        if (activeRoundByType != null) {
            this.activeRoundByType = activeRoundByType;
        }
    }

    private void ensureActiveSequenceByType() {
        if (activeSequenceByType == null) {
            activeSequenceByType = new HashMap<>(Map.of("KNOWN", 1, "UNKNOWN", 1, "FREESTYLE", 1));
        }
    }

    public int getActiveSequence(String roundType) {
        if (roundType == null) {
            roundType = "KNOWN";
        }
        ensureActiveSequenceByType();
        return activeSequenceByType.getOrDefault(roundType.toUpperCase(), 1);
    }

    public void setActiveSequence(String roundType, int activeSequence) {
        if (roundType == null) {
            roundType = "KNOWN";
        }
        ensureActiveSequenceByType();
        activeSequenceByType.put(roundType.toUpperCase(), activeSequence);
    }

    public void incrementActiveSequence(String roundType) {
        setActiveSequence(roundType, getActiveSequence(roundType) + 1);
    }

    public Map<String, Integer> getActiveSequenceByType() {
        ensureActiveSequenceByType();
        return activeSequenceByType;
    }

    public void setActiveSequenceByType(Map<String, Integer> activeSequenceByType) {
        if (activeSequenceByType != null) {
            this.activeSequenceByType = activeSequenceByType;
        }
    }

    public List<PScore> getScores() {
        return scores;
    }

    public void setScores(List<PScore> scores) {
        this.scores = scores;
    }

    public int getJudge_id() {
        return judge_id;
    }

    public void setJudge_id(int judge_id) {
        this.judge_id = judge_id;
    }

    

}
