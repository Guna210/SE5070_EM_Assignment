package com.example.boardnotes.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class EnrichmentResult {
    @Nullable
    public final String title;
    @Nullable public final String category;
    @Nullable public final String diagramJson;

    public EnrichmentResult(@Nullable String title,
                            @Nullable String category,
                            @Nullable String diagramJson) {
        this.title = title;
        this.category = category;
        this.diagramJson = diagramJson;
    }

    public boolean hasDiagram() {
        return diagramJson != null && !diagramJson.trim().isEmpty();
    }

    @NonNull
    @Override
    public String toString() {
        return "EnrichmentResult{title=" + title
                + ", category=" + category
                + ", diagramJson=" + (diagramJson == null ? "null" : diagramJson.length() + " chars")
                + "}";
    }
}
