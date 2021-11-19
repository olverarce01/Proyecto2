package com.example.proyecto2;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements  TextToSpeech.OnInitListener{

    ArrayList<String> lugaresGeneral=new ArrayList<>();
    int LOCATION_REQUEST_CODE = 10001;
    GeoPoint myCurrentLocation;
    String myAddress;
    Button button1;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback= new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(locationResult ==null){
                return;
            }
            for(Location location: locationResult.getLocations()) {
                //Log.d(TAG, "onLocationResult: " + location.toString());
                try {
                    Geocoder geocoder=new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                    myCurrentLocation= new GeoPoint(location.getLatitude(),location.getLongitude());
                    myAddress=addresses.get(0).getAddressLine(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    };

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
        //añadiendo los tipos de lugares
        lugaresGeneral.add("Panadería");


        button1=findViewById(R.id.button1);
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
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepararRespuesta("consulta lugar panaderia 1000 metros");

            }
        });
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
    private String limpiarPalabra(String palabra)
    {   String normalizar = Normalizer.normalize(palabra, Normalizer.Form.NFD);
        String sintilde = normalizar.replaceAll("[^\\p{ASCII}]", "");
        String loEscuchado= sintilde.toLowerCase();
        return loEscuchado;
    }
    private void prepararRespuesta(String escuchado) {/*incluyendo limpia palabra*/
        String loEscuchado= limpiarPalabra(escuchado);
        int resultado;
        String respuesta = respuest.get(0).getRespuestas();
        for (int i = 0; i < respuest.size(); i++) {
            resultado = loEscuchado.indexOf(respuest.get(i).getCuestion());
            if(resultado != -1){
                respuesta = respuest.get(i).getRespuestas();
            }
        }
        ArrayList<String> sentencia;
        if(loEscuchado.contains("consulta")
                && loEscuchado.contains("lugar")
                &&(sentencia=stringToList(loEscuchado)).size()>=4
                && containsLimpio(sentencia.get(2),lugaresGeneral))
        {   int metros=Integer.valueOf(sentencia.get(3));
            String tipoLugar=containsMatch(sentencia.get(2),lugaresGeneral);
            lugaresMasCercanos(tipoLugar,metros);
            return;
        }
        switch (loEscuchado){
            case "panaderia mas cercana": lugarMasCercano("Panadería");
                        return;
            case "mi ubicacion":
                        respuesta="tu ubicacion es: "+myAddress+", con coordenadas: "+" latitud:"+myCurrentLocation.getLatitude()+" longitud:"+myCurrentLocation.getLongitude();
        }

        responder(respuesta);

    }
    private String containsMatch(String nombre, ArrayList<String> lista){
        for(String item:lista){
            if(limpiarPalabra(item).compareTo(nombre)==0){return item;}
        }
        return "";
    }
    private boolean containsLimpio(String nombre, ArrayList<String> lista){
        for (String item:lista){
            if(limpiarPalabra(item).compareTo(nombre)==0){return true;}
        }
        return false;
    }
    private ArrayList<String> stringToList(String buffer){
        ArrayList<String> list=new ArrayList<>();
        StringBuilder tarp= new StringBuilder("");
        for (int i=0;i<buffer.length();i++)
        {   if(buffer.charAt(i)!=' '){
            tarp.append(buffer.charAt(i));
        }
        else{
            list.add(tarp.toString());
            tarp=new StringBuilder("");
        }

        }
        if(!tarp.toString().isEmpty())
        {
            list.add(tarp.toString());
        }
        return list;
    }
    private String siguientePalabra(int indice,String oracion){
        StringBuilder buffer=new StringBuilder("");
        int i=0;
        while(oracion.charAt(i)==' ' && i<oracion.length())i++;
        for (;i<oracion.length();i++)
        {   if(oracion.charAt(i)!=' ')
            {buffer.append(oracion.charAt(i));}
            else{
            return buffer.toString();
            }
        }
        if(!buffer.toString().isEmpty())
        { return buffer.toString();
        }
        return "";
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

        return respuestas;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            checkSettingsAndStartLocationUpdates();
        }
        else{
            askLocationPermission();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }
    private void checkSettingsAndStartLocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Settings of device are satisfied and we can start location updates
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(MainActivity.this, 1001);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
    private void startLocationUpdates() {
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }catch (SecurityException s){
            Log.d("xd", s.toString());
        }
    }
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "askLocationPermission: you should show an alert dialog...");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                checkSettingsAndStartLocationUpdates();
            }
        }
    }
    private void lugaresMasCercanos(String Lugar, int radio){
        db.collection("lugaresGeneral")
                .whereEqualTo("tipoLugar", Lugar)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            int contadorListado=1;

                            ArrayList<String> posiblesLugares=new ArrayList<>();
                            speak(Lugar);
                            speak(String.valueOf(radio));
                            speak("se encontraron los siguientes lugares posicion, IDlugar");

                            for(QueryDocumentSnapshot document: task.getResult()){
                                Map<String, Object> data = document.getData();
                                GeoPoint point= (GeoPoint) data.get("gps");
                                int dis=calculateDistanceByHaversineFormula(myCurrentLocation.getLongitude(),myCurrentLocation.getLatitude(),point.getLongitude(),point.getLatitude());
                                if(dis<= radio){
                                    posiblesLugares.add(String.valueOf(contadorListado)+","+document.getId());
                                    speak("posicion: "+contadorListado+", IDlugar: "+document.getId()+", con una distancia de: "+String.valueOf(dis)+" Metros");
                                    contadorListado++;
                                }
                            }
                            //speak(posiblesLugares.toString());
                            respuesta.setText(posiblesLugares.toString());

                            //digitar el numero asociado al lugar
                        }else{
                            Log.d("xd","Error getting documents:", task.getException());
                        }
                    }
                });
    }
    private void lugarMasCercano(String Lugar){
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
                                int dis=calculateDistanceByHaversineFormula(myCurrentLocation.getLongitude(),myCurrentLocation.getLatitude(),point.getLongitude(),point.getLatitude());
                                if(dis< distanciaMenor){
                                    distanciaMenor=dis;
                                    idMenor=document.getId();
                                }

                            }
                            speak("El lugar mas cercano es: "+idMenor+", con una distancia de: "+String.valueOf(distanciaMenor)+" Metros");
                            respuesta.setText("El lugar mas cercano es: "+idMenor+", con una distancia de: "+String.valueOf(distanciaMenor)+" Metros");
                        }else{
                            Log.d("xd","Error getting documents:", task.getException());
                        }
                    }
                });
    }
    private void speak(String text){
        mTTS.speak(text,TextToSpeech.QUEUE_ADD,null);
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