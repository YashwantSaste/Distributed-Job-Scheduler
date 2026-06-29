package com.project.scheduler.core.schedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

public final class SimpleCronExpression {

	public Instant next(String expression, Instant after) {
		if (expression == null || expression.isBlank()) {
			throw new IllegalArgumentException("cron expression required");
		}
		String[] fields = expression.trim().split("\\s+");
		if (fields.length != 5) {
			throw new IllegalArgumentException("Only five-field cron is supported");
		}
		Set<Integer> minutes = parse(fields[0], 0, 59);
		Set<Integer> hours = parse(fields[1], 0, 23);
		Set<Integer> days = parse(fields[2], 1, 31);
		Set<Integer> months = parse(fields[3], 1, 12);
		Set<Integer> daysOfWeek = parseDayOfWeek(fields[4]);
		boolean anyDayOfMonth = isUnrestricted(fields[2]);
		boolean anyDayOfWeek = isUnrestricted(fields[4]);
		ZonedDateTime candidate = ZonedDateTime.ofInstant(after, ZoneOffset.UTC).plusMinutes(1).withSecond(0)
				.withNano(0);
		for (int attempt = 0; attempt < 366 * 24 * 60; attempt++) {
			if (minutes.contains(candidate.getMinute()) && hours.contains(candidate.getHour())
					&& months.contains(candidate.getMonthValue())
					&& matchesDay(candidate, days, daysOfWeek, anyDayOfMonth, anyDayOfWeek)) {
				return candidate.toInstant();
			}
			candidate = candidate.plusMinutes(1);
		}
		throw new IllegalArgumentException("No cron fire time found within one year");
	}

	boolean matchesDay(ZonedDateTime candidate, Set<Integer> days, Set<Integer> daysOfWeek, boolean anyDayOfMonth,
			boolean anyDayOfWeek) {
		if (anyDayOfMonth && anyDayOfWeek) {
			return true;
		}
		if (anyDayOfMonth) {
			return daysOfWeek.contains(candidate.getDayOfWeek().getValue());
		}
		if (anyDayOfWeek) {
			return days.contains(candidate.getDayOfMonth());
		}
		return days.contains(candidate.getDayOfMonth())
				|| daysOfWeek.contains(candidate.getDayOfWeek().getValue());
	}

	boolean isUnrestricted(String field) {
		String trimmed = field.trim();
		return "*".equals(trimmed) || "?".equals(trimmed);
	}

	Set<Integer> parseDayOfWeek(String fieldExpression) {
		if (isUnrestricted(fieldExpression)) {
			Set<Integer> allDays = new HashSet<>();
			for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
				allDays.add(dayOfWeek.getValue());
			}
			return allDays;
		}
		Set<Integer> cronValues = parse(fieldExpression, 0, 7);
		Set<Integer> javaValues = new HashSet<>();
		for (int value : cronValues) {
			javaValues.add(value == 0 || value == 7 ? DayOfWeek.SUNDAY.getValue() : value);
		}
		return javaValues;
	}

	Set<Integer> parse(String fieldExpression, int min, int max) {
		Set<Integer> values = new HashSet<>();
		for (String part : fieldExpression.split(",")) {
			if ("*".equals(part) || "?".equals(part)) {
				add(values, min, max, 1);
			} else if (part.startsWith("*/")) {
				add(values, min, max, Integer.parseInt(part.substring(2)));
			} else if (part.contains("-")) {
				String[] range = part.split("-");
				add(values, Integer.parseInt(range[0]), Integer.parseInt(range[1]), 1);
			} else {
				values.add(Integer.parseInt(part));
			}
		}
		return values;
	}

	void add(Set<Integer> values, int min, int max, int step) {
		for (int value = min; value <= max; value += step) {
			values.add(value);
		}
	}
}
