package com.austinfay.beenthere;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class AboutActivity extends AppCompatActivity implements AboutOptionsList.OnOptionSelectedListener {

    boolean itemSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        itemSelected = false;

        inflateList();

    }

    @Override
    public void onBackPressed() {

        if(itemSelected) {
            inflateList();
            itemSelected = false;
        } else
            super.onBackPressed();

    }

    private void inflateList(){

        AboutOptionsList optionsFragment = new AboutOptionsList();

        optionsFragment.addOnOptionSelectedListener(this);

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(R.id.about_fragment_activity_about, optionsFragment);

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        ft.commit();

    }

    @Override
    public void onItemSelected(int index) {

        itemSelected = true;

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        switch (index){

            case 0:

                Fragment aboutFragment = new AboutFragment();

                ft.replace(R.id.about_fragment_activity_about, aboutFragment);

                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

                ft.commit();

                break;

            case 1:

                Fragment btpFragment = new BeenTherePrivacyFragment();

                ft.replace(R.id.about_fragment_activity_about, btpFragment);

                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

                ft.commit();

                break;

            case 2:

                GoogleTermsFragment gtFragment = new GoogleTermsFragment();

                ft.replace(R.id.about_fragment_activity_about, gtFragment);

                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

                ft.commit();

                break;

        }

    }
}