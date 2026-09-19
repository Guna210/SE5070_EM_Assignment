package com.example.boardnotes.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class OcrEngine {
    private static final String TAG = "BoardNotes";

    private static final long TIMEOUT_SECONDS = 30;

    private OcrEngine() { }

    public static String extractText(Context context, String imagePath) {
        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            InputImage image = InputImage.fromFilePath(context, Uri.fromFile(new File(imagePath)));

            long start = System.currentTimeMillis();
            Text result = Tasks.await(recognizer.process(image), TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - start;

            String text = result.getText();
            if (text.trim().isEmpty()) {
                Log.i(TAG, "OCR found no text in " + elapsed + " ms");
                return null;
            }

            Log.i(TAG, "OCR extracted " + text.length() + " chars in "
                    + result.getTextBlocks().size() + " blocks, " + elapsed + " ms");
            return text;

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "OCR failed for " + imagePath, e);
            return null;
        } finally {
            // Releases the native detector. Without this the model stays resident.
            recognizer.close();
        }
    }
}
