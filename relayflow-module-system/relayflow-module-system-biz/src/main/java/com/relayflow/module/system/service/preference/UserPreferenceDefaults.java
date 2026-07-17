package com.relayflow.module.system.service.preference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Code defaults + deep-merge for user preference (C-class provisioning).
 */
@Component
@RequiredArgsConstructor
public class UserPreferenceDefaults {

    public static final int SCHEMA_VERSION = 1;

    private static final Set<String> THEME_MODES = Set.of("light", "dark", "auto");
    private static final Set<String> BUBBLE_LAYOUTS = Set.of("left", "split");
    private static final Set<String> THEME_COLORS = Set.of(
            "teal", "green", "cyan", "blue", "emerald", "sky");

    private final ObjectMapper objectMapper;

    public Map<String, Object> codeDefaults() {
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("themeMode", "light");
        general.put("themeColor", "teal");

        Map<String, Object> im = new LinkedHashMap<>();
        im.put("chatBubbleLayout", "split");

        Map<String, Object> calendar = new LinkedHashMap<>();
        calendar.put("weekStartsOn", 0);
        calendar.put("defaultEventDurationMinutes", 30);
        calendar.put("defaultRemindBeforeMinutes", 5);
        calendar.put("allDayRemindTime", "08:00");
        calendar.put("dimPastEvents", true);
        calendar.put("showTaskLayer", true);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("general", general);
        root.put("im", im);
        root.put("calendar", calendar);
        return root;
    }

    public Map<String, Object> merge(Map<String, Object> storedOrNull) {
        ObjectNode base = objectMapper.valueToTree(codeDefaults());
        if (storedOrNull != null && !storedOrNull.isEmpty()) {
            ObjectNode patch = objectMapper.valueToTree(storedOrNull);
            mergeObjectNodes(base, patch);
        }
        return objectMapper.convertValue(base, new TypeReference<>() {});
    }

    public Map<String, Object> parseSettingsJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public String toSettingsJson(Map<String, Object> settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (Exception ex) {
            throw new IllegalStateException("serialize preference settings failed", ex);
        }
    }

    /**
     * Sanitize client payload: keep known namespaces/keys; drop unknown; validate enums.
     *
     * @return sanitized map or null if validation failed (caller throws)
     */
    public Map<String, Object> sanitizeOrNull(Map<String, Object> input, StringBuilder errorOut) {
        if (input == null) {
            errorOut.append("settings 不能为空");
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();

        Object generalObj = input.get("general");
        if (generalObj instanceof Map<?, ?> generalMap) {
            Map<String, Object> general = new LinkedHashMap<>();
            Object themeMode = generalMap.get("themeMode");
            if (themeMode != null) {
                String mode = String.valueOf(themeMode);
                if (!THEME_MODES.contains(mode)) {
                    errorOut.append("themeMode 非法");
                    return null;
                }
                general.put("themeMode", mode);
            }
            Object themeColor = generalMap.get("themeColor");
            if (themeColor != null) {
                String color = String.valueOf(themeColor);
                if (!THEME_COLORS.contains(color)) {
                    errorOut.append("themeColor 非法");
                    return null;
                }
                general.put("themeColor", color);
            }
            if (!general.isEmpty()) {
                result.put("general", general);
            }
        }

        Object imObj = input.get("im");
        if (imObj instanceof Map<?, ?> imMap) {
            Map<String, Object> im = new LinkedHashMap<>();
            Object layout = imMap.get("chatBubbleLayout");
            if (layout != null) {
                String value = String.valueOf(layout);
                if (!BUBBLE_LAYOUTS.contains(value)) {
                    errorOut.append("chatBubbleLayout 非法");
                    return null;
                }
                im.put("chatBubbleLayout", value);
            }
            if (!im.isEmpty()) {
                result.put("im", im);
            }
        }

        Object calendarObj = input.get("calendar");
        if (calendarObj instanceof Map<?, ?> calendarMap) {
            Map<String, Object> calendar = new LinkedHashMap<>();
            Object weekStartsOn = calendarMap.get("weekStartsOn");
            if (weekStartsOn != null) {
                int day;
                try {
                    day = Integer.parseInt(String.valueOf(weekStartsOn));
                } catch (NumberFormatException ex) {
                    errorOut.append("weekStartsOn 非法");
                    return null;
                }
                if (day < 0 || day > 6) {
                    errorOut.append("weekStartsOn 非法");
                    return null;
                }
                calendar.put("weekStartsOn", day);
            }
            Object duration = calendarMap.get("defaultEventDurationMinutes");
            if (duration != null) {
                int minutes;
                try {
                    minutes = Integer.parseInt(String.valueOf(duration));
                } catch (NumberFormatException ex) {
                    errorOut.append("defaultEventDurationMinutes 非法");
                    return null;
                }
                if (minutes <= 0 || minutes > 24 * 60) {
                    errorOut.append("defaultEventDurationMinutes 非法");
                    return null;
                }
                calendar.put("defaultEventDurationMinutes", minutes);
            }
            Object remind = calendarMap.get("defaultRemindBeforeMinutes");
            if (remind != null) {
                int minutes;
                try {
                    minutes = Integer.parseInt(String.valueOf(remind));
                } catch (NumberFormatException ex) {
                    errorOut.append("defaultRemindBeforeMinutes 非法");
                    return null;
                }
                if (minutes < 0 || minutes > 24 * 60) {
                    errorOut.append("defaultRemindBeforeMinutes 非法");
                    return null;
                }
                calendar.put("defaultRemindBeforeMinutes", minutes);
            }
            Object allDayRemind = calendarMap.get("allDayRemindTime");
            if (allDayRemind != null) {
                String value = String.valueOf(allDayRemind).trim();
                if (!value.matches("^\\d{2}:\\d{2}$")) {
                    errorOut.append("allDayRemindTime 非法");
                    return null;
                }
                calendar.put("allDayRemindTime", value);
            }
            Object dimPast = calendarMap.get("dimPastEvents");
            if (dimPast != null) {
                if (!(dimPast instanceof Boolean) && !"true".equalsIgnoreCase(String.valueOf(dimPast))
                        && !"false".equalsIgnoreCase(String.valueOf(dimPast))) {
                    errorOut.append("dimPastEvents 非法");
                    return null;
                }
                calendar.put("dimPastEvents", Boolean.parseBoolean(String.valueOf(dimPast)));
            }
            Object showTaskLayer = calendarMap.get("showTaskLayer");
            if (showTaskLayer != null) {
                if (!(showTaskLayer instanceof Boolean)
                        && !"true".equalsIgnoreCase(String.valueOf(showTaskLayer))
                        && !"false".equalsIgnoreCase(String.valueOf(showTaskLayer))) {
                    errorOut.append("showTaskLayer 非法");
                    return null;
                }
                calendar.put("showTaskLayer", Boolean.parseBoolean(String.valueOf(showTaskLayer)));
            }
            if (!calendar.isEmpty()) {
                result.put("calendar", calendar);
            }
        }

        // Persist full effective document after merge with defaults (stable GET shape).
        return merge(result);
    }

    private void mergeObjectNodes(ObjectNode target, ObjectNode patch) {
        patch.fields().forEachRemaining(entry -> {
            if (entry.getValue().isObject() && target.has(entry.getKey()) && target.get(entry.getKey()).isObject()) {
                mergeObjectNodes((ObjectNode) target.get(entry.getKey()), (ObjectNode) entry.getValue());
            } else {
                target.set(entry.getKey(), entry.getValue());
            }
        });
    }
}
