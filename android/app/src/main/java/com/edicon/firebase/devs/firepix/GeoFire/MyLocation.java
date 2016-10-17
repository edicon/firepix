package com.edicon.firebase.devs.firepix.GeoFire;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

// Location and GeoFire
//  -https://github.com/sidiqpermana/SampleGeoFire/blob/master/app/src/main/java/com/sidiq/samplegeofire/MainActivity.java

public class MyLocation  {

    private final static String TAG = "MYLOC";

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS    = 10000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent  than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    public final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    public final static String LOCATION_KEY                    = "location-key";
    public final static String LAST_UPDATED_TIME_STRING_KEY    = "last-updated-time-string-key";

    // Stores parameters for requests to the FusedLocationProviderApi.
    private LocationRequest locationRequest;
    // Represents a geographical location.
    public static Location currentLocation;
    // Time when the location was updated represented as a String.
    public static String lastUpdateTime;

    private String userKey;
    private GoogleApiClient googleApiClient;

    public MyLocation( GoogleApiClient apiClient ) {
        googleApiClient = apiClient;
        userKey = getUserKey();
        createLocationRequest();
    }

    private String getUserKey() {
        // Get UserID from FireAuth;
        userKey = "TestKey";
        return userKey;
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

    public void startLocationUpdates() {
        try {
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
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    public void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showMap(location.getLatitude(), location.getLongitude());
            }
        });
    }

    // ToDo: Check Ref. Source
    private void showMap(double latitude, double longitude) {
        if( MyGeoFire.getMap() != null )
            MyGeoFire.showMap( latitude, longitude);
    }

    private void sendLocationToGeoFire(String userKey, double latitude, double longitude) {
        if( MyGeoFire.getGeoFire() != null )
            MyGeoFire.sendLocationToGeoFire( userKey, latitude, longitude);
    }

    public void onStart( GoogleApiClient apiClient, MyGeoFire myGeoFire ) {
        // this.geoQuery.addGeoQueryEventListener(this);
        MyGeoFire.addGeoQueryEventListener( myGeoFire );
        apiClient.connect();
    }
}
