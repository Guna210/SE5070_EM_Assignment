package com.example.boardnotes.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.boardnotes.data.local.dao.DiagramDao;
import com.example.boardnotes.data.local.dao.NoteDao;
import com.example.boardnotes.data.local.dao.SyncQueueDao;
import com.example.boardnotes.data.local.entity.Diagram;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.data.local.entity.SyncQueueEntry;

@Database(
        entities = {Note.class, Diagram.class, SyncQueueEntry.class},
        version = 1,
        exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao();
    public abstract DiagramDao diagramDao();
    public abstract SyncQueueDao syncQueueDao();

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "boardnotes.db")
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return instance;
    }
}
