package com.example.boardnotes.data.local;

import androidx.room.TypeConverter;

import com.example.boardnotes.domain.QueueStatus;
import com.example.boardnotes.domain.SyncOperation;
import com.example.boardnotes.domain.SyncState;

public class Converters {
    @TypeConverter public static String fromSyncState(SyncState v) { return v == null ? null : v.name(); }
    @TypeConverter public static SyncState toSyncState(String v) { return v == null ? null : SyncState.valueOf(v); }

    @TypeConverter public static String fromOperation(SyncOperation v) { return v == null ? null : v.name(); }
    @TypeConverter public static SyncOperation toOperation(String v) { return v == null ? null : SyncOperation.valueOf(v); }

    @TypeConverter public static String fromQueueStatus(QueueStatus v) { return v == null ? null : v.name(); }
    @TypeConverter public static QueueStatus toQueueStatus(String v) { return v == null ? null : QueueStatus.valueOf(v); }
}
