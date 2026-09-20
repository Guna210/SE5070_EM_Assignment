package com.example.boardnotes;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;

public class BoardNotesApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        
        FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(AppCheckSetup.factory());
    }
}
