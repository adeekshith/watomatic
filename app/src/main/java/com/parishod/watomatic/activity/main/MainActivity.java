package com.parishod.watomatic.activity.main;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.parishod.watomatic.R;
import com.parishod.watomatic.fragment.BrandingFragment;
import com.parishod.watomatic.fragment.MainFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadMainFragment();
        loadBrandingFragment();
    }

    private void loadMainFragment(){
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.main_frame_layout, new MainFragment())
                .commit();
    }

    private void loadBrandingFragment(){
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.branding_frame_layout, new BrandingFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /*
    To Show backbutton on actionbar in settings fragment
     */
    public void showHideBackButton(boolean show){
        if(show){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }else{
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    /*
    Handle back button on settings fragment
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}