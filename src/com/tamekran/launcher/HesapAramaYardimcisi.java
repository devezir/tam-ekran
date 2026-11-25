package com.tamekran.launcher;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tam Ekran - Arama Çubuğu Akıllı Hesap Makinesi & Hızlı İfade Çözücü
 * Arama çubuğuna aritmetik işlem yazıldığında (25*4, 1200+350, 500*18%, (40+10)/2 vb.)
 * sıfır kütüphane bağımlılığıyla anında sonucu çözer ve kullanıcıya sunar.
 */
public class HesapAramaYardimcisi {

    public static class HesapSonucu {
        public boolean basarili;
        public String ifade;
        public double deger;
        public String formatliSonuc;
    }

    /**
     * Verilen arama metninin bir matematiksel ifade olup olmadığını inceler ve hesaplar.
     */
    public static HesapSonucu hesapla(String girdi) {
        HesapSonucu sonuc = new HesapSonucu();
        sonuc.basarili = false;
        if (girdi == null) return sonuc;

        String temiz = girdi.trim().replace(" ", "").replace("x", "*").replace("X", "*").replace("÷", "/").replace(":", "/").replace(",", ".");
        if (temiz.length() < 2) return sonuc;

        // En az bir işlem operatörü (+, -, *, /, %) veya parantez içermeli
        boolean operatorVar = false;
        for (int i = 0; i < temiz.length(); i++) {
            char c = temiz.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')') {
                // Sadece en başta tek bir eksi varsa henüz tek bir negatif sayıdır, işlem değildir
                if (c == '-' && i == 0) continue;
                operatorVar = true;
                break;
            }
        }
        if (!operatorVar) return sonuc;

        // Yalnızca rakam, nokta, operatör ve parantez karakterlerinden mi oluşuyor?
        for (int i = 0; i < temiz.length(); i++) {
            char c = temiz.charAt(i);
            if (!((c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')')) {
                return sonuc; // Harf veya alakasız karakter var, matematik işlemi değil
            }
        }

        // Yüzde işareti dönüşümü: örn. 200*18% -> 200*(18/100) veya 50% -> (50/100)
        temiz = yuzdeleriCoz(temiz);

        try {
            Cozucu cozucu = new Cozucu(temiz);
            double val = cozucu.parse();
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                return sonuc;
            }

            sonuc.basarili = true;
            sonuc.ifade = girdi.trim();
            sonuc.deger = val;

            DecimalFormatSymbols semboller = new DecimalFormatSymbols(Locale.US);
            semboller.setDecimalSeparator(',');
            semboller.setGroupingSeparator('.');
            DecimalFormat df;
            if (val == Math.floor(val) && !Double.isInfinite(val)) {
                df = new DecimalFormat("#,##0", semboller);
            } else {
                df = new DecimalFormat("#,##0.####", semboller);
            }
            sonuc.formatliSonuc = df.format(val);
        } catch (Throwable t) {
            sonuc.basarili = false;
        }

        return sonuc;
    }

    private static String yuzdeleriCoz(String str) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '%') {
                sb.append("/100");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Saf Java Recursive Descent Matematik İfade Ayrıştırıcısı
     * Toplama, Çıkarma, Çarpma, Bölme ve Parantez işlem önceliklerini eksiksiz uygular.
     */
    private static class Cozucu {
        private final String str;
        private int pos = -1;
        private int ch;

        public Cozucu(String str) {
            this.str = str;
        }

        private void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        public double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < str.length()) throw new RuntimeException("Beklenmeyen karakter: " + (char) ch);
            return x;
        }

        // Grammar:
        // expression = term | expression `+` term | expression `-` term
        // term = factor | term `*` factor | term `/` factor
        // factor = `+` factor | `-` factor | `(` expression `)` | number
        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) {
                    double bolen = parseFactor();
                    if (bolen == 0.0) throw new ArithmeticException("Sıfıra bölme hatası");
                    x /= bolen;
                } else return x;
            }
        }

        private double parseFactor() {
            if (eat('+')) return +parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Kapanmayan parantez");
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Beklenmeyen ifade: " + (char) ch);
            }

            return x;
        }
    }
}
