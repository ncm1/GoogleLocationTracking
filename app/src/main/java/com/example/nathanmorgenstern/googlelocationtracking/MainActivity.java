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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.R.attr.data;
import static android.R.attr.id;
import static android.R.attr.start;
import static com.example.nathanmorgenstern.googlelocationtracking.R.id.checkInList;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity{

    /*Variables for getting location data*/
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationManager  locationManager;
    private LocationListener locationListener;
    private LocationCallback mLocationCallback;
    private Boolean mRequestingLocationUpdates = false;

    /*Variables for storing location data */
    private Location mLastLocation;
    private double mLastLatitude  = -111.0;
    private double mLastLongitude = -111.0;
    private String mLastCheckInTime = "null";

    /* Location request parameters */
    private static final long MIN_TIME = 0;
    private static final float MIN_DISTANCE = 0;

    /* static final variables */
    private static final String TAG = "MAIN_ACTIVITY";
    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7172;
    private static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    private static final String LOCATION_ADDRESS_KEY = "location-address";

    /*Android widgets */
    private Button btn_start;
    private Button btn_trackLocation;
    private Button btn_check_in;
    private TextView latLongText;
    private ListView checkInListView;

    /* Variables used for Geocoder Service */
    private AddressResultReceiver mResultReceiver;
    private String mAddressOutput;
    private Boolean mAddressRequested;

    /* Variables used for database */
    private MySQLHelper sqlHelper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupWidgets();
        requestPermissions();
        getLastLocation();
        defineLocationCallback();
        defineLocationRequest();
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

    /* SETUP METHODS for onCreate */
    public void setupWidgets(){
        btn_start         = (Button)  findViewById(R.id.btnStart);
        btn_trackLocation = (Button)  findViewById(R.id.btnTrackLocation);
        btn_check_in      = (Button)  findViewById(R.id.btnCheckIn);
        latLongText       = (TextView)findViewById(R.id.txtCoordinates);
        checkInListView   = (ListView)findViewById(checkInList);

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMapActivity();
            }
        });

        /*btn_trackLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                fetchAddressButtonHandler();
                getLastLocation();
                updateText();
            }
        });*/

        btn_check_in.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                addLocationToDatabase();
                updateListView();
            }
        });

        checkInListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {

                TextView address = (TextView) view.findViewById(R.id.address_text);
                String addressResult =  address.getText().toString();
                Log.v(TAG, "onLongItemClicked: " + addressResult);
                removeLocationFromDatabase(addressResult);
                return false;
            }
        });

        sqlHelper = new MySQLHelper(this);
    }

    public void defineLocationCallback(){
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.v(TAG, "new location");
                    updateTime();
                    updateLocation(location);
                    // Update UI with location data
                    // ...
                }
            };
        };
    }

    public void defineLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
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

    /* END SETUP METHODS for onCreate */


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

    /* END ACTIVITY LIFE CYCLE METHODS */


    /* HELPER FUNCTIONS */
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
            updateText();
            updateListView();
        }
    }

    /* END HELPER FUNCTIONS */


    /* LOCATION METHODS  */
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

    public void updateLocation(Location location){
        Log.v(TAG,"updateLocation()");
        mLastLocation = location;
        startIntentService();
        mLastLatitude = location.getLatitude();
        mLastLongitude = location.getLongitude();
        //updateText();
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
                Log.v(TAG, "still null");
        }
    }

    public void updateTime(){
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        Date date = new Date();
        mLastCheckInTime = dateFormat.format(date);
    }

    /* END LOCATION METHODS */

    /* UI METHODS */
    public void updateText(){
        Log.v(TAG,"updateText()");
        String f1 = String.format("%.4f", mLastLatitude);
        String f2 = String.format("%.4f", mLastLongitude);
        latLongText.setText(f1 + "/" + f2 + "\n " + mAddressOutput);
    }

    public void updateListView(){
        ArrayList<LocationInfo> locationList = new ArrayList<LocationInfo>();
        locationList = sqlHelper.getAllLocations();

        LocationInfoAdapter array_adapter = new LocationInfoAdapter(this,R.layout.location_info_view, locationList);
        checkInListView.setAdapter(array_adapter);
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

    /* END UI METHODS */

    /* ACTIVITY / SERVICES METHODS */
    public void startIntentService() {
        Intent intent = new Intent(this, GeocoderIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    public void startMapActivity(){
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("mLatitude", mLastLatitude);
        intent.putExtra("mLongitude", mLastLongitude);
        startActivity(intent);
    }

    /* END ACTIVITY / SERVICES METHODS */

    /* DATABASE METHODS */
    public void addLocationToDatabase(){

        String mLat = Double.toString(mLastLatitude);
        String mLon = Double.toString(mLastLongitude);
        String mTim = mLastCheckInTime;
        String mAdd = mAddressOutput;

        sqlHelper.addLocationInfo(new LocationInfo(mLat, mLon, mTim, mAdd));
    }

    public void removeLocationFromDatabase(String address){
        sqlHelper.deleteLocationInfo(address);
        updateListView();
    }

    /* END DATABASE METHODS*/

}
