package com.example.boardnotes.ui.capture;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.boardnotes.data.repository.NoteRepository;

import java.io.File;

public class CaptureViewModel extends AndroidViewModel {
    private final NoteRepository repository;

    public CaptureViewModel(@NonNull Application application) {
        super(application);
        repository = NoteRepository.getInstance(application);
    }

    public void onImageCaptured(File captured) {
        if (captured != null) {
            repository.createNoteFromCapture(captured);
        }
    }
}
