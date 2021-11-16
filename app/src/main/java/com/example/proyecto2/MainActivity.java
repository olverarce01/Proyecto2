package com.example.proyecto2;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements  TextToSpeech.OnInitListener{
    private static final int RECONOCEDOR_VOZ=7;
    private TextView escuchando;
    private TextView respuesta;
    private ArrayList<Respuestas> respuest;
    private TextToSpeech leer;
    private static final String TAG= "MainActivity";
    private TextToSpeech mTTS;
    TextView tv1;
    FirebaseFirestore db = FirebaseFirestore.getInstance();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1=findViewById(R.id.text1);
        inicializar();
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == RECONOCEDOR_VOZ){
            ArrayList<String> reconocido = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String escuchado = reconocido.get(0);
            escuchando.setText(escuchado);
            prepararRespuesta(escuchado);
        }
    }
    private void prepararRespuesta(String escuchado) {
        String normalizar = Normalizer.normalize(escuchado, Normalizer.Form.NFD);
        String sintilde = normalizar.replaceAll("[^\\p{ASCII}]", "");

        int resultado;
        String respuesta = respuest.get(0).getRespuestas();
        for (int i = 0; i < respuest.size(); i++) {
            resultado = sintilde.toLowerCase().indexOf(respuest.get(i).getCuestion());
            if(resultado != -1){
                respuesta = respuest.get(i).getRespuestas();
            }
        }
        if(respuesta.compareTo("lmcp")==0)
        {   lugarMasCercano("Panadería");
            return;
        }
        responder(respuesta);
    }

    private void responder(String respuestita) {
        respuesta.setText(respuestita);
        leer.speak(respuestita,TextToSpeech.QUEUE_FLUSH,null,null);
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            leer.speak(respuestita, TextToSpeech.QUEUE_FLUSH, null, null);
        }else {
            leer.speak(respuestita, TextToSpeech.QUEUE_FLUSH, null);
        }*/
    }
    public void hablar(View v){
        Intent hablar = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        hablar.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "es-MX");
        startActivityForResult(hablar, RECONOCEDOR_VOZ);
    }
    private void inicializar() {
        escuchando = findViewById(R.id.tvEscuchando);
        respuesta= findViewById(R.id.tvRespondiendo);
        respuest=proveerDatos();
        leer = new TextToSpeech(this, this);
    }
    public ArrayList<Respuestas> proveerDatos(){
        ArrayList<Respuestas> respuestas = new ArrayList<>();
        respuestas.add(new Respuestas("defecto", "¡Aun no estoy programada para responder eso, lo siento!"));
        respuestas.add(new Respuestas("hola", "hola que tal"));
        respuestas.add(new Respuestas("chiste", "¿Sabes que mi hermano anda en bicicleta desde los 4 años? Mmm, ya debe estar lejos"));
        respuestas.add(new Respuestas("adios", "que descanses"));
        respuestas.add(new Respuestas("como estas", "esperando serte de ayuda"));
        respuestas.add(new Respuestas("nombre", "mis amigos me llaman Mina"));
        respuestas.add(new Respuestas("panaderia mas cercana", "lmcp"));

        return respuestas;
    }



    private void lugarMasCercano(String Lugar){                 /*falta el radio*/
        db.collection("lugaresGeneral")
                .whereEqualTo("tipoLugar", Lugar)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            int distanciaMenor=999999999;
                            String idMenor = "N.A";
                            for(QueryDocumentSnapshot document: task.getResult()){
                                Map<String, Object> data = document.getData();
                                GeoPoint point= (GeoPoint) data.get("gps");
                                int dis=calculateDistanceByHaversineFormula(-70.284974,-18.447101,point.getLongitude(),point.getLatitude());
                                if(dis< distanciaMenor){
                                    distanciaMenor=dis;
                                    idMenor=document.getId();
                                    //Log.d("xd",document.getId()+" d:"+ String.valueOf(dis));

                                }

                            }
                            speak("El lugar mas cercano es: "+idMenor+", con una distancia de: "+String.valueOf(distanciaMenor)+" kilometros");
                            respuesta.setText("El lugar mas cercano es: "+idMenor+", con una distancia de: "+String.valueOf(distanciaMenor)+" kilometros");
                        }else{
                            Log.d("xd","Error getting documents:", task.getException());
                        }
                    }
                });
    }
    private void speak(String text){
        mTTS.speak(text,TextToSpeech.QUEUE_FLUSH,null);
    }
    private static int calculateDistanceByHaversineFormula(double lon1, double lat1, double lon2, double lat2) {
        double earthRadius = 6371; // km
        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);
        double dlon = (lon2-lon1);
        double dlat = (lat2-lat1);
        double sinlat = Math.sin(dlat / 2);
        double sinlon = Math.sin(dlon / 2);
        double a = (sinlat * sinlat) + Math.cos(lat1)*Math.cos(lat2)*(sinlon*sinlon);
        double c = 2 * Math.asin (Math.min(1.0, Math.sqrt(a)));
        double distanceInMeters = earthRadius * c * 1000;
        return (int)distanceInMeters;
    }
    @Override
    public void onInit(int status) {

    }
}