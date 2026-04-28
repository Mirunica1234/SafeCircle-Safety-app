package com.example.safecircle;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String CHANNEL_ID = "safecircle_sos_channel";
    private static final int NOTIF_PERMISSION_REQUEST = 101;
    private int notifId = 1000;

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private Button btnEmergency;
    private Button btnDemoCompanion;


    private Marker markerMyself = null;
    private Marker markerCompanion = null;


    private double myLat = 0, myLng = 0;
    private boolean firstCameraDone = false;

    //  live tracking GPS
    private LocationCallback locationCallback;


    private final Set<String> utilizatoriAlertati = new HashSet<>();

    private String myUserId = "Utilizator_Principal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase
        try {
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.e("SafeCircleDebug", "Firebase init error: " + e.getMessage());
        }
        mDatabase = FirebaseDatabase.getInstance().getReference();

        creeazaCanalNotificari();
        cerePermisiuneNotificari();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Butoane
        btnEmergency = findViewById(R.id.btnEmergency);
        btnEmergency.setOnClickListener(v -> proceseazaSOS());

        btnDemoCompanion = findViewById(R.id.btnDemoCompanion);
        btnDemoCompanion.setOnClickListener(v -> simuleazaTovarasa());

        findViewById(R.id.btnBackToMenu).setOnClickListener(v -> finish());


        requestLocationPermission();
        ascultaReteaua();
    }



    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            pornesteLiveGPS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pornesteLiveGPS();
        }
        if (requestCode == NOTIF_PERMISSION_REQUEST) {

        }
    }

    private void pornesteLiveGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location loc = locationResult.getLastLocation();
                if (loc == null) return;

                myLat = loc.getLatitude();
                myLng = loc.getLongitude();

                actualizezaPinulMeu(myLat, myLng, false);


                HashMap<String, Object> data = new HashMap<>();
                data.put("lat", myLat);
                data.put("lng", myLng);
                data.put("status", "SAFE");
                mDatabase.child("users").child(myUserId).setValue(data);

                Log.d("SafeCircleDebug", "GPS update: " + myLat + ", " + myLng);
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
        Log.d("SafeCircleDebug", "Live GPS tracking pornit.");
    }


    private void actualizezaPinulMeu(double lat, double lng, boolean emergency) {
        if (mMap == null) return;
        LatLng pos = new LatLng(lat, lng);

        if (markerMyself == null) {

            markerMyself = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(myUserId)
                    .snippet(emergency ? "SOS!" : "Eu")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            emergency ? BitmapDescriptorFactory.HUE_RED
                                      : BitmapDescriptorFactory.HUE_RED))
                    .zIndex(10f));


            if (!firstCameraDone) {
                firstCameraDone = true;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
            }
        } else {

            markerMyself.setPosition(pos);
            markerMyself.setSnippet(emergency ? "SOS!" : "Eu");
        }

        if (markerMyself != null) markerMyself.showInfoWindow();
    }

    private void creeazaCanalNotificari() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Alerte SafeCircle", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notificări urgențe din cercul tău");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void cerePermisiuneNotificari() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIF_PERMISSION_REQUEST);
            }
        }
    }

    private void trimiteNotificareLocala(String numeUtilizator, double lat, double lng) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("URGENTA SafeCircle!")
                .setContentText(numeUtilizator + " are nevoie de ajutor!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(numeUtilizator + " a apasat SOS!\nLocatie: "
                                + String.format("%.4f", lat) + ", "
                                + String.format("%.4f", lng)))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(Color.RED)
                .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManagerCompat notifManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notifManager.notify(notifId++, builder.build());
            }
        } else {
            notifManager.notify(notifId++, builder.build());
        }
    }

    // ─────────────────────────────────────────────
    // ASCULTARE REȚEA FIREBASE (pentru ceilalți utilizatori)
    // ─────────────────────────────────────────────

    private void ascultaReteaua() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mMap == null) return;

                for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                    String name = userSnap.getKey();
                    Double lat = userSnap.child("lat").getValue(Double.class);
                    Double lng = userSnap.child("lng").getValue(Double.class);
                    String status = userSnap.child("status").getValue(String.class);

                    // Pinul propriu e gestionat de GPS direct — skip
                    if (myUserId.equals(name)) continue;

                    if (lat != null && lng != null) {
                        LatLng pos = new LatLng(lat, lng);
                        float color = "EMERGENCY".equals(status)
                                ? BitmapDescriptorFactory.HUE_RED
                                : BitmapDescriptorFactory.HUE_AZURE;

                        // Actualizăm markerul companion dacă e Ana_Demo, altfel adăugăm nou
                        if ("Ana_Demo".equals(name) && markerCompanion != null) {
                            markerCompanion.setPosition(pos);
                            markerCompanion.setSnippet("Status: " + status);
                            markerCompanion.setIcon(BitmapDescriptorFactory.defaultMarker(color));
                            markerCompanion.showInfoWindow();
                        } else {
                            Marker m = mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(name)
                                    .snippet("Status: " + status)
                                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
                            if (m != null) m.showInfoWindow();
                        }

                        if ("EMERGENCY".equals(status) && !utilizatoriAlertati.contains(name)) {
                            utilizatoriAlertati.add(name);
                            alertaVizuala(name, pos);
                            trimiteNotificareLocala(name, lat, lng);
                        } else if (!"EMERGENCY".equals(status)) {
                            utilizatoriAlertati.remove(name);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SafeCircleDebug", "Firebase read error: " + error.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────
    // BUTON SOS
    // ─────────────────────────────────────────────

    private void proceseazaSOS() {
        Log.d("SafeCircleDebug", "SOS apasat...");

        // Actualizăm pinul propriu vizual imediat
        if (myLat != 0) actualizezaPinulMeu(myLat, myLng, true);

        double sosLat = myLat != 0 ? myLat : 44.4355;
        double sosLng = myLng != 0 ? myLng : 26.1025;

        // Salvăm evenimentul în jurnal
        LogManager.adaugaEvent(this,
                LogManager.TIP_AM_CERUT_AJUTOR,
                "Cerc SafeCircle",
                sosLat, sosLng,
                System.currentTimeMillis(), 0);

        trimiteDateLaFirebase(sosLat, sosLng);
    }

    private void trimiteDateLaFirebase(double lat, double lng) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("lat", lat);
        data.put("lng", lng);
        data.put("status", "EMERGENCY");

        mDatabase.child("users").child(myUserId).setValue(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SafeCircleDebug", "SOS trimis!");
                    btnEmergency.setBackgroundColor(Color.BLACK);
                    btnEmergency.setText("SOS SENT");
                    Toast.makeText(this, "Alerta trimisa cercului tau!", Toast.LENGTH_LONG).show();
                });
    }

    // ─────────────────────────────────────────────
    // DEMO TOVARĂȘĂ — PIN PE HARTĂ + TIMER SOS
    // ─────────────────────────────────────────────

    private void simuleazaTovarasa() {
        btnDemoCompanion.setEnabled(false);

        final double demoLat = 44.4380;
        final double demoLng = 26.0970;
        final String demoName = "Ana_Demo";

        // Desenăm pinul tovarășei pe hartă IMEDIAT (albastru = SAFE)
        if (mMap != null) {
            LatLng pos = new LatLng(demoLat, demoLng);
            if (markerCompanion != null) markerCompanion.remove();
            markerCompanion = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(demoName)
                    .snippet("Status: SAFE")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(9f));
            if (markerCompanion != null) markerCompanion.showInfoWindow();
        }

        // Scriem și în Firebase (pentru celelalte instanțe ale app-ului)
        HashMap<String, Object> dataSafe = new HashMap<>();
        dataSafe.put("lat", demoLat);
        dataSafe.put("lng", demoLng);
        dataSafe.put("status", "SAFE");
        mDatabase.child("users").child(demoName).setValue(dataSafe);

        btnDemoCompanion.setText("Ana e OK... (5s)");
        Toast.makeText(this, "Ana s-a conectat! Urmareste harta...", Toast.LENGTH_SHORT).show();

        // După 5 sec → SOS
        new Handler(getMainLooper()).postDelayed(() -> {
            double emergLat = demoLat + 0.002;
            double emergLng = demoLng - 0.001;
            LatLng emergPos = new LatLng(emergLat, emergLng);

            // Actualizăm pinul pe hartă imediat → roșu
            if (markerCompanion != null && mMap != null) {
                markerCompanion.setPosition(emergPos);
                markerCompanion.setSnippet("Status: EMERGENCY");
                markerCompanion.setIcon(
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                markerCompanion.showInfoWindow();
            }

            // Scriem în Firebase
            HashMap<String, Object> dataEmergency = new HashMap<>();
            dataEmergency.put("lat", emergLat);
            dataEmergency.put("lng", emergLng);
            dataEmergency.put("status", "EMERGENCY");
            mDatabase.child("users").child(demoName).setValue(dataEmergency);

            // Alertă vizuală + notificare
            alertaVizuala(demoName, emergPos);
            trimiteNotificareLocala(demoName, emergLat, emergLng);
            utilizatoriAlertati.add(demoName);

            Toast.makeText(this, "Ana a trimis SOS!", Toast.LENGTH_LONG).show();
            btnDemoCompanion.setText("Demo Tovarasa");
            btnDemoCompanion.setEnabled(true);

            Log.d("SafeCircleDebug", "Ana_Demo -> EMERGENCY pe harta");
        }, 5000);
    }

    // ─────────────────────────────────────────────
    // ALERTĂ VIZUALĂ
    // ─────────────────────────────────────────────

    private void alertaVizuala(String nume, LatLng pozitie) {
        if (mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pozitie, 16));

        // Salvăm evenimentul în jurnal (sosTime = acum, helpedTime = 0 până la confirmare)
        final long sosTime = System.currentTimeMillis();
        final String[] logId = {null};
        logId[0] = LogManager.adaugaEvent(this,
                LogManager.TIP_AM_AJUTAT,
                nume,
                pozitie.latitude, pozitie.longitude,
                sosTime, 0);

        new AlertDialog.Builder(this)
                .setTitle("URGENTA!")
                .setMessage(nume + " are nevoie de ajutor!\nApasa 'Intervin acum' pentru a naviga.")
                .setPositiveButton("Intervin acum", (dialog, which) -> {
                    // Marcăm ora la care s-a intervenit efectiv
                    if (logId[0] != null) {
                        LogManager.marcheazaAjutat(this, logId[0]);
                    }
                })
                .setNegativeButton("Inchide", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // ─────────────────────────────────────────────
    // HARTĂ
    // ─────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(false); // oprim blue dot — avem pinul nostru roșu
        }

        LatLng start = new LatLng(44.4268, 26.1025);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 13));
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    // ─────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}