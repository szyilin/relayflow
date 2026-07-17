package com.relayflow.module.calendar.service.rrule;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Minimal RRULE expander: DAILY / WEEKLY / MONTHLY with INTERVAL, COUNT|UNTIL, BYDAY, BYMONTHDAY.
 */
public final class RecurrenceExpander {

    private static final int MAX_INSTANCES = 400;

    private RecurrenceExpander() {
    }

    public record Occurrence(OffsetDateTime start, OffsetDateTime end) {
    }

    public static void validate(String rrule) {
        if (!StringUtils.hasText(rrule)) {
            return;
        }
        parse(rrule.trim());
    }

    public static List<Occurrence> expand(OffsetDateTime seriesStart, OffsetDateTime seriesEnd,
                                          String rrule, OffsetDateTime windowFrom, OffsetDateTime windowTo) {
        if (!StringUtils.hasText(rrule)) {
            if (seriesStart.isBefore(windowTo) && seriesEnd.isAfter(windowFrom)) {
                return List.of(new Occurrence(seriesStart, seriesEnd));
            }
            return List.of();
        }
        ParsedRule rule = parse(rrule.trim());
        long durationSeconds = ChronoUnit.SECONDS.between(seriesStart, seriesEnd);
        if (durationSeconds <= 0) {
            throw new ServiceException(ErrorCodeConstants.EVENT_TIME_INVALID);
        }

        List<Occurrence> out = new ArrayList<>();
        OffsetDateTime cursor = seriesStart;
        int emitted = 0;
        int scanned = 0;
        while (scanned < MAX_INSTANCES * 4 && emitted < MAX_INSTANCES) {
            scanned++;
            if (rule.until != null && cursor.isAfter(rule.until)) {
                break;
            }
            if (rule.count != null && emitted >= rule.count) {
                break;
            }
            if (matches(cursor, seriesStart, rule)) {
                OffsetDateTime instEnd = cursor.plusSeconds(durationSeconds);
                if (cursor.isBefore(windowTo) && instEnd.isAfter(windowFrom)) {
                    out.add(new Occurrence(cursor, instEnd));
                }
                emitted++;
                if (rule.count != null && emitted >= rule.count) {
                    break;
                }
                if (cursor.isAfter(windowTo.plusDays(1))) {
                    break;
                }
            }
            cursor = advance(cursor, rule);
        }
        return out;
    }

    private static boolean matches(OffsetDateTime cursor, OffsetDateTime seriesStart, ParsedRule rule) {
        return switch (rule.freq) {
            case DAILY -> true;
            case WEEKLY -> rule.byDay.isEmpty()
                    || rule.byDay.contains(cursor.getDayOfWeek());
            case MONTHLY -> {
                int day = rule.byMonthDay != null ? rule.byMonthDay : seriesStart.getDayOfMonth();
                yield cursor.getDayOfMonth() == day;
            }
        };
    }

    private static OffsetDateTime advance(OffsetDateTime cursor, ParsedRule rule) {
        return switch (rule.freq) {
            case DAILY -> cursor.plusDays(rule.interval);
            case WEEKLY -> cursor.plusDays(1);
            case MONTHLY -> {
                if (cursor.getDayOfMonth() >= 28) {
                    yield cursor.plusMonths(rule.interval).withDayOfMonth(1);
                }
                yield cursor.plusDays(1);
            }
        };
    }

    private static ParsedRule parse(String rrule) {
        String body = rrule.toUpperCase(Locale.ROOT);
        if (body.startsWith("RRULE:")) {
            body = body.substring(6);
        }
        Map<String, String> parts = new HashMap<>();
        for (String token : body.split(";")) {
            if (!StringUtils.hasText(token) || !token.contains("=")) {
                continue;
            }
            String[] kv = token.split("=", 2);
            parts.put(kv[0].trim(), kv[1].trim());
        }
        String freqRaw = parts.get("FREQ");
        if (freqRaw == null) {
            throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
        }
        Freq freq;
        try {
            freq = Freq.valueOf(freqRaw);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
        }
        if (freq != Freq.DAILY && freq != Freq.WEEKLY && freq != Freq.MONTHLY) {
            throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
        }
        int interval = 1;
        if (parts.containsKey("INTERVAL")) {
            try {
                interval = Integer.parseInt(parts.get("INTERVAL"));
            } catch (NumberFormatException ex) {
                throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
            }
            if (interval < 1 || interval > 366) {
                throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
            }
        }
        Integer count = null;
        if (parts.containsKey("COUNT")) {
            try {
                count = Integer.parseInt(parts.get("COUNT"));
            } catch (NumberFormatException ex) {
                throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
            }
            if (count < 1 || count > MAX_INSTANCES) {
                throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
            }
        }
        OffsetDateTime until = null;
        if (parts.containsKey("UNTIL")) {
            until = parseUntil(parts.get("UNTIL"));
        }
        if (count != null && until != null) {
            throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
        }
        if (count == null && until == null) {
            count = 52;
        }
        Set<DayOfWeek> byDay = EnumSet.noneOf(DayOfWeek.class);
        if (parts.containsKey("BYDAY")) {
            for (String d : parts.get("BYDAY").split(",")) {
                DayOfWeek dow = parseDay(d.trim());
                if (dow == null) {
                    throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
                }
                byDay.add(dow);
            }
        }
        Integer byMonthDay = null;
        if (parts.containsKey("BYMONTHDAY")) {
            try {
                byMonthDay = Integer.parseInt(parts.get("BYMONTHDAY"));
            } catch (NumberFormatException ex) {
                throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
            }
            if (byMonthDay < 1 || byMonthDay > 31) {
                throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
            }
        }
        return new ParsedRule(freq, interval, count, until, byDay, byMonthDay);
    }

    private static OffsetDateTime parseUntil(String raw) {
        try {
            if (raw.endsWith("Z") && !raw.contains("-")) {
                // 20260717T100000Z
                int y = Integer.parseInt(raw.substring(0, 4));
                int m = Integer.parseInt(raw.substring(4, 6));
                int d = Integer.parseInt(raw.substring(6, 8));
                int hh = raw.length() >= 11 ? Integer.parseInt(raw.substring(9, 11)) : 0;
                int mm = raw.length() >= 13 ? Integer.parseInt(raw.substring(11, 13)) : 0;
                int ss = raw.length() >= 15 ? Integer.parseInt(raw.substring(13, 15)) : 0;
                return OffsetDateTime.of(y, m, d, hh, mm, ss, 0, java.time.ZoneOffset.UTC);
            }
            return OffsetDateTime.parse(raw);
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeConstants.EVENT_RRULE_INVALID);
        }
    }

    private static DayOfWeek parseDay(String token) {
        return switch (token) {
            case "MO" -> DayOfWeek.MONDAY;
            case "TU" -> DayOfWeek.TUESDAY;
            case "WE" -> DayOfWeek.WEDNESDAY;
            case "TH" -> DayOfWeek.THURSDAY;
            case "FR" -> DayOfWeek.FRIDAY;
            case "SA" -> DayOfWeek.SATURDAY;
            case "SU" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private enum Freq {
        DAILY, WEEKLY, MONTHLY
    }

    private record ParsedRule(Freq freq, int interval, Integer count, OffsetDateTime until,
                              Set<DayOfWeek> byDay, Integer byMonthDay) {
    }
}
