package com.example.nathanmorgenstern.googlelocationtracking;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    private Location mCurrentLocation;
    private double mLastLatitude  = -111.0;
    private double mLastLongitude = -111.0;
    private double mCurrentLatitude = mLastLatitude;
    private double mCurrentLongitude = mLastLongitude;
    private String mLastCheckInTime = "null";
    private String mLastCheckInName = "NULL";

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
    private Button btn_load_list;
    private Button btn_check_in;
    private Button btn_tracking;
    private TextView latLongText;
    private EditText checkInName;
    private ListView checkInListView;
    private Dialog dialog = null;

    /* Variables used for Geocoder Service */
    private AddressResultReceiver mResultReceiver;
    private String mAddressOutput;
    private Boolean mAddressRequested;

    /* Variables used for database */
    private MySQLHelper sqlHelper;

    //Helper/Utility variables
    private Boolean onResume = false;
    private Runnable runnable;
    private Boolean autoCheckInMode = false;
    private int autoCheckCounter = 1;
    private Boolean hundredMeterMovement = false;
    private Location capturedLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoCheckMode();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupWidgets();
        requestPermissions();
        //Get the last location and set it as current for default
        getLastLocation();
        defineLocationCallback();
        defineLocationRequest();
        mRequestingLocationUpdates = true;
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
        btn_check_in      = (Button)  findViewById(R.id.btnCheckIn);
        btn_load_list     = (Button)  findViewById(R.id.btnOpenList);
        btn_tracking      = (Button)  findViewById(R.id.btnTrackingMode);
        latLongText       = (TextView)findViewById(R.id.txtCoordinates);
        checkInName       = (EditText)findViewById(R.id.checkInNameInput);

        btn_tracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCheckCounter++;
                if(autoCheckCounter % 2 == 0) {
                    autoCheckInMode = true;
                    btn_tracking.setText("Disable Auto Check-in");
                    capturedLocation = mLastLocation;
                }
                else {
                    autoCheckInMode = false;
                    btn_tracking.setText("Enable Auto Check-in");
                }
            }
        });

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMapActivity();
            }
        });

        btn_check_in.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                updateTime();
                updateCheckInName();
                addLocationToDatabase();
                //updateListView();
            }
        });

        btn_load_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCheckListActivity();
            }
        });


        sqlHelper = new MySQLHelper(this);
    }

    /* Race conditions occurs when two thread operate on same object without proper synchronization
     * and there operation interleaves on each other.
     *
     * Race conditions have been avoided by waiting for the location to be received inside the callback
     * function, otherwise
     */

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
            onResume = true;
            startLocationUpdates();
            getLastLocation();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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

    public class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.v(TAG, "onResultsReceived");
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            mAddressRequested = false;
            updateText();
            //updateListView();
        }
    }

    /* END HELPER FUNCTIONS */


    /* LOCATION METHODS  */

    public void updateLocation(Location location){
        Log.v(TAG,"updateLocation()");
        //Log.v(TAG,"distance: " + location.distanceTo(mLastLocation));
        //mLastLocation = mCurrentLocation;

        if(mLastLocation != null && autoCheckInMode){
            Log.v(TAG, "location.distanceTo(capturedLocation): " + location.distanceTo(capturedLocation));
            if( location.distanceTo(capturedLocation) > 100) {
                Log.v(TAG, "hundredMeterMovement is greater than 100");
                hundredMeterMovement = true;
                capturedLocation = location; //Set a new captured location
            }
            else
                hundredMeterMovement = false;
        }

        mLastLocation = location;
        startIntentService();

        mLastLatitude  = location.getLatitude();
        mLastLongitude = location.getLongitude();

        String tempName = withinRadius(mLastLatitude,mLastLongitude);
        Log.v(TAG, "updateLocation()->tempName: " + tempName);

        if(!tempName.equals("No name in radius")) {
            String time = sqlHelper.getLastCheckInTimeForLocation(tempName);
            if(!time.equals("none found"))
                showDialog(tempName,time);
            else if(dialog != null)
                dialog.dismiss();
        }
        else if(dialog != null)
            dialog.dismiss();
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
                if(onResume) {
                    updateLocation(mLastLocation);
                    onResume = false;
                }
            }
        }
    }

    public void updateTime(){
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        Date date = new Date();
        mLastCheckInTime = dateFormat.format(date);
        Log.v(TAG,"mLastCheckInTime: " + mLastCheckInTime);
    }

    public void updateCheckInName(){
        if(checkInName != null)
            mLastCheckInName = checkInName.getText().toString();
        else
            mLastCheckInName = "NULL";
    }

    public String withinRadius(double lat, double lon){
        ArrayList<LocationInfo> locationInfoList = sqlHelper.getAllUniqueLocationCheckIns();
        int locationListSize = locationInfoList.size();

        Log.v(TAG, "unique locationInfoList.size(): " + locationListSize);

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

    /* END LOCATION METHODS */

    /* UI METHODS */

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

    public void updateText(){
        Log.v(TAG,"updateText()");
        String f1 = String.format("%.4f", mLastLatitude);
        String f2 = String.format("%.4f", mLastLongitude);
        latLongText.setText(f1 + "/" + f2 + "\n " + mAddressOutput);
        updateCheckInName();
    }

    public void startLocationUpdates(){
        if(mFusedLocationClient != null) {
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

    public void startCheckListActivity(){
        Intent intent = new Intent(this, CheckInListActivity.class);
        startActivity(intent);
    }
    /* END ACTIVITY / SERVICES METHODS */

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
                Log.v(TAG, "tempName: " + tempName + " fk: " + fk);
                Log.v(TAG, "address: " + mAddressOutput);
                int fk_checkIn = sqlHelper.getCheckInTablePrimaryKey(tempName, mAddressOutput);
                sqlHelper.addToNormalized(fk, fk_checkIn);
            }
        }
        else{
            sqlHelper.addCheckInName("", mAddressOutput, fk);
            int fk_checkIn = sqlHelper.getCheckInTablePrimaryKey("", mAddressOutput);
            sqlHelper.addToNormalized(fk, fk_checkIn);
        }

    }

    /* Background task that automatically checks in after 5 minutes, or the user
       has moved 100m or more */

    public void autoCheckMode(){
        final Handler handler = new Handler();
        final Handler handler2 = new Handler();
        final int delay = 1000 * 60 * 5; //milliseconds->minutes -> 5 minutes
        final Runnable runnable1;

        handler.postDelayed(new Runnable(){
            public void run(){
                //do something
                Log.v(TAG, "Hey i'm running here");
                if(autoCheckInMode) {
                    Log.v(TAG, "autoCheckInMode == true");
                    runnable = this;
                    handler.postDelayed(runnable, delay);
                    getLastLocation();
                    updateTime();
                    updateCheckInName();
                    addLocationToDatabase();
                }
                else {
                    Log.v(TAG, "autoCheckInMode == false");
                    runnable = this;
                    handler.postDelayed(runnable, delay);
                }

            }
        }, delay);

        final int delay2 = 1000*70; //check every 70 seconds for 100m movement
        handler2.postDelayed(new Runnable(){
            public void run(){
                //do something
                if(autoCheckInMode && hundredMeterMovement) {
                    Log.v(TAG, "hundredMeterMovement detected");
                    Runnable runnable1 = this;
                    handler.postDelayed(runnable1, delay2);
                    getLastLocation();
                    updateTime();
                    updateCheckInName();
                    addLocationToDatabase();
                    hundredMeterMovement = false;

                }
                else{
                    Log.v(TAG, "I'm running, no movement detected");
                    runnable = this;
                    handler.postDelayed(runnable, delay);
               }
            }
        }, delay2);

    }


    /* END DATABASE METHODS*/

}
