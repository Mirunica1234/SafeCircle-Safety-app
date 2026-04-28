package com.example.safecircle;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;


public class LogManager {

    private static final String PREFS = "safecircle_logs";
    private static final String KEY   = "events";

    // Tipuri de evenimente
    public static final String TIP_AM_AJUTAT    = "I_HELPED";      // eu am ajutat pe cineva
    public static final String TIP_AM_CERUT_AJUTOR = "I_NEEDED_HELP"; // eu am cerut ajutor

    /**
     * Adaugă un eveniment nou în log.
     *
     * @param ctx       context
     * @param tip       TIP_AM_AJUTAT sau TIP_AM_CERUT_AJUTOR
     * @param persoana  numele celeilalte persoane
     * @param lat       latitudine eveniment
     * @param lng       longitudine eveniment
     * @param sosTimpMs timestamp când a fost apăsat SOS (ms)
     * @param ajutatTimpMs timestamp când s-a intervenit (ms), 0 dacă necunoscut
     * @return id-ul eventului nou creat
     */
    public static String adaugaEvent(Context ctx, String tip, String persoana,
                                     double lat, double lng,
                                     long sosTimpMs, long ajutatTimpMs) {
        try {
            JSONArray logs = getLogs(ctx);
            String id = UUID.randomUUID().toString();
            JSONObject entry = new JSONObject();
            entry.put("id", id);
            entry.put("tip", tip);
            entry.put("persoana", persoana);
            entry.put("lat", lat);
            entry.put("lng", lng);
            entry.put("sosTimp", sosTimpMs);
            entry.put("ajutatTimp", ajutatTimpMs);
            logs.put(entry);
            saveLogs(ctx, logs);
            return id;
        } catch (Exception e) {
            return null;
        }
    }


    public static void marcheazaAjutat(Context ctx, String id) {
        try {
            JSONArray logs = getLogs(ctx);
            for (int i = 0; i < logs.length(); i++) {
                JSONObject o = logs.getJSONObject(i);
                if (id.equals(o.optString("id"))) {
                    o.put("ajutatTimp", System.currentTimeMillis());
                    break;
                }
            }
            saveLogs(ctx, logs);
        } catch (Exception ignored) {}
    }


    public static void stergeEvent(Context ctx, String id) {
        try {
            JSONArray logs = getLogs(ctx);
            JSONArray result = new JSONArray();
            for (int i = 0; i < logs.length(); i++) {
                JSONObject o = logs.getJSONObject(i);
                if (!id.equals(o.optString("id"))) result.put(o);
            }
            saveLogs(ctx, result);
        } catch (Exception ignored) {}
    }


    public static void stergeTot(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY).apply();
    }


    public static JSONArray getLogs(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return new JSONArray(p.getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void saveLogs(Context ctx, JSONArray logs) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, logs.toString()).apply();
    }
}
