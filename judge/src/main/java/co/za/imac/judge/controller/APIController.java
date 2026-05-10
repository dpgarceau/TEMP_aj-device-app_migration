package co.za.imac.judge.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import co.za.imac.judge.dto.*;
import co.za.imac.judge.service.RoundsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import co.za.imac.judge.utils.SettingUtils;

import co.za.imac.judge.service.CompService;
import co.za.imac.judge.service.InfoCollectorService;
import co.za.imac.judge.service.PilotService;
import co.za.imac.judge.service.ScheduleService;
import co.za.imac.judge.service.ScoreResolverService;
import co.za.imac.judge.service.SequenceService;
import co.za.imac.judge.service.SettingService;

@RestController
public class APIController {
    private static final Logger logger = LoggerFactory.getLogger(RootController.class);

    @Autowired
    private CompService compService;
    @Autowired
    private RoundsService roundsService;
    @Autowired
    private PilotService pilotService;
    @Autowired
    private SequenceService sequenceService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private InfoCollectorService infoCollectorService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScoreResolverService scoreResolverService;

    @GetMapping("/api/comp")
    public CompDTO getComp() throws IOException, ParserConfigurationException, SAXException {
        return compService.getComp();
    }

    /**
     * Update local comp settings without contacting Score server.
     * Accepts: sequences (int), sequenceType (String), score_mode (String)
     */
    @PostMapping("/api/comp/local")
    public ResponseEntity<String> updateLocalCompSettings(@RequestBody CompDTO comp)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = new HashMap<>();

