package com.example.nathanmorgenstern.googlelocationtracking;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class GeocoderIntentService extends IntentService {

    private static final String TAG = "Geocoder";
    protected ResultReceiver mReceiver;

    public GeocoderIntentService() {
        super(TAG);
        Log.v(TAG, "GeocoderIntentService() running");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG,"onHandleIntent()");
        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        if (mReceiver == null) {
            return;
        }

        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        if (location == null) {
            errorMessage = "No Location Data Provided";
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

        } catch (IOException ioe) {
            errorMessage = "Service Not Available";
            Log.e(TAG, errorMessage, ioe);
        } catch (IllegalArgumentException iae) {
            errorMessage = "Invalid Latitude and Longitude Used";
            Log.e(TAG, errorMessage + ". " + "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), iae);
        }

        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "No Address Found";
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            Log.v(TAG, "Address Found");
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }

    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        Log.v(TAG, "message: " + message);
        mReceiver.send(resultCode, bundle);
    }

}