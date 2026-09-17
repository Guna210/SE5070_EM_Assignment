package com.example.boardnotes.ui.notelist;

import com.example.boardnotes.data.local.entity.Note;

import java.util.Collections;
import java.util.List;

public final class NoteListUiState {
    public final List<Note> notes;
    public final int queueSize;
    public final boolean isOnline;
    public final int syncedCount;

    private NoteListUiState(List<Note> notes, int queueSize, boolean isOnline, int syncedCount) {
        this.notes = notes;
        this.queueSize = queueSize;
        this.isOnline = isOnline;
        this.syncedCount = syncedCount;
    }

    public static NoteListUiState empty() {
        return new NoteListUiState(Collections.emptyList(), 0, false, 0);
    }

    // Copy-on-write: returns a new state, never modifies this one.
    public NoteListUiState withNotes(List<Note> newNotes) {
        return new NoteListUiState(
                newNotes == null ? Collections.emptyList() : newNotes, queueSize, isOnline, syncedCount);
    }

    public NoteListUiState withQueueSize(int newQueueSize) {
        return new NoteListUiState(notes, newQueueSize, isOnline, syncedCount);
    }

    public int noteCount() {
        return notes.size();
    }

    public NoteListUiState withOnline(boolean online) {
        return new NoteListUiState(notes, queueSize, online, syncedCount);
    }

    public NoteListUiState withSyncedCount(int newSyncedCount) {
        return new NoteListUiState(notes, queueSize, isOnline, newSyncedCount);
    }
}
