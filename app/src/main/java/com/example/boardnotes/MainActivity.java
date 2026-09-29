package com.example.boardnotes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.example.boardnotes.ui.notelist.NoteListAdapter;
import com.example.boardnotes.ui.notelist.NoteListUiState;
import com.example.boardnotes.ui.notelist.NoteListViewModel;
import com.google.firebase.auth.FirebaseAuth;

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
        NoteListAdapter adapter = new NoteListAdapter();
        binding.notes.setAdapter(adapter);

        viewModel.getUiState().observe(this, state -> {
            if (!state.loaded) {
                return; // Keep showing "Loading…" until the database has answered once.
            }
            adapter.submitList(state.notes);
            binding.empty.setVisibility(state.notes.isEmpty() ? View.VISIBLE : View.GONE);
            binding.status.setText(statusLine(state));
        });

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
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result ->
                        Log.i("BoardNotes", "Signed in: " + result.getUser().getUid()))
                .addOnFailureListener(e ->
                        Log.e("BoardNotes", "Sign-in failed", e));
    }

    private void openCamera() {
        startActivity(new Intent(this, CaptureActivity.class));
    }

    private String statusLine(NoteListUiState state) {
        String network = getString(state.isOnline ? R.string.online : R.string.offline);
        String work = state.waitingCount == 0
                ? getString(R.string.all_synced)
                : getResources().getQuantityString(R.plurals.waiting_to_sync,
                state.waitingCount, state.waitingCount);
        return getString(R.string.status_line, network, work);
    }
}