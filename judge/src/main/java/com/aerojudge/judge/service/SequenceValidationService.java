package com.aerojudge.judge.service;

import com.aerojudge.judge.dto.Pilot;
import com.aerojudge.judge.dto.ScheduleDTO;
import com.aerojudge.judge.dto.ValidationResult;
import com.aerojudge.judge.dto.ValidationIssue;
import com.aerojudge.judge.utils.ContestClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-flight validation service for sequence configuration.
 * Validates that all sequence configurations are correct after sync,
 * before judging can begin.
 */
@Service
public class SequenceValidationService {

    private static final Logger logger = LoggerFactory.getLogger(SequenceValidationService.class);

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private PilotService pilotService;

    @Autowired
    private SequenceFolderResolver resolver;

    @Autowired
    private CompService compService;

    /**
     * Validates all sequence configurations after sync.
     * @return ValidationResult with errors and warnings
     */
    public ValidationResult validateAll() {
        logger.info("Starting sequence validation...");

        // Collect issues in temporary maps for grouping
        Map<String, List<String>> errorsByContext = new HashMap<>();
        Map<String, List<String>> warningsByContext = new HashMap<>();

        try {
            // Check 1: Pilot classes without sequences
            checkPilotClassCoverage(errorsByContext, warningsByContext);

            // Check 2: Folder resolution for each sequence
            checkFolderResolution(errorsByContext, warningsByContext);

            // Check 3: Round range conflicts
            checkRoundRangeConflicts(errorsByContext, warningsByContext);

            // Check 4: Round coverage gaps
            checkRoundCoverageGaps(errorsByContext, warningsByContext);

        } catch (Exception e) {
            logger.error("Error during validation", e);
            errorsByContext.computeIfAbsent("SYSTEM", k -> new ArrayList<>())
                .add("Validation failed: " + e.getMessage());
        }

        // Get classes with pilots to filter out irrelevant issues
        Set<String> classesWithPilots = getClassesWithPilots();

        // Build grouped validation result, filtering out issues for empty classes
        ValidationResult result = buildGroupedResult(errorsByContext, warningsByContext, classesWithPilots);

        logger.info("Validation complete: {} errors, {} warnings across {} contexts",
                result.getTotalErrorCount(), result.getTotalWarningCount(), result.getIssueCount());
        return result;
    }

    /**
     * Check 1: Pilot classes without sequences.
     * Ensures every pilot class has at least one sequence defined.
     */
    private void checkPilotClassCoverage(Map<String, List<String>> errorsByContext,
                                          Map<String, List<String>> warningsByContext) {
        List<Pilot> pilots;
        try {
            pilots = pilotService.getPilots(true);
        } catch (Exception e) {
            // If we can't read pilots, skip this check silently - the error will be apparent elsewhere
            logger.error("Could not read pilots for validation check", e);
            return;
        }

        if (pilots == null || pilots.isEmpty()) {
            logger.debug("No pilots loaded - skipping pilot class coverage check");
            return;
        }

        // Get unique pilot classes
        Set<String> pilotClasses = pilots.stream()
                .filter(p -> p.getClassString() != null)
                .map(p -> p.getClassString().toUpperCase())
                .collect(Collectors.toSet());

        // Get classes from schedules
        Map<Integer, ScheduleDTO> schedules = scheduleService.getSchedules();
        if (schedules == null || schedules.isEmpty()) {
            errorsByContext.computeIfAbsent("SEQUENCES", k -> new ArrayList<>())
                .add("No sequences loaded - The sequences.dat file may be missing or empty");
            return;
        }

        Set<String> sequenceClasses = schedules.values().stream()
                .filter(s -> s.getComp_class() != null)
                .map(s -> s.getComp_class().toUpperCase())
                .collect(Collectors.toSet());

        // Check each pilot class has sequences
        for (String pilotClass : pilotClasses) {
            if (!sequenceClasses.contains(pilotClass) &&
                !"FREESTYLE".equalsIgnoreCase(pilotClass)) {

                // Count affected pilots (but don't list names)
                long affectedCount = pilots.stream()
                        .filter(p -> p.getClassString() != null &&
                                p.getClassString().equalsIgnoreCase(pilotClass))
                        .count();

                // Use normalized context (just class name for class-level issues)
                errorsByContext.computeIfAbsent(pilotClass, k -> new ArrayList<>())
                    .add(affectedCount + " pilot(s) registered but NO SEQUENCES defined");
            }
        }
    }

