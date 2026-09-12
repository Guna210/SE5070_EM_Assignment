package com.example.boardnotes.domain;

public enum SyncOperation {
    ENRICH,   // send OCR text + image to Gemini via Cloud Function
    UPLOAD,   // push the local note to Firestore
    DELETE    // remove the note remotely after a local delete
}
