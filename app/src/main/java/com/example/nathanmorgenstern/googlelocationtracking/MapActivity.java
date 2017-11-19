package com.example.nathanmorgenstern.googlelocationtracking;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
    private GoogleMap mMap = null;

    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "MAIN_ACTIVITY";
    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7172;

    private double mLat;
    private double mLong;
    private Boolean extraSuccess = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //checkPermissions();
        Intent intent = getIntent();
        mLat = intent.getDoubleExtra("mLatitude", -500);
        mLong = intent.getDoubleExtra("mLongitude", -500);

        if(mLat != -500 && mLong != -500)
            extraSuccess = true;

        MapFragment mf = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFrag);
        mf.getMapAsync(this);
        //setUpMapIfNeeded();
    }

    /*public void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Run-time request permission
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        }
    }*/

    private void setUpMapIfNeeded() {

        Log.v(TAG, "setUpMapIfNeeded() called");
        if (mMap != null) {
            Log.v(TAG, "mMap != null");
            // Initialize map options. For example:
            //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            // Show current location
            if(extraSuccess)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLat, mLong), 17));
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

