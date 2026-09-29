package com.example.boardnotes.ui.notelist;

import com.example.boardnotes.data.local.entity.Note;

import java.util.Collections;
import java.util.List;

public final class NoteListUiState {
    public final List<Note> notes;
    public final boolean loaded;
    public final int waitingCount;
    public final boolean isOnline;

    private NoteListUiState(List<Note> notes, boolean loaded, int waitingCount, boolean isOnline) {
        this.notes = notes;
        this.loaded = loaded;
        this.waitingCount = waitingCount;
        this.isOnline = isOnline;
    }

    public static NoteListUiState loading() {
        return new NoteListUiState(Collections.emptyList(), false, 0, false);
    }

    // Copy-on-write: returns a new state, never modifies this one.
    public NoteListUiState withNotes(List<Note> newNotes) {
        return new NoteListUiState(newNotes == null ? Collections.emptyList() : newNotes,
                true, waitingCount, isOnline);
    }

    public NoteListUiState withWaitingCount(int newWaitingCount) {
        return new NoteListUiState(notes, loaded, newWaitingCount, isOnline);
    }

    public NoteListUiState withOnline(boolean online) {
        return new NoteListUiState(notes, loaded, waitingCount, online);
    }
}
