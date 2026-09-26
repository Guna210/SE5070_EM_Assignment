package com.example.boardnotes.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.boardnotes.data.local.entity.Diagram;

@Dao
public interface DiagramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Diagram diagram);

    @Query("SELECT * FROM diagrams WHERE noteId = :noteId")
    LiveData<Diagram> observeForNote(String noteId);

    @Query("SELECT * FROM diagrams WHERE noteId = :noteId")
    Diagram findForNote(String noteId);

    @Query("SELECT * FROM diagrams ORDER BY updatedAt DESC LIMIT 1")
    LiveData<Diagram> observeLatest();
}
