package com.example.michael.gasfinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.function.DoubleUnaryOperator;

public class GasFinder extends AppCompatActivity {

    private ArrayList<GasStation> nearbyStations;
    private Location currentLocation;
    private GasAPI gasAPI;
    boolean isBound;
    boolean isInitialized;
    boolean gotResponse;

    private GPSBinder binder;
    private final String INITIALIZE_STATUS = "initialization_status";
    public static final String NEARBY_STATIONS = "nearby_stations";
    public static final String CURRENT_LATITUDE = "current_latitude";
    public static final String CURRENT_LONGITUDE = "current_longitude";

    Button toHistory;
    Button toList;
    Button toMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gas_finder);

        toHistory = findViewById(R.id.toHistory);
        toList = findViewById(R.id.toList);
        toMap = findViewById(R.id.toMap);
        toHistory.setEnabled(false);
        toList.setEnabled(false);
        toMap.setEnabled(false);
        gotResponse = false;

        nearbyStations = new ArrayList<>();

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 0);

        gasAPI = new GasAPI(this);
        binder = new GPSBinder(this);

        if (savedInstanceState != null) {
            isInitialized = savedInstanceState.getBoolean(INITIALIZE_STATUS);
        }

        nearbyStations = new ArrayList<>();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            isBound = binder.bindGPSService();
        }
    }

    public void saveStations(JSONArray arr) {
        nearbyStations.clear();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                GasStation station = new GasStation(o);
                nearbyStations.add(station);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        gotResponse = true;
        toHistory.setEnabled(true);
        toList.setEnabled(true);
        toMap.setEnabled(true);
    }

    public void onClickList(View view){
        Intent intent = new Intent(this, ListActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(NEARBY_STATIONS, nearbyStations);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void onClickHistory(View view) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    public void onClickMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(NEARBY_STATIONS, nearbyStations);
        intent.putExtras(bundle);
        intent.putExtra(CURRENT_LATITUDE, currentLocation.getLatitude());
        intent.putExtra(CURRENT_LONGITUDE, currentLocation.getLongitude());

        startActivity(intent);
    }

    // this HAS to finish before sending to listings screen
    public void findStations() {
            currentLocation = binder.getSystemLocation();
            if (currentLocation != null) {
                gasAPI.getNearbyStations(currentLocation, 20, "reg", "distance");
            }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            binder.unBindGPSService();
            isBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInitialized && !isBound) {
            binder.bindGPSService();
        }
        gotResponse = false;
    }
}
