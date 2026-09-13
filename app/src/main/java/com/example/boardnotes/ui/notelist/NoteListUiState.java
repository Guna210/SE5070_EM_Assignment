package com.example.boardnotes.ui.notelist;

import com.example.boardnotes.data.local.entity.Note;

import java.util.Collections;
import java.util.List;

public final class NoteListUiState {
    public final List<Note> notes;
    public final int queueSize;

    private NoteListUiState(List<Note> notes, int queueSize) {
        this.notes = notes;
        this.queueSize = queueSize;
    }

    public static NoteListUiState empty() {
        return new NoteListUiState(Collections.emptyList(), 0);
    }

    // Copy-on-write: returns a new state, never modifies this one.
    public NoteListUiState withNotes(List<Note> newNotes) {
        return new NoteListUiState(
                newNotes == null ? Collections.emptyList() : newNotes,
                queueSize);
    }

    public NoteListUiState withQueueSize(int newQueueSize) {
        return new NoteListUiState(notes, newQueueSize);
    }

    public int noteCount() {
        return notes.size();
    }
}
