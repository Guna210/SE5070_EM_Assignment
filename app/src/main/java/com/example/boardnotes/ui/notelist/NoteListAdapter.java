package com.example.boardnotes.ui.notelist;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.boardnotes.R;
import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.databinding.ItemNoteBinding;
import com.example.boardnotes.domain.SyncState;

import java.util.Objects;

public class NoteListAdapter extends ListAdapter<Note, NoteListAdapter.NoteViewHolder> {

    public NoteListAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoteBinding binding;

        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Note note) {
            Context context = itemView.getContext();

            // The title and category come from Gemini, so a note has none until it is enriched.
            binding.title.setText(note.title != null
                    ? note.title : context.getString(R.string.untitled_note));

            String when = DateUtils.formatDateTime(context, note.createdAt,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                            | DateUtils.FORMAT_ABBREV_MONTH);
            binding.details.setText(note.category != null
                    ? context.getString(R.string.note_details, note.category, when) : when);

            binding.syncState.setText(syncLabel(note.syncState));
        }

        @StringRes
        private static int syncLabel(SyncState state) {
            switch (state) {
                case SYNCED:
                    return R.string.state_synced;
                case FAILED:
                    return R.string.state_upload_failed;
                default:
                    return R.string.state_not_synced;
            }
        }
    }

    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldNote, @NonNull Note newNote) {
            return oldNote.id.equals(newNote.id);
        }

        // Room returns new objects every time, so compare what the row shows.
        @Override
        public boolean areContentsTheSame(@NonNull Note oldNote, @NonNull Note newNote) {
            return Objects.equals(oldNote.title, newNote.title)
                    && Objects.equals(oldNote.category, newNote.category)
                    && oldNote.syncState == newNote.syncState;
        }
    };
}