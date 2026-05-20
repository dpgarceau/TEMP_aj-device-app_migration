package com.aerojudge.judge.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundsDTO {

    private Integer comp_id;                // Stored in the compDTO but lets keep it here so we know the rounds are from this comp.
    private Integer scoringRoundNum;        // Multiple rounds can be 'Flying' but we can only score a round at a time.
    private List<RoundDTO> rounds = null;   // The schedules for this instance.

    public RoundsDTO() {
        rounds = new ArrayList<>();
    }

    public RoundsDTO(Integer scoringRoundNum) {
        this();
        this.scoringRoundNum = scoringRoundNum;
    }

    public Integer getScoringRoundNum() { return scoringRoundNum; }
    public void setScoringRoundNum(Integer scoringRoundNum) { this.scoringRoundNum = scoringRoundNum; }
    public List<RoundDTO> getRounds() { return rounds; }
    public void setRounds(List<RoundDTO> roundsDTO) { this.rounds = roundsDTO; }
    public Integer getComp_id() { return comp_id; }
    public void setComp_id(Integer comp_id) { this.comp_id = comp_id; }
}
