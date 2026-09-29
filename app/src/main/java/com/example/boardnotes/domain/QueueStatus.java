package com.example.boardnotes.domain;

public enum QueueStatus {
    PENDING,    // waiting to be picked up
    IN_FLIGHT,  // claimed by a worker - prevents double dispatch
    FAILED      // out of attempts, or can never succeed.
}