    /**
     * Check 2: Folder resolution for each sequence.
     * Ensures each sequence's folder can be resolved.
     */
    private void checkFolderResolution(Map<String, List<String>> errorsByContext,
                                        Map<String, List<String>> warningsByContext) {
        Map<Integer, ScheduleDTO> schedules = scheduleService.getSchedules();
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        // Get default sequenceType from competition settings
        String defaultSequenceType = "std";
        try {
            var comp = compService.getComp();
            if (comp != null && comp.getSequenceType() != null) {
                defaultSequenceType = comp.getSequenceType();
            }
        } catch (Exception e) {
            logger.debug("Could not get competition settings for sequenceType");
        }

        for (ScheduleDTO schedule : schedules.values()) {
            // Test with min_round
            String resolved = resolver.resolve(
                    schedule.getComp_class(),
                    schedule.getType(),
                    schedule.getMin_round() != null ? schedule.getMin_round() : 1,
                    defaultSequenceType
            );

            if (SequenceFolderResolver.FAIL_FOLDER.equals(resolved)) {
                String shortDesc = schedule.getShort_desc();
                // Normalize context - use space separator
                String context = normalizeContext(schedule.getComp_class(), schedule.getType());

                String roundInfo = " - Rounds " + schedule.getMin_round() + "-" + schedule.getMax_round();

                if (shortDesc == null || shortDesc.trim().isEmpty()) {
                    errorsByContext.computeIfAbsent(context, k -> new ArrayList<>())
                        .add("No short_desc defined" + roundInfo + " - Will use defaults, No audio");
                } else {
                    errorsByContext.computeIfAbsent(context, k -> new ArrayList<>())
                        .add("Folder not found: " + shortDesc + roundInfo);
                }
            }
        }
    }

    /**
     * Check 3: Duplicate and overlapping round range conflicts.
     * Within same class+type:
     * - Same short_desc = WARNING (redundant but harmless)
     * - Different/no short_desc = ERROR (ambiguous, will use FAIL folder)
     */
    private void checkRoundRangeConflicts(Map<String, List<String>> errorsByContext,
                                           Map<String, List<String>> warningsByContext) {
        Map<String, List<ScheduleDTO>> byClassType = groupByClassType();

        for (Map.Entry<String, List<ScheduleDTO>> entry : byClassType.entrySet()) {
            String key = entry.getKey();
            List<ScheduleDTO> schedules = entry.getValue();

            if (schedules.size() < 2) {
                continue; // No conflicts possible with single entry
            }

            // Normalize the context key (convert colon to space)
            String normalizedKey = key.replace(":", " ");

            for (int i = 0; i < schedules.size(); i++) {
                for (int j = i + 1; j < schedules.size(); j++) {
                    ScheduleDTO a = schedules.get(i);
                    ScheduleDTO b = schedules.get(j);

                    // Check for duplicate min_round OR overlapping ranges
                    boolean isDuplicate = a.getMin_round() != null && b.getMin_round() != null &&
                            a.getMin_round().equals(b.getMin_round());
                    boolean isOverlapping = rangesOverlap(a, b);

                    if (isDuplicate || isOverlapping) {
                        String aDesc = a.getShort_desc() != null && !a.getShort_desc().trim().isEmpty()
                                ? a.getShort_desc() : null;
                        String bDesc = b.getShort_desc() != null && !b.getShort_desc().trim().isEmpty()
                                ? b.getShort_desc() : null;

                        boolean sameShortDesc = aDesc != null && bDesc != null && aDesc.equalsIgnoreCase(bDesc);

                        String conflictType = isDuplicate ? "Duplicate" : "Overlapping";
                        String roundInfo = isDuplicate
                                ? "min_round=" + a.getMin_round()
                                : "rounds " + a.getMin_round() + "-" + a.getMax_round() +
                                  " and " + b.getMin_round() + "-" + b.getMax_round();

                        if (sameShortDesc) {
                            // Same short_desc = WARNING (redundant but works)
                            warningsByContext.computeIfAbsent(normalizedKey, k -> new ArrayList<>())
                                .add(conflictType + " sequences (" + roundInfo + ")");
                        } else {
                            // Different or missing short_desc = ERROR (ambiguous)
                            errorsByContext.computeIfAbsent(normalizedKey, k -> new ArrayList<>())
                                .add(conflictType + " sequences (" + roundInfo + ") - Will use defaults, No audio");
                        }
                    }
                }
            }
        }
    }

