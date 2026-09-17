package com.example.boardnotes.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.boardnotes.data.local.AppDatabase;
import com.example.boardnotes.data.local.dao.NoteDao;
import com.example.boardnotes.data.local.dao.SyncQueueDao;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.local.entity.SyncQueueEntry;
import com.example.boardnotes.data.remote.FirestoreSyncService;
import com.example.boardnotes.data.remote.RemoteSyncService;
import com.example.boardnotes.domain.QueueStatus;
import com.example.boardnotes.domain.SyncState;

import java.util.List;

public class SyncWorker extends Worker {
    private static final int BATCH_SIZE = 20;

    // After this many failures an entry is parked instead of retried forever.
    private static final int MAX_ATTEMPTS = 5;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        SyncQueueDao queueDao = db.syncQueueDao();
        NoteDao noteDao = db.noteDao();

        RemoteSyncService remote = new FirestoreSyncService();

        List<SyncQueueEntry> batch = queueDao.claimBatch(BATCH_SIZE, System.currentTimeMillis());
        if (batch.isEmpty()) {
            return Result.success();
        }

        boolean anyRetryable = false;

        for (SyncQueueEntry entry : batch) {
            try {
                dispatch(entry, remote, noteDao);
                // Completed work is DELETED, not flagged - the queue stays small.
                queueDao.remove(entry.queueId);
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();

                if (entry.attemptCount >= MAX_ATTEMPTS) {
                    // Dead letter: stop retrying, keep the row so it can be inspected.
                    queueDao.markFailed(entry.queueId, QueueStatus.FAILED, message);
                } else {
                    // Back to PENDING so the next run picks it up.
                    queueDao.markFailed(entry.queueId, QueueStatus.PENDING, message);
                    anyRetryable = true;
                }
            }
        }

        // Result.retry() asks WorkManager to run again with exponential backoff.
        return anyRetryable ? Result.retry() : Result.success();
    }

    private void dispatch(SyncQueueEntry entry, RemoteSyncService remote, NoteDao noteDao)
            throws Exception {
        switch (entry.operation) {
            case UPLOAD:
                Note note = noteDao.findById(entry.noteId);
                if (note != null) {
                    remote.upload(note);
                    noteDao.updateState(note.id, SyncState.SYNCED, System.currentTimeMillis());
                }
                // A missing note means it was deleted before its upload ran. Entries are
                // processed oldest first, so the DELETE that replaced it is next in line.
                break;

            case DELETE:
                remote.delete(entry.noteId);
                break;

            case ENRICH:
                // Gemini via Cloud Functions in Firebase. Nothing enqueues ENRICH yet.
                break;
        }
    }
}
