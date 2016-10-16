package com.edicon.firebase.devs.firepix.GeoFire;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.edicon.firebase.devs.test.friendlypix.BuildConfig;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 Security Rule:  my-geofire
    http://stackoverflow.com/questions/28060069/using-an-unspecified-index-consider-adding-indexon-g
    "my-geofire": {
    ".indexOn": "g"
    }
 **/

public class MyGeofire implements
    GeoQueryEventListener,
    GeoFire.CompletionListener,
    LocationCallback,
    GoogleMap.OnCameraChangeListener {

    private static String TAG = "GEOFIRE";

    // private static final GeoLocation INITIAL_CENTER = new GeoLocation(37.7789, -122.4017);
    private static final GeoLocation INITIAL_CENTER = new GeoLocation(37.552042, 127.089785);
    private static final int INITIAL_ZOOM_LEVEL = 14;
    private static final int INITIAL_RADIUS     = 1; // in Km
    // private static final String GEO_FIRE_DB     = "https://publicdata-transit.firebaseio.com";
    // private static final String GEO_FIRE_REF    = GEO_FIRE_DB + "/_geofire";
    private static final String GEO_FIRE_DB     = "https://vivid-torch-3052.firebaseio.com";
    private static final String GEO_FIRE_REF    = GEO_FIRE_DB + "/my-geofire";

    private Context  context;
    private GoogleMap map;

    private Circle searchCircle;
    private GeoFire  geoFire;
    private GeoQuery geoQuery;

    private Map<String, Marker> markers;

    public MyGeofire(Context cx, GoogleMap map) {
        this.context  = cx;
        this.map = map;
    }

    public void startGeofire( GeoLocation initialCenter ) {
        if( initialCenter != null )
            initGeofire(map, GEO_FIRE_DB, GEO_FIRE_REF, initialCenter);
        else
            initGeofire(map, GEO_FIRE_DB, GEO_FIRE_REF, INITIAL_CENTER);
    }

    public void initGeofire(GoogleMap gMap, String geoFireDB, String geoFireRef, GeoLocation initialCenter ) {
        LatLng latLngCenter = new LatLng(initialCenter.latitude, initialCenter.longitude);
        this.searchCircle = this.map.addCircle(new CircleOptions().center(latLngCenter).radius(1000));
        this.searchCircle.setFillColor(Color.argb(66, 255, 0, 255));
        this.searchCircle.setStrokeColor(Color.argb(66, 0, 0, 0));
        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        this.map.setOnCameraChangeListener(this);

        // if (!FirebaseApp.getApps(context).isEmpty()) {
        //     FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        // }
        // FirebaseOptions options = new FirebaseOptions.Builder().setApplicationId("mygeofire").setDatabaseUrl(geoFireDB).build();
        // FirebaseApp app = FirebaseApp.initializeApp(context, options);
        // this.geoFire = new GeoFire(FirebaseDatabase.getInstance(app).getReferenceFromUrl(geoFireRef));
        // this.geoFire = new GeoFire(FirebaseUtil.getGerFireRef());
        // ToDo: make function
        this.geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference().child("my-geofire"));
        // radius in km
        this.geoQuery = this.geoFire.queryAtLocation(initialCenter, INITIAL_RADIUS);
        // setup markers
        this.markers = new HashMap<String, Marker>();
        // add an event listener to start updating locations again
        this.geoQuery.addGeoQueryEventListener(this);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // Update the search criteria for this geoQuery and the circle on the map
        LatLng center = cameraPosition.target;
        double radius = zoomLevelToRadius(cameraPosition.zoom);
        this.searchCircle.setCenter(center);
        this.searchCircle.setRadius(radius);
        this.geoQuery.setCenter(new GeoLocation(center.latitude, center.longitude));
        // radius in km
        this.geoQuery.setRadius(radius/1000);

        if(BuildConfig.DEBUG) {
            String geoInfo = String.format("--> onCameraChange: %s changed with [%f,%f]", cameraPosition.toString(), center.latitude, center.longitude);
            Log.d(TAG, geoInfo);
        }
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        // Add a new marker to the map
        Marker marker = this.map
                .addMarker( new MarkerOptions()
                    .title(key)
                    .position(new LatLng(location.latitude, location.longitude)));
        this.markers.put(key, marker);

        if(BuildConfig.DEBUG) {
            String geoInfo = String.format("    --> Key: %s entered with [%f,%f]", key, location.latitude, location.longitude);
            Log.d(TAG, geoInfo);
        }
    }

    @Override
    public void onKeyExited(String key) {
        // Remove any old marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            marker.remove();
            this.markers.remove(key);
        }

        if(BuildConfig.DEBUG) {
            String geoInfo = String.format("    <-- Key: %s exited", key);
            Log.d(TAG, geoInfo);
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        // Move the marker
        Marker marker = this.markers.get(key);
        if (marker != null) {
            this.animateMarkerTo(marker, location.latitude, location.longitude);
        }

        if(BuildConfig.DEBUG) {
            String geoInfo = String.format("    <--> Key: %s moved with [%f,%f]", key, location.latitude, location.longitude);
            Log.d(TAG, geoInfo);
        }
    }

    @Override
    public void onGeoQueryReady() {
        /*
        if(BuildConfig.DEBUG)
            Log.d(TAG, "All initial data has been loaded and events have been fired!");
        */
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("There was an unexpected error querying GeoFire: " + error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Animation handler for old APIs without animation support
    private void animateMarkerTo(final Marker marker, final double lat, final double lng) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long DURATION_MS = 3000;
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final LatLng startPosition = marker.getPosition();
        handler.post(new Runnable() {
            @Override
            public void run() {
                float elapsed = SystemClock.uptimeMillis() - start;
                float t = elapsed/DURATION_MS;
                float v = interpolator.getInterpolation(t);

                double currentLat = (lat - startPosition.latitude) * v + startPosition.latitude;
                double currentLng = (lng - startPosition.longitude) * v + startPosition.longitude;
                marker.setPosition(new LatLng(currentLat, currentLng));

                // if animation is not finished yet, repeat
                if (t < 1) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private double zoomLevelToRadius(double zoomLevel) {
        // Approximation to fit circle into view
        return 16384000/Math.pow(2, zoomLevel);
    }

    public void onStop() {
        this.geoQuery.removeAllListeners();
        for (Marker marker: this.markers.values()) {
            marker.remove();
        }
        this.markers.clear();
    }

    public void setLocation( String key, GeoLocation geoLocation) {
        if( this.geoFire == null ) {
            Log.e(TAG, "setLocation Error: No GeoFire");
            return;
        }
        this.geoFire.setLocation(key, geoLocation, this);
    }
    @Override
    public void onComplete(String key, DatabaseError error) {
        if (error != null) {
            Log.e(TAG, "Error: saving the location to GeoFire:" + error);
        } else {
            if( BuildConfig.DEBUG )
                Log.d(TAG, "OK: saved on GeoFire");
        }
    }

    public void getLocation( String key ) {
        if( this.geoFire == null ) {
            Log.e(TAG, "getLocation Error: No GeoFire");
            return;
        }
        this.geoFire.getLocation(key, this);
    }
    @Override
    public void onLocationResult(String key, GeoLocation location) {
        if (location != null) {
            if( BuildConfig.DEBUG )
                Log.d(TAG, String.format("The location for key %s is [%f,%f]", key, location.latitude, location.longitude));
        } else {
            Log.w(TAG, String.format("No location for key %s in GeoFire", key));
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.w(TAG, String.format("getLocation error: %s", databaseError));
    }

    public void removeLocation( String key ) {
        if( this.geoFire == null ) {
            Log.e(TAG, "removeLocation Error: No GeoFire");
            return;
        }
        this.geoFire.removeLocation(key);
    }
}
