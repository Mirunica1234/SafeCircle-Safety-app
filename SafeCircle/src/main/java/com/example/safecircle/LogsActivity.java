package com.example.safecircle;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {

    private LinearLayout logsContainer;
    private LinearLayout emptyState;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        logsContainer = findViewById(R.id.logsContainer);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearAll).setOnClickListener(v -> confirmClearAll());

        afiseazaLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        afiseazaLogs();
    }

    // ─────────────────────────────────────────────
    // Construire listă
    // ─────────────────────────────────────────────

    private void afiseazaLogs() {
        logsContainer.removeAllViews();
        JSONArray logs = LogManager.getLogs(this);

        if (logs.length() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            findViewById(R.id.scrollLogs).setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        findViewById(R.id.scrollLogs).setVisibility(View.VISIBLE);

        // Afișăm cele mai noi primele
        for (int i = logs.length() - 1; i >= 0; i--) {
            try {
                logsContainer.addView(construiesteCard(logs.getJSONObject(i)));
            } catch (Exception ignored) {}
        }
    }

    private View construiesteCard(JSONObject entry) throws Exception {
        String id        = entry.optString("id");
        String tip       = entry.optString("tip");
        String persoana  = entry.optString("persoana", "Necunoscut");
        double lat       = entry.optDouble("lat", 0);
        double lng       = entry.optDouble("lng", 0);
        long   sosTimp   = entry.optLong("sosTimp", 0);
        long   ajutatTimp = entry.optLong("ajutatTimp", 0);

        boolean eAjutat = LogManager.TIP_AM_AJUTAT.equals(tip);

        // ── Card container ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            card.setElevation(dp(3));

        // Fundal card cu colț rotunjit + bandă colorată stânga
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);

        // ── Rând 1: emoji tip + titlu + buton delete ──
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        row1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Badge tip (roșu / verde)
        TextView badge = new TextView(this);
        int badgeColor = eAjutat ? Color.parseColor("#2E7D32") : Color.parseColor("#B71C1C");
        String badgeText = eAjutat ? "✅ Am ajutat" : "🆘 Am cerut ajutor";
        badge.setText(badgeText);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(11f);
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(badgeColor);
        badgeBg.setCornerRadius(dp(20));
        badge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        badge.setLayoutParams(badgeParams);
        row1.addView(badge);

        // Spacer
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        row1.addView(spacer);

        // Buton delete ❌
        Button btnDelete = new Button(this);
        btnDelete.setText("❌");
        btnDelete.setTextSize(16f);
        btnDelete.setPadding(dp(4), 0, dp(4), 0);
        btnDelete.setBackground(null);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setOnClickListener(v -> {
            LogManager.stergeEvent(LogsActivity.this, id);
            afiseazaLogs();
        });
        row1.addView(btnDelete);

        card.addView(row1);

        // ── Separator ──
        card.addView(divider());

        // ── Rând: Persoana ──
        card.addView(randInfo("👤  Persoana", persoana, Color.parseColor("#1A1A2E"), true));

        // ── Rând: Coordonate ──
        String coordText = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
        card.addView(randInfo("📍  Coordonate", coordText, Color.parseColor("#37474F"), false));

        // ── Rând: Ora SOS ──
        String sosText = sosTimp > 0 ? sdf.format(new Date(sosTimp)) : "—";
        card.addView(randInfo("🚨  Ping SOS", sosText, Color.parseColor("#C62828"), false));

        // ── Rând: Ora ajutat ──
        String ajutatText = ajutatTimp > 0 ? sdf.format(new Date(ajutatTimp)) : "Neconfirmat";
        int ajutatColor = ajutatTimp > 0 ? Color.parseColor("#2E7D32") : Color.parseColor("#9E9E9E");
        card.addView(randInfo("🤝  Ajutat la", ajutatText, ajutatColor, false));

        return card;
    }

    // ── Row helper ──
    private LinearLayout randInfo(String cheie, String valoare, int culoareValoare, boolean bold) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, 0);
        row.setLayoutParams(params);

        TextView tvKey = new TextView(this);
        tvKey.setText(cheie);
        tvKey.setTextSize(12f);
        tvKey.setTextColor(Color.parseColor("#757575"));
        tvKey.setLayoutParams(new LinearLayout.LayoutParams(dp(120), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(tvKey);

        TextView tvVal = new TextView(this);
        tvVal.setText(valoare);
        tvVal.setTextSize(13f);
        tvVal.setTextColor(culoareValoare);
        if (bold) tvVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvVal);

        return row;
    }

    // ── Divider ──
    private View divider() {
        View d = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        p.setMargins(0, dp(10), 0, dp(4));
        d.setLayoutParams(p);
        d.setBackgroundColor(Color.parseColor("#F0F0F0"));
        return d;
    }

    // ─────────────────────────────────────────────
    // Clear All
    // ─────────────────────────────────────────────

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Stergi tot jurnalul?")
                .setMessage("Toate evenimentele vor fi sterse definitiv.")
                .setPositiveButton("Sterge tot", (d, w) -> {
                    LogManager.stergeTot(this);
                    afiseazaLogs();
                })
                .setNegativeButton("Anuleaza", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // ─────────────────────────────────────────────
    // Utils
    // ─────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
