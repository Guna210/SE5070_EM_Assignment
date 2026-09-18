package com.example.boardnotes.ui.notelist;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.repository.NoteRepository;

import java.util.List;

public class NoteListViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    private final MediatorLiveData<NoteListUiState> uiState = new MediatorLiveData<>();

    public NoteListViewModel(@NonNull Application application) {
        super(application);
        repository = NoteRepository.getInstance(application);

        uiState.setValue(NoteListUiState.empty());

        uiState.addSource(repository.observeNotes(), notes ->
                uiState.setValue(current().withNotes(notes)));

        uiState.addSource(repository.observeQueueSize(), size ->
                uiState.setValue(current().withQueueSize(size == null ? 0 : size)));

        uiState.addSource(repository.observeOnline(), online ->
                uiState.setValue(current().withOnline(online != null && online)));

        uiState.addSource(repository.observeSyncedCount(), synced ->
                uiState.setValue(current().withSyncedCount(synced == null ? 0 : synced)));
    }

    private NoteListUiState current() {
        NoteListUiState value = uiState.getValue();
        return value == null ? NoteListUiState.empty() : value;
    }

    public LiveData<NoteListUiState> getUiState() {
        return uiState;
    }

    // Will be replaced with real camera capture
    public void addTestNote() {
        repository.createNote("/placeholder/capture-" + System.currentTimeMillis() + ".jpg");
    }

    public void deleteOldest() {
        NoteListUiState state = current();
        List<Note> notes = state.notes;
        if (!notes.isEmpty()) {
            // observeAll() is ordered createdAt DESC, so the oldest is last.
            repository.deleteNote(notes.get(notes.size() - 1));
        }
    }

    public void importImage(Uri uri) {
        if (uri != null) {
            repository.createNoteFromUri(uri);
        }
    }
}
