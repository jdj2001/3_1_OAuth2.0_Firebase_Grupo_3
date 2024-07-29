package com.example.pm1personas;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class CreatePersonaActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PERMISSION_CAMERA = 101;

    private EditText nombresEditText, apellidosEditText, correoEditText, fechanacEditText;
    private Button createButton, selectPhotoButton, capturePhotoButton;
    private ImageView selectedImageView;
    private ProgressBar progressBar;
    private Uri selectedImageUri;
    private Button viewPersonasButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_persona);

        mAuth = FirebaseAuth.getInstance();

        nombresEditText = findViewById(R.id.nombres);
        apellidosEditText = findViewById(R.id.apellidos);
        correoEditText = findViewById(R.id.correo);
        fechanacEditText = findViewById(R.id.fechanac);
        createButton = findViewById(R.id.createButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);
        capturePhotoButton = findViewById(R.id.capturePhotoButton);
        selectedImageView = findViewById(R.id.selectedImage);
        progressBar = findViewById(R.id.progressBar);

        //Button logoutButton = findViewById(R.id.btn_logout);
        //logoutButton.setOnClickListener(v -> logout());

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPersona();
            }
        });

        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        capturePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission();
            }
        });

        viewPersonasButton = findViewById(R.id.viewPersonasButton);
        viewPersonasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreatePersonaActivity.this, ReadPersonasActivity.class);
                startActivity(intent);
            }
        });
    }

    /*private void logout() {
        mAuth.signOut();

        SharedPreferences sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(CreatePersonaActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }*/


    private void logout() {
        mAuth.signOut();

        Intent intent = new Intent(CreatePersonaActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void createPersona() {
        String nombres = nombresEditText.getText().toString().trim();
        String apellidos = apellidosEditText.getText().toString().trim();
        String correo = correoEditText.getText().toString().trim();
        String fechanac = fechanacEditText.getText().toString().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || fechanac.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String personaId = UUID.randomUUID().toString();
        Persona persona = new Persona(personaId, nombres, apellidos, correo, fechanac, null);

        if (selectedImageUri != null) {
            uploadImageToFirebase(personaId, new OnImageUploadListener() {
                @Override
                public void onImageUploaded(String profileImageUrl) {
                    persona.setFoto(profileImageUrl);
                    savePersonaData(persona);
                }

                @Override
                public void onImageUploadFailed() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreatePersonaActivity.this, "Fallo al subir la imagen de perfil.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            savePersonaData(persona);
        }
    }

    private void savePersonaData(Persona persona) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("personas").child(persona.getId());
        databaseReference.setValue(persona).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(CreatePersonaActivity.this, "Persona creada exitosamente.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CreatePersonaActivity.this, "Error al crear la persona.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadImageToFirebase(String personaId, OnImageUploadListener listener) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profile_images").child(personaId + ".jpg");
        storageReference.putFile(selectedImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String profileImageUrl = uri.toString();
                            listener.onImageUploaded(profileImageUrl);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onImageUploadFailed();
                        }
                    });
                } else {
                    listener.onImageUploadFailed();
                }
            }
        });
    }

    private interface OnImageUploadListener {
        void onImageUploaded(String profileImageUrl);
        void onImageUploadFailed();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
            selectedImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            selectedImageView.setImageURI(selectedImageUri);  // Mostrar la imagen seleccionada
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            selectedImageView.setImageURI(selectedImageUri);  // Mostrar la imagen capturada
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}


