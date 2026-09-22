package com.example.boardnotes.data.remote;

import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.domain.EnrichmentResult;

public interface EnrichmentService {
    EnrichmentResult enrich(Note note) throws EnrichmentException;
}
