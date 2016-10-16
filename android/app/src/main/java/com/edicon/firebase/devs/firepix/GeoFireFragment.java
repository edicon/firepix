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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.edicon.firebase.devs.firepix.GeoFire.MyGeofire;
import com.edicon.firebase.devs.test.friendlypix.BuildConfig;
import com.edicon.firebase.devs.test.friendlypix.R;
import com.firebase.geofire.GeoLocation;
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

/**
 * Shows map in pagerView.
 *  -http://stackoverflow.com/questions/19353255/how-to-put-google-maps-v2-on-a-fragment-using-viewpager
 */
public class GeoFireFragment extends Fragment {

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

    private MyGeofire myGeofire;
    private static boolean MY_GEOFIRE = true;
    private void initMaplocation( GoogleMap googleMap) {
        if( MY_GEOFIRE ) {
            myGeofire = new MyGeofire(getContext(), googleMap);
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
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
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
        int recyclerViewScrollPosition = 0; // ToDo: getRecyclerViewScrollPosition();
        Log.d(TAG, "Recycler view scroll position: " + recyclerViewScrollPosition);
        savedInstanceState.putSerializable(KEY_LAYOUT_POSITION, recyclerViewScrollPosition);
        super.onSaveInstanceState(savedInstanceState);
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
}
