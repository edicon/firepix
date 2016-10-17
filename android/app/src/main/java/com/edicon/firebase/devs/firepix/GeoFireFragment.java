/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.edicon.firebase.devs.firepix;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.edicon.firebase.devs.firepix.GeoFire.MyGeoFireNew;
import com.edicon.firebase.devs.firepix.GeoFire.MyLocation;
import com.edicon.firebase.devs.test.friendlypix.BuildConfig;
import com.edicon.firebase.devs.test.friendlypix.R;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.edicon.firebase.devs.firepix.GeoFire.MyLocation.LAST_UPDATED_TIME_STRING_KEY;
import static com.edicon.firebase.devs.firepix.PermUtil.RC_ACCESS_FINE_LOCATION;

/**
 * Shows map in pagerView.
 *  -http://stackoverflow.com/questions/19353255/how-to-put-google-maps-v2-on-a-fragment-using-viewpager
 */
public class GeoFireFragment extends Fragment implements
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    EasyPermissions.PermissionCallbacks {

    public static final String TAG = "GeoFireFragment";
    private static final String KEY_LAYOUT_POSITION = "layoutPosition";
    private static final String KEY_TYPE = "type";
    public static final int TYPE_HOME       = 1001;
    public static final int TYPE_GEOFIRE    = 1003;
    private int mRecyclerViewPosition = 0;
    private OnGeoFireSelectedListener mListener;

    public GeoFireFragment() {
        // Required empty public constructor
    }

    public static GeoFireFragment newInstance(int type) {
        GeoFireFragment fragment = new GeoFireFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateValuesFromBundle(savedInstanceState);
    }

    private MapView mapView;
    private GoogleMap googleMap;
    private MaterialCalendarView calendarView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_geo_cal, container, false);

        mapView = (MapView) rootView.findViewById(R.id.mapView);
        calendarView = (MaterialCalendarView) rootView.findViewById(R.id.calendarView);
        // calendarView.setTileHeight(LinearLayout.LayoutParams.MATCH_PARENT);
        calendarView.setTileSize(LinearLayout.LayoutParams.MATCH_PARENT);

        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                // For showing a move to my location button
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch( SecurityException e ) {
                    e.printStackTrace();
                }

                initMaplocation(googleMap);
                // ToDo: Check
                // buildGoogleApiClient();
            }
        });

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if(BuildConfig.DEBUG )
                    Log.d(TAG, "Day: " + widget.getSelectedDates());
            }
        });
        CalendarDay today = new CalendarDay();
        calendarView.setCurrentDate(today);
        calendarView.setSelectedDate(today);
        return rootView;
    }

    private MyLocation myLocation;
    private GoogleApiClient googleApiClient;
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        myLocation = new MyLocation( googleApiClient );
    }

    @Override
    public void onConnected(Bundle bundle) {
        askLocationTask( myLocation );
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    private MyGeoFireNew myGeofire;
    private static boolean MY_GEOFIRE = true;
    private void initMaplocation( GoogleMap googleMap) {
        if( MY_GEOFIRE ) {
            myGeofire = MyGeoFireNew.newInstance(getContext(), googleMap);
            // GeoLocation INITIAL_CENTER = new GeoLocation(37.7789, -122.4017); // SF
            GeoLocation INITIAL_CENTER = new GeoLocation(37.552042, 127.089785); // Acha
            myGeofire.startGeofire(INITIAL_CENTER);
            addGeoLocation();
            return;
        }
        // For dropping a marker at a point on the Map
        LatLng sydney = new LatLng(-34, 151);
        googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description"));
        // For zooming automatically to the location of the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void addGeoLocation() {
        /* Security Rule:
        "my-geofire": {
            ".indexOn": "g"
        } */
        double locations[][] = {
                { 37.552042, 127.089785 },
                { 37.551531, 127.090000 },
                { 37.550744, 127.090542 },
                { 37.550144, 127.090849 },
                { 37.550274, 127.091227 },
                { 37.552322, 127.092624 }
        };
        for( int i = 0;  i < locations.length; i ++ ) {
            double [] loc = locations[i];
            myGeofire.setLocation("acha: "+i, new GeoLocation(loc[0], loc[1]));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            myLocation.startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if( myLocation != null )
            myLocation.stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        // ToDo: getRecyclerViewScrollPosition();
        int recyclerViewScrollPosition = 0;
        Log.d(TAG, "Recycler view scroll position: " + recyclerViewScrollPosition);
        savedInstanceState.putSerializable(KEY_LAYOUT_POSITION, recyclerViewScrollPosition);

        savedInstanceState.putParcelable(MyLocation.LOCATION_KEY, myLocation.currentLocation);
        savedInstanceState.putString( MyLocation.LAST_UPDATED_TIME_STRING_KEY, myLocation.lastUpdateTime);

        super.onSaveInstanceState(savedInstanceState);
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
            if (savedInstanceState.keySet().contains(MyLocation.LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                MyLocation.currentLocation = savedInstanceState.getParcelable(MyLocation.LOCATION_KEY);
            }
            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                MyLocation.lastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            //updateUI();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     */
    public interface OnGeoFireSelectedListener {
        void onGeofireComment(String postKey);
        void onGeofireLike(String postKey);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGeoFireSelectedListener) {
            mListener = (OnGeoFireSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnPostSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @AfterPermissionGranted(RC_ACCESS_FINE_LOCATION)
    private void askLocationTask( MyLocation myLocation) {
        Context cx = getContext();
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(cx, perms)) {
            myLocation.startLocationUpdates();
        } else {
            EasyPermissions.requestPermissions( this, cx.getString(R.string.perm_access_fine_location), RC_ACCESS_FINE_LOCATION, perms);
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
        Log.d(TAG, "permission granted");
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        Log.d(TAG, "permission denied");
    }
}
