package com.example.boardnotes.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.boardnotes.data.local.AppDatabase;
import com.example.boardnotes.data.local.dao.NoteDao;
import com.example.boardnotes.data.local.dao.SyncQueueDao;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.local.entity.SyncQueueEntry;
import com.example.boardnotes.data.remote.AiLogicEnrichmentService;
import com.example.boardnotes.data.remote.EnrichmentException;
import com.example.boardnotes.data.remote.EnrichmentService;
import com.example.boardnotes.data.remote.FirestoreSyncService;
import com.example.boardnotes.data.remote.RemoteSyncService;
import com.example.boardnotes.domain.EnrichmentResult;
import com.example.boardnotes.domain.QueueStatus;
import com.example.boardnotes.domain.SyncOperation;
import com.example.boardnotes.domain.SyncState;

import java.util.Arrays;
import java.util.List;

public class SyncWorker extends Worker {
    private static final int BATCH_SIZE = 20;

    // After this many failures an entry is parked instead of retried forever.
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ENRICH_ATTEMPTS = 2;
    private static final int MAX_ENRICH_PER_RUN = 2;
    private static final long LEASE_MS = 15 * 60 * 1000L;

    private static final List<SyncOperation> DISPATCHABLE =
            Arrays.asList(SyncOperation.UPLOAD, SyncOperation.DELETE, SyncOperation.ENRICH);

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
        EnrichmentService enrichment = new AiLogicEnrichmentService();

        long now = System.currentTimeMillis();
        int reclaimed = queueDao.reclaimStale(
                QueueStatus.PENDING, QueueStatus.IN_FLIGHT, now - LEASE_MS);
        if (reclaimed > 0) {
            Log.w("BoardNotes", "Reclaimed " + reclaimed
                    + " entries abandoned IN_FLIGHT by an earlier run");
        }

        List<SyncQueueEntry> batch = queueDao.claimBatch(DISPATCHABLE, BATCH_SIZE, now);
        if (batch.isEmpty()) {
            return Result.success();
        }

        boolean anyRetryable = false;
        boolean quotaGone = false;
        int enrichThisRun = 0;
        boolean deferredEnrich = false;

        for (SyncQueueEntry entry : batch) {
            boolean isEnrich = entry.operation == SyncOperation.ENRICH;
            if (isEnrich && (quotaGone || enrichThisRun >= MAX_ENRICH_PER_RUN)) {
                // Hand it back untouched: no request spent, no attempt counted.
                queueDao.release(entry.queueId, QueueStatus.PENDING);
                deferredEnrich = true;
                continue;
            }
            if (isEnrich) {
                enrichThisRun++;
            }
            try {
                if (dispatch(entry, remote, enrichment, noteDao)) {
                    // Completed work is DELETED, not flagged - the queue stays small.
                    queueDao.remove(entry.queueId);
                } else {
                    // Claimed but not dispatchable yet. Hand it back without counting
                    // an attempt against it.
                    queueDao.release(entry.queueId, QueueStatus.PENDING);
                }
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();

                Log.w("BoardNotes", "Queue entry " + entry.queueId + " (" + entry.operation
                        + ") failed, attempt " + (entry.attemptCount + 1) + ": " + message, e);

                EnrichmentException.Kind kind = e instanceof EnrichmentException
                        ? ((EnrichmentException) e).kind()
                        : EnrichmentException.Kind.TRANSIENT;

                if (kind == EnrichmentException.Kind.QUOTA) {
                    // The allowance is gone. Every remaining ENRICH would fail the same
                    quotaGone = true;
                    queueDao.release(entry.queueId, QueueStatus.PENDING);
                    deferredEnrich = true;
                    continue;
                }

                int limit = isEnrich ? MAX_ENRICH_ATTEMPTS : MAX_ATTEMPTS;

                if (kind == EnrichmentException.Kind.PERMANENT || entry.attemptCount >= limit) {
                    queueDao.markFailed(entry.queueId, QueueStatus.FAILED, message);
                } else {
                    queueDao.markFailed(entry.queueId, QueueStatus.PENDING, message);
                    if (!isEnrich) {
                        // Only uploads and deletes may ask for an immediate rerun.
                        anyRetryable = true;
                    }
                }
            }
        }

        boolean batchWasFull = batch.size() == BATCH_SIZE;
        boolean moreWorkLikely = batchWasFull && !deferredEnrich;
        // Result.retry() asks WorkManager to run again with exponential backoff.
        return (anyRetryable || moreWorkLikely) ? Result.retry() : Result.success();
    }

    private boolean dispatch(SyncQueueEntry entry, RemoteSyncService remote, EnrichmentService enrichment,
                             NoteDao noteDao)
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
                return true;

            case DELETE:
                remote.delete(entry.noteId);
                return true;

            case ENRICH:
                Note toEnrich = noteDao.findById(entry.noteId);
                if (toEnrich == null) {
                    // Deleted before enrichment ran. Nothing to do, and the DELETE
                    // that replaced it is already in the queue behind this.
                    return true;
                }
                EnrichmentResult result = enrichment.enrich(toEnrich);
                Log.i("BoardNotes", "ENRICH result for "
                        + toEnrich.id.substring(0, 8) + ": " + result);
                return true;
        }
        return false;
    }
}
