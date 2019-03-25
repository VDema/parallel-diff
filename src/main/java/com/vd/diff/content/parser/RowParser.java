package com.vd.diff.content.parser;

import com.vd.diff.content.Row;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class RowParser {
    private final static Pattern VALUE_PATTERN = Pattern.compile("(\\d+)\\s+(\\d+)\\s(.+)");

    public static Row toRow(String rawRow) {
        if (StringUtils.isBlank(rawRow)) {
            throw new IllegalArgumentException("Line is blank");
        }

        String[] keyAndValues = rawRow.split("\t");

        if (keyAndValues.length < 2) {
            throw new IllegalStateException("Unexpected record");
        }

        String rawKey = keyAndValues[0];
        String rawEvents = keyAndValues[1];

        return new Row(rawRow, rawKey, toEvents(rawEvents));
    }

    private static Map<String, Row.Event> toEvents(String rawEvents) {
        if (StringUtils.isBlank(rawEvents) || !rawEvents.startsWith("(")) {
            return Collections.emptyMap();
        }

        Map<String, Row.Event> events = new HashMap<>();
        rawEvents = rawEvents.substring(1, rawEvents.length() - 1);
        String[] rawSplitEvents = rawEvents.split("\\)\\(");

        for (String rawEvent : rawSplitEvents) {
            Matcher valueMatcher = VALUE_PATTERN.matcher(rawEvent);

            if (!valueMatcher.matches()) {
                throw new IllegalArgumentException(String.format(
                        "Value has incorrect format: [%s]. Pattern is: [%s]", rawEvent, VALUE_PATTERN.pattern()));
            }

            String rawEventId = valueMatcher.group(1);
            String rawCreated = valueMatcher.group(2);
            String rawHost = valueMatcher.group(3);

            Row.Event rowValue = new Row.Event(
                    rawEvent,
                    Integer.parseInt(rawEventId),
                    Long.parseLong(rawCreated),
                    rawHost.equalsIgnoreCase("null") ? null : rawHost
            );

            events.put(rawEventId + "-" + rawCreated, rowValue);
        }
        return events;
    }
}
