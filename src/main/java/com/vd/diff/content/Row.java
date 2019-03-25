package com.vd.diff.content;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        List<Event> thisNewEvents = findDifferentEvents(events, anotherRow.events);
        List<Event> anotherNewEvents = findDifferentEvents(anotherRow.events, events);

        Set<String> differentRawEvents = new HashSet<>();

        differentRawEvents.addAll(convertToRaw(thisNewEvents));
        differentRawEvents.addAll(convertToRaw(anotherNewEvents));

        return key + "\t" + "(" + String.join(")(", differentRawEvents) + ")";
    }

    private Set<String> convertToRaw(Collection<Event> events) {
        return events.stream()
                .map(Event::getRawEvent)
                .collect(Collectors.toSet());
    }

    private List<Event> findDifferentEvents(Map<String, Event> events, Map<String, Event> otherEvents) {
        return events.entrySet()
                .stream()
                .filter(e -> {
                    String eventKey = e.getKey();
                    Event event = e.getValue();

                    Event otherEvent = otherEvents.get(eventKey);

                    if (otherEvent == null) {
                        return true;
                    } else {
                        return !Objects.equals(event.host, otherEvent.host);
                    }
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Data
    public static class Event {
        private final String rawEvent;
        private final Integer num;
        private final Long timestamp;
        private final String host;
    }
}
