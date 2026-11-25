package com.tamekran.launcher;

import java.util.Locale;

/**
 * Tam Ekran - T9 Akıllı Tuş Takımı Arama Motoru
 * Sayısal tuş takımı aramasıyla (2=ABC, 3=DEF...) uygulamaları
 * Türkçe karakter ve baş harf (initials) desteğiyle anında filtreler.
 */
public class T9AramaYardimcisi {

    public static char harfiT9RakaminaCevir(char ch) {
        char c = Character.toLowerCase(ch);
        switch (c) {
            case 'a': case 'b': case 'c': case 'ç':
                return '2';
            case 'd': case 'e': case 'f':
                return '3';
            case 'g': case 'ğ': case 'h': case 'ı': case 'i':
                return '4';
            case 'j': case 'k': case 'l':
                return '5';
            case 'm': case 'n': case 'o': case 'ö':
                return '6';
            case 'p': case 'q': case 'r': case 's': case 'ş':
                return '7';
            case 't': case 'u': case 'ü': case 'v':
                return '8';
            case 'w': case 'x': case 'y': case 'z':
                return '9';
            case ' ': case '-': case '_':
                return '0';
            default:
                if (Character.isDigit(c)) return c;
                return '?';
        }
    }

    public static String metniT9KoduYap(String metin) {
        if (metin == null) return "";
        StringBuilder sb = new StringBuilder(metin.length());
        for (int i = 0; i < metin.length(); i++) {
            char t9 = harfiT9RakaminaCevir(metin.charAt(i));
            if (t9 != '?') {
                sb.append(t9);
            }
        }
        return sb.toString();
    }

    /**
     * Aranan sayı dizisinin uygulama ismiyle veya kelime baş harfleriyle eşleşip eşleşmediğini denetler
     */
    public static boolean eslesiyorMu(String t9Sorgu, String appEtiket) {
        if (t9Sorgu == null || t9Sorgu.length() == 0) return true;
        if (appEtiket == null || appEtiket.length() == 0) return false;

        // Yalnızca rakam içeriyorsa T9 kontrolü yap
        for (int i = 0; i < t9Sorgu.length(); i++) {
            if (!Character.isDigit(t9Sorgu.charAt(i))) {
                return false;
            }
        }

        // 1. Düz T9 eşleşmesi
        String tamT9 = metniT9KoduYap(appEtiket);
        if (tamT9.contains(t9Sorgu)) {
            return true;
        }

        // 2. Baş Harfler (Initials) T9 eşleşmesi (Örn: "Google Play" -> GP -> 47)
        String[] kelimeler = appEtiket.split("[\\s\\-_]+");
        if (kelimeler.length > 1) {
            StringBuilder basHarflerT9 = new StringBuilder();
            for (String k : kelimeler) {
                if (k.length() > 0) {
                    basHarflerT9.append(harfiT9RakaminaCevir(k.charAt(0)));
                }
            }
            if (basHarflerT9.toString().startsWith(t9Sorgu)) {
                return true;
            }
        }

        return false;
    }
}
