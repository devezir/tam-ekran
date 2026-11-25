package com.tamekran.launcher;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Tam Ekran - İnteraktif Canlı Güvenli Alan Kalibratörü
 * Kullanıcı masaüstünü arkada canlı izlerken üst, alt, sol ve sağ güvenli alan kırpmasını ayarlar.
 */
public class GuvenliAlanKalibratoru {

    public static void goster(final AnaEkranActivity aktivite) {
        if (aktivite == null || aktivite.isFinishing()) return;

        final int baslangicSol = GuvenliAlanYoneticisi.getSol(aktivite);
        final int baslangicUst = GuvenliAlanYoneticisi.getUst(aktivite);
        final int baslangicSag = GuvenliAlanYoneticisi.getSag(aktivite);
        final int baslangicAlt = GuvenliAlanYoneticisi.getAlt(aktivite);

        final int[] sol = new int[]{baslangicSol};
        final int[] ust = new int[]{baslangicUst};
        final int[] sag = new int[]{baslangicSag};
        final int[] alt = new int[]{baslangicAlt};

        final Dialog dialog = new Dialog(aktivite);
        dialog.setContentView(R.layout.diyalog_guvenli_alan);
        ArayuzYardimcisi.kalibratorPenceresiAyarla(dialog, ArayuzYardimcisi.isKalibratorKonumAltta(), 0.12f);

        // Kılavuz çerçeveyi masaüstünde görünür yap
        aktivite.guvenliAlanKilavuzunuGoster(sol[0], ust[0], sag[0], alt[0]);

        final Button dugmeKonum = (Button) dialog.findViewById(R.id.dugme_konum);
        ArayuzYardimcisi.baglaKalibratorKonumDugmesi(dialog, dugmeKonum);

        // 1. ÜST (TOP)
        final TextView yaziUst = (TextView) dialog.findViewById(R.id.yazi_etiket_ust);
        final SeekBar seekUst = (SeekBar) dialog.findViewById(R.id.seekbar_ust);
        final Button dugmeUstEksi = (Button) dialog.findViewById(R.id.dugme_ust_eksi);
        final Button dugmeUstArti = (Button) dialog.findViewById(R.id.dugme_ust_arti);

        // 2. ALT (BOTTOM)
        final TextView yaziAlt = (TextView) dialog.findViewById(R.id.yazi_etiket_alt);
        final SeekBar seekAlt = (SeekBar) dialog.findViewById(R.id.seekbar_alt);
        final Button dugmeAltEksi = (Button) dialog.findViewById(R.id.dugme_alt_eksi);
        final Button dugmeAltArti = (Button) dialog.findViewById(R.id.dugme_alt_arti);

        // 3. SOL (LEFT)
        final TextView yaziSol = (TextView) dialog.findViewById(R.id.yazi_etiket_sol);
        final SeekBar seekSol = (SeekBar) dialog.findViewById(R.id.seekbar_sol);
        final Button dugmeSolEksi = (Button) dialog.findViewById(R.id.dugme_sol_eksi);
        final Button dugmeSolArti = (Button) dialog.findViewById(R.id.dugme_sol_arti);

        // 4. SAĞ (RIGHT)
        final TextView yaziSag = (TextView) dialog.findViewById(R.id.yazi_etiket_sag);
        final SeekBar seekSag = (SeekBar) dialog.findViewById(R.id.seekbar_sag);
        final Button dugmeSagEksi = (Button) dialog.findViewById(R.id.dugme_sag_eksi);
        final Button dugmeSagArti = (Button) dialog.findViewById(R.id.dugme_sag_arti);

        final Runnable guncelleGorunumler = new Runnable() {
            @Override
            public void run() {
                if (yaziUst != null) yaziUst.setText("⬆️ Üst Boşluk (Top): " + ust[0] + " dp");
                if (yaziAlt != null) yaziAlt.setText("⬇️ Alt Boşluk (Bottom): " + alt[0] + " dp");
                if (yaziSol != null) yaziSol.setText("⬅️ Sol Boşluk (Left): " + sol[0] + " dp");
                if (yaziSag != null) yaziSag.setText("➡️ Sağ Boşluk (Right): " + sag[0] + " dp");

                // Canlı masaüstü ve kılavuz çerçeve güncellemesi
                GuvenliAlanYoneticisi.uygulaPiksel(aktivite, sol[0], ust[0], sag[0], alt[0]);
                aktivite.guvenliAlanKilavuzunuGuncelle(sol[0], ust[0], sag[0], alt[0]);
            }
        };

        // Adımlayıcıları bağla
        ArayuzYardimcisi.baglaAyarAdimlayici(seekUst, dugmeUstEksi, dugmeUstArti, ust, 0, 120, guncelleGorunumler);
        ArayuzYardimcisi.baglaAyarAdimlayici(seekAlt, dugmeAltEksi, dugmeAltArti, alt, 0, 120, guncelleGorunumler);
        ArayuzYardimcisi.baglaAyarAdimlayici(seekSol, dugmeSolEksi, dugmeSolArti, sol, 0, 100, guncelleGorunumler);
        ArayuzYardimcisi.baglaAyarAdimlayici(seekSag, dugmeSagEksi, dugmeSagArti, sag, 0, 100, guncelleGorunumler);
        guncelleGorunumler.run();

        // Sıfırla
        Button dugmeSifirla = (Button) dialog.findViewById(R.id.dugme_sifirla);
        if (dugmeSifirla != null) {
            dugmeSifirla.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    sol[0] = 0; ust[0] = 0; sag[0] = 0; alt[0] = 0;
                    if (seekUst != null) seekUst.setProgress(0);
                    if (seekAlt != null) seekAlt.setProgress(0);
                    if (seekSol != null) seekSol.setProgress(0);
                    if (seekSag != null) seekSag.setProgress(0);
                    guncelleGorunumler.run();
                    ArayuzYardimcisi.toast(aktivite, "Güvenli alan sıfırlandı.");
                }
            });
        }

        // İptal
        Button dugmeIptal = (Button) dialog.findViewById(R.id.dugme_iptal);
        if (dugmeIptal != null) {
            dugmeIptal.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dialog.cancel();
                }
            });
        }

        // Kaydet
        Button dugmeKaydet = (Button) dialog.findViewById(R.id.dugme_kaydet);
        if (dugmeKaydet != null) {
            dugmeKaydet.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    GuvenliAlanYoneticisi.kaydet(aktivite, sol[0], ust[0], sag[0], alt[0]);
                    GuvenliAlanYoneticisi.uygula(aktivite);
                    aktivite.guvenliAlanKilavuzunuGizle();
                    dialog.dismiss();
                    ArayuzYardimcisi.toast(aktivite, "Güvenli alan ayarları kaydedildi.");
                }
            });
        }

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                // Değişiklikleri geri al
                GuvenliAlanYoneticisi.uygulaPiksel(aktivite, baslangicSol, baslangicUst, baslangicSag, baslangicAlt);
                aktivite.guvenliAlanKilavuzunuGizle();
            }
        });

        dialog.show();
    }
}
