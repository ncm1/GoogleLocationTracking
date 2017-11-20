package com.example.nathanmorgenstern.googlelocationtracking;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
    private GoogleMap mMap = null;

    /*Variables for getting location data*/
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LocationCallback mLocationCallback;
    private Boolean mRequestingLocationUpdates = false;


    /*Variables for storing location data */
    private Location mLastLocation;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private double mLastLatitude  = -111.0;
    private double mLastLongitude = -111.0;
    private double mCurrentLatitude = mLastLatitude;
    private double mCurrentLongitude = mLastLongitude;
    private String mLastCheckInTime = "null";
    private String mLastCheckInName = "null";


    private static final String TAG = "MAP_ACTIVITY";
    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7172;

    private double mLat;
    private double mLong;
    private Boolean extraSuccess = false;

    private String mAddressOutput;
    private String mNewLatitude;
    private String mNewLongitude;
    private Dialog dialog = null;

    /* Variables used for database */
    private MySQLHelper sqlHelper;

    //Helper/Utility variables
    private Boolean onResume = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        requestPermissions();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        sqlHelper = new MySQLHelper(this);
        defineLocationRequest();
        defineLocationCallback();
        mRequestingLocationUpdates = true;
        //TODO: add boolean variable to indicate if "tracking mode" is on or off
        startLocationUpdates();

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
                    .title("Current Location")).setDraggable(true);
            //Place markers of previous check ins
            placeMarkers();
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

    public void updateMap(){
        //Setting up Map Marker to current location
        LatLng currentLocation = new LatLng(mLat, mLong);
        mMap.addMarker(new MarkerOptions().position(currentLocation)
                .title("Current Location")).setDraggable(true);
        addDraggableActionListener();
        //Place markers of previous check ins
        placeMarkers();
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
        addDraggableActionListener();
        //placeMarkers();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    /* LOCATION METHODS  */

    public void updateLocation(Location location){

        //mLastLocation = mCurrentLocation;
        mLastLocation = location;

        mLastLatitude  = location.getLatitude();
        mLastLongitude = location.getLongitude();

        String tempName = withinRadius(mLastLatitude,mLastLongitude);
        Log.v(TAG, "updateLocation()->tempName: " + tempName);

        if(!tempName.equals("No name in radius")) {
            String time = sqlHelper.getLastCheckInTimeForLocation(tempName);
            if(!time.equals("none found")) {
                showDialog(tempName, time);
            }
            else if(dialog != null)
                dialog.dismiss();
        }
        else if(dialog != null)
            dialog.dismiss();
        //updateText();
    }

    public void defineLocationCallback(){
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location tempLocation = null;
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    tempLocation = location;
                }
                Log.v(TAG, "new location");
                updateLocation(tempLocation);
                updateMap();
            };
        };
    }

    public void defineLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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

    public void showDialog(String name, String time){
        // custom dialog
        if(dialog != null)
            dialog.dismiss();

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.simple_text_view);
        dialog.setTitle("Title...");

        // set the custom dialog components - text, image and button
        TextView check_in_name = (TextView) dialog.findViewById(R.id.locationCheckInName);
        check_in_name.setText("Check in name: " + name);
        TextView check_in_time = (TextView) dialog.findViewById(R.id.locationCheckInTime);
        check_in_time.setText("Check in time: " + time);

        Button dialogButton = (Button) dialog.findViewById(R.id.btnOkDialog);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dialog != null)
                    dialog.dismiss();
                //dialog = null;
            }
        });

        if(dialog != null)
            dialog.show();
    }

    public String withinRadius(double lat, double lon){
        ArrayList<LocationInfo> locationInfoList = sqlHelper.getAllUniqueLocationCheckIns();
        int locationListSize = locationInfoList.size();

        Log.v(TAG, "locationInfoList.size(): " + locationListSize);

        Location loc1 = new Location("temp");
        loc1.setLatitude(lat);
        loc1.setLongitude(lon);

        double dist = 0.0;
        for(int i = 0; i < locationListSize; i++){
            Location loc2 = new Location("temp");
            loc2.setLatitude(Double.parseDouble(locationInfoList.get(i).getLatitude()));
            loc2.setLongitude(Double.parseDouble(locationInfoList.get(i).getLongitude()));
            dist = loc1.distanceTo(loc2);

            if(dist <= 30.0) {
                Log.v(TAG, "dist: " + dist + "m");
                return locationInfoList.get(i).getCheckInName();
            }
        }

        return "No name in radius";
    }

    public void placeMarkers(){

        ArrayList<LocationInfo> locationInfoList = sqlHelper.getAllUniqueLocationCheckIns();

        Log.v(TAG, "locationInfoList.size(): " + locationInfoList.size());
        for (int i = 0; i < locationInfoList.size(); i++)
        {
            String locationName = locationInfoList.get(i).getCheckInName();

            LatLng tempLatLng = new LatLng(Double.parseDouble(locationInfoList.get(i).getLatitude()),
                    Double.parseDouble(locationInfoList.get(i).getLongitude()));

            Log.v(TAG, "locationInfoList.get().getName(): " + locationName);
            Log.v(TAG, "latitude: " + Double.parseDouble(locationInfoList.get(i).getLatitude()));
            Log.v(TAG, "longitude: " + Double.parseDouble(locationInfoList.get(i).getLongitude()));
            mMap.addMarker(new MarkerOptions()
                    .position(tempLatLng)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.measle_blue))
            );
        }
    }
    /* END LOCATION METHODS */

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    public void addDraggableActionListener(){
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener(){

            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                Double lat = marker.getPosition().latitude;
                Double lon = marker.getPosition().longitude;

                String locId = marker.getTitle();

                Log.v(TAG,"lat: " + lat + " long: " + lon + "locId: " + locId);

            }
        });
    }

    /* DATABASE METHODS */
    public void addLocationToDatabase(){
        Log.v(TAG,"addLocationToDatabase()");

        String mLat = Double.toString(mLastLatitude);
        String mLon = Double.toString(mLastLongitude);
        String mTim = mLastCheckInTime;
        String mAdd = mAddressOutput;

        LocationInfo tempLocation = new LocationInfo(0,mLat, mLon, mTim, mAdd);

        sqlHelper.addLocationInfo(tempLocation);
        int fk = sqlHelper.getLocationTablePrimaryKey(mLat,mLon, mTim);
        if(!mLastCheckInName.equals("NULL")){

            String tempName = withinRadius(mLastLatitude,mLastLongitude);
            Log.v(TAG, "tempName: " + tempName);

            //No name found within the radius, add the check in name
            if(tempName.equals("No name in radius")) {
                sqlHelper.addCheckInName(mLastCheckInName, mAddressOutput, fk);
                int fk_checkIn = sqlHelper.getCheckInTablePrimaryKey(mLastCheckInName, mAddressOutput);
                sqlHelper.addToNormalized(fk, fk_checkIn);
            }

            //Name/address pair are already in the table, set them accordingly
            else if(!tempName.equals("No name in radius")){
                String address = sqlHelper.getAddressOfCheckIn(tempName);
                int fk_checkIn = sqlHelper.getCheckInTablePrimaryKey(tempName, address);
                sqlHelper.addToNormalized(fk, fk_checkIn);
            }
        }
        else{
            sqlHelper.addCheckInName(" ", mAddressOutput, fk);
            int fk_checkIn = sqlHelper.getCheckInTablePrimaryKey(" ", mAddressOutput);
            sqlHelper.addToNormalized(fk, fk_checkIn);
        }

    }

    public void updateTime(){
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        Date date = new Date();
        mLastCheckInTime = dateFormat.format(date);
        Log.v(TAG,"mLastCheckInTime: " + mLastCheckInTime);
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
                if(onResume) {
                    updateLocation(mLastLocation);
                    onResume = false;
                }
            }
        }
    }

}

