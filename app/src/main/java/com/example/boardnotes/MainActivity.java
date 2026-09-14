package com.example.boardnotes;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.boardnotes.databinding.ActivityMainBinding;
import com.example.boardnotes.ui.notelist.NoteListViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

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

        NoteListViewModel viewModel = new ViewModelProvider(this).get(NoteListViewModel.class);

        viewModel.getUiState().observe(this, state -> binding.status.setText(String.format(
                Locale.getDefault(),
                "Notes: %d%nPending sync ops: %d%nNetwork: %s",
                state.noteCount(),
                state.queueSize,
                getString(state.isOnline ? R.string.online : R.string.offline))));

        binding.addNote.setOnClickListener(v -> viewModel.addTestNote());
        binding.deleteOldest.setOnClickListener(v -> viewModel.deleteOldest());

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