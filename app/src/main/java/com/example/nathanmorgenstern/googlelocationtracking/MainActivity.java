package com.example.nathanmorgenstern.googlelocationtracking;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import static android.R.attr.start;

public class MainActivity extends AppCompatActivity{

    private LocationRequest mLocationRequest;
    private LocationManager  locationManager;
    private LocationListener locationListener;
    private Location mLastLocation;
    private LocationCallback mLocationCallback;

    private FusedLocationProviderClient mFusedLocationClient;

    private static final String TAG = "MAIN_ACTIVITY";
    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7172;

    private static final long MIN_TIME = 0;
    private static final float MIN_DISTANCE = 0;

    private Button btn_start;
    private Button btn_trackLocation;
    private TextView latLongText;

    private double mLastLatitude  = -111.0;
    private double mLastLongitude = -111.0;

    private AddressResultReceiver mResultReceiver;
    private String mAddressOutput;
    private Boolean mAddressRequested;
    private Boolean mRequestingLocationUpdates = false;

    private static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    private static final String LOCATION_ADDRESS_KEY = "location-address";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupWidgets();
        requestPermissions();
        getLastLocation();
        defineLocationCallback();
        startLocationUpdates();

        mResultReceiver = new AddressResultReceiver(new Handler());

        mAddressRequested = false;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        if (mLastLocation != null)
            startIntentService();

        else
            Log.v(TAG, "mLastLocation == null \nStarting intent service");

        mAddressRequested = true;

    }

    public void setupWidgets(){
        btn_start         = (Button) findViewById(R.id.btnStart);
        btn_trackLocation = (Button) findViewById(R.id.btnTrackLocation);
        latLongText       = (TextView)findViewById(R.id.txtCoordinates);

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMapActivity();
            }
        });

        btn_trackLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                fetchAddressButtonHandler();
                getLastLocation();
                updateText();
            }
        });
    }

    public void requestPermissions() {
        Log.v(TAG, "requestPermissions()");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //Run-time request permission
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        }
    }

    public void getLastLocation() {
        Log.v(TAG,"getLastLocation()");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (mLastLocation == null) {
                mLastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (mLastLocation != null) {
                mLastLatitude  = mLastLocation.getLatitude();
                mLastLongitude = mLastLocation.getLongitude();
            }
            else
                Log.v(TAG, "still null motherfucker!");
        }
    }

    public void startIntentService() {
        Intent intent = new Intent(this, GeocoderIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    public void updateText(){
        Log.v(TAG,"updateText()");
        latLongText.setText(mLastLatitude + "/" + mLastLongitude + "\n " + mAddressOutput);
    }

    public void startMapActivity(){
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    /* ACTIVITY LIFE CYCLE METHODS */
    @Override
    protected void onStop() {
        super.onStop();// Remove the listener you previously added
        Log.v(TAG, "onStop()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //setupLocationUpdates();
                    getLastLocation();
                }
                break;
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                Log.v(TAG,"mAddressOutput: " + mAddressOutput);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }


        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.v(TAG, "onResultsReceived");
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            mAddressRequested = false;
        }
    }

    private void fetchAddressButtonHandler() {
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Location mLastKnownLocation = location;

                        // In some rare cases the location returned can be null
                        if (mLastKnownLocation == null) {
                            return;
                        }

                        if (!Geocoder.isPresent()) {
                            Log.v(TAG,"Geocoder is not present");
                            return;
                        }

                        // Start service and update UI to reflect new location
                        startIntentService();
                        //updateUI();
                        Log.v(TAG,"mAddressOutput: " + mAddressOutput);
                    }
                });
    }

    public void startLocationUpdates(){
        if(mFusedLocationClient != null) {
            Log.v(TAG, "mFusedLocationClient != null");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);

        }
        else
            Log.v(TAG, "mFusedLocationClient == null");
    }

    public void defineLocationCallback(){
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.v(TAG, "new location");
                    // Update UI with location data
                    // ...
                }
            };
        };
    }

    public void updateLocation(Location location){
        Log.v(TAG,"updateLocation()");
        mLastLocation = location;
        mLastLatitude = location.getLatitude();
        mLastLongitude = location.getLongitude();
        updateText();
    }



}
