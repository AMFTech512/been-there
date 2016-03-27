package com.austinfay.beenthere;

        import android.*;
        import android.app.AlertDialog;
        import android.app.ProgressDialog;
        import android.content.BroadcastReceiver;
        import android.content.ContentValues;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.Color;
        import android.graphics.Point;
        import android.graphics.drawable.ColorDrawable;
        import android.net.ConnectivityManager;
        import android.net.NetworkInfo;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Environment;
        import android.provider.MediaStore;
        import android.support.design.widget.Snackbar;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.ActionBar;
        import android.support.v7.app.ActionBarActivity;
        import android.os.Bundle;
        import android.support.v7.app.AppCompatActivity;
        import android.text.Html;
        import android.util.Log;
        import android.view.Display;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.ViewTreeObserver;
        import android.view.Window;
        import android.view.WindowManager;
        import android.widget.ProgressBar;
        import android.widget.ScrollView;
        import android.widget.TableLayout;
        import android.widget.TableRow;
        import android.widget.TextView;
        import com.google.android.gms.maps.CameraUpdate;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.MapFragment;
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
        import java.util.HashMap;


public class PlaceViewActivity extends AppCompatActivity {

    public static final int READ_WRITE_EXTERNAL_STORAGE = 1;

    TextView latitudeTextView, longitudeTextView, nameTextView, addressTextView;

    LocationDatabase database;

    GoogleMap googleMap;

    String titleColor;

    Marker marker;

    ScrollView scrollView;
    TableLayout infoTableLayout;

    AlertDialog deleteDialog;

    ProgressDialog loadingDataDialog;
    ProgressBar loadingBar;

    TableRow nameTableRow, addressTableRow;

    LatLng currentLatLng;
    String address, name;
    int color, markerID;

