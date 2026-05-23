package com.aerojudge.judge.service;

import com.aerojudge.judge.dto.FigureDTO;
import com.aerojudge.judge.dto.ScheduleDTO;
import com.aerojudge.judge.dto.SettingDTO;
import com.aerojudge.judge.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScheduleService {

    /***********
     * This is the schedule service.   It provides an interface to the schedule data.
     * Schedules are sometimes called Sequences, but rounds have sequences as well,
     * so it's better for the flown schedule to have it's own name.
     *
     */

    private static final Logger logger =
            LoggerFactory.getLogger(ScheduleService.class);

    private Map<Integer, ScheduleDTO> schedules = null;  // The schedules for this instance.

    // Score still calls them sequences...  :-)
    private static final String SEQUENCES_DAT_PATH = SettingUtils.getApplicationConfigPath() + "/sequences.dat";
    private String SEQUENCES_DAT_URL = "http://SCORE_HOST:SCORE_HTTP_PORT/scorepad/sequences.dat";

    @Autowired
    private SettingService settingService;

    public ScheduleService() {
    }

    public Map<Integer, ScheduleDTO> getSchedules() {
        if (schedules == null) {
            this.populateSequences();
        }
        return schedules;
    }

    public ScheduleService setSchedules(Map<Integer, ScheduleDTO> schedules) {
        this.schedules = schedules;
        return this;
    }

    /**
     * Returns a Set of class names that have UNKNOWN sequences defined in sequences.dat.
     */
    public Set<String> getClassesWithUnknown() {
        Set<String> classesWithUnknown = new HashSet<>();
        Map<Integer, ScheduleDTO> scheds = getSchedules();
        if (scheds == null) {
            return classesWithUnknown;
        }
        for (ScheduleDTO sched : scheds.values()) {
            if ("UNKNOWN".equalsIgnoreCase(sched.getType()) && sched.getComp_class() != null) {
                classesWithUnknown.add(sched.getComp_class().toUpperCase());
            }
        }
        return classesWithUnknown;
    }

    /**
     * Returns true if FREESTYLE sequences are defined in sequences.dat.
     */
    public boolean hasFreestyle() {
        Map<Integer, ScheduleDTO> scheds = getSchedules();
        if (scheds == null) {
            return false;
        }
        for (ScheduleDTO sched : scheds.values()) {
            if ("FREESTYLE".equalsIgnoreCase(sched.getType())) {
                return true;
            }
        }
        return false;
    }

    public void populateSequences() {
        try {
            this.loadSequenceFileIntoSchedules();
        } catch( Exception e ) {
            try {
                logger.error("There was an error loading the schedules data from the sequence.dat file..");
                e.printStackTrace();
            } catch (Exception logger_e) {
                logger_e.printStackTrace();
            }
        }
    }

    public void getSequenceFileFromScore() throws MalformedURLException, IOException {
        logger.info("Loading sequences from Score.");
        SettingDTO settingDTO = settingService.getSettings();
        int timeout = (int)Duration.ofSeconds(settingDTO.getScore_timeout()).toMillis();

        SEQUENCES_DAT_URL = SEQUENCES_DAT_URL.replace("SCORE_HOST", settingDTO.getScore_host()).replace("SCORE_HTTP_PORT", String.valueOf(settingDTO.getScore_http_port()));
        // Updated deprecated URL, changed new URL(x) to URI.create(x).toURL() 2025-11 DPG
        FileUtils.copyURLToFile(URI.create(SEQUENCES_DAT_URL).toURL(), new File(SEQUENCES_DAT_PATH),timeout,timeout);
        // Clear cache only after successful download - forces reload with new data on next access
        this.schedules = null;
        logger.info("Sequence cache cleared - will reload on next access.");
    }

    /************
     *
     *  Not sure we need this now..
     *
    public boolean addFigureToSchedule(Integer schedId, Integer figNum, FigureDTO fig) {

        if (fig == null || schedId == null || fig == null) {
            return false;
        }

        if (this.schedules == null) {
            this.schedules = new HashMap<>();
        }

        if(schedules.get(schedId) == null) {
            // No such schedule
            logger.info("Schedule not found: " + schedId);
            return false;
        }

        schedules.get(schedId).getFigures().put(figNum, fig);
        return true;
    }
    /*******/

    public FigureDTO getScheduleFigureByFigNum(Integer schedId, Integer figNum) {
        try {
            return schedules.get(schedId).getFigures().get(figNum);
        } catch (NullPointerException ne) {
            try {
                logger.error("Could not get figure " + figNum + " for schedule " + schedId);
                ne.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isSequence(){
        File targetFile = new File(SEQUENCES_DAT_PATH);
        return targetFile.exists();
    }

    public boolean loadSequenceFileIntoSchedules()
            throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {

        logger.info ("(Re)Loading sequences.");
        this.schedules = new HashMap<>();
        if(!isSequence()){
            getSequenceFileFromScore();
        }

        // Get Document Builder
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // parse XML file
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse( new File(SEQUENCES_DAT_PATH));
        doc.getDocumentElement().normalize();
        logger.info("Sequence file root Element: " + doc.getDocumentElement().getNodeName());

        NodeList sequenceList = doc.getElementsByTagName("sequence");
        //NodeList newList = (NodeList) list.item(0);
        logger.debug("Sequences length: " + sequenceList.getLength());
        //Map<String,List<FigureDTO>> figuresMap = new HashMap<>();
        for (int temp = 0; temp < sequenceList.getLength(); temp++) {
            Node node = sequenceList.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String index = element.getAttribute("index");
                String type = element.getElementsByTagName("type").item(0).getTextContent();
                String _class =  "";
                try {
                    _class = element.getElementsByTagName("class").item(0).getTextContent();
                } catch (Exception e) {
                    _class = "FREESTYLE";
                }
                logger.debug ("Sequence number: " + index + " Class: " + _class + " Type: " + type);
                ScheduleDTO newSched = new ScheduleDTO();
                newSched.setComp_class(_class);
                newSched.setType(type);
                newSched.setSequence_id(element.getElementsByTagName("sequence_id").item(0).getTextContent());
                newSched.setMax_round(Integer.parseInt(element.getElementsByTagName("max_round").item(0).getTextContent()));
                newSched.setMin_round(Integer.parseInt(element.getElementsByTagName("min_round").item(0).getTextContent()));
                newSched.setDescription(element.getElementsByTagName("description").item(0).getTextContent());
                newSched.setShort_desc(element.getElementsByTagName("short_desc").item(0).getTextContent());
                newSched.setLang(element.getElementsByTagName("lang").item(0).getTextContent());
                Map<Integer, FigureDTO>figures = new HashMap<>();
                Element figuresList = (Element) element.getElementsByTagName("figures").item(0);
                logger.debug("Figure count for seq: " + figuresList.getElementsByTagName("figure").getLength());
                for (int temp2 = 0; temp2 < figuresList.getElementsByTagName("figure").getLength(); temp2++) {
                    Node node2 = figuresList.getElementsByTagName("figure").item(temp2);
                    if (node2.getNodeType() == Node.ELEMENT_NODE) {
                        Element figureElement = (Element) node2;
                        String description = figureElement.getElementsByTagName("description").item(0).getTextContent();
                        String spokenDescription = figureElement.getElementsByTagName("spoken_desc").item(0).getTextContent();
                        int k_factor = Integer
                                .parseInt(figureElement.getElementsByTagName("k_factor").item(0).getTextContent());
                        String scoring = figureElement.getElementsByTagName("scoring").item(0).getTextContent();
                        figures.put((temp2+1), new FigureDTO((temp2+1), k_factor, description, scoring, spokenDescription));
                    }
                }
                newSched.setFigures(figures);
                logger.debug("Adding Schedule " + temp + " to the list.");
                schedules.put(temp, newSched);
            }
        }
        return true;
    }


    /**
     * Get schedule matching class, type, and round number.
     * Returns the most specific match (highest min_round that satisfies condition).
     * Falls back to highest max_round if no exact match.
     * 
     * @param pilotClass e.g., "SPORTSMAN"
     * @param roundType e.g., "KNOWN", "UNKNOWN", "FREESTYLE"
     * @param roundNumber the current round number
     * @return matching ScheduleDTO or null if none found
     */
    public ScheduleDTO getScheduleForRound(String pilotClass, String roundType, int roundNumber) {
        logger.info("getScheduleForRound called: class={}, type={}, round={}", pilotClass, roundType, roundNumber);

        if (schedules == null) {
            this.populateSequences();
        }
        if (schedules == null || schedules.isEmpty()) {
            logger.warn("No schedules loaded!");
            return null;
        }

        // Log all loaded schedules for debugging
        logger.info("=== All loaded schedules ({} total) ===", schedules.size());
        for (Map.Entry<Integer, ScheduleDTO> entry : schedules.entrySet()) {
            ScheduleDTO s = entry.getValue();
            logger.info("  Schedule[{}]: class={}, type={}, min={}, max={}, short_desc={}",
                    entry.getKey(), s.getComp_class(), s.getType(),
                    s.getMin_round(), s.getMax_round(), s.getShort_desc());
        }

        ScheduleDTO bestMatch = null;
        int bestMinRound = -1;

        // First pass: Find exact match (round in range)
        for (ScheduleDTO sched : schedules.values()) {
            boolean classMatches = "FREESTYLE".equalsIgnoreCase(roundType)
                || (sched.getComp_class() != null && sched.getComp_class().equalsIgnoreCase(pilotClass));
            boolean typeMatches = sched.getType() != null && sched.getType().equalsIgnoreCase(roundType);

            logger.debug("Checking schedule: class={}, type={}, min={}, max={} -> classMatches={}, typeMatches={}",
                    sched.getComp_class(), sched.getType(), sched.getMin_round(), sched.getMax_round(),
                    classMatches, typeMatches);

            if (classMatches && typeMatches &&
                sched.getMin_round() != null && sched.getMax_round() != null &&
                sched.getMin_round() <= roundNumber &&
                sched.getMax_round() >= roundNumber) {

                logger.info("Found matching schedule: min={}, max={}, short_desc={}, currentBestMin={}",
                        sched.getMin_round(), sched.getMax_round(), sched.getShort_desc(), bestMinRound);

                // Prefer schedule with highest min_round (most specific)
                if (sched.getMin_round() > bestMinRound) {
                    bestMinRound = sched.getMin_round();
                    bestMatch = sched;
                    logger.info("New best match: min_round={}, short_desc={}", bestMinRound, sched.getShort_desc());
                }
            }
        }

        // Second pass: No exact match - find nearest (highest max_round)
        if (bestMatch == null) {
            logger.warn("No exact match found, trying fallback...");
            int highestMaxRound = -1;
            for (ScheduleDTO sched : schedules.values()) {
                boolean classMatches = "FREESTYLE".equalsIgnoreCase(roundType)
                    || (sched.getComp_class() != null && sched.getComp_class().equalsIgnoreCase(pilotClass));
                boolean typeMatches = sched.getType() != null && sched.getType().equalsIgnoreCase(roundType);

                if (classMatches && typeMatches && sched.getMax_round() != null) {
                    if (sched.getMax_round() > highestMaxRound) {
                        highestMaxRound = sched.getMax_round();
                        bestMatch = sched;
                    }
                }
            }
            if (bestMatch != null) {
                logger.info("Fallback match: using schedule with max_round={}, short_desc={}",
                        highestMaxRound, bestMatch.getShort_desc());
            }
        }

        if (bestMatch != null) {
            logger.info("FINAL RESULT: For round {} returning schedule with short_desc={}",
                    roundNumber, bestMatch.getShort_desc());
        } else {
            logger.warn("FINAL RESULT: No matching schedule found for class={}, type={}, round={}",
                    pilotClass, roundType, roundNumber);
        }

        return bestMatch;
    }

    /**
     * @deprecated Use getScheduleForRound() for round-aware lookups.
     * This method ignores round numbers and returns the first match.
     */
    @Deprecated
    public  Map<String,List<FigureDTO>> getAllSequences_old()
            throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {

        if(!isSequence()){
            getSequenceFileFromScore();
        }
        // Get Document Builder
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // parse XML file
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse( new File(SEQUENCES_DAT_PATH));
        doc.getDocumentElement().normalize();
        System.out.println("Sequence file root Element :" + doc.getDocumentElement().getNodeName());

        NodeList list = doc.getElementsByTagName("sequences");
        NodeList newList = (NodeList) list.item(0);
        System.out.println(newList.getLength());
        Map<String,List<FigureDTO>> figuresMap = new HashMap<>();
        for (int temp = 0; temp < newList.getLength(); temp++) {
            Node node = newList.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String index = element.getAttribute("index");
                String type = element.getElementsByTagName("type").item(0).getTextContent();
                String _class =  "";
                try {
                     _class = element.getElementsByTagName("class").item(0).getTextContent();
                } catch (Exception e) {
                     _class = "FREESTYLE";
                }
                System.out.println(index);
                System.out.println(_class);
                // System.out.println(_class);
                List<FigureDTO> figures  = new ArrayList<>();
                Element figuresList = (Element) element.getElementsByTagName("figures").item(0);
                System.out.println(figuresList.getElementsByTagName("figure").getLength());
                for (int temp2 = 0; temp2 < figuresList.getElementsByTagName("figure").getLength(); temp2++) {
                    Node node2 = figuresList.getElementsByTagName("figure").item(temp2);
                    if (node2.getNodeType() == Node.ELEMENT_NODE) {
                        Element figureElement = (Element) node2;
                        String description = figureElement.getElementsByTagName("description").item(0).getTextContent();
                        int k_factor = Integer
                                .parseInt(figureElement.getElementsByTagName("k_factor").item(0).getTextContent());
                        String scoring = figureElement.getElementsByTagName("scoring").item(0).getTextContent();
                        figures.add(new FigureDTO((temp2+1), k_factor, description, scoring));
                    }
                }
                String key = _class.toUpperCase().trim() + ":" + type.toUpperCase().trim();
                figuresMap.put(key, figures);
            }
        }
       return figuresMap;
    }
    public  List<FigureDTO> getAllSequenceForClass(String _class, String type) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException{
        Map<String,List<FigureDTO>> sequencesMap = this.getAllSequences_old();
        String key = _class.toUpperCase().trim() + ":" + type.toUpperCase().trim();
        return sequencesMap.get(key);
    }
}
