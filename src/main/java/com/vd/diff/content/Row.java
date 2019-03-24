package com.vd.diff.content;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class Row {
    private final String raw;

    private final String key;
    private final Map<String, Event> events;

    public boolean isKeyLower(Row anotherRow) {
        return isKeyLower(anotherRow.key);
    }

    public boolean isKeyLower(String anotherKey) {
        return key.compareTo(anotherKey) < 0;
    }

    public boolean isKeyGreater(Row anotherRow) {
        return key.compareTo(anotherRow.key) > 0;
    }

    public boolean isKeyGreaterOrEqual(String anotherKey) {
        return key.compareTo(anotherKey) >= 0;
    }

    public String diffEvents(Row anotherRow) {
        Preconditions.checkState(key.equals(anotherRow.key));

        Map<String, Event> thisNewEvents = findDifferentEvents(events, anotherRow.events);
        Map<String, Event> anotherNewEvents = findDifferentEvents(anotherRow.events, events);

        Set<String> differentRawEvents = new HashSet<>();

        differentRawEvents.addAll(thisNewEvents.keySet());
        differentRawEvents.addAll(anotherNewEvents.keySet());

        return key + "\t" + String.join(":", differentRawEvents);
    }

    private Map<String, Event> findDifferentEvents(Map<String, Event> events, Map<String, Event> anotherEvents) {
        return events.entrySet()
                .stream()
                .filter(e -> anotherEvents.get(e.getKey()) == null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Data
    public static class Event {
        private final Integer num;
        private final Long timestamp;
        private final String host;

        public Event(Integer num, Long timestamp, String host) {
            this.num = num;
            this.timestamp = timestamp;
            this.host = host;
        }
    }
}
