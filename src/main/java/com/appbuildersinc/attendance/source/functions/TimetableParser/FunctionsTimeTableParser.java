package com.appbuildersinc.attendance.source.functions.TimetableParser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FunctionsTimeTableParser {

    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("\\b[A-Z]{3}[A-Z0-9]{4}\\b");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}");
    private static final Set<String> IGNORED_KEYWORDS = new HashSet<>(Arrays.asList(
            "FREE", "TUTORIAL", "MENTOR", "SELF", "CONTENT", "BREAK", "LUNCH", "LIBRARY", "SPORTS"
    ));

    public Map<String, List<Map<String, String>>> extractTimetableFromPdf(InputStream pdfInputStream) {
        Map<String, List<Map<String, String>>> finalSchedule = new LinkedHashMap<>();

        try (PDDocument document = PDDocument.load(pdfInputStream)) {

            ObjectExtractor oe = new ObjectExtractor(document);
            Page page = oe.extract(1);

            SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();
            List<Table> tables = algorithm.extract(page);

            if (tables.isEmpty()) throw new RuntimeException("No tables found in PDF");

            Table table = tables.get(0);
            List<List<RectangularTextContainer>> rows = table.getRows();

            int timeRowIndex = findBestHeaderRowIndex(rows);
            if (timeRowIndex == -1) throw new RuntimeException("Could not detect time headers");

            List<String> timeHeaders = new ArrayList<>();
            List<RectangularTextContainer> headerRow = rows.get(timeRowIndex);
            for (int i = 1; i < headerRow.size(); i++) {
                timeHeaders.add(cleanText(headerRow.get(i).getText()));
            }

            for (int r = timeRowIndex + 1; r < rows.size(); r++) {
                List<RectangularTextContainer> row = rows.get(r);
                if (row.isEmpty()) continue;

                String dayName = cleanText(row.get(0).getText());
                if (!isDayName(dayName)) continue;

                List<RawSlot> dailySlots = new ArrayList<>();
                int columnsToProcess = Math.min(row.size() - 1, timeHeaders.size());

                for (int c = 0; c < columnsToProcess; c++) {
                    String rawText = cleanText(row.get(c + 1).getText());
                    String timeRange = timeHeaders.get(c);
                    String[] times = parseTimeRange(timeRange);

                    List<String> codes = extractCleanClassCodes(rawText);
                    for (String code : codes) {
                        dailySlots.add(new RawSlot(code, times[0], times[1]));
                    }
                }

                List<Map<String, String>> mergedSchedule = processAndMergeSlots(dailySlots);

                if (!mergedSchedule.isEmpty()) {
                    finalSchedule.put(dayName, mergedSchedule);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error parsing PDF: " + e.getMessage());
        }

        return finalSchedule;
    }

    private List<Map<String, String>> processAndMergeSlots(List<RawSlot> rawSlots) {
        Map<String, List<RawSlot>> grouped = rawSlots.stream()
                .collect(Collectors.groupingBy(slot -> slot.classCode));

        List<RawSlot> mergedList = new ArrayList<>();

        for (List<RawSlot> group : grouped.values()) {
            group.sort(Comparator.comparing(s -> parseTime(s.startTime)));
            mergedList.addAll(mergeConsecutiveSlots(group));
        }

        mergedList.sort(Comparator.comparing(s -> parseTime(s.startTime)));

        return mergedList.stream().map(RawSlot::toMap).collect(Collectors.toList());
    }

    private List<RawSlot> mergeConsecutiveSlots(List<RawSlot> slots) {
        if (slots.isEmpty()) return slots;

        List<RawSlot> result = new ArrayList<>();
        RawSlot current = slots.get(0);

        for (int i = 1; i < slots.size(); i++) {
            RawSlot next = slots.get(i);

            if (isConsecutive(current.endTime, next.startTime)) {
                current.endTime = next.endTime;
            } else {
                result.add(current);
                current = next;
            }
        }
        result.add(current);
        return result;
    }

    private boolean isConsecutive(String endTime1, String startTime2) {
        try {
            LocalTime end1 = parseTime(endTime1);
            LocalTime start2 = parseTime(startTime2);
            long gapMinutes = ChronoUnit.MINUTES.between(end1, start2);
            return gapMinutes >= 0 && gapMinutes <= 45;
        } catch (Exception e) {
            return false;
        }
    }

    private LocalTime parseTime(String timeStr) {
        try {
            if (timeStr == null || timeStr.isEmpty()) return LocalTime.MAX;
            String[] parts = timeStr.split(":");
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h >= 1 && h <= 6) h += 12;
            return LocalTime.of(h, m);
        } catch (Exception e) {
            return LocalTime.MIN;
        }
    }

    private List<String> extractCleanClassCodes(String text) {
        List<String> codes = new ArrayList<>();
        if (text == null || text.isEmpty()) return codes;
        for (String ignored : IGNORED_KEYWORDS) {
            if (text.toUpperCase().contains(ignored)) return codes;
        }
        Matcher matcher = COURSE_CODE_PATTERN.matcher(text);
        while (matcher.find()) codes.add(matcher.group());
        return codes;
    }

    private String[] parseTimeRange(String timeRange) {
        if (timeRange == null) return new String[]{"", ""};
        String[] parts = timeRange.split("-");
        if (parts.length == 2) return new String[]{parts[0].trim(), parts[1].trim()};
        return new String[]{timeRange, ""};
    }

    private int findBestHeaderRowIndex(List<List<RectangularTextContainer>> rows) {
        int bestIndex = -1;
        int maxMatches = 0;
        int limit = Math.min(rows.size(), 6);
        for (int i = 0; i < limit; i++) {
            int matches = 0;
            for (RectangularTextContainer cell : rows.get(i)) {
                if (TIME_PATTERN.matcher(cell.getText()).find()) matches++;
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.replace("\r", " ").replace("\n", " ").trim();
    }

    private boolean isDayName(String text) {
        List<String> days = Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT");
        for (String day : days) {
            if (text.toUpperCase().contains(day)) return true;
        }
        return false;
    }

    public static class RawSlot {
        String classCode;
        String startTime;
        String endTime;

        public RawSlot(String classCode, String startTime, String endTime) {
            this.classCode = classCode;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public Map<String, String> toMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("classCode", classCode);
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            return map;
        }
    }
}