    /**
     * Check 4: Round coverage gaps.
     * Warns when there are gaps in round coverage (e.g., 1-3 and 5-6 defined, round 4 uncovered).
     */
    private void checkRoundCoverageGaps(Map<String, List<String>> errorsByContext,
                                         Map<String, List<String>> warningsByContext) {
        Map<String, List<ScheduleDTO>> byClassType = groupByClassType();

        for (Map.Entry<String, List<ScheduleDTO>> entry : byClassType.entrySet()) {
            String key = entry.getKey();
            List<ScheduleDTO> schedules = entry.getValue();

            if (schedules.isEmpty()) continue;

            // Normalize the context key (convert colon to space)
            String normalizedKey = key.replace(":", " ");

            // Sort by min_round
            schedules.sort((a, b) -> {
                int aMin = a.getMin_round() != null ? a.getMin_round() : 0;
                int bMin = b.getMin_round() != null ? b.getMin_round() : 0;
                return Integer.compare(aMin, bMin);
            });

            // Check for gaps between consecutive ranges
            for (int i = 0; i < schedules.size() - 1; i++) {
                ScheduleDTO current = schedules.get(i);
                ScheduleDTO next = schedules.get(i + 1);

                int currentMax = current.getMax_round() != null ? current.getMax_round() : 0;
                int nextMin = next.getMin_round() != null ? next.getMin_round() : 0;

                // Gap exists if next min_round > current max_round + 1
                if (nextMin > currentMax + 1) {
                    int gapStart = currentMax + 1;
                    int gapEnd = nextMin - 1;
                    String gapRange = gapStart == gapEnd ? "round " + gapStart : "rounds " + gapStart + "-" + gapEnd;
                    warningsByContext.computeIfAbsent(normalizedKey, k -> new ArrayList<>())
                        .add("Round coverage gap: " + gapRange + " undefined");
                }
            }
        }
    }

    /**
     * Group schedules by CLASS:TYPE key.
     */
    private Map<String, List<ScheduleDTO>> groupByClassType() {
        Map<String, List<ScheduleDTO>> result = new HashMap<>();

        Map<Integer, ScheduleDTO> schedules = scheduleService.getSchedules();
        if (schedules == null) {
            return result;
        }

        for (ScheduleDTO schedule : schedules.values()) {
            String key;
            if ("FREESTYLE".equalsIgnoreCase(schedule.getType())) {
                key = "FREESTYLE";
            } else {
                key = (schedule.getComp_class() != null ? schedule.getComp_class().toUpperCase() : "UNKNOWN")
                        + ":" + (schedule.getType() != null ? schedule.getType().toUpperCase() : "UNKNOWN");
            }

            result.computeIfAbsent(key, k -> new ArrayList<>()).add(schedule);
        }

        return result;
    }

    /**
     * Check if two schedule round ranges overlap.
     */
    private boolean rangesOverlap(ScheduleDTO a, ScheduleDTO b) {
        Integer aMin = a.getMin_round() != null ? a.getMin_round() : 0;
        Integer aMax = a.getMax_round() != null ? a.getMax_round() : Integer.MAX_VALUE;
        Integer bMin = b.getMin_round() != null ? b.getMin_round() : 0;
        Integer bMax = b.getMax_round() != null ? b.getMax_round() : Integer.MAX_VALUE;

        return aMin <= bMax && bMin <= aMax;
    }

