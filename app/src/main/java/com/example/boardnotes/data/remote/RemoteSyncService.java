package com.example.boardnotes.data.remote;

import com.example.boardnotes.data.local.entity.Note;

// SyncWorker depends on this interface and never imports Firebase.
public interface RemoteSyncService {
    void upload(Note note) throws Exception;

    void delete(String noteId) throws Exception;
}
