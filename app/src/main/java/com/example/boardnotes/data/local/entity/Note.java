package com.example.boardnotes.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.boardnotes.domain.SyncState;

import java.util.UUID;

@Entity(tableName = "notes", indices = {@Index("syncState"), @Index("createdAt")})
public class Note {
    @PrimaryKey
    @NonNull
    public String id = UUID.randomUUID().toString();

    public long createdAt;
    public long updatedAt;

    @NonNull
    public String imagePath = "";

    @Nullable public String ocrText;
    @Nullable public String title;
    @Nullable public String category;

    @NonNull
    public SyncState syncState = SyncState.CAPTURED;
}
