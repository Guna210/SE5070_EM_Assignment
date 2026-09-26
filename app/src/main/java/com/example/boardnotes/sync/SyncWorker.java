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
import com.example.boardnotes.data.local.entity.Diagram;
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

    private static final List<SyncOperation> CHEAP =
            Arrays.asList(SyncOperation.UPLOAD, SyncOperation.DELETE);

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

        for (SyncQueueEntry entry : batch) {
            boolean isEnrich = entry.operation == SyncOperation.ENRICH;
            if (isEnrich && (quotaGone || enrichThisRun >= MAX_ENRICH_PER_RUN)) {
                // Hand it back untouched: no request spent, no attempt counted.
                queueDao.release(entry.queueId, QueueStatus.PENDING);
                continue;
            }
            if (isEnrich) {
                enrichThisRun++;
            }
            try {
                if (dispatch(entry, remote, enrichment, db)) {
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
                        + ") failed, attempt " + (entry.attemptCount) + ": " + message, e);

                EnrichmentException.Kind kind = e instanceof EnrichmentException
                        ? ((EnrichmentException) e).kind()
                        : EnrichmentException.Kind.TRANSIENT;

                if (kind == EnrichmentException.Kind.QUOTA) {
                    // The allowance is gone. Every remaining ENRICH would fail the same
                    quotaGone = true;
                    queueDao.release(entry.queueId, QueueStatus.PENDING);
                    continue;
                }

                int limit = isEnrich ? MAX_ENRICH_ATTEMPTS : MAX_ATTEMPTS;

                if (kind == EnrichmentException.Kind.PERMANENT || entry.attemptCount >= limit) {
                    // Either it can never succeed, or it has had its chances.
                    queueDao.markFailed(entry.queueId, QueueStatus.FAILED, message);

                    if (entry.operation == SyncOperation.UPLOAD) {
                        noteDao.updateState(entry.noteId, SyncState.FAILED,
                                System.currentTimeMillis());
                    }
                } else {
                    queueDao.markFailed(entry.queueId, QueueStatus.PENDING, message);
                    if (!isEnrich) {
                        // Only uploads and deletes may ask for an immediate rerun.
                        anyRetryable = true;
                    }
                }
            }
        }

        // Ask the queue whether cheap work is still waiting. Enrichment is purposefully excluded.
        boolean moreCheapWork = queueDao.countPending(QueueStatus.PENDING, CHEAP) > 0;
        // Result.retry() asks WorkManager to run again with exponential backoff.
        return (anyRetryable || moreCheapWork) ? Result.retry() : Result.success();
    }

    private boolean dispatch(SyncQueueEntry entry, RemoteSyncService remote,
                             EnrichmentService enrichment, AppDatabase db) throws Exception {
        NoteDao noteDao = db.noteDao();

        switch (entry.operation) {
            case UPLOAD:
                Note note = noteDao.findById(entry.noteId);
                if (note != null) {
                    Diagram diagram = db.diagramDao().findForNote(note.id);
                    remote.upload(note, diagram == null ? null : diagram.diagramJson);
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
                saveEnrichment(db, toEnrich.id, result);
                return true;
        }
        return false;
    }

    private void saveEnrichment(AppDatabase db, String noteId, EnrichmentResult result) {
        long now = System.currentTimeMillis();

        SyncQueueEntry upload = new SyncQueueEntry();
        upload.noteId = noteId;
        upload.operation = SyncOperation.UPLOAD;
        upload.status = QueueStatus.PENDING;
        upload.createdAt = now;

        db.runInTransaction(() -> {
            int updated = db.noteDao().applyEnrichment(
                    noteId, result.title, result.category, SyncState.ENRICHED, now);
            if (updated == 0) {
                // Deleted while Gemini was answering. There is nothing to attach the result to
                Log.i("BoardNotes", "Note " + noteId.substring(0, 8)
                        + " was deleted during enrichment; result discarded");
                return;
            }

            if (result.hasDiagram()) {
                Diagram diagram = new Diagram();
                diagram.noteId = noteId;
                diagram.diagramJson = result.diagramJson;
                diagram.updatedAt = now;
                db.diagramDao().upsert(diagram);
            }

            // The note now differs from its server copy. Queue the upload in the same transaction
            db.syncQueueDao().enqueue(upload);

            Log.i("BoardNotes", "Saved enrichment for " + noteId.substring(0, 8)
                    + (result.hasDiagram() ? " with a diagram" : ", no diagram"));
        });
    }
}
