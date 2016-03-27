package com.austinfay.beenthere;

import android.content.Context;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Austin on 3/25/2016.
 */
public class AboutOptionsList extends ListFragment {

    public ArrayList<OnOptionSelectedListener> listeners = new ArrayList<>();

    public interface OnOptionSelectedListener{

        public void onItemSelected(int index);

    }

    OnOptionSelectedListener mListener;

    public void addOnOptionSelectedListener(OnOptionSelectedListener listener){
        listeners.add(listener);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        for(OnOptionSelectedListener currentListener : listeners){
            currentListener.onItemSelected(position);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
                new String[]{"About Been There", "Been There Privacy Policy", "Legal Notices"}));
    }
}