        CompDTO currentComp = compService.getComp();
        if (currentComp == null) {
            result.put("result", "fail");
            result.put("message", "No competition loaded. Load an event first.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        if (currentComp.getSequences() != comp.getSequences()) {
            Map<String, Object> blockPayload = scoreResolverService.evaluateFormatChangeBlock();
            if (blockPayload != null) {
                return new ResponseEntity<>(new Gson().toJson(blockPayload), HttpStatus.CONFLICT);
            }
        }

        // Update only the local settings that were provided
        if (comp.getSequences() > 0) {
            currentComp.setSequences(comp.getSequences());
            logger.info("Updated sequences to: {}", comp.getSequences());
        }
        if (comp.getSequenceType() != null) {
            currentComp.setSequenceType(comp.getSequenceType());
            logger.info("Updated sequenceType to: {}", comp.getSequenceType());
        }
        if (comp.getScore_mode() != null) {
            currentComp.setScore_mode(comp.getScore_mode());
            logger.info("Updated score_mode to: {}", comp.getScore_mode());
        }

        // Save locally without Score contact
        if (compService.saveCompToFileLocal()) {
            result.put("result", "ok");
            result.put("message", "Local settings updated.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
        } else {
            result.put("result", "fail");
            result.put("message", "Could not save settings.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/api/comp")
    public ResponseEntity<String> createComp(@RequestBody CompDTO comp,
            @RequestParam(name = "edit", required = true) Boolean editComp)
            throws IOException, ParserConfigurationException, SAXException, URISyntaxException {

        Map<String, Object> result = new HashMap<>();

        if (editComp) {
            CompDTO currentComp = compService.getComp();
            if (currentComp != null && currentComp.getSequences() != comp.getSequences()) {
                Map<String, Object> blockPayload = scoreResolverService.evaluateFormatChangeBlock();
                if (blockPayload != null) {
                    return new ResponseEntity<>(new Gson().toJson(blockPayload), HttpStatus.CONFLICT);
                }
            }
        }

        logger.debug("Comp data received:");
        logger.debug(new Gson().toJson(comp));
       
        // fetch pilots
        pilotService.getPilotsFileFromScore(); // Reloading here means we can add pilots mid comp. But if their id/name
        compService.enrichCompWithCompInfoFromScore(comp); // Add the names and ID.
       
       
        if (!editComp) {
        //archive exising pilots and scores on new comp creation
            settingService.backupAllFiles();
            pilotService.setupPilotScores();
        }

        // fetch seqs
        sequenceService.getSequenceFileFromScore();
        scheduleService.populateSequences();

        CompDTO newComp = compService.createCompFromRequest(comp);
        if (newComp == null) {
            result.put("result", "fail");
            if (editComp)
                result.put("message", "Could not edit comp.");
            else
                result.put("message", "Could not create new comp.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);

        } else {
            result.put("result", "ok");
            if (editComp) {
                result.put("action", "edit");
                result.put("message", "Comp " + newComp.getComp_name() + " is edited.");
            } else {
                result.put("action", "create");
                result.put("message", "New comp " + newComp.getComp_name() + " is created.");
            }

            result.put("comp", new Gson().toJson(newComp));

            //now try to fetch unknown figures if there are UNKNOWN sequences
            try {
                if (sequenceService.hasUnknownSequences()) {
                    sequenceService.getUnknownFiguresZip();
                } else {
                    logger.info("No UNKNOWN sequences found, skipping unknown figures download");
                }
            } catch (Exception e) {
                logger.warn("Failed to download unknown figures: {}", e.getMessage());
                // Continue with comp creation even if unknown figures download fails
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("Location", "/api/comp");

            return ResponseEntity.created(new URI("/api/comp")).body(new Gson().toJson(result));
        }
    }

    @PostMapping("/api/rounds")
    public ResponseEntity<String> createRound(@RequestBody RoundDTO round,
            @RequestParam(name = "edit", required = false, defaultValue = "false") Boolean editRound)
            throws IOException, ParserConfigurationException, SAXException {

        Map<String, Object> result = new HashMap<>();

        logger.debug("Creating new round:");
        logger.debug(new Gson().toJson(round));

        if (editRound) {
            // Do edit...
            result.put("result", "fail");
            result.put("message", "editing of rounds is not yet implemented.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.NOT_IMPLEMENTED);
        } else {
            // Before we can add a new round, we need to get round number / round ID
            // editRound is false so we're just going to overwrite what we were given.

            RoundDTO newRound = roundsService.addRound(round);
            if (newRound != null) {
                roundsService.saveRoundsToFile();
                result.put("result", "ok");
                result.put("message", "New round created.");
                result.put("new_round", new Gson().toJson(newRound));

                // Set it active if we are in byRound mode.
                if ("byRound".equalsIgnoreCase(compService.getComp().getScore_mode())) {
                    roundsService.activateRoundForScoring(newRound.getRound_id());
                }

                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
            } else
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/rounds/phase")
    public ResponseEntity<String> adjustRound(@RequestBody Map<String, Object> payload)
            throws IOException, ParserConfigurationException, SAXException {
        logger.debug("Performing action " + payload.get("action") + " on round " + payload.get("round_id") + ".");
        Map<String, Object> result = new HashMap<>();
        result.put("action", payload.get("action"));
        result.put("message", "");

        Map<String, Object> res;
        switch ((String) payload.get("action")) {
            case "fly":
                res = roundsService.activateRoundForScoring(Integer.parseInt((String) payload.get("round_id")));
                if ((Boolean) res.get("success")) {
                    result.put("result", "ok");
                } else {
                    result.put("result", "fail");
                    result.put("message", res.get("message"));
                    return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
                }
                break;
            case "close":
                res = roundsService.closeRound(Integer.parseInt((String) payload.get("round_id")));
                if ((Boolean) res.get("success")) {
                    result.put("result", "ok");
                } else {
                    result.put("result", "fail");
                    result.put("message", res.get("message"));
                    return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                result.put("result", "fail");
                result.put("message", "Unknown action.");
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
    }

    @GetMapping("/api/pilots/sync")
    public String syncPilots() throws IOException, ParserConfigurationException, SAXException {
        // fetch pilots
        pilotService.getPilotsFileFromScore();
        sequenceService.getSequenceFileFromScore();
        // Force reload ScheduleService cache with new sequence data
        scheduleService.populateSequences();
        // pilotService.setupPilotScores();
        Map<String, Object> result = new HashMap<>();
        result.put("sync", "ok");
        return new Gson().toJson(result);
    }

    @GetMapping("/api/scores/sync")
    public String syncScores() throws Exception {
        // fetch pilots
        pilotService.syncPilotsToScoreWebService();
        Map<String, Object> result = new HashMap<>();
        result.put("sync", "ok");
        return new Gson().toJson(result);
    }

    @PostMapping("/api/score")
    public PilotScores submitScores(@RequestBody PilotScoreDTO pilotScoreDTO)
            throws ParserConfigurationException, SAXException, IOException {
        System.out.println(new Gson().toJson(pilotScoreDTO));
        return pilotService.submitScore(pilotScoreDTO);
    }

    @GetMapping("/api/settings")
    public SettingDTO getSettings() throws IOException, ParserConfigurationException, SAXException {
        return settingService.getSettings();
    }
    
    @PostMapping("/api/settings")
    public ResponseEntity<String> updateSettings(@RequestBody SettingDTO setting)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = new HashMap<>();
        try {
            settingService.updateSettings(setting);
            result.put("result", "ok");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
        } catch (ConnectException e) {
            logger.error("Could not update settings. " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not update settings.");
        }
    }

    @GetMapping("/api/version")
    public Map<String, String> getVersion() {
        Map<String, String> result = new HashMap<>();
        result.put("appVersion", co.za.imac.judge.JudgeApplication.getAppVersion());
        return result;
    }

    @GetMapping("/api/getinfo")
    public InfoJson getLatestInfo() {
        InfoJson info = infoCollectorService.collectInfo();
        return info;
    }

    /**
     * Lightweight battery percentage endpoint.
     * Much faster than /api/getinfo - only reads I2C sensor.
     */
    @GetMapping("/api/battery")
    public Map<String, Integer> getBatteryPercent() {
        Map<String, Integer> result = new HashMap<>();
        result.put("percent", infoCollectorService.getBatteryPercent());
        return result;
    }

    @PostMapping("/api/pilot/{pilotId}/advance-round")
    public ResponseEntity<PilotScores> advanceRound(
            @PathVariable String pilotId,
            @RequestParam(name = "type", required = true) String roundType)
            throws IOException, ParserConfigurationException, SAXException {

        logger.info("Advancing round for pilot {} type {}", pilotId, roundType);

        // Get pilot
        Pilot pilot = pilotService.getPilot(pilotId);
        if (pilot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pilot not found");
        }

        // Get pilot scores
        PilotScores pilotScores = pilotService.getPilotScores(pilot);

        // Increment the round for this type and reset sequence to 1
        pilotScores.incrementActiveRound(roundType);
        pilotScores.setActiveSequence(roundType, 1);

        // Save the updated pilot scores
        pilotService.savePilotScoresToFile(pilotScores);

        logger.info("Advanced pilot {} to round {} sequence 1 for type {}",
                    pilotId, pilotScores.getActiveRound(roundType), roundType);

        return ResponseEntity.ok(pilotScores);
    }

    @GetMapping("/api/scores/mismatches")
    public ResponseEntity<String> getScoreMismatches() throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = scoreResolverService.getMismatches();
        return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
    }

    /**
     * Compare semantic versions in format #.# or #.#.#
     * @param version1 First version to compare
     * @param version2 Second version to compare
     * @return true if version1 > version2, false otherwise
     */
    private boolean isVersionGreater(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return false;
        }

        // Remove 'v' prefix if present
        version1 = version1.replaceFirst("^v", "");
        version2 = version2.replaceFirst("^v", "");

        // Split versions into parts
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        // Compare each part numerically
        for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            int v1Num = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Num = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Num > v2Num) {
                return true;
            } else if (v1Num < v2Num) {
                return false;
            }
        }

        // Versions are equal
        return false;
    }

    @PostMapping("/api/scores/move-round")
    public ResponseEntity<String> moveRound(@RequestBody Map<String, Object> payload)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = new HashMap<>();

        String sourcePilotId = (String) payload.get("sourcePilotId");
        String destPilotId = (String) payload.get("destPilotId");
        String roundType = (String) payload.get("roundType");
        int sourceRound = ((Number) payload.get("sourceRound")).intValue();

        logger.info("Moving {} round {} from pilot {} to pilot {}",
                roundType, sourceRound, sourcePilotId, destPilotId);

        // Get pilots and their scores
        Pilot sourcePilot = pilotService.getPilot(sourcePilotId);
        Pilot destPilot = pilotService.getPilot(destPilotId);

        if (sourcePilot == null || destPilot == null) {
            result.put("result", "fail");
            result.put("message", "Pilot not found");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        PilotScores sourceScores = pilotService.getPilotScores(sourcePilot);
        PilotScores destScores = pilotService.getPilotScores(destPilot);

        int sourceCount = scoreResolverService.countRoundsForType(sourceScores, roundType);
        int destCount = scoreResolverService.countRoundsForType(destScores, roundType);
        if (sourceCount <= destCount) {
            result.put("result", "fail");
            result.put("message", sourcePilot.getName() + " has no extra round to give to " + destPilot.getName() + ".");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        List<Pilot> classPeers = pilotService.getPilots(true).stream()
                .filter(p -> sourcePilot.getClassString().equalsIgnoreCase(p.getClassString()))
                .toList();
        Integer sourceMissing = scoreResolverService.findUnresolvedMissingSeq2Round(sourcePilot, classPeers);
        if (sourceMissing != null) {
            result.put("result", "fail");
            result.put("message", sourcePilot.getName() + " has an unresolved missing sequence 2 of round "
                    + sourceMissing + " — Fix Missing Sequence on " + sourcePilot.getName() + " first.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }
        Integer destMissing = scoreResolverService.findUnresolvedMissingSeq2Round(destPilot, classPeers);
        if (destMissing != null) {
            result.put("result", "fail");
            result.put("message", destPilot.getName() + " has an unresolved missing sequence 2 of round "
                    + destMissing + " — Fix Missing Sequence on " + destPilot.getName() + " first.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        // Find the scores to move (all sequences for that round+type)
        List<PScore> scoresToMove = new ArrayList<>();
        List<PScore> remainingSourceScores = new ArrayList<>();

        for (PScore score : sourceScores.getScores()) {
            if (roundType.equalsIgnoreCase(score.getType()) && score.getRound() == sourceRound) {
                scoresToMove.add(score);
            } else {
                remainingSourceScores.add(score);
            }
        }

        if (scoresToMove.isEmpty()) {
            result.put("result", "fail");
            result.put("message", "No scores found for that round");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        // Determine destination round number
        int destRound = scoreResolverService.countRoundsForType(destScores, roundType) + 1;

        // Add audit comment timestamp
        String auditComment = String.format("[%s] Round moved from pilot %s",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                sourcePilot.getName());

        // Move scores to destination with new round number
        List<PScore> destScoreList = new ArrayList<>(destScores.getScores());
        for (PScore score : scoresToMove) {
            PScore movedScore = new PScore(destRound, score.getSequence(), score.getScores(), score.getType());
            // Note: audit comment would need to be added to PScore if we want per-score comments
            destScoreList.add(movedScore);
        }
        destScores.setScores(destScoreList);

        // Renumber source pilot's remaining rounds (decrement rounds higher than moved one)
        for (PScore score : remainingSourceScores) {
            if (roundType.equalsIgnoreCase(score.getType()) && score.getRound() > sourceRound) {
                // Create new score with decremented round number
                PScore renumbered = new PScore(score.getRound() - 1, score.getSequence(), score.getScores(), score.getType());
                remainingSourceScores.set(remainingSourceScores.indexOf(score), renumbered);
            }
        }
        sourceScores.setScores(remainingSourceScores);

        // Source lost a round in the move
        sourceScores.decrementActiveRound(roundType);
        // Dest gained a round in the move
        destScores.incrementActiveRound(roundType);

        // Save both pilots
        pilotService.savePilotScoresToFile(sourceScores);
        pilotService.savePilotScoresToFile(destScores);

        logger.info("Successfully moved round. Source now has {} {} rounds, dest has {} {} rounds",
                scoreResolverService.countRoundsForType(sourceScores, roundType), roundType,
                scoreResolverService.countRoundsForType(destScores, roundType), roundType);

        result.put("result", "ok");
        result.put("message", "Round moved successfully");
        result.put("audit", auditComment);
        return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
    }

    @PostMapping("/api/scores/fix-missing-sequence")
    public ResponseEntity<String> fixMissingSequence(@RequestBody Map<String, Object> payload)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = new HashMap<>();

        String pilotId = (String) payload.get("pilotId");
        String roundType = (String) payload.get("roundType");
        int round = ((Number) payload.get("round")).intValue();

        if (!"KNOWN".equalsIgnoreCase(roundType)) {
            result.put("result", "fail");
            result.put("message", "Fix Missing Sequence is only valid for KNOWN rounds.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        Pilot pilot = pilotService.getPilot(pilotId);
        if (pilot == null) {
            result.put("result", "fail");
            result.put("message", "Pilot not found.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        PilotScores scores = pilotService.getPilotScores(pilot);

        // Pilot must have seq 1 of (KNOWN, round) but not seq 2
        boolean hasSeq1 = false;
        boolean hasSeq2 = false;
        for (PScore s : scores.getScores()) {
            if ("KNOWN".equalsIgnoreCase(s.getType()) && s.getRound() == round) {
                if (s.getSequence() == 1) hasSeq1 = true;
                else if (s.getSequence() == 2) hasSeq2 = true;
            }
        }
        if (!hasSeq1 || hasSeq2) {
            result.put("result", "fail");
            result.put("message", pilot.getName() + " has no missing seq 2 for KNOWN round " + round + ".");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        // Peer evidence: another pilot in the same class must have scored seq 2 of this round.
        // Borrow that peer's figure count so the new PScore matches what the round was scored against.
        int figureCount = -1;
        for (Pilot peer : pilotService.getPilots(true)) {
            if (peer.getPrimary_id().equals(pilotId)) continue;
            if (!pilot.getClassString().equalsIgnoreCase(peer.getClassString())) continue;
            PilotScores peerScores = pilotService.getPilotScores(peer);
            if (peerScores == null || peerScores.getScores() == null) continue;
            for (PScore ps : peerScores.getScores()) {
                if ("KNOWN".equalsIgnoreCase(ps.getType())
                        && ps.getRound() == round
                        && ps.getSequence() == 2
                        && ps.getScores() != null) {
                    figureCount = ps.getScores().length;
                    break;
                }
            }
            if (figureCount != -1) break;
        }
        if (figureCount == -1) {
            result.put("result", "fail");
            result.put("message", "No peer pilot in this class has scored seq 2 of round " + round + " — nothing to borrow figure count from.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        int beforeCount = scoreResolverService.countRoundsForType(scores, roundType);
        int beforeActiveRound = scores.getActiveRound(roundType);
        int beforeActiveSeq = scores.getActiveSequence(roundType);

        float[] zeros = new float[figureCount];
        scores.getScores().add(new PScore(round, 2, zeros, "KNOWN"));

        // Round complete — advance state
        scores.incrementActiveRound(roundType);
        scores.setActiveSequence(roundType, 1);

        pilotService.savePilotScoresToFile(scores);

        logger.info("Fix Missing Sequence: pilot={} ({}), class={}, round={}, roundCount {} -> {}, activeRound {} -> {}, activeSequence {} -> {}",
                pilot.getPrimary_id(), pilot.getName(), pilot.getClassString(), round,
                beforeCount, scoreResolverService.countRoundsForType(scores, roundType),
                beforeActiveRound, scores.getActiveRound(roundType),
                beforeActiveSeq, scores.getActiveSequence(roundType));

        result.put("result", "ok");
        result.put("message", "Sequence marked as not flown.");
        return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
    }

    @PostMapping("/api/scores/zero-fill")
    public ResponseEntity<String> zeroFill(@RequestBody Map<String, Object> payload)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = new HashMap<>();

        String pilotId = (String) payload.get("pilotId");
        String roundType = (String) payload.get("roundType");
        int round = ((Number) payload.get("round")).intValue();

        Pilot pilot = pilotService.getPilot(pilotId);
        if (pilot == null) {
            result.put("result", "fail");
            result.put("message", "Pilot not found.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        boolean isFreestyle = "FREESTYLE".equalsIgnoreCase(roundType);
        boolean isKnown = "KNOWN".equalsIgnoreCase(roundType);
        List<Pilot> peers;
        if (isFreestyle) {
            peers = pilotService.getPilots(true).stream()
                    .filter(p -> Boolean.TRUE.equals(p.getFreestyle()))
                    .toList();
        } else {
            peers = pilotService.getPilots(true).stream()
                    .filter(p -> pilot.getClassString().equalsIgnoreCase(p.getClassString()))
                    .toList();
        }

        PilotScores scores = pilotService.getPilotScores(pilot);

        if (isKnown) {
            Integer blockingRound = scoreResolverService.findUnresolvedMissingSeq2Round(pilot, peers);
            if (blockingRound != null) {
                result.put("result", "fail");
                result.put("message", pilot.getName() + " has an unresolved missing seq 2 of KNOWN round "
                        + blockingRound + " — Fix Missing Sequence first.");
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
            }
        }

        int targetCount = scoreResolverService.countRoundsForType(scores, roundType);

        int maxPeerCount = -1;
        for (Pilot peer : peers) {
            if (peer.getPrimary_id().equals(pilotId)) continue;
            PilotScores peerScores = pilotService.getPilotScores(peer);
            int pc = scoreResolverService.countRoundsForType(peerScores, roundType);
            if (pc > maxPeerCount) maxPeerCount = pc;
        }

        if (maxPeerCount == -1) {
            result.put("result", "fail");
            result.put("message", "No peers in this group — nothing to catch up to.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }
        if (targetCount >= maxPeerCount) {
            result.put("result", "fail");
            result.put("message", pilot.getName() + " is already caught up.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }
        if (round != targetCount + 1) {
            result.put("result", "fail");
            result.put("message", pilot.getName() + " must zero round " + (targetCount + 1)
                    + " before round " + round + " (zero-fill is sequential).");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        int seq1FigureCount = -1;
        int seq2FigureCount = -1;
        for (Pilot peer : peers) {
            if (peer.getPrimary_id().equals(pilotId)) continue;
            PilotScores peerScores = pilotService.getPilotScores(peer);
            if (peerScores == null || peerScores.getScores() == null) continue;
            for (PScore ps : peerScores.getScores()) {
                if (!roundType.equalsIgnoreCase(ps.getType())) continue;
                if (ps.getRound() != round) continue;
                if (ps.getScores() == null) continue;
                if (ps.getSequence() == 1 && seq1FigureCount == -1) seq1FigureCount = ps.getScores().length;
                else if (ps.getSequence() == 2 && seq2FigureCount == -1) seq2FigureCount = ps.getScores().length;
            }
        }
        if (!isKnown) seq2FigureCount = -1;
        boolean filledSeq2 = seq2FigureCount != -1;

        int beforeCount = targetCount;
        int beforeActiveRound = scores.getActiveRound(roundType);
        int beforeActiveSeq = scores.getActiveSequence(roundType);

        String typeUpper = roundType.toUpperCase();
        scores.getScores().add(new PScore(round, 1, new float[seq1FigureCount], typeUpper));
        if (filledSeq2) {
            scores.getScores().add(new PScore(round, 2, new float[seq2FigureCount], typeUpper));
        }

        int compSequences = compService.getComp().getSequences();
        if (!isKnown || compSequences == 1 || filledSeq2) {
            scores.incrementActiveRound(roundType);
            scores.setActiveSequence(roundType, 1);
        } else {
            scores.setActiveSequence(roundType, 2);
        }

        pilotService.savePilotScoresToFile(scores);

        String sequencesFilled = filledSeq2 ? "1+2" : "1";
        logger.info("Zero-fill: pilot={} ({}), class={}, type={}, round={}, sequences filled={}, roundCount {} -> {}, activeRound {} -> {}, activeSequence {} -> {}",
                pilot.getPrimary_id(), pilot.getName(), pilot.getClassString(), roundType, round,
                sequencesFilled,
                beforeCount, scoreResolverService.countRoundsForType(scores, roundType),
                beforeActiveRound, scores.getActiveRound(roundType),
                beforeActiveSeq, scores.getActiveSequence(roundType));

        result.put("result", "ok");
        result.put("message", "Round zeroed.");
        return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
    }

    @PostMapping("/api/scores/swap-round")
    public ResponseEntity<String> swapRound(@RequestBody Map<String, Object> payload)
            throws IOException, ParserConfigurationException, SAXException {
        Map<String, Object> result = new HashMap<>();

        String pilotIdA = (String) payload.get("pilotIdA");
        String pilotIdB = (String) payload.get("pilotIdB");
        String roundType = (String) payload.get("roundType");
        int round = ((Number) payload.get("round")).intValue();

        if (Objects.equals(pilotIdA, pilotIdB)) {
            result.put("result", "fail");
            result.put("message", "Two distinct pilots are required.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        Pilot pilotA = pilotService.getPilot(pilotIdA);
        Pilot pilotB = pilotService.getPilot(pilotIdB);
        if (pilotA == null || pilotB == null) {
            result.put("result", "fail");
            result.put("message", "Pilot not found.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }
        if (!pilotA.getClassString().equalsIgnoreCase(pilotB.getClassString())) {
            result.put("result", "fail");
            result.put("message", pilotA.getName() + " and " + pilotB.getName()
                    + " are not in the same class.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        PilotScores aScores = pilotService.getPilotScores(pilotA);
        PilotScores bScores = pilotService.getPilotScores(pilotB);

        List<PScore> aMatching = new ArrayList<>();
        List<PScore> aOther = new ArrayList<>();
        for (PScore s : aScores.getScores()) {
            if (roundType.equalsIgnoreCase(s.getType()) && s.getRound() == round) aMatching.add(s);
            else aOther.add(s);
        }
        List<PScore> bMatching = new ArrayList<>();
        List<PScore> bOther = new ArrayList<>();
        for (PScore s : bScores.getScores()) {
            if (roundType.equalsIgnoreCase(s.getType()) && s.getRound() == round) bMatching.add(s);
            else bOther.add(s);
        }

        Set<Integer> aSeqs = new HashSet<>();
        for (PScore s : aMatching) aSeqs.add(s.getSequence());
        Set<Integer> bSeqs = new HashSet<>();
        for (PScore s : bMatching) bSeqs.add(s.getSequence());
        if (!aSeqs.equals(bSeqs)) {
            result.put("result", "fail");
            result.put("message", pilotA.getName() + " and " + pilotB.getName()
                    + " have different sequence sets for " + roundType + " round " + round
                    + " — fix incomplete round first.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.BAD_REQUEST);
        }

        List<PScore> aNew = new ArrayList<>(aOther);
        aNew.addAll(bMatching);
        List<PScore> bNew = new ArrayList<>(bOther);
        bNew.addAll(aMatching);
        aScores.setScores(aNew);
        bScores.setScores(bNew);

        pilotService.savePilotScoresToFile(aScores);
        pilotService.savePilotScoresToFile(bScores);

        logger.info("Swap Round: pilotA={} ({}), pilotB={} ({}), class={}, type={}, round={}, sequences={}",
                pilotA.getPrimary_id(), pilotA.getName(),
                pilotB.getPrimary_id(), pilotB.getName(),
                pilotA.getClassString(), roundType, round, aSeqs);

        result.put("result", "ok");
        result.put("message", "Round scores swapped.");
        return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
    }

    /**
     * Check for available updates by querying GitHub releases API.
     * Compares current version with latest release tag.
     */
    @GetMapping("/api/system/check-update")
    public ResponseEntity<String> checkForUpdate() {
        Map<String, Object> result = new HashMap<>();

        String currentVersion = co.za.imac.judge.JudgeApplication.getAppVersion();
        result.put("currentVersion", currentVersion);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/IMAC-ORG/imac-judge-app/releases/latest"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String latestTag = json.get("tag_name").getAsString();
                // Remove 'v' prefix if present for comparison
                String latestVersion = latestTag.replaceFirst("^v", "");
                boolean updateAvailable = isVersionGreater(latestVersion, currentVersion);

                result.put("latestVersion", latestVersion);
                result.put("latestTag", latestTag);
                result.put("updateAvailable", updateAvailable);
                result.put("result", "ok");

                logger.info("Version check: current={}, latest={}, updateAvailable={}",
                        currentVersion, latestVersion, updateAvailable);

                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
            } else {
                result.put("result", "fail");
                result.put("message", "GitHub API returned status: " + response.statusCode());
                logger.warn("GitHub API check failed with status: {}", response.statusCode());
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.SERVICE_UNAVAILABLE);
            }

        } catch (Exception e) {
            logger.error("Failed to check for updates: {}", e.getMessage());
            result.put("result", "fail");
            result.put("message", "Could not connect to update server. Check internet connection.");
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Run system update using a two-phase approach:
     *
     * 1. Download phase: runs synchronously so we can report the outcome.
     *    The script checks for a newer version and downloads assets to
     *    /tmp/judge-update/. Exit codes: 0 = no update, 1 = error, 2 = ready.
     *
     * 2. Install phase: launched via systemd-run in its own scope so it
     *    survives the judge.service restart. Backs up the current JAR,
     *    installs the update, health-checks, and rolls back on failure.
     */
    @PostMapping("/api/system/update")
    public ResponseEntity<String> runSystemUpdate() {
        Map<String, Object> result = new HashMap<>();

        logger.info("System update requested");

        try {
            // Phase 1: Download (synchronous — we need the exit code)
            int exitCode = runDownloadPhase();

            if (exitCode == 0) {
                // No update was needed - already running latest
                result.put("result", "ok");
                result.put("message", "Already up to date");
                result.put("restart", false);
                logger.info("System already up to date");
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
            } else if (exitCode == 2) {
                // Assets downloaded — notify UI first, then launch install phase
                result.put("result", "ok");
                result.put("message", "Update applied successfully - restarting...");
                result.put("restart", true);
                logger.info("Install phase launched in independent systemd scope");
                launchInstallPhase();
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.OK);
            } else {
                // Error occurred (exit code 1 or other)
                result.put("result", "fail");
                result.put("message", "Update script returned error code: " + exitCode);
                logger.error("Download phase failed with exit code: {}", exitCode);
                return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            logger.error("System update failed: {}", e.getMessage());
            result.put("result", "fail");
            result.put("message", "Update failed: " + e.getMessage());
            return new ResponseEntity<>(new Gson().toJson(result), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Download phase: check for a newer version and download assets to
     * /tmp/judge-update/. Does NOT stop services or modify installed files.
     *
     * Exit codes (from fetch_update.sh --download-only):
     *   0 = No update needed (already running latest version)
     *   1 = Error occurred during download
     *   2 = Assets downloaded to /tmp/judge-update/ and ready to install
     */
    private int runDownloadPhase() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/home/judge/fetch_update.sh", "--download-only");
        pb.directory(new File("/home/judge"));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output for logging
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Update [download]: {}", line);
            }
        }

        return process.waitFor();
    }

    /**
     * Install phase: launched via systemd-run so it runs in its own systemd
     * scope, independent of judge.service. When the script calls
     * "systemctl stop judge.service", only the Spring Boot process stops —
     * the install script continues running under its own scope.
     *
     * The script will:
     *   1. Backup current JAR to judge.jar.bak
     *   2. Stop judge.service and kiosk.service
     *   3. Install downloaded assets from /tmp/judge-update/
     *   4. Start services
     *   5. Health check via /actuator/health
     *   6. If healthy: update .judge_last_release, clean up, done
     *   7. If NOT healthy: restore backup, restart services
     *
     * Output is logged to /var/opt/judge/judge-update.log for post-mortem debugging.
     */
    private void launchInstallPhase() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            "sudo", "systemd-run",
            "--uid=judge",
            "--scope",
            "/home/judge/fetch_update.sh", "--install"
        );
        pb.directory(new File("/home/judge"));
        pb.redirectErrorStream(true);
        pb.redirectOutput(new File("/var/opt/judge/judge-update.log"));
        pb.start();
    }
}
