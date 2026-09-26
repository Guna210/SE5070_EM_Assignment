package com.example.boardnotes.data.remote;

import androidx.annotation.Nullable;

import com.example.boardnotes.data.local.entity.Note;

// SyncWorker depends on this interface and never imports Firebase.
public interface RemoteSyncService {
    void upload(Note note, @Nullable String diagramJson) throws Exception;

    void delete(String noteId) throws Exception;
}
