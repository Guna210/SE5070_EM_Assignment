package com.example.boardnotes.ui.notedetail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.boardnotes.R;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.local.entity.SyncQueueEntry;
import com.example.boardnotes.databinding.ActivityNoteDetailBinding;
import com.example.boardnotes.domain.QueueStatus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete) {
                confirmDelete(viewModel);
                return true;
            }
            return false;
        });

        binding.diagram.setOnNodeSelectedListener((nodeId, label) ->
                binding.selectedLabel.setText(label));
        binding.retryAi.setOnClickListener(v -> viewModel.retryEnrichment());

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
        showAiStatus(note, state.enrichment);

        boolean hasDiagram = state.diagramJson != null;
        binding.diagram.setDiagramJson(state.diagramJson);
        binding.diagram.setVisibility(hasDiagram ? View.VISIBLE : View.GONE);
        binding.noDiagram.setVisibility(hasDiagram ? View.GONE : View.VISIBLE);
        binding.selectedLabel.setVisibility(hasDiagram ? View.VISIBLE : View.GONE);

        binding.photo.setImageBitmap(state.photo);
        binding.photo.setVisibility(state.photo != null ? View.VISIBLE : View.GONE);

        binding.ocrText.setText(note.ocrText != null ? note.ocrText : getString(R.string.no_text));
    }

    private void confirmDelete(NoteDetailViewModel viewModel) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.delete())
                .show();
    }

    private void showAiStatus(Note note, @Nullable SyncQueueEntry enrichment) {
        boolean read = note.title != null;
        boolean reading = enrichment != null && enrichment.status == QueueStatus.IN_FLIGHT;
        binding.aiStatus.setVisibility(read ? View.GONE : View.VISIBLE);
        binding.retryAi.setVisibility(read || reading ? View.GONE : View.VISIBLE);
        if (read) {
            return;
        }
        if (enrichment == null) {
            binding.aiStatus.setText(R.string.ai_not_read);
            binding.retryAi.setText(R.string.read_with_ai);
        } else if (reading) {
            binding.aiStatus.setText(R.string.ai_reading);
        } else if (enrichment.lastError == null) {
            binding.aiStatus.setText(R.string.ai_waiting);
            binding.retryAi.setText(R.string.try_now);
        } else {
            binding.aiStatus.setText(getString(R.string.ai_failed, enrichment.lastError));
            binding.retryAi.setText(R.string.try_again);
        }
    }
}
