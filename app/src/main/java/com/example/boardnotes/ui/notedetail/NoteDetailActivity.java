package com.example.boardnotes.ui.notedetail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.boardnotes.R;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.databinding.ActivityNoteDetailBinding;

public class NoteDetailActivity extends AppCompatActivity {
    private static final String EXTRA_NOTE_ID = "com.example.boardnotes.NOTE_ID";

    private ActivityNoteDetailBinding binding;

    public static Intent newIntent(Context context, String noteId) {
        return new Intent(context, NoteDetailActivity.class).putExtra(EXTRA_NOTE_ID, noteId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNoteDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        NoteDetailViewModel viewModel = new ViewModelProvider(this).get(NoteDetailViewModel.class);
        viewModel.start(getIntent().getStringExtra(EXTRA_NOTE_ID));

        viewModel.getUiState().observe(this, state -> {
            if (!state.loaded) {
                return;
            }
            if (state.note == null) {
                finish(); // The note no longer exists.
                return;
            }
            show(state.note, state);
        });
    }

    private void show(Note note, NoteDetailUiState state) {
        binding.toolbar.setTitle(note.title != null ? note.title : getString(R.string.untitled_note));

        String when = DateUtils.formatDateTime(this, note.createdAt,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_ABBREV_MONTH);
        binding.details.setText(note.category != null
                ? getString(R.string.note_details, note.category, when) : when);

        boolean hasDiagram = state.diagramJson != null;
        binding.diagram.setDiagramJson(state.diagramJson);
        binding.diagram.setVisibility(hasDiagram ? View.VISIBLE : View.GONE);
        binding.noDiagram.setVisibility(hasDiagram ? View.GONE : View.VISIBLE);

        binding.photo.setImageBitmap(state.photo);
        binding.photo.setVisibility(state.photo != null ? View.VISIBLE : View.GONE);

        binding.ocrText.setText(note.ocrText != null ? note.ocrText : getString(R.string.no_text));
    }
}
