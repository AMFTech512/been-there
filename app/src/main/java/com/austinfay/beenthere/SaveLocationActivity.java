package com.austinfay.beenthere;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import yuku.ambilwarna.AmbilWarnaDialog;

/**
 * Created by Austin on 7/15/2015.
 * Been there
 */


public class SaveLocationActivity extends AppCompatActivity {

    public static final int SETTINGS_RESULT = 1;
    public static final int GPS_PERMISSION_REQUEST_CODE = 1;
    public static final String COORDINATES_FILTER = "com.austinfay.beenthere.SaveLocationActivity.COORDINATES_FILTER";

    ProgressDialog loadingDataDialog;
    AlertDialog gpsDialog, discardDialog;

    Bitmap highResMarker;

    ImageView googleAttr;

    LatLng currentLatLng;

    String address, name;

    GoogleMap googleMap;

    Marker marker;

    LocationManager locMngr;

    int color, markerID;

    boolean colorDialogShowing, loadingDialogShowing, gpsDialogOpened, edit, iconGenerated;

    LocationDatabase database;

    EditText nameEditText, addressEditText;
    TextView latitudeTextView, longitudeTextView;
    Button getLocationButton, colorButton, clrNameTextButton;
    ImageButton searchButton;
    AmbilWarnaDialog colorDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_location);

        iconGenerated = false;

        IntentFilter receiverAddressNormalFilter = new IntentFilter(GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
        registerReceiver(receiverAddressNormal, receiverAddressNormalFilter);

        IntentFilter receiverAddressExitFilter = new IntentFilter(GetAddressFromCoordinates.GET_ADDRESS_FILTER_FOR_EXIT);
        registerReceiver(receiverAddressExit, receiverAddressExitFilter);

        IntentFilter receiverCoordinatesNormalFilter = new IntentFilter(GetCoordinatesFromAddress.GET_COORDINATES_FILTER_NORMAL);
        registerReceiver(receiverCoordinatesNormal, receiverCoordinatesNormalFilter);

        IntentFilter receiverCoordinatesExitFilter = new IntentFilter(GetCoordinatesFromAddress.GET_COORDINATES_FILTER_FOR_EXIT);
        registerReceiver(receiverCoordinatesExit, receiverCoordinatesExitFilter);

        IntentFilter receiverCoordinatesFilter = new IntentFilter(COORDINATES_FILTER);
        registerReceiver(receiverCoordinates, receiverCoordinatesFilter);

        IntentFilter receiverFix = new IntentFilter(GPSLocationMGR.FIX_OBTAINED);
        registerReceiver(fixObtainReceiver, receiverFix);

        final ActionBar bar = getSupportActionBar();

        loadingDataDialog = new ProgressDialog(this);
        loadingDialogShowing = false;

        database = new LocationDatabase(SaveLocationActivity.this);

        locMngr = (LocationManager) getSystemService(LOCATION_SERVICE);

        nameEditText = (EditText) findViewById(R.id.name_edittext);
        addressEditText = (EditText) findViewById(R.id.address_edittext);
        latitudeTextView = (TextView) findViewById(R.id.latitude_textview);
        longitudeTextView = (TextView) findViewById(R.id.longitude_textview);

        nameEditText.setNextFocusDownId(R.id.address_edittext);

        googleAttr = (ImageView) findViewById(R.id.google_attr_img_save_location_activity);

        name = "New Location";

        color = (int) (Math.random() * -100000000);

        colorDialogShowing = false;

        colorButton = (Button) findViewById(R.id.save_location_color_button);
        colorButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                colorDialogShowing = true;

                colorDialog = new AmbilWarnaDialog(SaveLocationActivity.this,
                        color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                        colorDialogShowing = false;
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog ambilWarnaDialog, int i) {

                        colorDialogShowing = false;

                        color = i;

                        setThemeColor(i, name);
                    }
                });

                colorDialog.show();
            }
        });

        clrNameTextButton = (Button) findViewById(R.id.clr_name_edittext);
        clrNameTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                nameEditText.setText("");

            }
        });

        MapReady asyncMapReady = new MapReady();

        asyncMapReady.setValues(savedInstanceState, bar);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_preview_save_location);

        mapFragment.getMapAsync(asyncMapReady);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        getLocationButton = (Button) findViewById(R.id.get_location_button);
        searchButton = (ImageButton) findViewById(R.id.search_button_save_location);

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                name = s.toString();

                bar.setTitle(name);

                if (s.toString().equals(""))
                    clrNameTextButton.setVisibility(View.INVISIBLE);
                else
                    clrNameTextButton.setVisibility(View.VISIBLE);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!addressEditText.getText().toString().equals("")) {

                    if (isConnected()) {

                        View view = SaveLocationActivity.this.getCurrentFocus();

                        if(view != null){

                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);

                        }
                        loadingDataDialog = ProgressDialog.show(SaveLocationActivity.this, "", "Loading, Please wait...");
                        loadingDialogShowing = true;

                        Intent getCoordsIntent = new Intent(SaveLocationActivity.this, GetCoordinatesFromAddress.class);
                        getCoordsIntent.putExtra("FILTER", GetCoordinatesFromAddress.GET_COORDINATES_FILTER_NORMAL);
                        getCoordsIntent.putExtra("ADDRESS", addressEditText.getText().toString());
                        SaveLocationActivity.this.startService(getCoordsIntent);

                    } else {
                        Toast.makeText(SaveLocationActivity.this, "Please connect to the internet before searching with address", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SaveLocationActivity.this, "Please enter address", Toast.LENGTH_SHORT).show();
                }

            }
        });

        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(SaveLocationActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(SaveLocationActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            GPS_PERMISSION_REQUEST_CODE);

                } else {

                    getCoordinates();

                }

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case GPS_PERMISSION_REQUEST_CODE:

                if(grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    getCoordinates();

                    loadingDataDialog.dismiss();

                } else {

                    Snackbar.make(getLocationButton, "Oops! Can't get your location without your permission!",
                            Snackbar.LENGTH_LONG).show();

                }

                break;

        }
    }

    private void setThemeColor(int myColor, String title){

        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(myColor));
        float colorOff[] = new float[3];
        Color.colorToHSV(myColor, colorOff);
        colorOff[2] = (float) (colorOff[2] - 0.15);
        if(Build.VERSION.SDK_INT >= 21)
            getWindow().setStatusBarColor(Color.HSVToColor(colorOff));

        colorButton.setBackgroundColor(myColor);

        /*float colorHSV[] = new float[3];
        Color.colorToHSV(myColor, colorHSV);
        if(colorHSV[2] * 100 < 50){
            colorButton.setTextColor(Color.parseColor("#ffffff"));
            bar.setTitle(Html.fromHtml("<font color=\"#ffffff\">" + text + "</font>"));
        } else if(colorHSV[1] * 100 < 50) {
            colorButton.setTextColor(Color.parseColor("#000000"));
            bar.setTitle(Html.fromHtml("<font color=\"#000000\">" + text + "</font>"));
        } else {
            colorButton.setTextColor(Color.parseColor("#ffffff"));
            bar.setTitle(Html.fromHtml("<font color=\"#ffffff\">" + text + "</font>"));
        }*/

        bar.setTitle(title);

        if(marker != null) marker.setIcon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color, SaveLocationActivity.this,
                R.drawable.map_marker_low_res)));

        final int highResMarkerColor = myColor;

        iconGenerated = false;

        Thread markerGenThread = new Thread(){

            @Override
            public void run() {

                highResMarker = MapColors.generateIcon(highResMarkerColor, SaveLocationActivity.this, R.drawable.map_marker_high_res);

                if(iconGenerated)
                    saveHighResIcon();
                else iconGenerated = true;

            }
        };

        markerGenThread.start();


    }


    BroadcastReceiver receiverCoordinates = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            currentLatLng = new LatLng(intent.getDoubleExtra("LAT", 0), intent.getDoubleExtra("LNG", 0));

            if(isConnected()) {

                Intent getAddIntent = new Intent(SaveLocationActivity.this, GetAddressFromCoordinates.class);
                getAddIntent.putExtra("FILTER", GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
                getAddIntent.putExtra("LAT", currentLatLng.latitude);
                getAddIntent.putExtra("LNG", currentLatLng.longitude);
                SaveLocationActivity.this.startService(getAddIntent);

            } else {

                loadingDataDialog.dismiss();
                loadingDialogShowing = false;
                latitudeTextView.setText(Double.toString(currentLatLng.latitude));
                longitudeTextView.setText(Double.toString(currentLatLng.longitude));

                if (marker == null) {

                    marker = googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(
                            color, SaveLocationActivity.this, R.drawable.map_marker_low_res)))
                            .position(currentLatLng));

                } else {

                    marker.setPosition(currentLatLng);

                }

                moveGoogleCam(currentLatLng, true);

            }

        }
    };

    BroadcastReceiver receiverAddressNormal = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            address = intent.getStringExtra("ADDRESS");

            addressEditText.setText(address);
            latitudeTextView.setText(Double.toString(currentLatLng.latitude));
            longitudeTextView.setText(Double.toString(currentLatLng.longitude));
            loadingDataDialog.dismiss();
            loadingDialogShowing = false;

            if(nameEditText.getText().toString().equals("")){
                name = address;
                nameEditText.setText(name);
            }

            if(marker == null){

                marker = googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color,
                        SaveLocationActivity.this, R.drawable.map_marker_low_res)))
                        .position(currentLatLng));

            } else {

                marker.setPosition(currentLatLng);

            }

            moveGoogleCam(currentLatLng, true);

        }
    };

    public void saveHighResIcon(){

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

    BroadcastReceiver receiverAddressExit = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            address = intent.getStringExtra("ADDRESS");

            if(nameEditText.getText().toString().equals(""))
                name = address;

            if(!edit)
                database.insertLocation(name, address, currentLatLng.latitude, currentLatLng.longitude, color);
            else
                database.updateLocation(markerID, name, address, currentLatLng.latitude, currentLatLng.longitude, color);
            database.close();

            saveHighResIcon();

            loadingDataDialog.dismiss();

            if(!edit){
                Intent saveIntent = new Intent(SaveLocationActivity.this, MapActivity.class);
                startActivity(saveIntent);
                finish();
            } else {
                Intent saveIntent = new Intent(SaveLocationActivity.this, PlaceViewActivity.class);
                saveIntent.putExtra("markerID", markerID);
                startActivity(saveIntent);
                finish();
            }

        }
    };


    BroadcastReceiver receiverCoordinatesNormal = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(!intent.getBooleanExtra("NETERROR", true)) {

                if (intent.getBooleanExtra("VALID", false)) {

                    currentLatLng = new LatLng(intent.getDoubleExtra("LAT", 0), intent.getDoubleExtra("LNG", 0));
                    address = intent.getStringExtra("ADDRESS");

                    loadingDataDialog.dismiss();
                    loadingDialogShowing = false;
                    addressEditText.setText(address);
                    latitudeTextView.setText(Double.toString(currentLatLng.latitude));
                    longitudeTextView.setText(Double.toString(currentLatLng.longitude));
                    addressEditText.selectAll();

                    if (marker == null) {

                        marker = googleMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color, SaveLocationActivity.this,
                                        R.drawable.map_marker_low_res)))
                                .position(currentLatLng));

                    } else {

                        marker.setPosition(currentLatLng);

                    }

                    moveGoogleCam(currentLatLng, true);

                } else {

                    Toast.makeText(SaveLocationActivity.this, "Couldn't find address", Toast.LENGTH_SHORT).show();
                    loadingDataDialog.dismiss();
                    loadingDialogShowing = false;

                }

            } else {
                Toast.makeText(SaveLocationActivity.this, "Network error occurred, please try again", Toast.LENGTH_SHORT).show();
                loadingDataDialog.dismiss();
                loadingDialogShowing = false;
            }

        }
    };

    BroadcastReceiver receiverCoordinatesExit = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(!intent.getBooleanExtra("NETERROR", true)) {

                if (intent.getBooleanExtra("VALID", false)) {

                    currentLatLng = new LatLng(intent.getDoubleExtra("LAT", 0), intent.getDoubleExtra("LNG", 0));
                    address = intent.getStringExtra("ADDRESS");

                    if(!edit)
                        database.insertLocation(name, address, currentLatLng.latitude, currentLatLng.longitude, color);
                    else
                        database.updateLocation(markerID, name, address, currentLatLng.latitude, currentLatLng.longitude, color);
                    database.close();

                    saveHighResIcon();

                    loadingDataDialog.dismiss();
                    loadingDialogShowing = false;

                    if(!edit){
                        Intent saveIntent = new Intent(SaveLocationActivity.this, MapActivity.class);
                        startActivity(saveIntent);
                        finish();
                    } else {
                        Intent saveIntent = new Intent(SaveLocationActivity.this, PlaceViewActivity.class);
                        saveIntent.putExtra("markerID", markerID);
                        startActivity(saveIntent);
                        finish();
                    }

                } else {

                    Toast.makeText(SaveLocationActivity.this, "Couldn't find address", Toast.LENGTH_SHORT).show();
                    loadingDataDialog.dismiss();
                    loadingDialogShowing = false;

                }

            } else {
                Toast.makeText(SaveLocationActivity.this, "Network error occurred, please try again", Toast.LENGTH_SHORT).show();
                loadingDataDialog.dismiss();
                loadingDialogShowing = false;
            }

        }
    };

    private void confirmDiscard(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Discard Location");
        builder.setMessage("Do you want to discard this new location?");

        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                database.close();
                if (!edit) {
                    Intent saveIntent = new Intent(SaveLocationActivity.this, MapActivity.class);
                    startActivity(saveIntent);
                    finish();
                } else {
                    Intent saveIntent = new Intent(SaveLocationActivity.this, PlaceViewActivity.class);
                    saveIntent.putExtra("markerID", markerID);
                    startActivity(saveIntent);
                    finish();
                }

            }
        });

        builder.setPositiveButton("Cancel", null);

        discardDialog = builder.create();
        discardDialog.show();

    }

    public void unregisterAllReceivers(){

        unregisterReceiver(receiverAddressNormal);
        unregisterReceiver(receiverAddressExit);
        unregisterReceiver(receiverCoordinatesNormal);
        unregisterReceiver(receiverCoordinatesExit);
        unregisterReceiver(receiverCoordinates);
        unregisterReceiver(fixObtainReceiver);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(loadingDataDialog != null){

            outState.putBoolean("DIALOG_VISIBLE", loadingDataDialog.isShowing());
            if(loadingDataDialog.isShowing()) loadingDataDialog.dismiss();

        } else {
            outState.putBoolean("DIALOG_VISIBLE", false);
        }

        if(discardDialog != null) {
            outState.putBoolean("DISCARD_DIALOG_VISIBLE", discardDialog.isShowing());
            if (discardDialog.isShowing()) {
                discardDialog.dismiss();
            }
        } else {
            outState.putBoolean("DISCARD_DIALOG_VISIBLE", false);
        }

        if(currentLatLng != null){

            outState.putBoolean("LATLNG_NULL", false);
            outState.putDouble("LAT", currentLatLng.latitude);
            outState.putDouble("LNG", currentLatLng.longitude);

        } else {

            outState.putBoolean("LATLNG_NULL", true);

        }

        if(gpsDialog != null){

            outState.putBoolean("GPS_DIALOG_VISIBLE", gpsDialog.isShowing());
            if(gpsDialog.isShowing()) gpsDialog.dismiss();

        } else {
            outState.putBoolean("GPS_DIALOG_VISIBLE", false);
        }

        if(colorDialogShowing){
            colorDialog.getDialog().dismiss();
        }

        outState.putBoolean("EDIT", edit);

        outState.putBoolean("COLOR_DIALOG_VISIBLE", colorDialogShowing);
        outState.putInt("COLOR", color);
        outState.putBoolean("IS_SEARCHING", loadingDialogShowing);
        if(name == null) outState.putString("NAME", "");
        else outState.putString("NAME", name);
        database.close();

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d("Pause", "onPause() called");
        loadingDataDialog.dismiss();
    }

    @Override
    protected void onResume() {

        if(loadingDialogShowing && !gpsDialogOpened){
            Log.d("Resume", Boolean.toString(loadingDialogShowing));
            loadingDataDialog = ProgressDialog.show(this, "", "Loading, Please wait...");
        }

        gpsDialogOpened = false;

        super.onResume();

    }

    @Override
    public void onBackPressed() {

        confirmDiscard();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterAllReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save_location, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case R.id.action_save:

                View view = SaveLocationActivity.this.getCurrentFocus();

                if(view != null){

                    InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.hideSoftInputFromWindow(view.getWindowToken(), 0);

                }

                loadingDataDialog = ProgressDialog.show(this, "", "Loading, Please Wait...");
                loadingDialogShowing = true;

                address = addressEditText.getText().toString();
                name = nameEditText.getText().toString();

                currentLatLng = null;

                boolean isConnected = isConnected();

                if(!name.equals("")) {

                    if(!address.equals("") && !longitudeTextView.getText().toString().equals("") &&
                            !latitudeTextView.getText().toString().equals("")){

                        currentLatLng = new LatLng(Double.parseDouble(latitudeTextView.getText().toString()),
                                Double.parseDouble(longitudeTextView.getText().toString()));

                        if(!edit)
                            database.insertLocation(name, address, currentLatLng.latitude, currentLatLng.longitude, color);
                        else
                            database.updateLocation(markerID, name, address, currentLatLng.latitude, currentLatLng.longitude, color);


                        if(iconGenerated)
                            saveHighResIcon();
                        else
                            iconGenerated = true;

                        database.close();

                        loadingDataDialog.dismiss();
                        loadingDialogShowing = false;

                        if(!edit){
                            Intent saveIntent = new Intent(SaveLocationActivity.this, MapActivity.class);
                            startActivity(saveIntent);
                            finish();
                        } else {
                            Intent saveIntent = new Intent(SaveLocationActivity.this, PlaceViewActivity.class);
                            saveIntent.putExtra("markerID", markerID);
                            startActivity(saveIntent);
                            finish();
                        }

                    } else {

                        if (address.equals("")) {

                            if (latitudeTextView.getText().toString().equals("") || longitudeTextView.getText().toString().equals("")) {

                                Log.d("Empty", "All necessary fields empty");
                                Toast.makeText(SaveLocationActivity.this, "Please enter address or get your current location", Toast.LENGTH_LONG).show();

                            } else if(isConnected) {

                                currentLatLng = new LatLng(Double.parseDouble(latitudeTextView.getText().toString()),
                                        Double.parseDouble(longitudeTextView.getText().toString()));

                                Intent getAddIntent = new Intent(SaveLocationActivity.this, GetAddressFromCoordinates.class);
                                getAddIntent.putExtra("FILTER", GetAddressFromCoordinates.GET_ADDRESS_FILTER_FOR_EXIT);
                                getAddIntent.putExtra("LAT", currentLatLng.latitude);
                                getAddIntent.putExtra("LNG", currentLatLng.longitude);
                                SaveLocationActivity.this.startService(getAddIntent);

                            } else {

                                currentLatLng = new LatLng(Double.parseDouble(latitudeTextView.getText().toString()),
                                        Double.parseDouble(longitudeTextView.getText().toString()));

                                if(!edit)
                                    database.insertLocation(name, "Unknown", currentLatLng.latitude, currentLatLng.longitude, color);
                                else
                                    database.updateLocation(markerID, name, "Unknown", currentLatLng.latitude, currentLatLng.longitude, color);
                                database.close();

                                saveHighResIcon();

                                loadingDataDialog.dismiss();
                                loadingDialogShowing = false;

                                if(!edit){
                                    Intent saveIntent = new Intent(SaveLocationActivity.this, MapActivity.class);
                                    startActivity(saveIntent);
                                    finish();
                                } else {
                                    Intent saveIntent = new Intent(SaveLocationActivity.this, PlaceViewActivity.class);
                                    saveIntent.putExtra("markerID", markerID);
                                    startActivity(saveIntent);
                                    finish();
                                }

                            }

                        } else if(isConnected) {

                            Intent getCoordsIntent = new Intent(SaveLocationActivity.this, GetCoordinatesFromAddress.class);
                            getCoordsIntent.putExtra("FILTER", GetCoordinatesFromAddress.GET_COORDINATES_FILTER_FOR_EXIT);
                            getCoordsIntent.putExtra("ADDRESS", address);
                            SaveLocationActivity.this.startService(getCoordsIntent);

                        } else {

                            Toast.makeText(SaveLocationActivity.this, "Please connect to the internet before searching with address", Toast.LENGTH_LONG).show();
                            loadingDataDialog.dismiss();
                            loadingDialogShowing = false;

                        }

                    }

                } else {

                    Toast.makeText(SaveLocationActivity.this, "Please enter name", Toast.LENGTH_SHORT).show();
                    loadingDataDialog.dismiss();
                    loadingDialogShowing = false;

                }

        }

        return super.onOptionsItemSelected(item);
    }

    BroadcastReceiver fixObtainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Coords", "fixObtainReceiver called");

            loadingDataDialog.dismiss();
            loadingDialogShowing = false;
            GPSLocationMGR.setObtainFixCall(false);
            getCoordinates();

        }
    };

    public void getCoordinates() {

        loadingDataDialog = ProgressDialog.show(SaveLocationActivity.this, "", "Loading, Please Wait...");
        loadingDialogShowing = true;

        View view = SaveLocationActivity.this.getCurrentFocus();

        if (view != null) {

            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);

        }

        if(!locMngr.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locMngr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

            openGpsDialog();

        } else {

            LatLng gpsLatLng = null, netLatLng = null;

            if (locMngr.isProviderEnabled(LocationManager.GPS_PROVIDER) && GPSLocationMGR.hasFixBeenObtained()){

                Location myGPSLocation = locMngr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(myGPSLocation != null) gpsLatLng = new LatLng(myGPSLocation.getLatitude(), myGPSLocation.getLongitude());

            }

            if(locMngr.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && isConnected()){

                Location myNetworkLocation = locMngr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if(myNetworkLocation != null) netLatLng = new LatLng(myNetworkLocation.getLatitude(), myNetworkLocation.getLongitude());

            }

            if(gpsLatLng != null && netLatLng != null){

                currentLatLng = new LatLng(((gpsLatLng.latitude + netLatLng.latitude)/2),
                        ((gpsLatLng.longitude + netLatLng.longitude)/2));

            } else if(gpsLatLng != null) currentLatLng = gpsLatLng;
            else if (netLatLng != null) currentLatLng = netLatLng;

            Log.d("Coordinates", "Coordinates obtained");

            if(currentLatLng != null) {

                if (isConnected()) {

                    Intent getAddressIntent = new Intent(SaveLocationActivity.this, GetAddressFromCoordinates.class);
                    getAddressIntent.putExtra("FILTER", GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
                    getAddressIntent.putExtra("LAT", currentLatLng.latitude);
                    getAddressIntent.putExtra("LNG", currentLatLng.longitude);
                    SaveLocationActivity.this.startService(getAddressIntent);



                } else {

                    latitudeTextView.setText(Double.toString(currentLatLng.latitude));
                    longitudeTextView.setText(Double.toString(currentLatLng.longitude));

                    loadingDataDialog.dismiss();
                    loadingDialogShowing = false;
                }

            } else {

                GPSLocationMGR.setObtainFixCall(true);

                Log.d("Coords", "Coordinates were null");

            }

        }

    }

    private boolean isConnected(){

        ConnectivityManager connectMgr =
                (ConnectivityManager) SaveLocationActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());

    }

    private void openGpsDialog() {

        gpsDialogOpened = true;

        if(loadingDataDialog != null){
            if(loadingDataDialog.isShowing())loadingDataDialog.dismiss();
        }

        loadingDialogShowing = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("No Location Services Enabled");
        builder.setMessage("Please enable GPS location service");

        builder.setPositiveButton("Goto Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(settingsIntent, SETTINGS_RESULT);

            }
        });

        builder.setNegativeButton("Cancel", null);

        gpsDialog = builder.create();
        gpsDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == SETTINGS_RESULT){

            boolean isConnected = isConnected();

            if(locMngr.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    isConnected && locMngr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

                loadingDataDialog.dismiss();
                loadingDialogShowing = false;

                getCoordinates();

            }

        }

    }

    private void moveGoogleCam(LatLng location, boolean animate){

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        LatLng top = new LatLng(location.latitude + .03, location.longitude);
        LatLng bottom = new LatLng(location.latitude - .03, location.longitude);
        LatLng right = new LatLng(location.latitude, location.longitude + .03);
        LatLng left = new LatLng(location.latitude, location.longitude - .03);
        builder.include(location);
        builder.include(bottom);
        builder.include(top);
        builder.include(right);
        builder.include(left);
        LatLngBounds bounds = builder.build();
        if(animate) {
            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, 10);
            googleMap.animateCamera(update);
        } else {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, size.x, size.y, 80);
            googleMap.moveCamera(update);
        }

    }

    public class MapReady implements OnMapReadyCallback {

        private Bundle savedInstanceState;
        private ActionBar bar;

        public void setValues(Bundle instanceState, ActionBar tempBar){

            savedInstanceState = instanceState;
            bar = tempBar;

        }

        @Override
        public void onMapReady(GoogleMap map) {

            SaveLocationActivity.this.googleMap = map;

            googleMap.setMyLocationEnabled(false);
            googleMap.setBuildingsEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            googleMap.setPadding(0, 0, 0, 10);

            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {

                    currentLatLng = latLng;

                    if (marker == null) {

                        marker = googleMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color, SaveLocationActivity.this,
                                        R.drawable.map_marker_low_res))).position(currentLatLng));

                    } else {

                        marker.setPosition(currentLatLng);

                    }


                    if (isConnected()) {

                        loadingDialogShowing = true;

                        Intent getAddIntent = new Intent(SaveLocationActivity.this, GetAddressFromCoordinates.class);
                        getAddIntent.putExtra("FILTER", GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
                        getAddIntent.putExtra("LAT", currentLatLng.latitude);
                        getAddIntent.putExtra("LNG", currentLatLng.longitude);
                        SaveLocationActivity.this.startService(getAddIntent);

                    } else {

                        loadingDataDialog.dismiss();
                        loadingDialogShowing = false;
                        latitudeTextView.setText(Double.toString(currentLatLng.latitude));
                        longitudeTextView.setText(Double.toString(currentLatLng.longitude));

                        moveGoogleCam(currentLatLng, false);

                    }

                }
            });

            if(savedInstanceState != null){

                color = savedInstanceState.getInt("COLOR");
                name = savedInstanceState.getString("NAME");
                loadingDialogShowing = savedInstanceState.getBoolean("IS_SEARCHING");

                if(savedInstanceState.getBoolean("COLOR_DIALOG_VISIBLE")){

                    colorDialogShowing = true;

                    colorDialog = new AmbilWarnaDialog(SaveLocationActivity.this,
                            color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                        @Override
                        public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                            colorDialogShowing = false;
                        }

                        @Override
                        public void onOk(AmbilWarnaDialog ambilWarnaDialog, int i) {

                            colorDialogShowing = false;

                            color = i;

                            setThemeColor(i, name);
                        }
                    });

                    colorDialog.show();

                }

                edit = savedInstanceState.getBoolean("EDIT", false);

                if(savedInstanceState.getBoolean("DIALOG_VISIBLE")) {

                    loadingDataDialog = ProgressDialog.show(SaveLocationActivity.this, "", "Loading, Please wait...");
                    loadingDialogShowing = true;

                }

                if(savedInstanceState.getBoolean("DISCARD_DIALOG_VISIBLE")){
                    confirmDiscard();
                }

                if(savedInstanceState.getBoolean("GPS_DIALOG_VISIBLE")){

                    openGpsDialog();
                }

                if(!savedInstanceState.getBoolean("LATLNG_NULL")){

                    currentLatLng = new LatLng(savedInstanceState.getDouble("LAT"), savedInstanceState.getDouble("LNG"));

                    marker = googleMap.addMarker(new MarkerOptions().position(currentLatLng)
                            .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color, SaveLocationActivity.this,
                                    R.drawable.map_marker_low_res))));

                    latitudeTextView.setText(Double.toString(currentLatLng.latitude));
                    longitudeTextView.setText(Double.toString(currentLatLng.longitude));

                    moveGoogleCam(currentLatLng, false);

                }

            } else {

                Intent getIntent = getIntent();

                edit = getIntent.getBooleanExtra("EDIT", false);

                int mapType = getIntent.getIntExtra("MAP_TYPE", GoogleMap.MAP_TYPE_NORMAL);

                googleMap.setMapType(mapType);

                if(mapType == GoogleMap.MAP_TYPE_SATELLITE || mapType == GoogleMap.MAP_TYPE_HYBRID)

                    googleAttr.setImageDrawable(ContextCompat.getDrawable(SaveLocationActivity.this, R.drawable.powered_by_google_dark));

                else

                    googleAttr.setImageDrawable(ContextCompat.getDrawable(SaveLocationActivity.this, R.drawable.powered_by_google_light));

                if (getIntent.getBooleanExtra("MAP_HOLD_TAP", false)) {

                    currentLatLng = new LatLng(getIntent.getDoubleExtra("LAT", 0), getIntent.getDoubleExtra("LNG", 0));

                    if (isConnected()) {

                        loadingDialogShowing = true;

                        Intent getAddIntent = new Intent(SaveLocationActivity.this, GetAddressFromCoordinates.class);
                        getAddIntent.putExtra("FILTER", GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
                        getAddIntent.putExtra("LAT", currentLatLng.latitude);
                        getAddIntent.putExtra("LNG", currentLatLng.longitude);
                        SaveLocationActivity.this.startService(getAddIntent);

                    }  else {

                        loadingDataDialog.dismiss();
                        loadingDialogShowing = false;
                        latitudeTextView.setText(Double.toString(currentLatLng.latitude));
                        longitudeTextView.setText(Double.toString(currentLatLng.longitude));

                        if (marker == null) {

                            marker = googleMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color, SaveLocationActivity.this,
                                            R.drawable.map_marker_low_res))).position(currentLatLng));

                        } else {

                            marker.setPosition(currentLatLng);

                        }

                        moveGoogleCam(currentLatLng, false);

                    }

                } else if (edit) {

                    database = new LocationDatabase(SaveLocationActivity.this);

                    HashMap<String, String> currentLocationValues = database.getLocation(getIntent.getIntExtra("PLACE_ID", 0));

                    color = Integer.parseInt(currentLocationValues.get("markerColor"));

                    currentLatLng = new LatLng(Double.parseDouble(currentLocationValues.get("locationLat")),
                            Double.parseDouble(currentLocationValues.get("locationLng")));

                    marker = googleMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(color, SaveLocationActivity.this,
                                    R.drawable.map_marker_low_res))).position(currentLatLng));

                    latitudeTextView.setText(currentLocationValues.get("locationLat"));
                    longitudeTextView.setText(currentLocationValues.get("locationLng"));

                    name = currentLocationValues.get("locationName");

                    bar.setTitle(name);

                    markerID = getIntent.getIntExtra("PLACE_ID", 0);

                    nameEditText.setText(name);

                    address = currentLocationValues.get("locationAddress");

                    addressEditText.setText(address);

                    moveGoogleCam(currentLatLng, false);

                } else {

                    markerID = database.getNextAvailableID();

                }

            }

            setThemeColor(color, name);

        }
    }
}


