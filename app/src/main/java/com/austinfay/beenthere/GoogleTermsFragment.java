package com.austinfay.beenthere;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by titan on 3/26/2016.
 */
public class GoogleTermsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View parent = inflater.inflate(R.layout.google_legal_layout, container, false);

        TextView googleLegalTextView = (TextView) parent.findViewById(R.id.google_legal_text_google_fragment);

        googleLegalTextView.setText(GoogleApiAvailability.getInstance()
                .getOpenSourceSoftwareLicenseInfo(getActivity()));

        return parent;
    }
}
