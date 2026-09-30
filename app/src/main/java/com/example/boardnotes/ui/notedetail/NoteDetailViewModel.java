package com.example.boardnotes.ui.notedetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.boardnotes.data.repository.NoteRepository;

public class NoteDetailViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    private final MediatorLiveData<NoteDetailUiState> uiState = new MediatorLiveData<>();
    private boolean started;
    private boolean photoRequested;

    public NoteDetailViewModel(@NonNull Application application) {
        super(application);
        repository = NoteRepository.getInstance(application);
        uiState.setValue(NoteDetailUiState.loading());
    }

    // Called from every onCreate, including after the phone is rotated.
    public void start(String noteId) {
        if (started) {
            return;
        }
        started = true;

        uiState.addSource(repository.observeNote(noteId), note -> {
            uiState.setValue(current().withNote(note));
            // The photo's path is only known once the note has loaded. Decode it once.
            if (note != null && !photoRequested) {
                photoRequested = true;
                uiState.addSource(repository.loadPhoto(note.imagePath), photo ->
                        uiState.setValue(current().withPhoto(photo)));
            }
        });

        uiState.addSource(repository.observeDiagram(noteId), diagram ->
                uiState.setValue(current().withDiagramJson(
                        diagram == null ? null : diagram.diagramJson)));
    }

    private NoteDetailUiState current() {
        NoteDetailUiState value = uiState.getValue();
        return value == null ? NoteDetailUiState.loading() : value;
    }

    public LiveData<NoteDetailUiState> getUiState() {
        return uiState;
    }

    public void delete() {
        NoteDetailUiState state = current();
        if (state.note != null) {
            repository.deleteNote(state.note);
        }
    }
}