package com.example.boardnotes.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public final class ImageStore {
    private static final String TAG = "BoardNotes";

    private static final int MAX_EDGE_PX = 1600;

    private static final int JPEG_QUALITY = 80;

    private static final String IMAGE_DIR = "captures";

    private ImageStore() { }

    // Runs on a background thread
    public static String saveCompressed(Context context, Uri source) throws IOException {
        long originalBytes = sourceSize(context, source);

        Bitmap bitmap = decodeDownsampled(context, source);
        bitmap = applyExifRotation(context, source, bitmap);

        File dir = new File(context.getFilesDir(), IMAGE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create directory: " + dir);
        }

        File target = new File(dir, UUID.randomUUID() + ".jpg");
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        try (OutputStream out = new FileOutputStream(target)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        } finally {
            bitmap.recycle();
        }

        Log.i(TAG, "Imported image: " + originalBytes + " bytes -> " + target.length()
                + " bytes (" + width + "x" + height + ")");

        return target.getAbsolutePath();
    }

    private static Bitmap decodeDownsampled(Context context, Uri source) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(context, source)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight);

        try (InputStream in = open(context, source)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
            if (bitmap == null) {
                throw new IOException("Could not decode image: " + source);
            }
            return bitmap;
        }
    }

    private static int calculateInSampleSize(int width, int height) {
        int sample = 1;
        int longestEdge = Math.max(width, height);
        while (longestEdge / (sample * 2) >= MAX_EDGE_PX) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap applyExifRotation(Context context, Uri source, Bitmap bitmap)
            throws IOException {
        int orientation;
        try (InputStream in = open(context, source)) {
            orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }

        int degrees;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    private static long sourceSize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index >= 0 && !cursor.isNull(index)) {
                    return cursor.getLong(index);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not read source size", e);
        }
        return -1;
    }

    private static InputStream open(Context context, Uri uri) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) {
            throw new IOException("Could not open: " + uri);
        }
        return in;
    }
}
