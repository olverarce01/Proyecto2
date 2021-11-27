package com.example.proyecto2;
import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

public class CameraActivity extends AppCompatActivity{
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private String bufferObject="";
    private TextToSpeech mTTS;
    private FirebaseTranslator englishEspanishTranslator;
    private FrameLayout frameLayout;
    Draw marco;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        FirebaseTranslatorOptions optionsTranslate=
                new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(FirebaseTranslateLanguage.EN)
                .setTargetLanguage(FirebaseTranslateLanguage.ES)
                .build();
        englishEspanishTranslator=
                FirebaseNaturalLanguage.getInstance().getTranslator(optionsTranslate);
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        englishEspanishTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(@NonNull Void unused) {
                        //model descargo
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //model no descargo
                    }
                });

        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);
        frameLayout= findViewById(R.id.frameLay);
        mTTS=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS){
                    int result=mTTS.setLanguage(Locale.getDefault());
                    if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("TTS","Language not supported");
                    }
                }
                else{
                    Log.e("TTS","Initialization failed");
                }
            }
        });
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
                    LocalModel localModel=
                            new LocalModel.Builder()
                                    .setAssetFilePath("mnasnet_1.3_224_1_metadata_1.tflite")
                                    .build();
                    CustomObjectDetectorOptions customObjectDetectorOptions =
                            new CustomObjectDetectorOptions.Builder(localModel)
                                    .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                                    .enableClassification()
                                    .setClassificationConfidenceThreshold(0.5f)
                                    .setMaxPerObjectLabelCount(3)
                                    .build();
                    ObjectDetector objectDetector =
                            ObjectDetection.getClient(customObjectDetectorOptions);
                    /*ObjectDetector objectDetector = ObjectDetection.getClient(options);*/
                    objectDetector.process(image)
                            .addOnSuccessListener(detectedObjects -> {
                                for (DetectedObject detectedObject : detectedObjects) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    crearMarco(boundingBox,detectedObject);
                                    Integer trackingId = detectedObject.getTrackingId();
                                    for (DetectedObject.Label label : detectedObject.getLabels()) {
                                        String text = label.getText();
                                        if (PredefinedCategory.FOOD.equals(text)) {
                                        }
                                        int index = label.getIndex();
                                        if (PredefinedCategory.FOOD_INDEX == index) {
                                        }
                                        float d=label.getConfidence();
                                        float confidence = label.getConfidence();
                                        if(bufferObject!=label.getText()){
                                            bufferObject=label.getText();
                                            /*
                                            englishEspanishTranslator.translate(label.getText())
                                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                                        @Override
                                                        public void onSuccess(@NonNull String s) {
                                                            textView.setText(s);
                                                        }
                                                    });*/
                                            //textView.setText("objeto: "+label.getText()+" confidence:"+d);
                                            //speak(label.getText()+" con un porcentaje de confianza del:"+label.getConfidence());
                                        }

                                    }

                                }
                            })
                            .addOnFailureListener(e -> Log.e("TAG", e.getLocalizedMessage()))
                            .addOnCompleteListener(result -> imageProxy.close());
                }

            }
        });
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,imageAnalysis,preview);
    }

    private void crearMarco(Rect boundingBox, DetectedObject detectedObject) {
        if(frameLayout.getChildCount()>1){frameLayout.removeViewAt(1);}
        marco= new Draw(this,boundingBox,detectedObject.getLabels().get(0).getText());
        frameLayout.addView(marco);
    }
    private void speak(String text){
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH,null);
    }



}