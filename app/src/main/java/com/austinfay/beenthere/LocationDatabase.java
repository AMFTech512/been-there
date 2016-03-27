package com.austinfay.beenthere;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Austin on 7/13/2015.
 * Been there
 */
public class LocationDatabase extends SQLiteOpenHelper {


    public LocationDatabase(Context context) {

        super(context, "locations.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        Log.d("Database", "Creating database");

        String query =
                "CREATE TABLE locations(locationId INTEGER PRIMARY KEY, locationName TEXT, locationAddress TEXT, locationLat DOUBLE, locationLng DOUBLE, markerColor INTEGER)";

        database.execSQL(query);

    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

        String query = "DROP TABLE IF EXISTS locations";

        database.execSQL(query);
        onCreate(database);

    }

    @Override
    public synchronized void close() {
        super.close();
    }

    public void insertLocation(String name, String address, double lat, double lng, int color){


        Log.d("Inserting data", "Inserting data into database");

        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("locationName", name);
        values.put("locationAddress", address);
        values.put("locationLat", lat);
        values.put("locationLng", lng);
        values.put("markerColor", color);

        database.insert("locations", null, values);

        database.close();

    }

    public HashMap<String, String> getLocation(int id){

        HashMap<String, String> currentLocationInfo = new HashMap<>();

        SQLiteDatabase database = this.getWritableDatabase();

        String query = "SELECT * FROM locations where locationId=" + Integer.toString(id);

        Cursor cursor = database.rawQuery(query, null);

        if(cursor.moveToFirst()){

            do{

                currentLocationInfo.put("locationId", Integer.toString(cursor.getInt(0)));
                currentLocationInfo.put("locationName", cursor.getString(1));
                currentLocationInfo.put("locationAddress", cursor.getString(2));
                currentLocationInfo.put("locationLat", Double.toString(cursor.getDouble(3)));
                currentLocationInfo.put("locationLng", Double.toString(cursor.getDouble(4)));
                currentLocationInfo.put("markerColor", Integer.toString(cursor.getInt(5)));

            } while(cursor.moveToNext());

        }

        return currentLocationInfo;

    }

    public void deleteLocation(int id){

        SQLiteDatabase database = this.getWritableDatabase();

        String query = "DELETE FROM locations where locationId=" + Integer.toString(id);

        database.execSQL(query);

        database.close();

    }

    public int updateLocation(int id, String name, String address, double lat, double lng, int color){

        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("locationName", name);
        values.put("locationAddress", address);
        values.put("locationLat", lat);
        values.put("locationLng", lng);
        values.put("markerColor", color);

        return database.update("locations", values, "locationId" + " = ?", new String[] {Integer.toString(id)});

    }

    public ArrayList<HashMap<String, String>> getAllLocations(){

        ArrayList<HashMap<String, String>> locationList = new ArrayList<>();

        String query = "SELECT * FROM locations";

        SQLiteDatabase database = this.getWritableDatabase();

        Cursor cursor = database.rawQuery(query, null);

        if(cursor.moveToFirst()){

            do {

                HashMap<String, String> locationMap = new HashMap<>();

                locationMap.put("locationId", Integer.toString(cursor.getInt(0)));
                locationMap.put("locationName", cursor.getString(1));
                locationMap.put("locationAddress", cursor.getString(2));
                locationMap.put("locationLat", Double.toString(cursor.getDouble(3)));
                locationMap.put("locationLng", Double.toString(cursor.getDouble(4)));
                locationMap.put("markerColor", Integer.toString(cursor.getInt(5)));

                locationList.add(locationMap);

            } while(cursor.moveToNext());

        }

        return locationList;
    }

    public int getNextAvailableID(){

        ArrayList<HashMap<String, String>> locationList = new ArrayList<>();

        String query = "SELECT * FROM locations";

        SQLiteDatabase database = this.getWritableDatabase();

        Cursor cursor = database.rawQuery(query, null);

        if(cursor.moveToFirst()){

            do {

                HashMap<String, String> locationMap = new HashMap<>();

                locationMap.put("locationId", Integer.toString(cursor.getInt(0)));

                locationList.add(locationMap);

            } while(cursor.moveToNext());

        }

        if(locationList.size() != 0) {

            HashMap<String, String> lastLocationMap = locationList.get(locationList.size() - 1);

            return Integer.parseInt(lastLocationMap.get("locationId")) + 1;

        } else {
            return 1;
        }

    }
}
