package com.example.abdul.heatmap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.example.abdul.heatmap.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class HeatMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "HeatMapActivity";
//    private static final int ALT_HEATMAP_RADIUS = 100;
//    private static final double ALT_HEATMAP_OPACITY = 1.0;
//    private static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
//            Color.argb(0, 0, 255, 255),// transparent
//            Color.argb(255 / 3 * 2, 0, 255, 255),
//            Color.rgb(0, 191, 255),
//            Color.rgb(0, 0, 127),
//            Color.rgb(255, 0, 0)
//    };
//    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = {0.0f, 0.10f, 0.20f, 0.60f, 1.0f};
//    public static final Gradient ALT_HEATMAP_GRADIENT = new Gradient(ALT_HEATMAP_GRADIENT_COLORS, ALT_HEATMAP_GRADIENT_START_POINTS);

    private HeatmapTileProvider tileProvider;
    private TileOverlay tileOverlay;
    private GoogleMap googleMap;
    private HashMap<String, DataSet> mLists = new HashMap<>();

    //    private boolean mDefaultGradient = true;
//    private boolean mDefaultRadius = true;
//    private boolean mDefaultOpacity = true;
    private String jsonData = "";
    private double latitude = 0, longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heatmaps_demo);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            jsonData = intent.getExtras().getString("jsonData");
            latitude = intent.getExtras().getDouble("latitude");
            longitude = intent.getExtras().getDouble("longitude");
        }
        setUpMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMap();
    }

    private void setUpMap() {
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }
    }

//    protected GoogleMap getMap() {
//        return googleMap;
//    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (googleMap != null) {
            return;
        }
        googleMap = map;
        startDemo();
    }

    protected void startDemo() {
        Log.d(TAG, "startDemo: start");
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 12));
        googleMap.setMapType(3);

        try {
            mLists.put(getString(R.string.police_stations), new DataSet(readItems(R.raw.police), getString(R.string.police_stations_url)));
            mLists.put(getString(R.string.medicare), new DataSet(readItems(R.raw.medicare), getString(R.string.medicare_url)));
            mLists.put(getString(R.string.gsm_signal), new DataSet(readItems(jsonData), "https://www.google.com"));
        } catch (JSONException e) {
            Log.d(TAG, "startDemo: e -> " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.d(TAG, "startDemo: e -> " + e.getMessage());
            Toast.makeText(this, "Big Exception.", Toast.LENGTH_LONG).show();
        }

        // Set up the spinner/dropdown list
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.heatmaps_datasets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new SpinnerActivity());

        // Make the handler deal with the map
        // Input: list of WeightedLatLngs, minimum and maximum zoom levels to calculate custom
        // intensity from, and the map to draw the heatmap on
        // radius, gradient and opacity not specified, so default are used
        Log.d(TAG, "startDemo: end");
    }

    // Dealing with spinner choices
    public class SpinnerActivity implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Log.d(TAG, "onItemSelected: start");
            String dataset = parent.getItemAtPosition(pos).toString();
            if (tileProvider == null) {
                Log.d(TAG, "onItemSelected: mLists -> " + mLists.get(getString(R.string.gsm_signal)).getData());
                tileProvider = new HeatmapTileProvider.Builder().weightedData(mLists.get(getString(R.string.gsm_signal)).getData()).build();
                tileOverlay = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
            } else {
                tileProvider.setWeightedData(mLists.get(dataset).getData());
                tileOverlay.clearTileCache();
            }
            Log.d(TAG, "onItemSelected: end");
        }

        public void onNothingSelected(AdapterView<?> parent) {
            Log.d(TAG, "onNothingSelected: start");
            // Another interface callback
            Log.d(TAG, "onNothingSelected: end");
        }
    }

    // Datasets from http://data.gov.au
    private ArrayList<WeightedLatLng> readItems(int resource) throws JSONException {
        Log.d(TAG, "readItems: start");
        ArrayList<WeightedLatLng> list = new ArrayList<>();
        InputStream inputStream = getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        Log.d(TAG, "readItems: json -> " + json);
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new WeightedLatLng(new LatLng(lat, lng), 1));
        }
        Log.d(TAG, "readItems: end");
        return list;
    }

    private ArrayList<WeightedLatLng> readItems(String resource) throws JSONException {
        Log.d(TAG, "readItems: start");
        ArrayList<WeightedLatLng> list = new ArrayList<>();
        String json = new Scanner(resource).useDelimiter("\\A").next();
        Log.d(TAG, "readItems: json -> " + json);
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double signalLevel = object.getDouble("signalData");
            double lat = object.getDouble("latitude");
            double lng = object.getDouble("longitude");
            list.add(new WeightedLatLng(new LatLng(lat, lng), signalLevel));
        }
        Log.d(TAG, "readItems: end");
        return list;
    }

    private class DataSet {
        private ArrayList<WeightedLatLng> mDataset;
        private String mUrl;

        public DataSet(ArrayList<WeightedLatLng> dataSet, String url) {
            Log.d(TAG, "DataSet: start");
            this.mDataset = dataSet;
            this.mUrl = url;
            Log.d(TAG, "DataSet: end");
        }

        public ArrayList<WeightedLatLng> getData() {
            return mDataset;
        }

        public String getUrl() {
            return mUrl;
        }
    }

}
