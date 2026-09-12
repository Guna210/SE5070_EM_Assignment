package com.example.boardnotes.domain;

public enum QueueStatus {
    PENDING,    // waiting to be picked up
    IN_FLIGHT,  // claimed by a worker - prevents double dispatch
    FAILED      // retryable; attemptCount tracks how many tries
}
