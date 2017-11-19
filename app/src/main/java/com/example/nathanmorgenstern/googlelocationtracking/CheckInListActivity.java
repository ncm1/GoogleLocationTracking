package com.example.nathanmorgenstern.googlelocationtracking;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class CheckInListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_list);
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
}
