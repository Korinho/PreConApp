package com.precon.apppreconcretotest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    EditText user;
    EditText pass;
    Button boton;
    public int userid;
    private SharedPreferences preferences;
    public static final String SHARED_PREFS = "sharedPrefs";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        user = findViewById(R.id.usuario);
        pass = findViewById(R.id.contraseña);
        boton = findViewById(R.id.boton_login);

         preferences = getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);



         if(preferences.getBoolean("isLogged", false) == true){
             Intent intent = new Intent(MainActivity.this, MapsActivity.class);
             startActivity(intent);
         }
        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = user.getText().toString().trim();
                String password = pass.getText().toString().trim();
               /*try {
                    password = "$a#.2" + SHA1(pass.getText().toString().trim());
                } catch(Exception e){}
*/
                if (validateLogin(username, password) ){
                    doLogin(username, password);

                }else {
                }
            }
        });

    }

    ///////////////Encriptar contraseña

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] textBytes = text.getBytes("iso-8859-1");
        md.update(textBytes, 0, textBytes.length);
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }


    /////////////////////////////////



    private boolean validateLogin(String username, String password) {
        if (username == null || username.trim().length() == 0) {
            Toast.makeText(this, "se requiere usuario", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password == null || password.trim().length() == 0) {
            Toast.makeText(this, "se requiere contraseña", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;

    }
    // en este metodo guardaremos nuestro ID de usuario en un SharedPreferences para trasladar su valor a la siguiente actividad

    private void guardarID(String response){
        try {

        JSONObject ob = new JSONObject(response);  //creamos un objeto Json que espere el string response
            JSONArray arr = ob.getJSONArray("usuario");   //creamos un arreglo para buscar nuestro usuario
            for (int i = 0; i < arr.length(); i++) {    //recorremos el arreglo con un for para encontrar el usuario y la almacenamos en una variable: userid
                JSONObject obj = arr.getJSONObject(i);
                userid=obj.getInt("id_usuario");
                String nombre = obj.getString("nombre");
                SharedPreferences.Editor editor= preferences.edit(); //metemos nuestra variable en el SharedPreferences para que quede almacenada
                editor.putInt("id_usuario",userid);
                editor.putString("nombre",nombre);
                editor.putBoolean("isLogged", true);
                editor.commit();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //metodo para enviar usuario y contraseña a la api y que devuelva acceso a la siguiente actividad
    private void doLogin(final String username, final String password) {

        RequestQueue queue = Volley.newRequestQueue(this); //creamos nuestro objeto RequestQue

        String URL = "https://preconcretover.com/restapi/v2/login"; //creamos nuestra variable con la direccion de la API

        StringRequest jsonRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            //Declaramos nuestro StringReguest el cual cual le vamos a asignar el metodo POST para que mande la informacion a la API
            @Override
            public void onResponse(String response) {
                try {     //en este metodo creamos un objeto Json y le pasaremos el response para asi guardarlo en el metodo guardarID
                    JSONObject obj = new JSONObject(response);
                    boolean login = obj.getBoolean("login"); //tambien creamos esta variable booleana para verificar si el login es true o false
                    guardarID(response);

                    if(login==true){  //creamos un metodo if para verficar si la variable login es verdadera, ingresa a la otra actividad, si no, se muestra un mensaje de error

                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        startActivity(intent);
                    }else{
                        Toast.makeText(MainActivity.this,"usuario o contraseña incorrecto",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("ErrorTest", error.toString());
                Toast.makeText(MainActivity.this,"Por el momento no es posible acceder al servicio",Toast.LENGTH_SHORT).show();
                //TODO: handle failure
            }
        }) {

            @Override
            public String getBodyContentType() {  //es necesario implementar estas lineas de codigo ya que es la manera en la que se va a enviar la informacion
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError { //aqui es donde vamos a poner nuestras variables username y password
                Map<String, String> params = new HashMap();
                params.put("usuario", username);    //indicamos en los parametros a enviar, primero el nombre de los campos de la API seguido de una coma y el nombre de la variable
                params.put("password", password);
                return params;
            }

            //en esta seccion del codigo le pasaremos por el Header el token de autorizacion, esto es para poder acceder a la API
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "3d524a53c110e4c22463b10ed32cef9d");
                return headers;
            }
        };

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(  // Se le asignan politicas tiempo de espera y reintentos a la petición
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonRequest);
    }
}




