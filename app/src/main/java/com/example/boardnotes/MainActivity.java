package com.example.boardnotes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.boardnotes.databinding.ActivityMainBinding;
import com.example.boardnotes.ui.capture.CaptureActivity;
import com.example.boardnotes.ui.notelist.NoteListViewModel;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private NoteListViewModel viewModel;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    viewModel.importImage(uri);
                }
            });

    private final ActivityResultLauncher<String> requestCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(NoteListViewModel.class);

        viewModel.getUiState().observe(this, state -> binding.status.setText(getString(
                R.string.status_format,
                state.noteCount(),
                state.textCount,
                state.syncedCount,
                state.queueSize,
                getString(state.isOnline ? R.string.online : R.string.offline))));

        binding.addNote.setOnClickListener(v -> viewModel.addTestNote());
        binding.deleteOldest.setOnClickListener(v -> viewModel.deleteOldest());

        binding.importImage.setOnClickListener(v -> pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        binding.captureImage.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestCamera.launch(Manifest.permission.CAMERA);
            }
        });

        signInAnonymously();
    }

    private void signInAnonymously() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            Log.i("BoardNotes", "Already signed in: " + auth.getCurrentUser().getUid());
            pingGemini();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                        Log.i("BoardNotes", "Signed in: " + result.getUser().getUid());
                        pingGemini();
                })
                .addOnFailureListener(e ->
                        Log.e("BoardNotes", "Sign-in failed", e));
    }

    private void openCamera() {
        startActivity(new Intent(this, CaptureActivity.class));
    }

    private void pingGemini() {
        GenerativeModel model = FirebaseAI.getInstance()
                .generativeModel("gemini-3.8-flash");
        GenerativeModelFutures futures = GenerativeModelFutures.from(model);

        Content prompt = new Content.Builder()
                .addText("Reply with exactly one word: ready")
                .build();

        long start = System.currentTimeMillis();

        Futures.addCallback(
                futures.generateContent(prompt),
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        Log.i("BoardNotes", "Gemini OK in "
                                + (System.currentTimeMillis() - start) + " ms: "
                                + result.getText());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e("BoardNotes", "Gemini failed", t);
                    }
                },
                Executors.newSingleThreadExecutor());
    }
}