package com.parking_app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private LocationManager locationManager;
    private ImageButton myLocationCard, startNav, deleteAll, btnMas, btnMenos;
    private CardView btnAddCar, cardView, btnMyLocation, start;
    Button btnParking;
    private boolean isFollowingLocation = true;
    private double coordsLocationLat, coordsLocationLon;
    private CardView deletes;
    private TextView txtMin, txtKm;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // WebView setup
        webView = findViewById(R.id.webview);
        webView.clearCache(true);
        webView.clearHistory();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setGeolocationEnabled(true); // ← Habilita geolocalización JS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false); // Otorga permisos de ubicación desde JS
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                SharedPreferences prefs = getSharedPreferences("MisDatos", MODE_PRIVATE);
                float lat = prefs.getFloat("lat", 0f);
                float lon = prefs.getFloat("lon", 0f);

                if (lat != 0f && lon != 0f) {
                    Toast.makeText(MainActivity.this, "Shared recuperado", Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "lat: " + lat, Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "lon: " + lon, Toast.LENGTH_SHORT).show();

                    webView.evaluateJavascript(
                        "javascript:addMarkerParking(" + lat + ", " + lon + ");",
                        null
                    );
                }
            }
        });


        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.loadUrl("file:///android_asset/map.html");

        // Inicialización de LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Botones
        myLocationCard = findViewById(R.id.myLocation);
        startNav = findViewById(R.id.startNav);
        deleteAll = findViewById(R.id.delete);
        deletes = findViewById(R.id.deleteAll);
        btnMas = findViewById(R.id.mas);
        btnMenos = findViewById(R.id.menos);
        btnParking = findViewById(R.id.btnPark);
        btnAddCar = findViewById(R.id.btnAddCar);
        cardView = findViewById(R.id.cardView);
        txtMin = findViewById(R.id.txtMin);
        txtKm = findViewById(R.id.txtKm);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        start = findViewById(R.id.start);

        myLocationCard.setOnClickListener(v -> requestLocationPermissions());
        myLocationCard.performClick();

        btnParking.setOnClickListener(v -> {
            webView.evaluateJavascript(
                "javascript:addMarkerParking(" + coordsLocationLat + ", " + coordsLocationLon + ");",
                null
            );

            SharedPreferences prefs = getSharedPreferences("MisDatos", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("lat", (float) coordsLocationLat);
            editor.putFloat("lon", (float) coordsLocationLon);
            editor.apply();

            float lat = prefs.getFloat("lat", 0f);
            float lon = prefs.getFloat("lon", 0f);
            Toast.makeText(this, "lat: " + lat, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "lon: " + lon, Toast.LENGTH_SHORT).show();
            Log.d("SharedPrefs", "Latitud guardada: " + lat + ", Longitud guardada: " + lon);
        });


        startNav.setOnClickListener(v -> {
            webView.evaluateJavascript("navigateTo()", null);
            Toast.makeText(this, "Calculando la ruta, espera un momento...", Toast.LENGTH_SHORT).show();
            deletes.setVisibility(View.VISIBLE);
        });

        deleteAll.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar el marcador y la ruta?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    webView.evaluateJavascript("clearMap()", null);
                    cardView.setVisibility(View.GONE);
                    btnAddCar.setVisibility(View.VISIBLE);
                    int marginInPx = (int) getResources().getDisplayMetrics().density * 60; // 16dp a px
                    int marginInPx1 = (int) getResources().getDisplayMetrics().density * 40;

                    // Aplica el margen inferior a los botones
                    ViewGroup.MarginLayoutParams paramsUno = (ViewGroup.MarginLayoutParams) btnAddCar.getLayoutParams();
                    paramsUno.topMargin = marginInPx;
                    btnAddCar.setLayoutParams(paramsUno);

                    ViewGroup.MarginLayoutParams paramsDos = (ViewGroup.MarginLayoutParams) btnMyLocation.getLayoutParams();
                    paramsDos.topMargin = marginInPx;
                    btnMyLocation.setLayoutParams(paramsDos);

                    ViewGroup.MarginLayoutParams paramsTres = (ViewGroup.MarginLayoutParams) start.getLayoutParams();
                    paramsTres.topMargin = marginInPx1;
                    start.setLayoutParams(paramsTres);
                    SharedPreferences prefs = getSharedPreferences("MisDatos", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("lat");
                    editor.remove("lon");
                    editor.apply();
                    myLocationCard.performClick();
                    webView.reload();
                    Toast.makeText(this, "Se han eliminado las coordenadas y el marcador del auto.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();

        });

        btnMas.setOnClickListener(v -> webView.evaluateJavascript("zoomInMap()", null));
        btnMenos.setOnClickListener(v -> webView.evaluateJavascript("zoomOutMap()", null));
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndSendToJS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSendToJS();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndSendToJS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    coordsLocationLat = location.getLatitude();
                    coordsLocationLon = location.getLongitude();

                    webView.post(() -> webView.evaluateJavascript(
                        "javascript:addMarker(" + coordsLocationLat + ", " + coordsLocationLon + ");",
                        null
                    ));

                    locationManager.removeUpdates(this);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    Toast.makeText(MainActivity.this, "GPS desactivado", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Permisos no concedidos", Toast.LENGTH_SHORT).show();
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public void hideBtnParking() {
            runOnUiThread(() -> {
                if (btnAddCar != null) {
                    btnAddCar.setVisibility(View.GONE);
                }
            });
        }

        @JavascriptInterface
        public void showCardInfo() {
            runOnUiThread(() -> {
                if (cardView != null) {
                    cardView.setVisibility(View.VISIBLE);

                    int marginInPx = (int) getResources().getDisplayMetrics().density * 90; // 16dp a px
                    int marginInPx1 = (int) getResources().getDisplayMetrics().density * 170;

                    // Aplica el margen inferior a los botones
                    ViewGroup.MarginLayoutParams paramsUno = (ViewGroup.MarginLayoutParams) btnAddCar.getLayoutParams();
                    paramsUno.bottomMargin = marginInPx;
                    btnAddCar.setLayoutParams(paramsUno);

                    ViewGroup.MarginLayoutParams paramsDos = (ViewGroup.MarginLayoutParams) btnMyLocation.getLayoutParams();
                    paramsDos.bottomMargin = marginInPx;
                    btnMyLocation.setLayoutParams(paramsDos);

                    ViewGroup.MarginLayoutParams paramsTres = (ViewGroup.MarginLayoutParams) start.getLayoutParams();
                    paramsTres.bottomMargin = marginInPx1;
                    start.setLayoutParams(paramsTres);
                }
            });
        }

        @JavascriptInterface
        public void mostrarInfoRuta(String distancia, String duracion) {
            runOnUiThread(() -> {
                // Aquí puedes actualizar un TextView o mostrar un Toast, por ejemplo:
                txtMin.setText(duracion + " min");
                txtKm.setText(distancia + " Km");
            });
        }

    }
}
