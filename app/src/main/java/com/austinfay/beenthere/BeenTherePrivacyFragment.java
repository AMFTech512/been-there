package com.austinfay.beenthere;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by titan on 3/26/2016.
 */
public class BeenTherePrivacyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View parent = inflater.inflate(R.layout.google_legal_layout, container, false);

        final TextView privacyText = (TextView) parent.findViewById(R.id.google_legal_text_google_fragment);

        AsyncTask task = new AsyncTask() {

            String returnedText = "";

            @Override
            protected String doInBackground(Object[] params) {

                String uri = "http://been-there-1019.appspot.com/privacy-plain/";

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

                    returnedText = stringBuilder.toString();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {

                privacyText.setText(returnedText);

            }
        };

        task.execute();

        return parent;
    }
}
