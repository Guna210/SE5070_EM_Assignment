package com.example.boardnotes.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.boardnotes.domain.QueueStatus;
import com.example.boardnotes.domain.SyncOperation;

// Purposefully left out a foreign key to Note in order to support offline deletes.
@Entity(tableName = "sync_queue", indices = {@Index("status"), @Index("noteId")})
public class SyncQueueEntry {
    @PrimaryKey(autoGenerate = true)
    public long queueId;

    @NonNull public String noteId = "";
    @NonNull public SyncOperation operation = SyncOperation.ENRICH;
    @NonNull public QueueStatus status = QueueStatus.PENDING;

    public long createdAt;
    public int attemptCount;
    public long lastAttemptAt;

    // Captured for the retry logic.
    @Nullable public String lastError;
}
