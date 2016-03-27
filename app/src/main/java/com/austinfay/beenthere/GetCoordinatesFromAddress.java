package com.austinfay.beenthere;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

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
public class GetCoordinatesFromAddress extends IntentService {

    public static final String GET_COORDINATES_FILTER_NORMAL = "com.austinfay.beenthere.GetCoordinatesFromAddress.NORMAL";
    public static final String GET_COORDINATES_FILTER_FOR_EXIT = "com.austinfay.beenthere.GetCoordinatesFromAddress.FOR_EXIT";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GetCoordinatesFromAddress(String name) {
        super(name);
    }

    public GetCoordinatesFromAddress(){
        super(GetCoordinatesFromAddress.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String args = intent.getStringExtra("ADDRESS").replaceAll(" ", "%20");

        String uri = "http://maps.google.com/maps/api/geocode/json?address=" + args + "&sensor=false";

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
        }

        JSONObject jsonObject;
        LatLng currentLatLng;
        String address = null;

        Intent broadcastIntent = new Intent(intent.getStringExtra("FILTER"));

        try {

            jsonObject = new JSONObject(stringBuilder.toString());

            if(jsonObject.getString("status").equals("ZERO_RESULTS")){

                broadcastIntent.putExtra("VALID", false);


            } else {

                currentLatLng = new LatLng(((JSONArray) jsonObject.get("results"))
                        .getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lat"),
                        ((JSONArray) jsonObject.get("results"))
                                .getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng"));

                String types = jsonObject.getJSONArray("results").getJSONObject(0).getJSONArray("types").getString(0);

                String[] useAddress = {"street_address", "locality", "administrative_area_level_1", "political", "country"};

                for (String type : useAddress) {

                    if (types.equals(type)) {
                        address = ((JSONArray) jsonObject.get("results")).getJSONObject(0).getString("formatted_address");
                    }

                }

                if (address == null) {

                    address = jsonObject.getJSONArray("results").getJSONObject(0).getJSONArray("address_components")
                            .getJSONObject(0).getString("long_name");
                }

                broadcastIntent.putExtra("ADDRESS", address);
                broadcastIntent.putExtra("LAT", currentLatLng.latitude);
                broadcastIntent.putExtra("LNG", currentLatLng.longitude);
                broadcastIntent.putExtra("VALID", true);

                Log.d("Address from Google", address);
                Log.d("LatLng from Google", Double.toString(currentLatLng.latitude) + "," + Double.toString(currentLatLng.longitude));

            }

            broadcastIntent.putExtra("NETERROR", false);

        } catch (JSONException e) {
            broadcastIntent.putExtra("VALID", true);
            broadcastIntent.putExtra("NETERROR", true);
            e.printStackTrace();
        }

        sendBroadcast(broadcastIntent);

    }
}
