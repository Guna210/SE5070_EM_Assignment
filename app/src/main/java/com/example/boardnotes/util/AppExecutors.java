package com.example.boardnotes.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static volatile AppExecutors instance;

    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();

    private AppExecutors() { }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }
}
