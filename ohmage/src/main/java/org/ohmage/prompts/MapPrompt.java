package org.ohmage.prompts;

import android.app.Activity;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ohmage.app.R;
import org.ohmage.app.SurveyActivity;

import java.util.HashMap;
import java.util.List;

/**
 * Created by changun on 12/12/15.
 */
public class MapPrompt extends AnswerablePrompt<MapPrompt.Coordinates> {
    @Override
    public SurveyItemFragment getFragment() {
        return MapPromptFragment.getInstance(this);
    }

    static public class Coordinates{
        Double latitude,longitude;
        public Coordinates(Double latitude, Double longitude){
            this.latitude = latitude;
            this.longitude = longitude;
        }

    }

    static public class MapPromptFragment extends  AnswerablePromptFragment<MapPrompt>{
        MapView mapView;
        public static MapPromptFragment getInstance(MapPrompt prompt) {

            MapPromptFragment fragment = new MapPromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if(mapView != null){
                mapView.onCreate(savedInstanceState);
            }

        }
        @Override
        public void  onPause(){
            super.onPause();
            if(mapView != null){
                mapView.onPause();
            }
        }

        @Override
        public void  onResume(){
            super.onResume();
            if(mapView != null){
                mapView.onResume();
            }
        }

        @Override
        public void  onDestroy(){
            super.onDestroy();
            if(mapView != null){
                mapView.onDestroy();
            }
        }

        @Override
        public void  onSaveInstanceState(Bundle outState){
            super.onSaveInstanceState(outState);
            if(mapView != null){
                mapView.onSaveInstanceState(outState);
            }
        }
        private GoogleMap map;
        private Marker marker;
        @Override
        public void onCreatePromptView(LayoutInflater inflater, final ViewGroup container,
                                       Bundle savedInstanceState) {
            final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_map, container, true);
            MapView mapView = (MapView) view.findViewById(R.id.mapview);
            mapView.onCreate(savedInstanceState);
            // Gets to GoogleMap from the MapView and does initialization stuff
            map = mapView.getMap();
            map.getUiSettings().setMyLocationButtonEnabled(true);
            // show blue circle that indicate the current location
            map.setMyLocationEnabled(true);
            // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
            MapsInitializer.initialize(this.getActivity());

            final LatLng defaultLocation = getPrompt().defaultResponse  != null ? new LatLng(getPrompt().defaultResponse.latitude, getPrompt().defaultResponse.longitude) : null;


            // If default response is available, updates the location and zoom of the MapView
            // otherwise, move to the current location
            if(defaultLocation != null) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(defaultLocation, 12);
                map.animateCamera(cameraUpdate);
                // create marker
                marker = map.addMarker(new MarkerOptions()
                        .position(defaultLocation)
                        .title("Here!"));
            }else{

                // Get LocationManager object from System Service LOCATION_SERVICE
                LocationManager locationManager = (LocationManager) getActivity().getSystemService(Activity.LOCATION_SERVICE);

                // Create a criteria object to retrieve provider
                Criteria criteria = new Criteria();

                // Get the name of the best provider
                String provider = locationManager.getBestProvider(criteria, true);

                // Get Current Location
                Location myLocation = locationManager.getLastKnownLocation(provider);

                if(myLocation != null){
                    // Get latitude of the current location
                    double latitude = myLocation.getLatitude();

                    // Get longitude of the current location
                    double longitude = myLocation.getLongitude();

                    // Create a LatLng object for the current location
                    LatLng latLng = new LatLng(latitude, longitude);

                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);

                    map.animateCamera(cameraUpdate);
                }

            }

            map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {



                @Override
                public void onMapLongClick(LatLng latLng) {
                    // when long click, move existing marker or create a new one
                    if(marker == null) {
                        marker = map.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Here!"));
                    }else {
                        marker.setPosition(latLng);
                    }
                    setValue(new Coordinates(latLng.longitude, latLng.longitude));
                }
            });
            this.mapView = mapView;



        }
        @Override protected void onSkipPressed() {
            super.onSkipPressed();
            if(marker != null){
                marker.remove();
            }
        }
    }
}
