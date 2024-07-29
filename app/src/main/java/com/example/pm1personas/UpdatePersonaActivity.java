package com.example.pm1personas;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdatePersonaActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;

    private EditText editTextNombres, editTextApellidos, editTextCorreo, editTextFechaNac;
    private ImageView imageViewFoto;
    private Button buttonActualizar, buttonCancelar, buttonSelectFoto;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private String personaId;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_persona);

        // Inicialización de vistas
        editTextNombres = findViewById(R.id.editTextNombres);
        editTextApellidos = findViewById(R.id.editTextApellidos);
        editTextCorreo = findViewById(R.id.editTextCorreo);
        editTextFechaNac = findViewById(R.id.editTextFechaNac);
        imageViewFoto = findViewById(R.id.imageViewFoto);
        buttonActualizar = findViewById(R.id.buttonActualizar);
        buttonCancelar = findViewById(R.id.buttonCancelar);
        buttonSelectFoto = findViewById(R.id.buttonSelectFoto);

        databaseReference = FirebaseDatabase.getInstance().getReference("personas");
        storageReference = FirebaseStorage.getInstance().getReference("fotos");

        personaId = getIntent().getStringExtra("PERSONA_ID");

        loadPersonaData(personaId);

        buttonSelectFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSelectionDialog();
            }
        });

        buttonActualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualizarPersona(personaId);
            }
        });

        buttonCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable("photoUri");
            if (photoUri != null) {
                imageViewFoto.setImageURI(photoUri);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putParcelable("photoUri", photoUri);
        }
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Imagen")
                .setItems(new CharSequence[]{"Tomar Foto", "Elegir desde Galería"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            checkCameraPermission();
                        } else {
                            dispatchPickImageIntent();
                        }
                    }
                });
        builder.create().show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                //photoUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), photoFile);

                photoUri = FileProvider.getUriForFile(this, "com.example.pm1personas.fileprovider", createImageFile());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void dispatchPickImageIntent() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imageViewFoto.setImageURI(photoUri);
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                photoUri = data.getData();
                imageViewFoto.setImageURI(photoUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadPersonaData(String personaId) {
        databaseReference.child(personaId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nombres = dataSnapshot.child("nombres").getValue(String.class);
                    String apellidos = dataSnapshot.child("apellidos").getValue(String.class);
                    String correo = dataSnapshot.child("correo").getValue(String.class);
                    String fechaNac = dataSnapshot.child("fechanac").getValue(String.class);
                    String fotoUrl = dataSnapshot.child("foto").getValue(String.class);

                    editTextNombres.setText(nombres);
                    editTextApellidos.setText(apellidos);
                    editTextCorreo.setText(correo);
                    editTextFechaNac.setText(fechaNac);

                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(UpdatePersonaActivity.this)
                                .load(fotoUrl)
                                .apply(new RequestOptions().placeholder(R.drawable.default_image))
                                .into(imageViewFoto);
                    } else {
                        imageViewFoto.setImageResource(R.drawable.default_image);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar posibles errores
                Toast.makeText(UpdatePersonaActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarPersona(String personaId) {
        String nombres = editTextNombres.getText().toString().trim();
        String apellidos = editTextApellidos.getText().toString().trim();
        String correo = editTextCorreo.getText().toString().trim();
        String fechaNac = editTextFechaNac.getText().toString().trim();
        String fotoPath = null;

        if (photoUri != null) {
            fotoPath = photoUri.getLastPathSegment();
            StorageReference fotoRef = storageReference.child(fotoPath);
            String finalFotoPath = fotoPath;
            fotoRef.putFile(photoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    fotoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            actualizarEnDatabase(personaId, nombres, apellidos, correo, fechaNac, uri.toString());
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(UpdatePersonaActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            actualizarEnDatabase(personaId, nombres, apellidos, correo, fechaNac, fotoPath);
        }
    }

    private void actualizarEnDatabase(String personaId, String nombres, String apellidos, String correo, String fechaNac, String fotoPath) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nombres", nombres);
        updates.put("apellidos", apellidos);
        updates.put("correo", correo);
        updates.put("fechaNac", fechaNac);
        if (fotoPath != null) {
            updates.put("foto", fotoPath);
        }

        databaseReference.child(personaId).updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(UpdatePersonaActivity.this, "Datos actualizados con éxito", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(UpdatePersonaActivity.this, "Error al actualizar datos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}





