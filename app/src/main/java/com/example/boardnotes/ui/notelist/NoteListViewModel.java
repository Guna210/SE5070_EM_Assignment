package com.example.boardnotes.ui.notelist;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.boardnotes.data.repository.NoteRepository;

public class NoteListViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    private final MediatorLiveData<NoteListUiState> uiState = new MediatorLiveData<>();

    public NoteListViewModel(@NonNull Application application) {
        super(application);
        repository = NoteRepository.getInstance(application);

        uiState.setValue(NoteListUiState.loading());

        uiState.addSource(repository.observeNotes(), notes ->
                uiState.setValue(current().withNotes(notes)));

        uiState.addSource(repository.observeOnline(), online ->
                uiState.setValue(current().withOnline(online != null && online)));

        uiState.addSource(repository.observeNotes(), notes ->
                uiState.setValue(current().withNotes(notes)));

        uiState.addSource(repository.observeWaitingCount(), count ->
                uiState.setValue(current().withWaitingCount(count == null ? 0 : count)));
    }

    private NoteListUiState current() {
        NoteListUiState value = uiState.getValue();
        return value == null ? NoteListUiState.loading() : value;
    }

    public LiveData<NoteListUiState> getUiState() {
        return uiState;
    }

    public void importImage(Uri uri) {
        if (uri != null) {
            repository.createNoteFromUri(uri);
        }
    }
}
