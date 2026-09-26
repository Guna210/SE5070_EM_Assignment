package com.example.boardnotes.ui.notelist;

import androidx.annotation.Nullable;

import com.example.boardnotes.data.local.entity.Note;

import java.util.Collections;
import java.util.List;

public final class NoteListUiState {
    public final List<Note> notes;
    public final int queueSize;
    public final boolean isOnline;
    public final int syncedCount;
    public final int textCount;
    // The most recently saved diagram, until the note screens exist to show each one.
    @Nullable public final String latestDiagramJson;

    private NoteListUiState(List<Note> notes, int queueSize, boolean isOnline, int syncedCount,
                            int textCount, @Nullable String latestDiagramJson) {
        this.notes = notes;
        this.queueSize = queueSize;
        this.isOnline = isOnline;
        this.syncedCount = syncedCount;
        this.textCount = textCount;
        this.latestDiagramJson = latestDiagramJson;
    }

    public static NoteListUiState empty() {
        return new NoteListUiState(Collections.emptyList(), 0, false, 0, 0, null);
    }

    // Copy-on-write: returns a new state, never modifies this one.
    public NoteListUiState withNotes(List<Note> newNotes) {
        return new NoteListUiState(newNotes == null ? Collections.emptyList() : newNotes,
                queueSize, isOnline, syncedCount, textCount, latestDiagramJson);
    }

    public NoteListUiState withQueueSize(int newQueueSize) {
        return new NoteListUiState(notes, newQueueSize, isOnline, syncedCount, textCount,
                latestDiagramJson);
    }

    public int noteCount() {
        return notes.size();
    }

    public NoteListUiState withOnline(boolean online) {
        return new NoteListUiState(notes, queueSize, online, syncedCount, textCount,
                latestDiagramJson);
    }

    public NoteListUiState withSyncedCount(int newSyncedCount) {
        return new NoteListUiState(notes, queueSize, isOnline, newSyncedCount, textCount,
                latestDiagramJson);
    }

    public NoteListUiState withTextCount(int newTextCount) {
        return new NoteListUiState(notes, queueSize, isOnline, syncedCount, newTextCount,
                latestDiagramJson);
    }

    public NoteListUiState withLatestDiagramJson(@Nullable String json) {
        return new NoteListUiState(notes, queueSize, isOnline, syncedCount, textCount, json);
    }
}
