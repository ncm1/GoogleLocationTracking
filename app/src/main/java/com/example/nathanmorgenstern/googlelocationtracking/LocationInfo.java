package com.example.nathanmorgenstern.googlelocationtracking;


public class LocationInfo {
    private String latitude;
    private String longitude;
    private String time;
    private String address;
    private String checkInName;

    LocationInfo(String lat, String lon, String t, String add){
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
}
