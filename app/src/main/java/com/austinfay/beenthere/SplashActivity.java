package com.austinfay.beenthere;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Permission;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.HashMap;


public class SplashActivity extends Activity {

    private static int DELAY = 1000;

    SharedPreferences preferences;
    LocationDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (savedInstanceState == null) {

            if(Build.VERSION.SDK_INT >= 23){

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED){

                    new GPSLocationMGR(SplashActivity.this);

                }


            } else {

                new GPSLocationMGR(SplashActivity.this);

            }

            preferences = this.getPreferences(MODE_PRIVATE);

            database = new LocationDatabase(this);

            boolean updated = preferences.getBoolean("1.0.10_UPDATE", false);

            if(!updated){

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        new finishUpdate().execute();

                    }
                }, DELAY);


            } else {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Intent finishIntent = new Intent(SplashActivity.this, MapActivity.class);
                        startActivity(finishIntent);
                        finish();

                    }
                }, DELAY);

            }

        }

    }

    private class finishUpdate extends AsyncTask{

        ProgressDialog dialog;

        ArrayList<HashMap<String, String>> allLocations;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            allLocations = database.getAllLocations();

            dialog = new ProgressDialog(SplashActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setTitle("Finishing Update");
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.setMax(allLocations.size());
            dialog.setProgress(0);
            dialog.show();

        }

        @Override
        protected Object doInBackground(Object[] params) {

            Log.d("UPDATE", "First time opening application since update to 1.0.10");

            if(allLocations.size() != 0){

                for(int i = 0; i < allLocations.size(); i++){

                    dialog.setProgress(i);

                    HashMap<String, String> currentLocation = allLocations.get(i);

                    int markerColor = Integer.parseInt(currentLocation.get("markerColor"));

                    int markerID = Integer.parseInt(currentLocation.get("locationId"));

                    Bitmap highResMarker = MapColors.generateIcon(markerColor, SplashActivity.this, R.drawable.map_marker_high_res);

                    final File markerFile = new File(getFilesDir(), (markerID + ".png"));

                    try{
                        FileOutputStream outputStream = new FileOutputStream(markerFile);
                        highResMarker.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                        outputStream.flush();
                        outputStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

            SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean("1.0.10_UPDATE", true);
            editor.apply();

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            dialog.dismiss();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    Intent finishIntent = new Intent(SplashActivity.this, MapActivity.class);
                    startActivity(finishIntent);
                    finish();

                }
            }, DELAY);

        }
    }

}
