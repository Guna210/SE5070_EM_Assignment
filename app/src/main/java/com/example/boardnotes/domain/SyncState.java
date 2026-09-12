package com.example.boardnotes.domain;

public enum SyncState {
    CAPTURED,   // image on disk, OCR not yet run
    OCR_DONE,   // text extracted - note is fully usable offline from here
    ENRICHED,   // AI results stored locally
    SYNCED,     // local matches remote
    FAILED      // see the queue entry for attemptCount / lastError
}
