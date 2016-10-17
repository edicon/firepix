package com.edicon.firebase.devs.firepix.GeoFire;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.edicon.firebase.devs.test.friendlypix.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.edicon.firebase.devs.firepix.PermUtil.RC_ACCESS_FINE_LOCATION;

// Location and GeoFire
//  -https://github.com/sidiqpermana/SampleGeoFire/blob/master/app/src/main/java/com/sidiq/samplegeofire/MainActivity.java

public class MyLocation implements
    EasyPermissions.PermissionCallbacks {

    private final static String TAG = "MYLOC";

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS    = 10000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent  than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LOCATION_KEY                    = "location-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY    = "last-updated-time-string-key";

    // Stores parameters for requests to the FusedLocationProviderApi.
    private LocationRequest locationRequest;
    // Represents a geographical location.
    private Location currentLocation;
    // Time when the location was updated represented as a String.
    private String lastUpdateTime;

    public MyLocation() {

    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location * updates.
     */
    public void createLocationRequest() {
        locationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @AfterPermissionGranted(RC_ACCESS_FINE_LOCATION)
    public void startLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest) {
        Context cx = googleApiClient.getContext();
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions( cx, perms)) {
            try {
                // The final argument to {@code requestLocationUpdates()} is a LocationListener
                // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        currentLocation = location;
                        lastUpdateTime  = DateFormat.getTimeInstance().format(new Date());
                        showMap(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "Your location updated");
                        sendLocationToGeoFire( userKey, location.getLatitude(), location.getLongitude());
                    }
                });
            } catch( SecurityException se) {
               se.printStackTrace();
            }
        } else {
            EasyPermissions.requestPermissions( this, cx.getString(R.string.perm_access_fine_location), RC_ACCESS_FINE_LOCATION, perms);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    public void stopLocationUpdates( GoogleApiClient apiClient) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showMap(location.getLatitude(), location.getLongitude());
            }
        });
    }

    /**
     * Updates fields based on data stored in the bundle.
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of currentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                lastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            //updateUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        // ...
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        // ...
    }

    private void sendLocationToGeoFire(String userKey, double latitude, double longitude) {
        MyGeofire.sendLocationToGeoFire( userKey, latitude, longitude);
    }

    public void onPause( GoogleApiClient apiClient ) {
        stopLocationUpdates( apiClient);
    }
}
