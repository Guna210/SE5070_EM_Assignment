package com.example.boardnotes.data.remote;

import androidx.annotation.Nullable;

import com.example.boardnotes.data.local.entity.Note;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// The only class in the project that knows Firestore exists.
public class FirestoreSyncService implements RemoteSyncService {
    private static final long TIMEOUT_SECONDS = 30;

    private static volatile boolean configured = false;

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public FirestoreSyncService() {
        firestore = configuredInstance();
        auth = FirebaseAuth.getInstance();
    }

    private static synchronized FirebaseFirestore configuredInstance() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (!configured) {
            db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build());
            configured = true;
        }
        return db;
    }

    @Override
    public void upload(Note note, @Nullable String diagramJson) throws Exception {
        String uid = requireUid();

        Map<String, Object> document = new HashMap<>();
        document.put("id", note.id);
        document.put("createdAt", note.createdAt);
        document.put("updatedAt", note.updatedAt);
        document.put("ocrText", note.ocrText);
        document.put("title", note.title);
        document.put("category", note.category);
        document.put("diagramJson", diagramJson);

        Tasks.await(
                firestore.collection("users").document(uid)
                        .collection("notes").document(note.id)
                        .set(document),
                TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void delete(String noteId) throws Exception {
        String uid = requireUid();

        // Deleting a document that was never uploaded succeeds silently.
        Tasks.await(
                firestore.collection("users").document(uid)
                        .collection("notes").document(noteId)
                        .delete(),
                TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private String requireUid() throws Exception {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            user = Tasks.await(auth.signInAnonymously(), TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getUser();
        }
        if (user == null) {
            throw new IllegalStateException("No Firebase user after anonymous sign-in");
        }
        return user.getUid();
    }
}
