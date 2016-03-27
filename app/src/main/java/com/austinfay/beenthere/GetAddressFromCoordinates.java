package com.austinfay.beenthere;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Austin on 7/15/2015.
 * Been there
 */
public class GetAddressFromCoordinates extends IntentService {

    public static final String GET_ADDRESS_FILTER_NORMAL = "com.austinfay.beenthere.GetAddressFromCoordinates.NORMAL";
    public static final String GET_ADDRESS_FILTER_FOR_EXIT = "com.austinfay.beenthere.GetAddressFromCoordinates.FOR_EXIT";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GetAddressFromCoordinates(String name) {
        super(name);
    }

    public GetAddressFromCoordinates(){
        super(GetAddressFromCoordinates.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Intent broadcastIntent = new Intent(intent.getStringExtra("FILTER"));

            String uri = "http://maps.google.com/maps/api/geocode/json?latlng=" +
                    Double.toString(intent.getDoubleExtra("LAT", 0)) + "," + Double.toString(intent.getDoubleExtra("LNG", 0)) +
                    "&sensor=false";

            Log.d("URI", uri);

            HttpGet httpGet = new HttpGet(uri);

            HttpClient client = new DefaultHttpClient();

            HttpResponse response;

            StringBuilder stringBuilder = new StringBuilder();

            try {

                response = client.execute(httpGet);

                HttpEntity entity = response.getEntity();

                InputStream inStream = entity.getContent();

                InputStreamReader reader = new InputStreamReader(inStream, "UTF-8");

                int byteData;

                while ((byteData = reader.read()) != -1) {

                    stringBuilder.append((char) byteData);

                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            JSONObject jsonObject;

            try {

                jsonObject = new JSONObject(stringBuilder.toString());

                String types = jsonObject.getJSONArray("results").getJSONObject(0).getJSONArray("types").getString(0);
                Log.d("Type", types);
                String address = null;

                String [] useAddress = {"street_address", "locality", "administrative_area_level_1", "political", "country"};

                for(String type : useAddress){

                    if(types.equals(type)){
                        address = ((JSONArray) jsonObject.get("results")).getJSONObject(0).getString("formatted_address");
                    }

                }

                if(address == null){

                        address = jsonObject.getJSONArray("results").getJSONObject(0).getJSONArray("address_components")
                                .getJSONObject(0).getString("long_name");
                }

                Log.d("Address from Google", address);

                broadcastIntent.putExtra("ADDRESS", address);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        sendBroadcast(broadcastIntent);

    }
}
