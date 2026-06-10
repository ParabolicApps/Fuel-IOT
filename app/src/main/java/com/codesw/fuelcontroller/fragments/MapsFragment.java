package com.codesw.fuelcontroller.fragments;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.codesw.fuelcontroller.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * MapsFragment provides a high-fidelity visualization of the vehicle's location.
 * It features a Google MapView and a floating station information card.
 * Consistent with the "Fuel Guard" branding, it uses neon accents and Ubuntu typography.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final LatLng DEFAULT_LOCATION = new LatLng(23.8103, 90.4125);
    private static final float DEFAULT_ZOOM = 14f;

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker vehicleMarker;
    
    @SuppressLint("ResourceType")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        _changeActivityFont(view, "ubuntu_medium");
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
        vehicleMarker = googleMap.addMarker(new MarkerOptions()
                .position(DEFAULT_LOCATION)
                .title("Fuel Guard Vehicle"));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        googleMap = null;
        vehicleMarker = null;
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    /**
     * Prepares the asset path and triggers the recursive font injector.
     */
    private void _changeActivityFont(View view, String fontName) {
        String fontPath = "fonts/".concat(fontName.concat(".ttf"));
        overrideFonts(view, fontPath);
    }

    /**
     * Recursively injects the custom Typeface into all TextView components 
     * within the fragment's view hierarchy.
     */
    private void overrideFonts(final View v, String fontPath) {
        try {
            if (getContext() == null) return;
            Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), fontPath);
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    overrideFonts(child, fontPath);
                }
            } else if (v instanceof TextView) {
                ((TextView) v).setTypeface(typeface);
            }
        } catch(Exception e) {
            // Quiet fail to maintain default system fonts if assets are missing
        }
    }

    /**
     * Dynamic callback for future real-time GPS telemetry from the IOT server.
     * 
     * @param data Raw telemetry string containing coordinate or station data.
     */
    public void setProgress(String data){
        LatLng location = parseLocation(data);
        if (location == null || googleMap == null) {
            return;
        }

        if (vehicleMarker == null) {
            vehicleMarker = googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Fuel Guard Vehicle"));
        } else {
            vehicleMarker.setPosition(location);
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM));
    }

    /**
     * Accepts telemetry in "latitude,longitude" format.
     */
    private LatLng parseLocation(String data) {
        if (data == null) {
            return null;
        }
        String[] parts = data.trim().split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return null;
            }
            return new LatLng(latitude, longitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
