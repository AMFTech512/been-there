package com.austinfay.beenthere;

import android.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class GPSLocationMGR {

    public static boolean firstFixObtained = false, obtainFixCall = false;

    public static String FIX_OBTAINED = "com.austinfay.beenthere.FIX_OBTAINED";

    LocationListener listener;

    Context passedContext;

    public GPSLocationMGR(Context context) {

        passedContext = context;

        final LocationManager locMgr = (LocationManager) passedContext.getSystemService(Activity.LOCATION_SERVICE);

        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

                Log.d("Provider enabled", "Provider enabled");

                updateLocation(locMgr);

            }

            @Override
            public void onProviderDisabled(String provider) {

                Log.d("Provider disabled", "Provider Disabled");

            }
        };

        updateLocation(locMgr);

    }

    @SuppressLint("LongLogTag")
    private void updateLocation(LocationManager locMgr) {



            locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, listener);
            locMgr.addGpsStatusListener(gpsStatusListener);
            locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, listener);



    }



    private boolean isConnected(){

        ConnectivityManager connectMgr =
                (ConnectivityManager) passedContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());

    }

    public static void setObtainFixCall(boolean val){
        obtainFixCall = val;
    }

    public static boolean hasFixBeenObtained(){
        return firstFixObtained;
    }

    private GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener(){

        @Override
        public void onGpsStatusChanged(int event) {

            if(event == GpsStatus.GPS_EVENT_FIRST_FIX){

                firstFixObtained = true;
                Log.d("GPS Fix", "First Fix has been obtained");
                if(obtainFixCall){
                    Intent broadcastIntent = new Intent(FIX_OBTAINED);
                    passedContext.sendBroadcast(broadcastIntent);
                }

            }

        }
    };

}