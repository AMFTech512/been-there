package com.austinfay.beenthere;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Inflater;


public class PlacesActivity extends ActionBarActivity {

    private final static String action = "com.austinfay.beenthere.PlacesActivity.ITEMS_LOADED";

    ListView placesListView;
    ProgressBar loadingImagesProgressBar;
    TableRow noItemTableRow;

    HashMap<String, String> itemIDs;

    int scrollIndex, scrollTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        IntentFilter filter = new IntentFilter(action);
        registerReceiver(onItemsLoaded, filter);

        loadingImagesProgressBar = (ProgressBar) findViewById(R.id.places_activity_progress_bar);
        loadingImagesProgressBar.setVisibility(View.VISIBLE);

        placesListView = (ListView) findViewById(R.id.places_activity_list_view);
        placesListView.setVisibility(View.GONE);

        noItemTableRow = (TableRow) findViewById(R.id.places_activity_no_item);

        itemIDs = new HashMap<>();

        new loadItems().execute();

        if(savedInstanceState != null){
            scrollIndex = savedInstanceState.getInt("SCROLL_INDEX");
            scrollTop = savedInstanceState.getInt("SCROLL_TOP");
        } else {
            scrollIndex = 0;
            scrollTop = 0;
        }

        placesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id1) {

                Log.d("Item clicked", Integer.toString(position));

                int id = Integer.parseInt(itemIDs.get(Integer.toString(position)));

                Intent viewIntent = new Intent(PlacesActivity.this, PlaceViewActivity.class);
                viewIntent.putExtra("markerID", id);
                viewIntent.putExtra("listView", true);
                startActivity(viewIntent);
                finish();

            }
        });

    }

    BroadcastReceiver onItemsLoaded = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            loadingImagesProgressBar.setVisibility(View.GONE);
            placesListView.setVisibility(View.VISIBLE);
            placesListView.setSelectionFromTop(scrollIndex, scrollTop);

        }
    };

    private class loadItems extends AsyncTask<String, String, CustomAdapter> {

        @Override
        protected void onPostExecute(CustomAdapter adapter) {
            super.onPostExecute(adapter);

            if(adapter == null){
                noItemTableRow.setVisibility(View.VISIBLE);
            } else {
                noItemTableRow.setVisibility(View.GONE);
            }

            placesListView.setAdapter(adapter);

            Intent broadcastIntent = new Intent(action);
            PlacesActivity.this.sendBroadcast(broadcastIntent);

        }

        @Override
        protected CustomAdapter doInBackground(String... params) {

            LocationDatabase database = new LocationDatabase(PlacesActivity.this);
            ArrayList<HashMap<String, String>> placesList = database.getAllLocations();

            CustomAdapter adapter = null;

            String from[] = {"name", "address", "bitmap"};
            int to[] = {
                    R.id.places_activity_name_text_view,
                    R.id.places_activity_address_text_view,
                    R.id.places_activity_marker_image_view
            };

            List<HashMap<String, Object>> fillPlaces = new ArrayList<>();

            if(placesList.size() > 0){

                for(int i = 0; i < placesList.size(); i++){

                    HashMap<String, String> currentPlace = placesList.get(i);
                    HashMap<String, Object> currentListItem = new HashMap<>();

                    String name = currentPlace.get("locationName");

                    String address = currentPlace.get("locationAddress");

                    itemIDs.put(Integer.toString(i), currentPlace.get("locationId"));

                    currentListItem.put("name", name);
                    currentListItem.put("address", address);

                    File markerFile = new File(getFilesDir(), (currentPlace.get("locationId") + ".png"));

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(markerFile.getAbsolutePath(), options);

                    currentListItem.put("bitmap", bitmap);
                    fillPlaces.add(currentListItem);

                }

                adapter = new CustomAdapter(PlacesActivity.this, R.layout.place_table_row, fillPlaces);

            }
                return adapter;

        }


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int index = placesListView.getFirstVisiblePosition();
        View v = placesListView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - placesListView.getPaddingTop());

        outState.putInt("SCROLL_INDEX", index);
        outState.putInt("SCROLL_TOP", top);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(onItemsLoaded);
    }

    @Override
    public void onBackPressed() {

        Intent backIntent = new Intent(PlacesActivity.this, MapActivity.class);
        startActivity(backIntent);
        finish();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_places, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == android.R.id.home){

            Intent back = new Intent(PlacesActivity.this, MapActivity.class);
            startActivity(back);
            finish();

        }

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    public class CustomAdapter extends ArrayAdapter<HashMap<String, Object>> {

        List<HashMap<String, Object>> items;

        public CustomAdapter(Context context, int resource, List<HashMap<String, Object>> objects) {
            super(context, resource, objects);
            items = objects;
        }


        @Override
        public View getView(int position, View convertView1, ViewGroup parent) {

            HashMap<String, Object> currentItem = items.get(position);

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View convertView = inflater.inflate(R.layout.place_table_row, null);

            TextView nameET = (TextView) convertView.findViewById(R.id.places_activity_name_text_view);
            TextView addressET = (TextView) convertView.findViewById(R.id.places_activity_address_text_view);

            ImageView mapIcon = (ImageView) convertView.findViewById(R.id.places_activity_marker_image_view);

            nameET.setText((String) currentItem.get("name"));
            addressET.setText((String) currentItem.get("address"));

            mapIcon.setImageBitmap((Bitmap) currentItem.get("bitmap"));

            return convertView;

        }
    }
}
