package com.example.pm1personas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReadPersonasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PersonaAdapter personaAdapter;
    private List<Persona> personaList;
    private Button viewPersonasButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_personas);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        personaList = new ArrayList<>();
        personaAdapter = new PersonaAdapter(this, personaList);
        recyclerView.setAdapter(personaAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("personas");
        loadPersonas();
    }

    private void loadPersonas() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("personas");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                personaList.clear();
                for (DataSnapshot personaSnapshot : snapshot.getChildren()) {
                    Persona persona = personaSnapshot.getValue(Persona.class);
                    personaList.add(persona);
                }

                if (personaAdapter == null) {
                    personaAdapter = new PersonaAdapter(ReadPersonasActivity.this, personaList);
                    recyclerView.setAdapter(personaAdapter);
                } else {
                    personaAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReadPersonasActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

