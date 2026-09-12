package com.example.boardnotes.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "diagrams",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "noteId",
                onDelete = ForeignKey.CASCADE))
public class Diagram {
    @PrimaryKey
    @NonNull
    public String noteId = "";

//    Node/edge structure returned by Gemini, rendered by DiagramView.
    @NonNull
    public String diagramJson = "";

    public long updatedAt;
}
