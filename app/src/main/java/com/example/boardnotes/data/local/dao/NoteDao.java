package com.example.boardnotes.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.domain.SyncState;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    LiveData<List<Note>> observeAll();

    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<Note> observeById(String id);

    // Non-observing read, for use on a background thread.
    @Query("SELECT * FROM notes WHERE id = :id")
    Note findById(String id);

    @Query("UPDATE notes SET syncState = :state, updatedAt = :now WHERE id = :id")
    void updateState(String id, SyncState state, long now);

    @Query("SELECT COUNT(*) FROM notes WHERE syncState = :state")
    LiveData<Integer> observeCountByState(SyncState state);
}
