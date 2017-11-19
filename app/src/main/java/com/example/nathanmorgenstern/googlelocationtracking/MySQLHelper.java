package com.example.nathanmorgenstern.googlelocationtracking;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.id;
import static android.R.attr.name;
import static android.R.attr.value;
import static android.provider.Contacts.SettingsColumns.KEY;
import static android.view.View.Y;

public class MySQLHelper extends SQLiteOpenHelper {

    private static final String SQL_DEBUGGER = "SQL_DEBUG";
    //Contact table name
    private static final String TABLE_CHECK_IN = "check_in_info";
    private static final String TABLE_LOCATION_INFO = "location_info";

    // Location info Columns names
    private static final String KEY_ID       = "id";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_TIME = "time";
    private static final String KEY_ADDRESS = "address";

    // Table check in Column names
    private static final String KEY_CHECK_IN_NAME = "check_in_name";


    private static final String[] LOCATION_COLUMNS = {KEY_LATITUDE,KEY_LONGITUDE,KEY_TIME,KEY_ADDRESS};
    private static final String[] CHECK_IN_COLUMNS = {KEY_CHECK_IN_NAME};

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "LocationInfo";

    public MySQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL statement to create Table Location Info
        String CREATE_LOCATION_INFO_TABLE = "CREATE TABLE " +  TABLE_LOCATION_INFO +  " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_LATITUDE + " TEXT," +
                KEY_LONGITUDE + " TEXT," +
                KEY_TIME+ " TEXT," +
                KEY_ADDRESS + " TEXT)";

        String CREATE_CHECK_IN_TABLE = "CREATE TABLE " +  TABLE_CHECK_IN +  " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_CHECK_IN_NAME + " TEXT)";

        // create contacts table
        db.execSQL(CREATE_LOCATION_INFO_TABLE);
        //db.execSQL();
        //db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older books table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHECK_IN);
        // create fresh table
        onCreate(db);
    }


    /* SQL TABLE LOCATION HELPER METHODS */

    public void addLocationInfo(LocationInfo loc_info){
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();

        values.put(KEY_LATITUDE,  loc_info.getLatitude());
        values.put(KEY_LONGITUDE, loc_info.getLongitude());
        values.put(KEY_TIME,      loc_info.getTime());
        values.put(KEY_ADDRESS,   loc_info.getAddress());

        Log.v(SQL_DEBUGGER, "latitude: "   + loc_info.getLatitude());
        Log.v(SQL_DEBUGGER, "longitude: "  + loc_info.getLongitude());
        Log.v(SQL_DEBUGGER, "time: "       + loc_info.getTime());
        Log.v(SQL_DEBUGGER, "address: "    + loc_info.getAddress());

        // 3. insert
        db.insert(TABLE_LOCATION_INFO, // table
                    null, //nullColumnHack
                    values); // key/value -> keys = column names/ values = column values
        // 4. close
        db.close();
    }

    // Getting All Contacts
    public ArrayList<LocationInfo> getAllLocations() {
        ArrayList<LocationInfo> locationInfoList = new ArrayList<LocationInfo>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_LOCATION_INFO;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        String temp_lat;
        String temp_long;
        String temp_time;
        String temp_address;
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                temp_lat     = cursor.getString(1);
                temp_long    = cursor.getString(2);
                temp_time    = cursor.getString(3);
                temp_address = cursor.getString(4);
                LocationInfo temp_location_info = new LocationInfo(temp_lat,temp_long,temp_time,temp_address);
                locationInfoList.add(temp_location_info);
            } while (cursor.moveToNext());
        }
        db.close();
        // return contact list
        return locationInfoList;
    }

    public void deleteLocationInfo(String address){
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        // 2. delete item from DB
        db.delete(TABLE_LOCATION_INFO,"address = ?", new String[] { address });
        db.close();
    }

    /* END LOCATION*/

}


//References: https://www.codeproject.com/Articles/119293/Using-SQLite-Database-with-Android
//http://www.androidhive.info/2011/11/android-sqlite-database-tutorial/