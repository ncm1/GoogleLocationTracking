package com.example.nathanmorgenstern.googlelocationtracking;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
    private GoogleMap mMap = null;

    /*Variables for getting location data*/
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LocationCallback mLocationCallback;
    private Boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "MAP_ACTIVITY";
    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7172;

    private double mLat;
    private double mLong;
    private Boolean extraSuccess = false;

    private String mAddressOutput;
    private String mNewLatitude;
    private String mNewLongitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        /*
        requestPermissions();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        defineLocationRequest();
        defineLocationCallback();
        //TODO: add boolean variable to indicate if "tracking mode" is on or off
        startLocationUpdates();
        */


        Intent intent = getIntent();
        mLat = intent.getDoubleExtra("mLatitude", -500);
        mLong = intent.getDoubleExtra("mLongitude", -500);

        if(mLat != -500 && mLong != -500)
            extraSuccess = true;

        MapFragment mf = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFrag);
        mf.getMapAsync(this);

        View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        };

        //setUpMapIfNeeded();
    }

    @SuppressLint("MissingPermission")
    private void setUpMapIfNeeded() {

        Log.v(TAG, "setUpMapIfNeeded() called");
        if (mMap != null) {
            Log.v(TAG, "mMap != null");
            // Initialize map options.
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            //Setting up Map Marker to current location
            LatLng currentLocation = new LatLng(mLat, mLong);
            mMap.addMarker(new MarkerOptions().position(currentLocation)
                    .title("Current Location"));
            // Show current location
            if(extraSuccess)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17));
            return;
        }

        if (mMap == null){
            Log.v(TAG, "mMap == null");
            //mMap = (GoogleMap) getFragmentManager().findFragmentById(R.id.mapFrag);
        }

    }

    /* ACTIVITY LIFE CYCLE METHODS */
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onMapReady(GoogleMap mMap) { // map is loaded but not laid out yet
        this.mMap = mMap;
        mMap.setOnMapLoadedCallback(this);
    }

    @Override
    public void onMapLoaded() {
        //code to run when the map has loaded;
        Log.v(TAG, "Map Loaded");
        setUpMapIfNeeded();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

}

