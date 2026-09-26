package com.example.boardnotes;

import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

final class AppCheckSetup {
    private AppCheckSetup() { }

    static AppCheckProviderFactory factory() {
        return PlayIntegrityAppCheckProviderFactory.getInstance();
    }
}