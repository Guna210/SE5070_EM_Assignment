package com.example.boardnotes;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.boardnotes.databinding.ActivityMainBinding;
import com.example.boardnotes.ui.notelist.NoteListViewModel;

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
                "Notes: %d%nPending sync ops: %d",
                state.noteCount(),
                state.queueSize)));

        binding.addNote.setOnClickListener(v -> viewModel.addTestNote());
        binding.deleteOldest.setOnClickListener(v -> viewModel.deleteOldest());
    }
}