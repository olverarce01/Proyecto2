package com.example.proyecto2;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

public class CameraActivity extends AppCompatActivity{
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);
        /*
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                }catch (ExecutionException|InterruptedException e){
                    e.printStackTrace();
                }
            }
        },ContextCompat.getMainExecutor(this));*/
        
        cameraProviderFuture.addListener(()->{
            try {
                ProcessCameraProvider cameraProvider=cameraProviderFuture.get();
                //bindPreview(cameraProvider);
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        },ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        Preview preview=new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        ImageAnalysis imageAnalysis=
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(previewView.getWidth(),previewView.getHeight()))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees=imageProxy.getImageInfo().getRotationDegrees();
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage=imageProxy.getImage();
                if(mediaImage!=null){
                    InputImage image= InputImage.fromMediaImage(mediaImage,rotationDegrees);
                    ObjectDetectorOptions options =
                            new ObjectDetectorOptions.Builder()
                                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                                    .enableClassification()  // Optional
                                    .build();
                    ObjectDetector objectDetector = ObjectDetection.getClient(options);
                    objectDetector.process(image)
                            .addOnSuccessListener(detectedObjects -> {
                                for (DetectedObject detectedObject : detectedObjects) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    Integer trackingId = detectedObject.getTrackingId();
                                    for (DetectedObject.Label label : detectedObject.getLabels()) {
                                        String text = label.getText();
                                        if (PredefinedCategory.FOOD.equals(text)) {
                                        }
                                        int index = label.getIndex();
                                        if (PredefinedCategory.FOOD_INDEX == index) {
                                        }
                                        float confidence = label.getConfidence();
                                        Log.d("xd", "objeto: "+label.getText()+" index: "+label.getIndex()+" confidence:"+label.getConfidence());
                                    }

                                }

                                //Log.d("xd", "#objetos: "+String.valueOf(detectedObjects.size()));
                                //Log.d("TAG", "onSuccess" + detectedObjects.size());
                            })
                            .addOnFailureListener(e -> Log.e("TAG", e.getLocalizedMessage()))
                            .addOnCompleteListener(result -> imageProxy.close());
                }//imageProxy.close();

            }
        });
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis,preview);
    }

/*
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        Preview preview=new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        ImageAnalysis imageAnalysis=
                new ImageAnalysis.Builder()
                .setTargetResolution(new Size(previewView.getWidth(),previewView.getHeight()))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees=imageProxy.getImageInfo().getRotationDegrees();
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage=imageProxy.getImage();
                if(mediaImage!=null){
                    InputImage image= InputImage.fromMediaImage(mediaImage,rotationDegrees);
                    ObjectDetectorOptions options =
                            new ObjectDetectorOptions.Builder()
                                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                                    .enableClassification()  // Optional
                                    .build();
                    ObjectDetector objectDetector = ObjectDetection.getClient(options);
                    objectDetector.process(image)
                            .addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
                                @Override
                                public void onSuccess(@NonNull List<DetectedObject> detectedObjects) {
                                    for (DetectedObject detectedObject : detectedObjects) {
                                        Rect boundingBox = detectedObject.getBoundingBox();
                                        Integer trackingId = detectedObject.getTrackingId();
                                        for (DetectedObject.Label label : detectedObject.getLabels()) {
                                            String text = label.getText();
                                            if (PredefinedCategory.FOOD.equals(text)) {
                                            }
                                            int index = label.getIndex();
                                            if (PredefinedCategory.FOOD_INDEX == index) {
                                            }
                                            float confidence = label.getConfidence();
                                        }
                                    }
                                    Log.d("xd", String.valueOf(detectedObjects.size()));
                                }
                            });
                }imageProxy.close();

            }
        });
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this,cameraSelector,preview,imageAnalysis);
    }*/
}