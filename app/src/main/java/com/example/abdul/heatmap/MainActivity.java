package com.example.abdul.heatmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "MainActivity";
    private LocationManager locationManager;
    private PhoneStateListener phoneListener;
    private TelephonyManager manager;
    private LinkedList<SignalData> signalData = new LinkedList<>();
    private TextView locationTextView, signalTextView;
    private EditText databaseEditText;
    private double latitude, longitude, signalLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatabaseHandler.initialize(this);

        locationTextView = findViewById(R.id.locationTextView);
        signalTextView = findViewById(R.id.signalTextView);
        databaseEditText = findViewById(R.id.databaseEditText);
        databaseEditText.setFocusable(false);

        phoneListener = new PhoneStateHandler();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) && checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }

        signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
        databaseEditText.setText(jsonData());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.startRecording:
                getLocation();
                Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show();
                break;
            case R.id.stopRecording:
                locationManager.removeUpdates(this);
                manager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
                Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
                break;
            case R.id.showHeatMap:
                locationManager.removeUpdates(this);
                Intent intent = new Intent(this, HeatMapActivity.class);
                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                intent.putExtra("jsonData", jsonData());
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                startActivity(intent);
                break;
            case R.id.loadDatabase:
                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                databaseEditText.setText(jsonData());
                break;
            case R.id.deleteDatabase:
                DatabaseHandler.deleteTable(this);
                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                databaseEditText.setText("");
                break;
            case R.id.saveDataToFile:
                generateNoteOnSD(this, "HeatMap Data.txt", jsonData());
                break;
            case R.id.about:
                Log.i(TAG, "onOptionsItemSelected : " + "case about");
                Toast.makeText(this, "Application for showing Heat Map for Cellular Signals.", Toast.LENGTH_SHORT).show();
            default:
                Log.d(TAG, "onOptionsItemSelected: default case");
        }
        return true;
    }

    public String jsonData() {
        StringBuilder databaseText = new StringBuilder();
        databaseText.append("[\n");
        if (signalData.size() > 0) {
            for (int i = 0; i < signalData.size() - 1; i++) {
                databaseText.append(signalData.get(i).getJsonFormat()).append(",\n");
            }
            databaseText.append(signalData.getLast().getJsonFormat());
        }
        databaseText.append("\n]");
        return databaseText.toString();
    }

    public void getSignalStrength() {
        Log.d(TAG, "getSignalStrength: starts");
        signalLevel = 0;
        manager.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        Log.d(TAG, "getSignalStrength: ends");
    }

    public void getLocation() {
        Log.d(TAG, "getLocation: starts");
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 1, MainActivity.this);
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, "error on getLocationButton Clicked", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        Log.d(TAG, "getLocation: ends");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: starts");
        Toast.makeText(this, "location changed", Toast.LENGTH_SHORT).show();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        locationTextView.setText(new StringBuilder("Latitude: " + latitude + "\nLongitude: " + longitude));
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            Log.d(TAG, "onLocationChanged: addresses[0] -> " + addresses.get(0));
            locationTextView.setText(new StringBuilder(locationTextView.getText() + "\n" + addresses.get(0).getAddressLine(0) + ", " + addresses.get(0).getAddressLine(1) + ", " + addresses.get(0).getAddressLine(2)));
            getSignalStrength();
            Log.d(TAG, "onLocationChanged: signalLevel -> " + signalLevel + ", longitude -> " + longitude + ", latitude -> " + latitude);
        } catch (Exception e) {
            Toast.makeText(this, "error in onLocationChanged", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onLocationChanged: error in onLocationChanged");
        }
        Log.d(TAG, "onLocationChanged: ends");
    }

    @Override
    public void onProviderDisabled(String provider) {
//        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet\nprovider -> " + provider, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
//        Toast.makeText(MainActivity.this, "provider -> " + provider + ", status -> " + status + ", extras -> " + extras, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider + ", status -> " + status + ", extras -> " + extras);
    }

    @Override
    public void onProviderEnabled(String provider) {
//        Toast.makeText(MainActivity.this, "provider -> " + provider, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider);
    }

    public boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED;
    }

    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                Toast.makeText(context, "Directory Created -> " + root.mkdirs(), Toast.LENGTH_SHORT).show();
            }
            File file = new File(root, sFileName);
            if(!file.exists()) {
                Toast.makeText(context, "File Created -> " + file.createNewFile(), Toast.LENGTH_SHORT).show();
            }
            FileWriter writer = new FileWriter(file);
            writer.write(sBody);
            writer.flush();
            writer.close();
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class PhoneStateHandler extends PhoneStateListener {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.d(TAG, "onSignalStrengthsChanged: starts");
            Toast.makeText(MainActivity.this, "signal changed", Toast.LENGTH_SHORT).show();
            if (manager != null) {
                signalLevel = signalStrength.getLevel();
//                    if (signalStrength.isGsm()) {
//                        if (signalLevel != 99) {
//                            signalLevel = signalStrength.getLevel() * 2 - 113;
//                        } else {
//                            signalLevel = signalStrength.getLevel();
//                        }
//                    }
                signalTextView.setText(new StringBuilder("Signal Level: " + signalLevel));
                signalData.add(new SignalData(signalLevel, latitude, longitude));
                Log.d(TAG, "onLocationChanged: signalData.getLast() -> " + signalData.getLast());
                Log.d(TAG, "onLocationChanged: length -> " + signalData.size());
//                    Toast.makeText(MainActivity.this, "size -> " + signalData.size(), Toast.LENGTH_SHORT).show();
                DatabaseHandler.insertData(MainActivity.this, signalData.getLast());
                signalData = DatabaseHandler.selectSignalData(MainActivity.this, "id = id");
                databaseEditText.setText(jsonData());
            }
            Log.d(TAG, "onSignalStrengthsChanged: ends");
        }
    }

}
