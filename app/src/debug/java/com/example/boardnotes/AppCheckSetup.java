package com.example.boardnotes;

import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

final class AppCheckSetup {
    private AppCheckSetup() { }

    static AppCheckProviderFactory factory() {
        return DebugAppCheckProviderFactory.getInstance();
    }
}
