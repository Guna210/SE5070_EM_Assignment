package com.example.boardnotes.ui.capture;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.boardnotes.R;
import com.example.boardnotes.databinding.ActivityCaptureBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class CaptureActivity extends AppCompatActivity {
    private static final String TAG = "BoardNotes";

    private ActivityCaptureBinding binding;
    private CaptureViewModel viewModel;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CaptureViewModel.class);

        binding.shutter.setOnClickListener(v -> takePhoto());

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.preview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                provider.unbindAll();
                provider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Could not start the camera", e);
                Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        binding.shutter.setEnabled(false);

        final File temp;
        try {
            temp = File.createTempFile("capture", ".jpg", getCacheDir());
        } catch (IOException e) {
            Log.e(TAG, "Could not create a temporary capture file", e);
            binding.shutter.setEnabled(true);
            return;
        }

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(temp).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        viewModel.onImageCaptured(temp);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "Capture failed", e);
                        Toast.makeText(CaptureActivity.this,
                                R.string.capture_failed, Toast.LENGTH_SHORT).show();
                        if (!temp.delete()) {
                            Log.w(TAG, "Temporary capture not deleted: " + temp);
                        }
                        binding.shutter.setEnabled(true);
                    }
                });
    }
}