    /**
     * Normalize context string to use consistent format.
     * @param compClass the competition class (may be null for FREESTYLE)
     * @param type the sequence type
     * @return normalized context string using space separator
     */
    private String normalizeContext(String compClass, String type) {
        if ("FREESTYLE".equalsIgnoreCase(type)) {
            return "FREESTYLE";
        }
        String classStr = compClass != null ? compClass.toUpperCase() : "UNKNOWN";
        String typeStr = type != null ? type.toUpperCase() : "UNKNOWN";
        return classStr + " " + typeStr;
    }

    /**
     * Get set of competition classes that have pilots registered.
     * Reuses logic from checkPilotClassCoverage.
     */
    private Set<String> getClassesWithPilots() {
        try {
            List<Pilot> pilots = pilotService.getPilots(true);
            if (pilots == null || pilots.isEmpty()) {
                return new HashSet<>();
            }
            return pilots.stream()
                    .filter(p -> p.getClassString() != null)
                    .map(p -> p.getClassString().toUpperCase())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Could not get pilot classes", e);
            return new HashSet<>();
        }
    }

    /**
     * Build grouped validation result from collected issues.
     * Creates one ValidationIssue per context, combining all errors and warnings.
     * Filters out issues for classes with no pilots.
     * Sorts by competition class order.
     */
    private ValidationResult buildGroupedResult(Map<String, List<String>> errorsByContext,
                                                 Map<String, List<String>> warningsByContext,
                                                 Set<String> classesWithPilots) {
        ValidationResult result = new ValidationResult();

        // Combine all contexts
        Set<String> allContexts = new HashSet<>();
        allContexts.addAll(errorsByContext.keySet());
        allContexts.addAll(warningsByContext.keySet());

        // Sort contexts by competition class order
        List<String> sortedContexts = new ArrayList<>(allContexts);
        sortedContexts.sort((a, b) -> {
            // Extract class from context
            String classA = extractClass(a);
            String classB = extractClass(b);

            // Special handling for FREESTYLE - always last
            if ("FREESTYLE".equals(a)) return 1;
            if ("FREESTYLE".equals(b)) return -1;

            // Special handling for SYSTEM and SEQUENCES - always first
            if ("SYSTEM".equals(a) || "SEQUENCES".equals(a)) return -1;
            if ("SYSTEM".equals(b) || "SEQUENCES".equals(b)) return 1;

            // Get order indices (orderIndex pushes unknown / FREESTYLE to the end)
            int orderA = ContestClasses.orderIndex(classA);
            int orderB = ContestClasses.orderIndex(classB);

            // Compare by order
            int classCompare = Integer.compare(orderA, orderB);
            if (classCompare != 0) return classCompare;

            // Within same class, sort by type (KNOWN before UNKNOWN)
            return a.compareTo(b);
        });

        // Create one ValidationIssue per context with all messages combined
        for (String context : sortedContexts) {
            // Extract class from context to check if it has pilots
            String contextClass = extractClass(context);

            // Skip issues for classes with no pilots (except system-level issues)
            if (!classesWithPilots.contains(contextClass) &&
                !"FREESTYLE".equals(contextClass) &&  // Always show FREESTYLE issues
                !"SYSTEM".equals(context) &&
                !"SEQUENCES".equals(context)) {
                // Silently skip - no pilots in this class
                continue;
            }

            ValidationIssue issue = new ValidationIssue(context);

            List<String> errors = errorsByContext.get(context);
            List<String> warnings = warningsByContext.get(context);

            // Add all error messages to this issue
            if (errors != null) {
                for (String error : errors) {
                    issue.addError(error);
                }
            }

            // Add all warning messages to this issue
            if (warnings != null) {
                for (String warning : warnings) {
                    issue.addWarning(warning);
                }
            }

            // Add the combined issue to the result
            result.addIssue(issue);
        }

        return result;
    }

    /**
     * Extract competition class from context string.
     */
    private String extractClass(String context) {
        if (context == null) return "";
        if ("FREESTYLE".equals(context)) return "FREESTYLE";
        if ("SYSTEM".equals(context)) return "SYSTEM";
        if ("SEQUENCES".equals(context)) return "SEQUENCES";

        // Context is either "CLASS" or "CLASS TYPE"
        String[] parts = context.split(" ");
        return parts[0];
    }
}
