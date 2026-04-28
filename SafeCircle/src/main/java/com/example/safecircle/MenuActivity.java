package com.example.safecircle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnMap      = findViewById(R.id.btnGoToMap);
        Button btnContacts = findViewById(R.id.btnContacts);
        Button btnLogs     = findViewById(R.id.btnLogs);

        // Deschide Harta
        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(MenuActivity.this, MainActivity.class));
        });

        // Placeholder Contacte
        btnContacts.setOnClickListener(v ->
                Toast.makeText(this, "Functie disponibila in curand!", Toast.LENGTH_SHORT).show());

        // Jurnal Evenimente
        btnLogs.setOnClickListener(v ->
                startActivity(new Intent(MenuActivity.this, LogsActivity.class)));
    }
}