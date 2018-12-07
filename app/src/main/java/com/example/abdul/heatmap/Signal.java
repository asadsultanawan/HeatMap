//package com.example.abdul.heatmap;
//
//import android.content.Context;
//import android.os.Build;
//import android.os.Bundle;
//import android.support.annotation.RequiresApi;
//import android.support.v7.app.AppCompatActivity;
//import android.telephony.PhoneStateListener;
//import android.telephony.SignalStrength;
//import android.telephony.TelephonyManager;
//import android.widget.TextView;
//
//public class Signal extends AppCompatActivity {
//
//    private static final String TAG = "Signal";
//    private TextView signalTextView_level;
//    private TextView signalTextView_dbm;
//    private TelephonyManager telephonyManager;
//    private myPhoneStateListener psListener;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_signal);
//
//        signalTextView_level = findViewById(R.id.signalLevelTextView);
//        signalTextView_dbm = findViewById(R.id.signalDBMTextView);
//
//        psListener = new myPhoneStateListener();
//        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
//    }
//
//    public class myPhoneStateListener extends PhoneStateListener {
//        public int signalStrengthValue_level;
//        public int signalStrengthValue_dbm;
//
//        @RequiresApi(api = Build.VERSION_CODES.M)
//        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
//            super.onSignalStrengthsChanged(signalStrength);
//            signalStrengthValue_level = signalStrength.getLevel();
//            signalStrengthValue_dbm = signalStrength.getGsmSignalStrength();
//            if (signalStrength.isGsm()) {
//                if (signalStrength.getGsmSignalStrength() != 99)
//                    signalStrengthValue_dbm = signalStrength.getGsmSignalStrength() * 2 - 113;
//                else
//                    signalStrengthValue_dbm = signalStrength.getGsmSignalStrength();
//            }
//            /*else {
//                signalStrengthValue = signalStrength.getLevel();
//            }*/
//            signalTextView_level.setText("Signal Strength level: " + signalStrengthValue_level);
//            signalTextView_dbm.setText("Signal Strength in dbm: " + signalStrengthValue_dbm);
//        }
//    }
//
//}
