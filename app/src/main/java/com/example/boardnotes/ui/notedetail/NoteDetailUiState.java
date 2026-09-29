package com.example.boardnotes.ui.notedetail;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.example.boardnotes.data.local.entity.Note;

public final class NoteDetailUiState {
    public final boolean loaded;
    @Nullable public final Note note;
    @Nullable public final String diagramJson;
    @Nullable public final Bitmap photo;

    private NoteDetailUiState(boolean loaded, @Nullable Note note, @Nullable String diagramJson,
                              @Nullable Bitmap photo) {
        this.loaded = loaded;
        this.note = note;
        this.diagramJson = diagramJson;
        this.photo = photo;
    }

    public static NoteDetailUiState loading() {
        return new NoteDetailUiState(false, null, null, null);
    }

    public NoteDetailUiState withNote(@Nullable Note newNote) {
        return new NoteDetailUiState(true, newNote, diagramJson, photo);
    }

    public NoteDetailUiState withDiagramJson(@Nullable String newDiagramJson) {
        return new NoteDetailUiState(loaded, note, newDiagramJson, photo);
    }

    public NoteDetailUiState withPhoto(@Nullable Bitmap newPhoto) {
        return new NoteDetailUiState(loaded, note, diagramJson, newPhoto);
    }
}