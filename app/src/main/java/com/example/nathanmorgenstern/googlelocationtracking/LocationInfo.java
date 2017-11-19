package com.example.nathanmorgenstern.googlelocationtracking;


public class LocationInfo {
    private int    location_id;
    private String latitude;
    private String longitude;
    private String time;
    private String address;
    private String checkInName;

    LocationInfo(int id, String lat, String lon, String t, String add){
        location_id = id;
        this.latitude = lat;
        this.longitude = lon;
        this.time = t;
        this.address = add;
        this.checkInName = "none";
    }


    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCheckInName() {
        return checkInName;
    }

    public void setCheckInName(String checkInName) {
        this.checkInName = checkInName;
    }

    public int getLocation_id() {
        return location_id;
    }

    public void setLocation_id(int location_id) {
        this.location_id = location_id;
    }
}