    boolean isSearching = false;
    boolean fromList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_view);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent getIntent = getIntent();
        markerID = getIntent.getIntExtra("markerID", 0);
        fromList = getIntent.getBooleanExtra("listView", false);

        IntentFilter getAddressFilter = new IntentFilter(GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
        registerReceiver(getAddressReceiver, getAddressFilter);


        try {

            if (googleMap == null) {

                googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_preview_place_view)).getMap();

            }
            googleMap.setMyLocationEnabled(false);
            googleMap.setBuildingsEnabled(true);
            googleMap.getUiSettings().setAllGesturesEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadingDataDialog = new ProgressDialog(this);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        infoTableLayout = (TableLayout) findViewById(R.id.info_table_layout_place_view);

        nameTableRow = (TableRow) findViewById(R.id.name_table_row_place_view);
        addressTableRow = (TableRow) findViewById(R.id.address_table_row_place_view);

        nameTextView = (TextView) findViewById(R.id.name_edittext_place_view);
        addressTextView = (TextView) findViewById(R.id.address_edittext_place_view);
        latitudeTextView = (TextView) findViewById(R.id.latitude_textview_place_view);
        longitudeTextView = (TextView) findViewById(R.id.longitude_textview_place_view);

        nameTableRow.setOnTouchListener(new View.OnTouchListener() {
            boolean isHidden = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (isHidden) {
                    scrollView.animate().translationY(0);
                    googleMap.setPadding(0, 0, 0, scrollView.getHeight() + 10);
                    isHidden = false;
                } else {
                    isHidden = true;
                    scrollView.animate().translationY(scrollView.getHeight() - nameTableRow.getHeight());
                    googleMap.setPadding(0, 0, 0, nameTableRow.getHeight() + 10);
                }

                return false;
            }
        });





        loadingBar = (ProgressBar) findViewById(R.id.progressBar_place_view);
        loadingBar.setVisibility(View.GONE);

        database = new LocationDatabase(this);

        final HashMap<String, String> currentLocationValues = database.getLocation(markerID);

        color = Integer.parseInt(currentLocationValues.get("markerColor"));

        float colorHSV[] = new float[3];
        Color.colorToHSV(color, colorHSV);
        if(colorHSV[2] * 100 < 50){
            titleColor = "#ffffff";
        } else if(colorHSV[1] * 100 < 50) {
            titleColor = "#000000";
        } else {
            titleColor = "#ffffff";
        }

        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(color));
        float colorOff[] = new float[3];
        Color.colorToHSV(color, colorOff);
        colorOff[2] = (float) (colorOff[2] - 0.15);
        if(Build.VERSION.SDK_INT >= 21)
            getWindow().setStatusBarColor(Color.HSVToColor(colorOff));

        nameTableRow.setBackgroundColor(color);
        addressTableRow.setBackgroundColor(color);

        currentLatLng = new LatLng(Double.parseDouble(currentLocationValues.get("locationLat")),
                Double.parseDouble(currentLocationValues.get("locationLng")));

        loadingDataDialog = new ProgressDialog(this);

        address = currentLocationValues.get("locationAddress");

        if (isConnected() && address.equals("Unknown")) {

            addressTextView.setVisibility(View.GONE);
            loadingBar.setVisibility(View.VISIBLE);

            addressTableRow.setBackgroundColor(0xffffffff);

            Intent getAddressIntent = new Intent(PlaceViewActivity.this, GetAddressFromCoordinates.class);
            getAddressIntent.putExtra("LAT", currentLatLng.latitude);
            getAddressIntent.putExtra("LNG", currentLatLng.longitude);
            getAddressIntent.putExtra("FILTER", GetAddressFromCoordinates.GET_ADDRESS_FILTER_NORMAL);
            PlaceViewActivity.this.startService(getAddressIntent);

        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        name = currentLocationValues.get("locationName");

        nameTextView.setText(currentLocationValues.get("locationName"));
        addressTextView.setText(currentLocationValues.get("locationAddress"));
        latitudeTextView.setText(currentLocationValues.get("locationLat"));
        longitudeTextView.setText(currentLocationValues.get("locationLng"));

        marker = googleMap.addMarker(new MarkerOptions().position(currentLatLng)
                .icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(
                        Integer.parseInt(currentLocationValues.get("markerColor")), PlaceViewActivity.this,
                        R.drawable.map_marker_low_res))));

        moveGoogleCam(marker.getPosition(), false);

        if(savedInstanceState != null) {

            name = savedInstanceState.getString("NAME");

            scrollView.setTranslationY(savedInstanceState.getFloat("INFOBAR_POS"));

            CameraPosition pos = savedInstanceState.getParcelable("CAMERA_POS");
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));

            String name = savedInstanceState.getString("NAME");

            if(savedInstanceState.getBoolean("DELETE_DIALOG_VISIBLE")){
                confirmDelete(name);
            }


        }

        setBarTitle(name);

    }

    BroadcastReceiver getAddressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            address = intent.getStringExtra("ADDRESS");

            addressTextView.setText(address);
            latitudeTextView.setText(Double.toString(currentLatLng.latitude));
            longitudeTextView.setText(Double.toString(currentLatLng.longitude));
            if(loadingDataDialog != null) loadingDataDialog.dismiss();

            database.updateLocation(markerID, name, address, currentLatLng.latitude, currentLatLng.longitude,
            color);

            addressTableRow.setBackgroundColor(color);

            addressTextView.setVisibility(View.VISIBLE);
            loadingBar.setVisibility(View.GONE);

            if(marker == null){

                marker = googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(MapColors.generateIcon(
                        color,
                        PlaceViewActivity.this,
                        R.drawable.map_marker_low_res
                )))
                        .position(currentLatLng));

            } else {

                marker.setPosition(currentLatLng);

            }

            moveGoogleCam(currentLatLng, true);
        }
    };

    private void setBarTitle(String title){

        ActionBar bar = getSupportActionBar();
        bar.setTitle(Html.fromHtml("<font color=\"" + titleColor + "\">" + title + "</font>"));

    }


    @Override
    protected void onResume() {
        super.onResume();
        if(isSearching){
            loadingDataDialog = ProgressDialog.show(PlaceViewActivity.this, "", "Loading, Please Wait...");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("NAME", name);
        outState.putParcelable("CAMERA_POS", googleMap.getCameraPosition());
        outState.putDouble("LAT", currentLatLng.latitude);
        outState.putDouble("LNG", currentLatLng.longitude);
        outState.putBoolean("PROGRESS_DIALOG_VISIBLE", loadingDataDialog.isShowing());
        if(loadingDataDialog.isShowing()) loadingDataDialog.dismiss();

        if(deleteDialog != null) {
            outState.putBoolean("DELETE_DIALOG_VISIBLE", deleteDialog.isShowing());
            if (deleteDialog.isShowing()) {
                deleteDialog.dismiss();
            }
        } else {
            outState.putBoolean("DELETE_DIALOG_VISIBLE", false);
        }

        outState.putString("NAME", nameTextView.getText().toString());

        outState.putFloat("INFOBAR_POS", scrollView.getTranslationY());

        database.close();
    }

    private void confirmDelete(String name){

        AlertDialog.Builder builder = new AlertDialog.Builder(PlaceViewActivity.this);

        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete " + name + "?");

        builder.setPositiveButton("No", null);
        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                database.deleteLocation(markerID);
                database.close();

                Intent deleteIntent = new Intent(PlaceViewActivity.this, MapActivity.class);
                startActivity(deleteIntent);
                finish();

            }
        });

        deleteDialog = builder.create();
        deleteDialog.show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
        unregisterReceiver(getAddressReceiver);
    }

    @Override
    public void onBackPressed() {

        if(fromList){
            Intent backIntent = new Intent(PlaceViewActivity.this, PlacesActivity.class);
            startActivity(backIntent);
            finish();
        } else {
            Intent backIntent = new Intent(PlaceViewActivity.this, MapActivity.class);
            startActivity(backIntent);
            finish();
        }

        //super.onBackPressed();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_place_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        switch(id){

            case R.id.action_delete_edited_place:

                confirmDelete(nameTextView.getText().toString());

                return true;

            case R.id.action_share_edited_place:

                if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(PlaceViewActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(PlaceViewActivity.this,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                            READ_WRITE_EXTERNAL_STORAGE);

                } else {

                    if(!new File("/sdcard/BeenThere/").exists()){
                        new File("/sdcard/BeenThere/").mkdir();
                    }

                    final String fileName = "/sdcard/BeenThere/" + System.currentTimeMillis() + ".png";

                    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {

                            try{
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
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "I've been to " + addressTextView.getText().toString() + "!");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, "Share snapshot"));

                }

                return true;

            case R.id.action_edit_place:

                Intent editIntent = new Intent(PlaceViewActivity.this, SaveLocationActivity.class);
                editIntent.putExtra("EDIT", true);
                editIntent.putExtra("PLACE_ID", markerID);
                startActivity(editIntent);
                finish();

                return true;

            case android.R.id.home:

                if(fromList){
                    Intent backIntent = new Intent(PlaceViewActivity.this, PlacesActivity.class);
                    startActivity(backIntent);
                    finish();
                } else {
                    Intent backIntent = new Intent(PlaceViewActivity.this, MapActivity.class);
                    startActivity(backIntent);
                    finish();
                }

                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case READ_WRITE_EXTERNAL_STORAGE:

                if(grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if(!new File("/sdcard/BeenThere/").exists()){
                        new File("/sdcard/BeenThere/").mkdir();
                    }

                    final String fileName = "/sdcard/BeenThere/" + System.currentTimeMillis() + ".png";

                    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {

                            try{
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
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "I've been to " + addressTextView.getText().toString() + "!");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, "Share snapshot"));

                } else {

                    Snackbar.make(scrollView, "Oops! Can't save your screenshot without your permission!",
                            Snackbar.LENGTH_LONG).show();

                }

                break;

        }
    }

    public boolean isConnected(){

        ConnectivityManager connectMgr =
                (ConnectivityManager) PlaceViewActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());

    }

    public void moveGoogleCam(final LatLng location, final boolean animate){

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                googleMap.setPadding(0, 0, 0, scrollView.getHeight() + 10);
                Log.d("ScrollView height:", Integer.toString(scrollView.getHeight()));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                LatLng top = new LatLng(location.latitude + .01, location.longitude);
                LatLng bottom = new LatLng(location.latitude - .01, location.longitude);
                LatLng right = new LatLng(location.latitude, location.longitude + .01);
                LatLng left = new LatLng(location.latitude, location.longitude - .01);
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
                    CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, size.x, size.y, 100);
                    googleMap.moveCamera(update);

                }

                ViewTreeObserver obs = scrollView.getViewTreeObserver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }

            }
        });



    }

}

