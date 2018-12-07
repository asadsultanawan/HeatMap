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
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "MainActivity";
    private LocationManager locationManager;
    private LinkedList<SignalData> signalData = new LinkedList<>();
    private TextView locationTextView, signalTextView;
    private EditText databaseEditText;
    private double latitude, longitude, signalDBM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseHandler.initialize(this);

        locationTextView = findViewById(R.id.locationTextView);
        signalTextView = findViewById(R.id.signalTextView);
        databaseEditText = findViewById(R.id.databaseEditText);
        databaseEditText.setFocusable(false);

        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) && checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
    }

    public void setupSignalStrength() {
        Log.d(TAG, "setupSignalStrength: starts");
        signalDBM = 0;
        final TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneListener = new PhoneStateListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                Log.d(TAG, "onSignalStrengthsChanged: starts");
                if (manager != null) {
                    signalDBM = signalStrength.getGsmSignalStrength();
                    if (signalStrength.isGsm()) {
                        if (signalDBM != 99) {
                            signalDBM = signalStrength.getGsmSignalStrength() * 2 - 113;
                        } else {
                            signalDBM = signalStrength.getGsmSignalStrength();
                        }
                    }
                    signalTextView.setText("Signal Strength in dbm: " + signalDBM);
                }
                getLocation();
                Log.d(TAG, "onSignalStrengthsChanged: ends");
            }
        };
        manager.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        Log.d(TAG, "setupSignalStrength: ends");
    }

    public void getLocation() {
        Log.d(TAG, "getLocation: starts");
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000, 0, MainActivity.this);
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, "error on getLocationButton Clicked", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        Log.d(TAG, "getLocation: ends");
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
                break;
            case R.id.showHeatMap:
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

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: starts");
        locationTextView.setText("Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude());
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            locationTextView.setText(locationTextView.getText() + "\n" + addresses.get(0).getAddressLine(0) + ", " + addresses.get(0).getAddressLine(1) + ", " + addresses.get(0).getAddressLine(2));
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.d(TAG, "onLocationChanged: longitude -> " + longitude + ", latitude -> " + latitude);
        } catch (Exception e) {
            Toast.makeText(this, "error in onLocationChanged", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onLocationChanged: error in onLocationChanged");
        } finally {
            signalData.add(new SignalData(signalDBM, latitude, longitude));
            Log.d(TAG, "onLocationChanged: signalData.getLast() -> " + signalData.getLast());
            Log.d(TAG, "onLocationChanged: length -> " + signalData.size());
            Toast.makeText(MainActivity.this, "size -> " + signalData.size(), Toast.LENGTH_SHORT).show();
            DatabaseHandler.insertData(MainActivity.this, signalData.getLast());
        }
        Log.d(TAG, "onLocationChanged: ends");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "Please Enable GPS and Internet\nprovider -> " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(MainActivity.this, "provider -> " + provider + ", status -> " + status + ", extras -> " + extras, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider + ", status -> " + status + ", extras -> " + extras);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(MainActivity.this, "provider -> " + provider, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStatusChanged: " + "provider -> " + provider);
    }

    public boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED;
    }

}
