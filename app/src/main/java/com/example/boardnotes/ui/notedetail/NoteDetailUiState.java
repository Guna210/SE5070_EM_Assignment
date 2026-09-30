package com.example.boardnotes.ui.notedetail;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.local.entity.SyncQueueEntry;

public final class NoteDetailUiState {
    public final boolean loaded;
    @Nullable public final Note note;
    @Nullable public final String diagramJson;
    @Nullable public final Bitmap photo;
    @Nullable public final SyncQueueEntry enrichment;

    private NoteDetailUiState(boolean loaded, @Nullable Note note, @Nullable String diagramJson,
                              @Nullable Bitmap photo, @Nullable SyncQueueEntry enrichment) {
        this.loaded = loaded;
        this.note = note;
        this.diagramJson = diagramJson;
        this.photo = photo;
        this.enrichment = enrichment;
    }

    public static NoteDetailUiState loading() {
        return new NoteDetailUiState(false, null, null, null, null);
    }

    public NoteDetailUiState withNote(@Nullable Note newNote) {
        return new NoteDetailUiState(true, newNote, diagramJson, photo, enrichment);
    }

    public NoteDetailUiState withDiagramJson(@Nullable String newDiagramJson) {
        return new NoteDetailUiState(loaded, note, newDiagramJson, photo, enrichment);
    }

    public NoteDetailUiState withPhoto(@Nullable Bitmap newPhoto) {
        return new NoteDetailUiState(loaded, note, diagramJson, newPhoto, enrichment);
    }

    public NoteDetailUiState withEnrichment(@Nullable SyncQueueEntry newEnrichment) {
        return new NoteDetailUiState(loaded, note, diagramJson, photo, newEnrichment);
    }
}