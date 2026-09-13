package com.example.boardnotes.data.repository;

import android.content.Context;

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

import java.util.List;

public class NoteRepository {
    private static volatile NoteRepository instance;

    private final AppDatabase db;
    private final NoteDao noteDao;
    private final SyncQueueDao queueDao;
    private final AppExecutors executors;

    private NoteRepository(Context context) {
        db = AppDatabase.getInstance(context);
        noteDao = db.noteDao();
        queueDao = db.syncQueueDao();
        executors = AppExecutors.getInstance();
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

    // Creates a note and queues it for upload, atomically.
    public void createNote(String imagePath) {
        executors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();

            Note note = new Note();           // id is a fresh UUID (see Note.java)
            note.imagePath = imagePath;
            note.createdAt = now;
            note.updatedAt = now;
            note.syncState = SyncState.CAPTURED;

            SyncQueueEntry entry = new SyncQueueEntry();
            entry.noteId = note.id;
            entry.operation = SyncOperation.UPLOAD;
            entry.status = QueueStatus.PENDING;
            entry.createdAt = now;

            // Both writes, or neither. This is the DR-27 mitigation.
            db.runInTransaction(() -> {
                noteDao.insert(note);
                queueDao.enqueue(entry);
            });
        });
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
        });
    }
}
