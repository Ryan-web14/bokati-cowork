package com.sni.bokaticowork.features.ressource.service.support;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lot R5a : jours feries nationaux de la Republique du Congo, calcules (aucune saisie annuelle).
 *
 * <p>Dates fixes : 1er janvier, 1er mai, 10 juin (Reconciliation nationale), 15 aout (Fete
 * nationale), 1er novembre (Toussaint), 28 novembre (Proclamation de la Republique), 25 decembre.
 * Dates mobiles : lundi de Paques, Ascension, lundi de Pentecote.</p>
 */
public final class CongoHolidays {

    private CongoHolidays() {
    }

    public static Map<LocalDate, String> of(int year) {
        Map<LocalDate, String> days = new TreeMap<>();
        days.put(LocalDate.of(year, Month.JANUARY, 1), "Jour de l'an");
        days.put(LocalDate.of(year, Month.MAY, 1), "Fête du travail");
        days.put(LocalDate.of(year, Month.JUNE, 10), "Fête de la Réconciliation nationale");
        days.put(LocalDate.of(year, Month.AUGUST, 15), "Fête nationale");
        days.put(LocalDate.of(year, Month.NOVEMBER, 1), "Toussaint");
        days.put(LocalDate.of(year, Month.NOVEMBER, 28), "Proclamation de la République");
        days.put(LocalDate.of(year, Month.DECEMBER, 25), "Noël");
        LocalDate easter = easter(year);
        days.put(easter.plusDays(1), "Lundi de Pâques");
        days.put(easter.plusDays(39), "Ascension");
        days.put(easter.plusDays(50), "Lundi de Pentecôte");
        return days;
    }

    /** Libelle du jour ferie national, ou {@code null}. */
    public static String label(LocalDate date) {
        return of(date.getYear()).get(date);
    }

    public static boolean isHoliday(LocalDate date) {
        return label(date) != null;
    }

    /** Dimanche de Paques (calendrier gregorien, algorithme de Meeus). */
    static LocalDate easter(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
