package com.example.boardnotes.data.remote;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.boardnotes.data.local.entity.Note;
import com.example.boardnotes.domain.EnrichmentResult;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.ContentBlockedException;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.InvalidAPIKeyException;
import com.google.firebase.ai.type.PromptBlockedException;
import com.google.firebase.ai.type.QuotaExceededException;
import com.google.firebase.ai.type.Schema;
import com.google.firebase.ai.type.ServiceDisabledException;
import com.google.firebase.ai.type.UnsupportedUserLocationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AiLogicEnrichmentService implements EnrichmentService {
    private static final String TAG = "BoardNotes";
    //private static final String MODEL = "gemini-3.8-flash";
    private static final String MODEL = "gemini-3.5-flash-lite";
    private static final long TIMEOUT_SECONDS = 120;

    private static final String PROMPT =
            "You are reading a photograph of a whiteboard or handwritten study notes.\n"
                    + "Return JSON only.\n"
                    + "1. title: a short descriptive title, at most 8 words.\n"
                    + "2. category: ONE subject area, e.g. Mathematics, Software Engineering, "
                    + "Chemistry, Biology.\n"
                    + "3. diagram: if the image shows anything whose meaning is carried by CONNECTIONS "
                    + "between labelled parts, describe it as nodes and edges. That includes flowcharts, "
                    + "mind maps, architecture and network diagrams, state machines, and ALSO chemical "
                    + "reaction schemes - in a reaction scheme each distinct compound or species is a "
                    + "node, and each reaction arrow is an edge from reactant to product labelled with "
                    + "its reagents and conditions. Node ids must be short and unique. "
                    + "Include EVERY connection that is visible: where several parts play the same "
                    + "role, repeat the edges for each of them instead of showing one example. "
                    + "If the page is only prose, lists, or statements with no arrows or connections "
                    + "between them, return empty arrays for nodes and edges.\n"
                    + "Do not invent content that is not visible in the image.";

    private static Schema responseSchema() {
        Map<String, Schema> node = new LinkedHashMap<>();
        node.put("id", Schema.str());
        node.put("label", Schema.str());

        Map<String, Schema> edge = new LinkedHashMap<>();
        edge.put("from", Schema.str());
        edge.put("to", Schema.str());
        edge.put("label", Schema.str());

        Map<String, Schema> diagram = new LinkedHashMap<>();
        diagram.put("nodes", Schema.array(Schema.obj(node)));
        diagram.put("edges", Schema.array(Schema.obj(edge)));

        Map<String, Schema> root = new LinkedHashMap<>();
        root.put("title", Schema.str());
        root.put("category", Schema.str());
        root.put("diagram", Schema.obj(diagram));

        return Schema.obj(root);
    }

    @Override
    public EnrichmentResult enrich(Note note) throws EnrichmentException {
        Bitmap image = BitmapFactory.decodeFile(note.imagePath);
        if (image == null) {
            // The file is gone or unreadable. Another attempt will not conjure it back.
            throw new EnrichmentException(
                    "Could not decode image: " + note.imagePath, null,
                    EnrichmentException.Kind.PERMANENT);
        }

        GenerationConfig config = new GenerationConfig.Builder()
                .setResponseMimeType("application/json")
                .setResponseSchema(responseSchema())
                .build();

        GenerativeModel model = FirebaseAI.getInstance()
                .generativeModel(MODEL, config);

        Content.Builder content = new Content.Builder()
                .addText(PROMPT)
                .addImage(image);

        if (note.ocrText != null && !note.ocrText.trim().isEmpty()) {
            // The OCR text is already on the device. Sending it costs a few hundred bytes
            content.addText("Text already extracted from this image by on-device OCR:\n"
                    + note.ocrText);
        }

        long start = System.currentTimeMillis();
        try {
            GenerateContentResponse response =
                    GenerativeModelFutures.from(model)
                            .generateContent(content.build())
                            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            long elapsed = System.currentTimeMillis() - start;
            String json = response.getText();

            if (json == null || json.trim().isEmpty()) {
                throw new EnrichmentException(
                        "Model returned no text", null, EnrichmentException.Kind.TRANSIENT);
            }
            Log.d(TAG, "Raw model JSON: " + json);
            EnrichmentResult result = parse(json);
            Log.i(TAG, "Enriched " + note.id.substring(0, 8) + " in " + elapsed + " ms: " + result);
            return result;

        } catch (TimeoutException e) {
            throw new EnrichmentException(
                    "Enrichment timed out", e, EnrichmentException.Kind.TRANSIENT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnrichmentException(
                    "Interrupted", e, EnrichmentException.Kind.TRANSIENT);
        } catch (ExecutionException e) {
            throw new EnrichmentException(
                    "Enrichment call failed: " + e.getMessage(), e, classify(e));
        } finally {
            image.recycle();
        }
    }

    private static EnrichmentException.Kind classify(Throwable t) {
        Throwable cause = t.getCause() != null ? t.getCause() : t;

        if (cause instanceof QuotaExceededException) {
            return EnrichmentException.Kind.QUOTA;
        }
        if (cause instanceof InvalidAPIKeyException
                || cause instanceof ServiceDisabledException
                || cause instanceof UnsupportedUserLocationException
                || cause instanceof PromptBlockedException
                || cause instanceof ContentBlockedException) {
            return EnrichmentException.Kind.PERMANENT;
        }
        return EnrichmentException.Kind.TRANSIENT;
    }

    private static EnrichmentResult parse(String json) throws EnrichmentException {
        try {
            JSONObject root = new JSONObject(json);
            String title = root.optString("title", null);
            String category = root.optString("category", null);

            String diagramJson = null;
            JSONObject diagram = root.optJSONObject("diagram");
            if (diagram != null && diagram.optJSONArray("nodes") != null
                    && diagram.optJSONArray("nodes").length() > 0) {
                // Stored verbatim. DiagramView parses it.
                diagramJson = diagram.toString();
            }

            return new EnrichmentResult(title, category, diagramJson);

        } catch (JSONException e) {
            // The schema should make this impossible. If it happens, the shape changed.
            // Retrying the same request will produce the same unparseable answer.
            throw new EnrichmentException("Could not parse model response", e,
                    EnrichmentException.Kind.PERMANENT);
        }
    }
}
