package com.austinfay.beenthere;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Austin on 7/15/2015.
 * Been there
 */


public class MapActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public NavigationDrawerFragment mNavigationDrawerFragment;

    Button saveLocationButton, shareSnapshotButton;

    public static final int GPS_PERMISSION_REQUEST_CODE = 1;
    public static final int READ_WRITE_EXTERNAL_STORAGE = 2;

    GoogleApiAvailability apiAvailability;

    ImageView googleAttr;

    public GoogleMap googleMap;

    LocationDatabase database;

    HashMap<String, String> markerIDs;

    SharedPreferences preferences;

    AlertDialog setMapDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    GPS_PERMISSION_REQUEST_CODE);

        }

        database = new LocationDatabase(this);

        apiAvailability = GoogleApiAvailability.getInstance();

        setMapDialog = new AlertDialog.Builder(MapActivity.this).setNegativeButton("Cancel", null).create();

        markerIDs = new HashMap<>();

        googleAttr = (ImageView) findViewById(R.id.google_attr_img_map_activity);

        saveLocationButton = (Button) findViewById(R.id.save_location_button);
        shareSnapshotButton = (Button) findViewById(R.id.take_snapshot_button);

        if(apiAvailability.isGooglePlayServicesAvailable(MapActivity.this) == ConnectionResult.API_UNAVAILABLE){

            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

            builder.setTitle("Google Play Services");

            builder.setMessage("You don't have Google Play Services installed on your device. Would you like to install it?");

            builder.setPositiveButton("Install Google Play Services", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {

                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)));
                    } catch (ActivityNotFoundException e) {

                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https:///play.google.com/store/apps/details?id=" + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)));


                        Log.d("Google Play", "User does not have the Google Play store installed");
                    }
                }
            });

            builder.setNegativeButton("Close App", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_HOME)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(homeIntent);

                }
            });

            AlertDialog dialog = builder.create();

            dialog.show();

        } else {

            initMap(savedInstanceState);

        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);


        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent callSaveLocationIntent = new Intent(MapActivity.this, SaveLocationActivity.class);
                callSaveLocationIntent.putExtra("MAP_TYPE", googleMap.getMapType());
                startActivity(callSaveLocationIntent);
                finish();

            }
        });

        shareSnapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(MapActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                            READ_WRITE_EXTERNAL_STORAGE);

                } else {

                    if (!new File("/sdcard/BeenThere/").exists()) {
                        new File("/sdcard/BeenThere/").mkdir();
                    }

                    final String fileName = "/sdcard/BeenThere/" + System.currentTimeMillis() + ".png";

                    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {

                            try {
                                FileOutputStream outputStream = new FileOutputStream(fileName);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                                outputStream.flush();
                                outputStream.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    };

                    googleMap.snapshot(callback);

                    File file = new File(fileName);

                    final ContentValues values = new ContentValues(2);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());

                    final Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpeg");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Look at my map!");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, "Share snapshot"));

                }


            }
        });

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));




    }

    private void initMap(final Bundle savedInstanceState){

        try {

            //googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {

                    MapActivity.this.googleMap = map;

                    googleMap.setMyLocationEnabled(false);
                    googleMap.setBuildingsEnabled(true);
                    googleMap.getUiSettings().setMapToolbarEnabled(false);

                    ArrayList<HashMap<String, String>> locations = database.getAllLocations();

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    if(locations.size() != 0){

                        for (int i = 0; i < locations.size(); i++) {

                            HashMap<String, String> currentMap = locations.get(i);

                            LatLng currentLocation = new LatLng(Double.parseDouble(currentMap.get("locationLat")),
                                    Double.parseDouble(currentMap.get("locationLng")));

                            String address = currentMap.get("locationAddress");

                            String name = currentMap.get("locationName");

                            String id = currentMap.get("locationId");

                            int markerColor = Integer.parseInt(currentMap.get("markerColor"));

                            Marker marker = googleMap.addMarker(new MarkerOptions().position(currentLocation).title(name)
                                    .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(markerColor, MapActivity.this,
                                            R.drawable.map_marker_low_res))));

                            LatLng markerLatLng = marker.getPosition();

                            builder.include(markerLatLng);

                            if (locations.size() == 1) {

                                LatLng top = new LatLng(markerLatLng.latitude + .01, markerLatLng.longitude);
                                LatLng bottom = new LatLng(markerLatLng.latitude - .01, markerLatLng.longitude);
                                LatLng right = new LatLng(markerLatLng.latitude, markerLatLng.longitude + .01);
                                LatLng left = new LatLng(markerLatLng.latitude, markerLatLng.longitude - .01);
                                builder.include(bottom);
                                builder.include(top);
                                builder.include(right);
                                builder.include(left);

                            }

                            markerIDs.put(marker.getId(), id);

                        }


                    }

                    database.close();

                    setMapDialog.setTitle("Set Map Type");

                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    final View dialogView = inflater.inflate(R.layout.set_map_dialog, null);

                    final RadioGroup group = (RadioGroup) dialogView.findViewById(R.id.dialog_radio_group);

                    group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(RadioGroup group, int checkedId) {

                            final SharedPreferences.Editor editor = preferences.edit();

                            switch (group.getCheckedRadioButtonId()) {

                                case R.id.dialog_radio_hybrid:

                                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                                    googleAttr.setImageDrawable(ContextCompat.getDrawable(MapActivity.this, R.drawable.powered_by_google_dark));

                                    break;

                                case R.id.dialog_radio_normal:

                                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                                    googleAttr.setImageDrawable(ContextCompat.getDrawable(MapActivity.this, R.drawable.powered_by_google_light));

                                    break;

                                case R.id.dialog_radio_satellite:

                                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                                    googleAttr.setImageDrawable(ContextCompat.getDrawable(MapActivity.this, R.drawable.powered_by_google_dark));

                                    break;

                                case R.id.dialog_radio_terrain:

                                    googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

                                    googleAttr.setImageDrawable(ContextCompat.getDrawable(MapActivity.this, R.drawable.powered_by_google_light));

                                    break;

                            }

                            editor.putInt("MAP_TYPE", googleMap.getMapType());

                            editor.apply();

                            setMapDialog.dismiss();
                        }
                    });


                    setMapDialog.setView(dialogView);

                    preferences = MapActivity.this.getPreferences(MODE_PRIVATE);

                    if(savedInstanceState == null) {

                        int mapType = preferences.getInt("MAP_TYPE", GoogleMap.MAP_TYPE_NORMAL);

                        if(mapType == GoogleMap.MAP_TYPE_HYBRID || mapType == GoogleMap.MAP_TYPE_SATELLITE)

                            googleAttr.setImageDrawable(ContextCompat.getDrawable(MapActivity.this, R.drawable.powered_by_google_dark));

                        else
                            googleAttr.setImageDrawable(ContextCompat.getDrawable(MapActivity.this, R.drawable.powered_by_google_light));

                        googleMap.setMapType(mapType);

                        if(locations.size() > 0) {

                            LatLngBounds bounds = builder.build();
                            Display display = getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, size.x, size.y, 100);
                            googleMap.moveCamera(update);

                        }

                    } else {

                        googleMap.setMapType(savedInstanceState.getInt("MAP_TYPE"));

                        CameraPosition pos = savedInstanceState.getParcelable("CAMERA_POS");
                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
                        if(savedInstanceState.getBoolean("MAP_SELECTOR_VISIBLE", false)) setMapDialog.show();

                    }




                    googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick(Marker marker) {

                            int id = Integer.parseInt(markerIDs.get(marker.getId()));

                            Intent viewIntent = new Intent(MapActivity.this, PlaceViewActivity.class);
                            viewIntent.putExtra("markerID", id);
                            viewIntent.putExtra("listView", false);
                            startActivity(viewIntent);
                            finish();

                        }
                    });



                    googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                        @Override
                        public void onMapClick(LatLng latLng) {
                            ActionBar bar = getSupportActionBar();
                            bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#212121")));
                            bar.setTitle(Html.fromHtml("<font color=\"#ffffff\">Been there</font>"));
                            if (Build.VERSION.SDK_INT >= 21)
                                getWindow().setStatusBarColor(Color.parseColor("#000000"));
                        }
                    });

                    googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(LatLng latLng) {

                            Intent saveIntent = new Intent(MapActivity.this, SaveLocationActivity.class);
                            saveIntent.putExtra("LAT", latLng.latitude);
                            saveIntent.putExtra("LNG", latLng.longitude);
                            saveIntent.putExtra("MAP_HOLD_TAP", true);
                            saveIntent.putExtra("MAP_TYPE", googleMap.getMapType());
                            startActivity(saveIntent);
                            finish();

                        }
                    });

                    googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {

                            int id = Integer.parseInt(markerIDs.get(marker.getId()));

                            HashMap<String, String> locationInfo = database.getLocation(id);

                            ActionBar bar = getSupportActionBar();

                            bar.setBackgroundDrawable(new ColorDrawable(Integer.parseInt(locationInfo.get("markerColor"))));
                            float colorOff[] = new float[3];
                            Color.colorToHSV(Integer.parseInt(locationInfo.get("markerColor")), colorOff);
                            colorOff[2] = (float) (colorOff[2] - 0.15);
                            if(Build.VERSION.SDK_INT >= 21)
                                getWindow().setStatusBarColor(Color.HSVToColor(colorOff));

                            float colorHSV[] = new float[3];
                            Color.colorToHSV(Integer.parseInt(locationInfo.get("markerColor")), colorHSV);
                            if(colorHSV[2] * 100 < 50){
                                bar.setTitle(Html.fromHtml("<font color=\"#ffffff\">Been there</font>"));
                            } else if(colorHSV[1] * 100 < 50) {
                                bar.setTitle(Html.fromHtml("<font color=\"#000000\">Been there</font>"));
                            } else {
                                bar.setTitle(Html.fromHtml("<font color=\"#ffffff\">Been there</font>"));
                            }

                            return false;
                        }
                    });

                }
            });

            //googleMap.setPadding(0, 0, 0, 100);

        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case GPS_PERMISSION_REQUEST_CODE:

                if(grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    new GPSLocationMGR(MapActivity.this);

                }

                break;

            case READ_WRITE_EXTERNAL_STORAGE:

                if(grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if (!new File("/sdcard/BeenThere/").exists()) {
                        new File("/sdcard/BeenThere/").mkdir();
                    }

                    final String fileName = "/sdcard/BeenThere/" + System.currentTimeMillis() + ".png";

                    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {

                            try {
                                FileOutputStream outputStream = new FileOutputStream(fileName);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                                outputStream.flush();
                                outputStream.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    };

                    googleMap.snapshot(callback);

                    File file = new File(fileName);

                    final ContentValues values = new ContentValues(2);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());

                    final Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpeg");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Look at my map!");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, "Share snapshot"));

                } else {

                    Snackbar.make(shareSnapshotButton, "Oops! Can't save your screenshot without your permission!",
                            Snackbar.LENGTH_LONG).show();

                }

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(googleMap != null){

            outState.putParcelable("CAMERA_POS", googleMap.getCameraPosition());
            outState.putInt("MAP_TYPE", googleMap.getMapType());

        }


        outState.putBoolean("MAP_SELECTOR_VISIBLE", setMapDialog.isShowing());
        if(setMapDialog.isShowing()) setMapDialog.dismiss();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        database.close();
    }

    @Override
    public void onBackPressed() {

        database.close();

    }

    @Override
    public void whenDrawerOpens(){

        googleMap.setPadding(250, 0, 0, 0);

    }

    @Override
    public void whenDrawerCloses() {

        googleMap.setPadding(0, 0, 0, 0);

    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {

        if (googleMap != null) {

            switch (position) {

                case 0:

                    setMapDialog.show();

                    break;

                case 1:

                    Intent placesIntent = new Intent(MapActivity.this, PlacesActivity.class);
                    startActivity(placesIntent);
                    finish();

                    break;

                case 2:

                    Intent aboutIntent = new Intent(MapActivity.this, AboutActivity.class);
                    startActivity(aboutIntent);

                    break;

            }

        }
    }

}


