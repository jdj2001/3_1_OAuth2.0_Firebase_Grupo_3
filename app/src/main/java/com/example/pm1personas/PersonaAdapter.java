package com.example.pm1personas;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PersonaAdapter extends RecyclerView.Adapter<PersonaAdapter.PersonaViewHolder> {

    private Context context;
    private List<Persona> personaList;
    private DatabaseReference databaseReference;

    public PersonaAdapter(Context context, List<Persona> personaList) {
        this.context = context;
        this.personaList = personaList;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("personas");
    }

    @NonNull
    @Override
    public PersonaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_persona, parent, false);
        return new PersonaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonaViewHolder holder, int position) {
        Persona persona = personaList.get(position);
        holder.textViewNombre.setText(persona.getNombres());
        holder.textViewApellido.setText(persona.getApellidos());
        holder.textViewCorreo.setText(persona.getCorreo());
        holder.textViewFechaNac.setText(persona.getFechanac());

        // Obtener la URL de la imagen desde la base de datos
        DatabaseReference personaRef = databaseReference.child(persona.getId());
        personaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String fotoUrl = dataSnapshot.child("foto").getValue(String.class);

                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(context)
                                .load(fotoUrl)
                                .placeholder(R.drawable.default_image)
                                .error(R.drawable.default_image)
                                .into(holder.imageView);
                    } else {
                        holder.imageView.setImageResource(R.drawable.default_image);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("PersonaAdapter", "Error al cargar la imagen", databaseError.toException());
                holder.imageView.setImageResource(R.drawable.default_image);
            }
        });

        holder.buttonActualizar.setOnClickListener(v -> {
            Intent intent = new Intent(context, UpdatePersonaActivity.class);
            intent.putExtra("PERSONA_ID", persona.getId());
            context.startActivity(intent);
        });

        holder.buttonEliminar.setOnClickListener(v -> {
            eliminarPersona(persona.getId());
        });
    }

    @Override
    public int getItemCount() {
        return personaList.size();
    }

    private void eliminarPersona(String personaId) {
        databaseReference.child(personaId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Persona eliminada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class PersonaViewHolder extends RecyclerView.ViewHolder {

        TextView textViewNombre, textViewApellido, textViewCorreo, textViewFechaNac;
        ImageView imageView;
        Button buttonActualizar, buttonEliminar;

        public PersonaViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNombre = itemView.findViewById(R.id.textViewNombre);
            textViewApellido = itemView.findViewById(R.id.textViewApellido);
            textViewCorreo = itemView.findViewById(R.id.textViewCorreo);
            textViewFechaNac = itemView.findViewById(R.id.textViewFechaNac);
            imageView = itemView.findViewById(R.id.imageView);
            buttonActualizar = itemView.findViewById(R.id.updateButton);
            buttonEliminar = itemView.findViewById(R.id.deleteButton);
        }
    }
}




