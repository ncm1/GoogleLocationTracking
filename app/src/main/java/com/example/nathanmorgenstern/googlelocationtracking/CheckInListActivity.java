package com.example.nathanmorgenstern.googlelocationtracking;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class CheckInListActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST_CODE = 7171;

    /*Variables for getting location data*/
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationManager locationManager;
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
    private String mLastCheckInName = "null";

    /* Variables used for database */
    private MySQLHelper sqlHelper;

    private Dialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_list);

        requestPermissions();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sqlHelper = new MySQLHelper(this);
        defineLocationRequest();
        defineLocationCallback();
        mRequestingLocationUpdates = true;
        //TODO: add boolean variable to indicate if "tracking mode" is on or off
        startLocationUpdates();

        updateListView();
    }

    public void updateListView(){
        MySQLHelper sqlHelper = new MySQLHelper(this);
        ArrayList<LocationInfo> locationList = new ArrayList<LocationInfo>();
        locationList = sqlHelper.getAllLocations();

        ListView checkInListView = (ListView) findViewById(R.id.check_in_activity_list);
        LocationInfoAdapter array_adapter = new LocationInfoAdapter(this,R.layout.location_info_view, locationList);
        checkInListView.setAdapter(array_adapter);
    }

    /* LOCATION METHODS  */

    public void updateLocation(Location location){

        //mLastLocation = mCurrentLocation;
        mLastLocation = location;

        mLastLatitude  = location.getLatitude();
        mLastLongitude = location.getLongitude();

        String tempName = withinRadius(mLastLatitude,mLastLongitude);

        if(!tempName.equals("No name in radius")) {
            String time = sqlHelper.getLastCheckInTimeForLocation(tempName);
            if(!time.equals("none found"))
                showDialog(tempName,time);
            else if(dialog != null)
                dialog.dismiss();
        }
        else if(dialog != null)
            dialog.dismiss();
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
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);

        }
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


        Location loc1 = new Location("temp");
        loc1.setLatitude(lat);
        loc1.setLongitude(lon);

        double dist = 0.0;
        for(int i = 0; i < locationListSize; i++){
            Location loc2 = new Location("temp");
            loc2.setLatitude(Double.parseDouble(locationInfoList.get(i).getLatitude()));
            loc2.setLongitude(Double.parseDouble(locationInfoList.get(i).getLongitude()));
            dist = loc1.distanceTo(loc2);

            if(dist <= 30.0)
                return locationInfoList.get(i).getCheckInName();

        }

        return "No name in radius";
    }

    /* END LOCATION METHODS */

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateListView();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}
