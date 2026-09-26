package com.example.boardnotes.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.boardnotes.data.local.entity.SyncQueueEntry;
import com.example.boardnotes.domain.QueueStatus;
import com.example.boardnotes.domain.SyncOperation;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface SyncQueueDao {
    @Insert
    void enqueue(SyncQueueEntry entry);

    @Query("SELECT * FROM sync_queue WHERE status = :status AND operation IN (:operations) ORDER BY " +
            "createdAt ASC LIMIT :limit")
    List<SyncQueueEntry> peek(QueueStatus status, List<SyncOperation> operations, int limit);

    @Query("UPDATE sync_queue SET status = :status, attemptCount = attemptCount + 1, "
            + "lastAttemptAt = :now WHERE queueId IN (:ids)")
    void mark(List<Long> ids, QueueStatus status, long now);

    @Query("UPDATE sync_queue SET status = :status, lastError = :error WHERE queueId = :queueId")
    void markFailed(long queueId, QueueStatus status, String error);

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    void remove(long queueId);

    @Query("SELECT COUNT(*) FROM sync_queue")
    LiveData<Integer> observeQueueSize();

    @Query("UPDATE sync_queue SET status = :status, attemptCount = CASE WHEN attemptCount > 0 THEN attemptCount - 1 ELSE 0 END "
            + "WHERE queueId = :queueId")
    void release(long queueId, QueueStatus status);

    @Query("UPDATE sync_queue SET status = :pending WHERE status = :inFlight AND lastAttemptAt < :cutoff")
    int reclaimStale(QueueStatus pending, QueueStatus inFlight, long cutoff);

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status AND operation IN (:operations)")
    int countPending(QueueStatus status, List<SyncOperation> operations);

    /* Atomically take a batch of pending work and mark it IN_FLIGHT, so two
       worker runs can never dispatch the same entry twice. */
    @Transaction
    default List<SyncQueueEntry> claimBatch(List<SyncOperation> operations, int limit, long now) {
        List<SyncQueueEntry> batch = peek(QueueStatus.PENDING, operations, limit);
        if (batch.isEmpty()) {
            return batch;
        }
        List<Long> ids = new ArrayList<>(batch.size());
        for (SyncQueueEntry e : batch) {
            ids.add(e.queueId);
        }
        mark(ids, QueueStatus.IN_FLIGHT, now);
        for (SyncQueueEntry e : batch) {
            e.attemptCount++;
            e.lastAttemptAt = now;
        }
        return batch;
    }
}
