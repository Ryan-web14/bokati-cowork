package com.sni.bokaticowork.features.analytics.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Decoupage d'un intervalle de dates en periodes (jour, semaine commencant le lundi comme
 * {@code date_trunc('week')} de PostgreSQL, ou mois), toutes presentes meme vides, pour une courbe
 * continue.
 */
final class TrendPeriods {

    static final int DEFAULT_DAYS = 30;
    static final int MAX_PERIODS = 400;

    /** Une periode : son premier jour (cle de regroupement) et ses jours compris dans l'intervalle. */
    record Period(LocalDate start, int days) {
    }

    private TrendPeriods() {
    }

    /** {@code day}, {@code week} ou {@code month}, sans casse ; {@code day} par defaut. */
    static String unit(String interval) {
        String normalized = interval == null || interval.isBlank() ? "day" : interval.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "day", "week", "month" -> normalized;
            default -> throw new BadRequestException("interval doit valoir DAY, WEEK ou MONTH");
        };
    }

    static LocalDate defaultTo(LocalDate to) {
        return to == null ? LocalDate.now() : to;
    }

    static LocalDate defaultFrom(LocalDate from, LocalDate to) {
        return from == null ? to.minusDays(DEFAULT_DAYS - 1L) : from;
    }

    static List<Period> periods(LocalDate from, LocalDate to, String unit) {
        if (from.isAfter(to)) {
            throw new BadRequestException("fromDate doit preceder toDate");
        }
        List<Period> periods = new ArrayList<>();
        LocalDate start = truncate(from, unit);
        while (!start.isAfter(to)) {
            LocalDate next = next(start, unit);
            LocalDate first = start.isBefore(from) ? from : start;
            LocalDate last = next.minusDays(1).isAfter(to) ? to : next.minusDays(1);
            periods.add(new Period(start, (int) ChronoUnit.DAYS.between(first, last) + 1));
            if (periods.size() > MAX_PERIODS) {
                throw new BadRequestException("Intervalle trop long : " + MAX_PERIODS + " periodes au plus, elargir interval");
            }
            start = next;
        }
        return periods;
    }

    static LocalDate truncate(LocalDate date, String unit) {
        return switch (unit) {
            case "week" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private static LocalDate next(LocalDate start, String unit) {
        return switch (unit) {
            case "week" -> start.plusWeeks(1);
            case "month" -> start.plusMonths(1);
            default -> start.plusDays(1);
        };
    }
}
