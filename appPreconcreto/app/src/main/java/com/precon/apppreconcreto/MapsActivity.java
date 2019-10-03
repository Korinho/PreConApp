package com.precon.apppreconcreto;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.precon.apppreconcreto.MainActivity.SHARED_PREFS;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {


    private static final int PERMISSION_CODE = 1000 ;
    private static final int REQUEST_TAKE_PHOTO = 1 ;
    private static final int PICK_IMAGE = 2 ;
    private GoogleMap mMap;
    public int userid;
    public String user;
    private Uri uri= null;
    private boolean seTomofoto = false;
    private Button Savemycomment;
    private Button gallery;
    Bitmap bitmap;
    private ImageView fotoTomada;
    private SharedPreferences preferences;
    Dialog dialogObs;
    private EditText write;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private String textoObservacion = "";
    private static final String FINE_LOCATION=Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 7f;
    public Boolean mLocationPermissionsGranted = false;
    private ProgressDialog progress;
    private boolean cargando = false;

    //En nuestro metodo onCreate referenciaremos nuestras variables asi como tambien ciertos metodos
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        dialogObs = new Dialog(MapsActivity.this); //inicializamos nuestra ventana de dialogo que aparecera cuando agregamos un marcador.
        dialogObs.setContentView(R.layout.dialog_template);
        write = dialogObs.findViewById(R.id.write); //instanciamos la variable del EditText de nuestro dialogo emergente
        Savemycomment = dialogObs.findViewById(R.id.save);// instanciamos el boton de GUARDAR de nuestro dialogo emergente
        gallery = dialogObs.findViewById(R.id.gallery);
        fotoTomada = dialogObs.findViewById(R.id.TomarFoto); //instanciamos nuestro imageView que actua como boton para tomar la foto
        write.setEnabled(true); //asignamos las variables como verdaderas para que se puedan visualizar
        Savemycomment.setEnabled(true);
        fotoTomada.setEnabled(true);

        setContentView(R.layout.activity_maps);
        preferences = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE); //mandamos a llamar nuestra variable para entrar nuestro userid a esta actividad

        if(preferences.getBoolean("isLogged", false) == false){
            finish();
        }

        userid = preferences.getInt("id_usuario",0); //Obtenemos su valor tal cual se llama su parametro en la actividad anterior
        String nombre = preferences.getString("nombre",""); //Obtenemos el nombre del usuario
        getLocationPermission(); //inicializamos nuestro metodo para obtener permisos de localizacion en nuestro mapa
        initMap(); //metodo para iniciar nuestro mapa con todos sus componentes
        setTitle(nombre); // Se establece el nombre de usuario en la barra superior de la App

        progress = new ProgressDialog(this); // barra de progreso
        progress.setTitle("Subiendo ubicación");
        progress.setMessage("Un momento...");
        progress.setCancelable(false); // previene sea ocultado vía clic

        dialogObs.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                write.setText("");
                bitmap = null;
                uri = null;
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap; //inicializamos nuestra variable del mapa

     if(mLocationPermissionsGranted){  // iniciamos nuestro if para verificar si tenemos permisos activos o denegados
            getDeviceLocation(); //metodo para recuperar la ubicacion actual de nuestro dispositivo
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) // verifica si el permiso de localizacion fue permitido o denegado
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                    (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){ //verifica si el permiso de camara y para acceder a galeria fue permitido o denegado
                if (checkSelfPermission(Manifest.permission.CAMERA)==
                PackageManager.PERMISSION_DENIED||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED){
                    String[] permission = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(permission,PERMISSION_CODE);
                }
            }
            mMap.setMyLocationEnabled(true); //concatenamos nuestra variable de mapa para activar el boton de "mi localizacion", se lo asignamos con valor verdadero
            mMap.getUiSettings().setZoomControlsEnabled(true); //concatenamos de igual manera los controles para dar zoom al mapa, se lo asignamos con valor verdadero
        }


        //METODO PARA AGREGAR UN MARCADOR
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() { //metodo de long click que nos permitira agregar un marcador en el mapa
            @Override
            public void onMapLongClick(LatLng latLng) {
                try { //encerramos nuestro siguiente codigo en un try & catch
                    seTomofoto = false; //ponemos nuestra variable falsa por que se inicia por primera vez el dialogo para tomar foto, cuando se tome se vuelve true
                    final LatLng latLng1 = latLng; //declaramos nuestra variable latLng1 para obtener latitud y longitud de la ubicacion que querramos guardar
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault()); //creamos una varialble geocoder para obtener la direccion, latitud y longitud de cualquier ubicacion
                    final List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); //creamos una lista de donde obtendremos la latitud y// longitud
                    //Log.d("Test", Double.toString(latLng.latitude))
                    ;
                    Savemycomment.setOnClickListener(new View.OnClickListener() { //creamos el metodo onClickListener para que guarde la observacion y la foto en la API
                        @Override
                        public void onClick(View v) {
                            textoObservacion = write.getText().toString(); //Obtenemos el texto escrito de nuestra variable EditText

                            if(validateObservacion(textoObservacion,fotoTomada)){ //agregamos con un if nuestro metodo para validar observacion y foto
                                fotoTomada.setImageBitmap(bitmap);
                                postUbicacion(addresses,latLng1,textoObservacion); // posteamos la ubicacion con los datos requeridos
                                dialogObs.cancel();
                                uri=null;
                                progress.show();
                            }else{
                            }
                        }
                    });
                    dialogObs.show();
                    fotoTomada.setImageDrawable(getResources().getDrawable(R.drawable.imagencamara));
                    fotoTomada.setOnClickListener(new View.OnClickListener() { // metodo que se ejecuta al presionar el imageView de la camara
                        @Override
                        public void onClick(View v) {
                            Log.d("foto","iniciando camara");
                            tomarFoto();
                        }
                    });

                    gallery.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            galeria();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
          volleyProcess(); //mandamos a llamar nuestro metodo de actualizar marcadores

    }

    //// Método para crear el menú

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.hibrido:
                CambiarHibrido();
                return true;
            case R.id.satelital:
                CambiarSatelital();
                return true;
            case R.id.normal:
                CambiarNormal();
                return true;
            case R.id.terreno:
                CambiarTerreno();
                return true;
            case R.id.close_session:
                cerrar();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    //////////////// Tomar la foto

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void tomarFoto(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.precon.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                uri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    /////////////   Foto elegida de galería

    public void galeria (){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }


    //Metodo que nos verifica si esta vacio o ya hay una foto tomada, se muestra la foto en el imageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            seTomofoto = true; //se valida que hay foto

            try {
                fotoTomada.setImageURI(uri); // Se asigna la foto a nuestro ImageView
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //super.onActivityResult(requestCode,resultCode, data);

        //// Esa acción se inicia solo si se eligió una imagen de galería
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            uri = data.getData();
            try {
                fotoTomada.setImageURI(uri);
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


//Metodo para pasar la image a formato String
    private String imageToString(Bitmap bitmap){ //creamos nuestro metodo imageToString que recibira un objeto bitmap
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // creamos un objeto ByteArrayOutputStream
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream); //concatenamos nuestra variable bitmap para convertirla a formato JPEG
        byte[] imgBytes = byteArrayOutputStream.toByteArray(); //creamos un arreglo de bytes y lo igualamos a nuestra variable byteArray...
        String encodedImage = Base64.encodeToString(imgBytes,Base64.DEFAULT); //creamos una variable String en la cual convertiremos nuestra imagen en bytes a codificarla en formato BASE64
        return encodedImage; //devolvemos nuestra variable String
    }
    //Metodo para validar si el usuario ya escribio su comentario o si ya ya tomo la foto, el usuario no puede agregar una ubicacion si no contiene ninguna de estas dos variables
    private boolean validateObservacion(String observacion, ImageView fotoTomada){
        if (observacion == null || observacion.trim().length() == 0) {
            Toast.makeText(this, "Se requiere su comentario", Toast.LENGTH_LONG).show();
            return false;
        }
        /*if (seTomofoto == false) {
            Toast.makeText(this, "Se requiere foto", Toast.LENGTH_LONG).show();      //Comentariado debido a que se puede enviar la foto vacía
            return false;
        }*/
        return true;
    }

    //metodo para POSTEAR Y GUARDAR ubicaciones
    private void postUbicacion( final List<Address> address, final LatLng latLng, final String textoObservacion) {
        // 2019-07-10 18:01:00.755 6573-6573/com.example.apppreconcreto D/postUbicaciones:: [Address[addressLines=[0:"Justino Sarmiento 63, El Maestro, 91920 Veracruz, Ver., México"],feature=63,admin=Veracruz,sub-admin=null,locality=Veracruz,thoroughfare=Justino Sarmiento,postalCode=91920,countryCode=MX,countryName=México,hasLatitude=true,latitude=19.159224,hasLongitude=true,longitude=-96.1323003,phone=null,url=null,extras=null]] - lat/lng: (19.15922132332285,-96.13221075385809)
        RequestQueue queue = Volley.newRequestQueue(this); //creamos nuestro objeto RequestQueue
        String URL = "https://preconcretover.com/restapi/v2/ubicaciones"; //creamos una variable para nuestra URL la cual es nuestra direccion para postear en nuestra API rest

        StringRequest jsonRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() { //En nuestro jsonRequest le indicamos que usaremos el metodo POST y agregamos nuestra variable URL
            @Override
            public void onResponse(String response) {
                try { //rodeamos nuestro metodo con un try & catch
                    JSONObject obj = new JSONObject(response); //creamos un objeto json al cual le pasaremos nuestro String response
                    boolean error = obj.getBoolean("error"); //creamos una variable booleana la cual le agregaremos nuestro objeto Json con el nombre "error"
                    if(error==false){ //validamos si error es falso, se agrega nuestro marcador
                        cargando = true;
                    }else{
                        Toast.makeText(MapsActivity.this,"Error al agregar un marcador",Toast.LENGTH_SHORT).show(); //en caso de error mostramos el mensaje de error con un toast
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                //TODO: handle failure
            }
        }) {

            @Override
            public String getBodyContentType() { //declaramos este metodo para que nos regrese el formado urlencoded
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            //en esta seccion creamos un mapa de Strings en el cual pasaremos nuestros parametros para postearlos en la API REST
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap();
                String imageData = bitmap == null ? null : imageToString(bitmap) ; //obtenemos nuestra imagen ya transformada a String
                params.put("id_usuario", String.valueOf(userid)); //pasamos nuestra variable userID previamente obtenida de la clase anterior
                params.put("nombre", address.get(0).getThoroughfare() + '#'+ address.get(0).getFeatureName()); //obtenemos y pasamos el nombre de la direccion de la ubicacion marcada
                params.put("latitud",Double.toString(latLng.latitude) ); //obtenemos y pasamos la latitud de la ubicacion marcada
                params.put("longitud", Double.toString(latLng.longitude)); //obtenemos y pasamos la longitud de la ubicacion marcada
                params.put("imagen", imageData == null ? "" : imageData); //obtenemos y pasamos nuestra imagen
                params.put("tipo", Integer.toString(0)); //pasamos una variable entera convertida a string con valor de 0
                params.put("direccion", address.get(0).getAddressLine(0)); //obtenemos y pasamos la direccion de la ubicacion
                params.put("ciudad", address.get(0).getLocality() == null ? "" : address.get(0).getLocality()); //obtenemos y pasamos la localidad osea la ciudad de la ubicacion
                params.put("observaciones",textoObservacion ); //obtenemos y pasamos nuestro texto con la observacion ya escrita
                bitmap = null;
                return params;
            }


            //Metodo para pasar por el Header el token de autorizacion para acceder a la API
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "3d524a53c110e4c22463b10ed32cef9d");
                return headers;
            }
        };
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(   // Se le asignan politicas tiempo de espera y reintentos a la petición
                25000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonRequest); //a nuestro objeto queue le agregamos nuestro jsonRequest para completar nuestro metodo de registrar una ubicacion en la API REST
    }




    //// Cambiar configuración del botón de atrás, para que al presionarlo se salga de la aplicación
    public void onBackPressed(){
        moveTaskToBack(true);

    }


//Metodo que nos pide permisos de localizacion al iniciar la actividad con mapa
    public void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
  //En esta seccion de metodos ponemos en funcion los botenes para cambiar de vista el mapa
    public void CambiarHibrido() {
     mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

     public void CambiarSatelital() {
       mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
     }

     public void CambiarTerreno() {
       mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
     }

    public void CambiarNormal() {
      mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }
 // Metodo para acceder a la localizacion de nuestro telefono
    private void getDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){ //si el task encuentra nuestra ubicacion manda a la vista a nuestra localizacion actual
                            //Log.d("maps","onComplete: found location!");
                            Location currentLocation = (Location) task.getResult(); //concatenamos nuestra variable locacion con el resultado obtenido del task
                            moveCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()), DEFAULT_ZOOM); //mueve la vista hacia nuestra localizacion actual
                        }else {
                            //Log.d("maps","onComplete: current location is null!"); // de lo contrario mostramos un mensaje en consola diciendo que la localizacion es nula
                        }
                    }
                });
            }
        }catch (SecurityException e){
            //Log.e("maps","getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }
    //Metodo que mueve la camara de la aplicacion hacia nuestra ubicacion actual
    private void moveCamera(LatLng latLng, float zoom){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
    }


//METODO GET PARA RECUPERAR MARCADORES

    public void volleyProcess(){ //este metodo lo mandamos a llamar cuando iniciamos por primera vez nuestro mapa y posteriormente en el metodo para refrescar marcadores
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://preconcretover.com/restapi/v2/ubicaciones";
        //Log.d("Test","Prueba");
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() { //como el metodo post declaramos ua variable requestQueue, una variable para la URL y le indicamos a nuestro metodo que sera una peticion GET
            @Override
            public void onResponse(String response) {
                //Log.d("MapsResponse", response);
                crearMarcadores(response); //en nuestro metodo onResponse le pasamos el string response a nuestro metodo crearMarcadores
                if (cargando == true){
                    progress.dismiss();
                    cargando = false;
                }
                refreshAllContent(10000); //tambien mandamos a llamar al metodo refreshAllContent y se le da el valor de 10000 osea 10 segundos
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.d("MapsError", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError { //le pasamos el metodo para el token de autorizacion
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "3d524a53c110e4c22463b10ed32cef9d");
                return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                25000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }
    //En este metodo se crearan los marcadores que ya estan almacenados en la API
    private void crearMarcadores(String response) {
        try {
            //creamos un objeto json y un jsonArray en el cual buscara las "ubicaciones"
            JSONObject object = new JSONObject(response);
            JSONArray ubicaciones = object.getJSONArray("ubicaciones");
            //con un for recorremos el arreglo ubicaciones
            for(int i = 0; i < ubicaciones.length();i++){
                JSONObject ubicacion  	= ubicaciones.getJSONObject(i);
                //obtenemos en variables String la latitud y longitud y las pasamos a variables doubles
                String latitud  = ubicacion.getString("latitud");
                String longitud 	= ubicacion.getString("longitud");

                double lat = Double.valueOf(latitud);
                double lon = Double.valueOf(longitud);
                //ahora crea marcador
                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                LatLng latLng = new LatLng(lat,lon);
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                //validamos si el mapa viene vacio, si lo esta nos crea los marcadores ya guardados
                if(addresses.size() > 0){
                    mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marcador_pcv))
                            .anchor(1.0f, 1.0f)
                            .title(addresses.get(0).getAddressLine(0))
                            .position(latLng));
                }


            }

       } catch (JSONException e) {
           e.printStackTrace();
       } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //metodo que determina el tiempo en que se actualizan los marcadores guardados en la API REST
    public  void refreshAllContent(final long timetoupdate) {
        new CountDownTimer(timetoupdate, 10000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
               // Log.i("SCROLLS ", "UPDATE CONTENT HERE ");
                volleyProcess();
            }
        }.start();
    }
//Metodo que inicializa el fragmento del mapa y lo valida que no venga vacio
    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null){
            mapFragment.onCreate(null);
            mapFragment.onResume();
            mapFragment.setRetainInstance(true);
            mapFragment.getMapAsync(MapsActivity.this);
        }
    }
        //metodo que valida que los permisos sean concedidos o denegados, permisos de localizacion y de acceso a camara y galeria
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            mLocationPermissionsGranted = false;
            switch (requestCode){
                case LOCATION_PERMISSION_REQUEST_CODE:{
                    if(grantResults.length > 0 ){
                        for(int i=0;i < grantResults.length; i++){
                            if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                                mLocationPermissionsGranted = false;
                                return;
                            }
                        }
                        mLocationPermissionsGranted=true;
                        initMap();
                    }
                }
                case PERMISSION_CODE:{
                    if(grantResults.length > 0 && grantResults [0] ==
                            PackageManager.PERMISSION_GRANTED){
                    }else{
                        Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }


    //////////////// Cargar y guardar configuración


    //////// Guardar la configuración

    public void guardarConfiguracion() {
        SharedPreferences prefs =
                getSharedPreferences( SHARED_PREFS , MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("IsLogged", true);
        editor.putInt("id_usuario", userid);
        editor.commit();
    }


    // Cerrar la sesión

    public void cerrar(){
        SharedPreferences prefs =
                getSharedPreferences( SHARED_PREFS , MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLogged", false);
        editor.putInt("id_usuario", userid);
        editor.commit();
        finish();
    }





}





