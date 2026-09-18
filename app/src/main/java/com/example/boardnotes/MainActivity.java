package com.example.boardnotes;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.boardnotes.databinding.ActivityMainBinding;
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
                state.syncedCount,
                state.queueSize,
                getString(state.isOnline ? R.string.online : R.string.offline))));

        binding.addNote.setOnClickListener(v -> viewModel.addTestNote());
        binding.deleteOldest.setOnClickListener(v -> viewModel.deleteOldest());

        binding.importImage.setOnClickListener(v -> pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

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
}