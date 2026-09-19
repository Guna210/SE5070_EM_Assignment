package com.example.boardnotes.data.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.boardnotes.data.local.AppDatabase;
import com.example.boardnotes.data.local.dao.NoteDao;
import com.example.boardnotes.data.local.dao.SyncQueueDao;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.local.entity.SyncQueueEntry;
import com.example.boardnotes.domain.QueueStatus;
import com.example.boardnotes.domain.SyncOperation;
import com.example.boardnotes.domain.SyncState;
import com.example.boardnotes.util.AppExecutors;
import com.example.boardnotes.util.ImageStore;
import com.example.boardnotes.util.NetworkMonitor;
import com.example.boardnotes.sync.SyncScheduler;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NoteRepository {
    private static volatile NoteRepository instance;

    private final AppDatabase db;
    private final NoteDao noteDao;
    private final SyncQueueDao queueDao;
    private final AppExecutors executors;
    private final NetworkMonitor networkMonitor;
    private final Context appContext;

    private NoteRepository(Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
        noteDao = db.noteDao();
        queueDao = db.syncQueueDao();
        executors = AppExecutors.getInstance();
        networkMonitor = NetworkMonitor.getInstance(context);
    }

    public static NoteRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (NoteRepository.class) {
                if (instance == null) {
                    instance = new NoteRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // Reads (the "Change Listener" in the sync diagram)
    public LiveData<List<Note>> observeNotes() {
        return noteDao.observeAll();
    }

    public LiveData<Integer> observeQueueSize() {
        return queueDao.observeQueueSize();
    }

    public LiveData<Integer> observeSyncedCount() {
        return noteDao.observeCountByState(SyncState.SYNCED);
    }

    public LiveData<Boolean> observeOnline() {
        return networkMonitor;
    }

    // Creates a note and queues it for upload, atomically.
    public void createNote(String imagePath) {
        executors.diskIO().execute(() -> insertNoteWithUploadQueued(imagePath));
    }

    // Deletes a note locally and queues the remote delete, atomically.
    public void deleteNote(Note note) {
        executors.diskIO().execute(() -> {
            SyncQueueEntry entry = new SyncQueueEntry();
            entry.noteId = note.id;
            entry.operation = SyncOperation.DELETE;
            entry.status = QueueStatus.PENDING;
            entry.createdAt = System.currentTimeMillis();

            db.runInTransaction(() -> {
                noteDao.delete(note);
                queueDao.enqueue(entry);
            });
            SyncScheduler.requestSync(appContext);
        });
    }

    public void createNoteFromUri(Uri source) {
        executors.diskIO().execute(() -> {
            try {
                String imagePath = ImageStore.saveCompressed(appContext, source);
                insertNoteWithUploadQueued(imagePath);
            } catch (IOException e) {
                Log.e("BoardNotes", "Image import failed", e);
            }
        });
    }

    public void createNoteFromCapture(File captured) {
        executors.diskIO().execute(() -> {
            try {
                String imagePath = ImageStore.saveCompressed(appContext, Uri.fromFile(captured));
                insertNoteWithUploadQueued(imagePath);
            } catch (IOException e) {
                Log.e("BoardNotes", "Capture import failed", e);
            } finally {
                if (captured.exists() && !captured.delete()) {
                    Log.w("BoardNotes", "Temporary capture not deleted: " + captured);
                }
            }
        });
    }

    private void insertNoteWithUploadQueued(String imagePath) {
        long now = System.currentTimeMillis();

        Note note = new Note();
        note.imagePath = imagePath;
        note.createdAt = now;
        note.updatedAt = now;
        note.syncState = SyncState.CAPTURED;

        SyncQueueEntry entry = new SyncQueueEntry();
        entry.noteId = note.id;
        entry.operation = SyncOperation.UPLOAD;
        entry.status = QueueStatus.PENDING;
        entry.createdAt = now;

        db.runInTransaction(() -> {
            noteDao.insert(note);
            queueDao.enqueue(entry);
        });

        SyncScheduler.requestSync(appContext);
    }
}
