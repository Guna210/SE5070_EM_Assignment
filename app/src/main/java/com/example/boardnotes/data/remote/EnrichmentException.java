package com.example.boardnotes.data.remote;

public class EnrichmentException extends Exception {
    public enum Kind {
        // A timeout, a 5xx, a preempted request. Worth another attempt soon.
        TRANSIENT,
        // The allowance is gone. Another attempt fails identically.
        QUOTA,
        // A rejected request, an unreadable file. Will fail the same way forever.
        PERMANENT
    }

    private final Kind kind;

    public EnrichmentException(String message, Throwable cause, Kind kind) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isRetryable() {
        return kind != Kind.PERMANENT;
    }
}
