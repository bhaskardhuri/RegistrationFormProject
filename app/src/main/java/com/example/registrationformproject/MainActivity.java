package com.example.registrationformproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSubmit;
    private LinearProgressIndicator progressBar;
    private TextView tvProgress;
    private ImageView ivUploadedImage;

    private int progress = 0;
    private Uri imageUri;
    private String uploadedImageBase64 = null;



    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        tvProgress = findViewById(R.id.tvProgress);
        ivUploadedImage = findViewById(R.id.ivUploadedImage);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Add text watchers to update progress
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateProgress();
            }


            @Override
            public void afterTextChanged(Editable s) {}
        };

        etName.addTextChangedListener(textWatcher);
        etEmail.addTextChangedListener(textWatcher);
        etPassword.addTextChangedListener(textWatcher);
        etConfirmPassword.addTextChangedListener(textWatcher);

        // Button to choose an image
        ivUploadedImage.setOnClickListener(v -> {
            chooseImage();
            // Trigger progress update after choosing an image
            ivUploadedImage.postDelayed(this::updateProgress, 500);
        });

        // Submit button
        btnSubmit.setOnClickListener(v -> submitDataToFirestore());
    }

    private void updateProgress() {
        progress = 0;
        if (ivUploadedImage.getDrawable() != null) { // Ensures image is set
            progress += 10;
        }

        if (!etName.getText().toString().isEmpty()) {
            progress += 20;
        }
        if (!etEmail.getText().toString().isEmpty()) {
            progress += 20;
        }
        if (!etPassword.getText().toString().isEmpty() && etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            progress += 50;
        }

        progressBar.setProgressCompat(progress, true);
        tvProgress.setText(progress + "%");
        btnSubmit.setEnabled(progress == 100);
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivUploadedImage.setImageURI(imageUri);
            convertImageToBase64();
        }
    }

    private void convertImageToBase64() {
        try {
            // Open input stream for the image
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            // Convert Bitmap to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            uploadedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Toast.makeText(MainActivity.this, "Image converted to Base64", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error in converting image", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitDataToFirestore() {
        if (uploadedImageBase64 == null) {
            Toast.makeText(this, "Please upload an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure that the passwords match before submitting
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", etName.getText().toString());
        userData.put("email", etEmail.getText().toString());
        userData.put("password", etPassword.getText().toString());
        userData.put("imageBase64", uploadedImageBase64);

        firestore.collection("users").add(userData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(false);  // Disable submit button after success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
