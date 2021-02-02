package com.parishod.watomatic.activity.about;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.parishod.watomatic.R;
import com.parishod.watomatic.BuildConfig;

public class AboutActivity extends AppCompatActivity {
    CardView privacyPolicyCard;
    TextView privacyPolicyLink;
    TextView developerAttrLink, appVersionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        privacyPolicyCard = findViewById(R.id.privacyPolicyCardView);
        privacyPolicyLink = findViewById(R.id.privacyPolicyLink);
        developerAttrLink = findViewById(R.id.developerLink);
        appVersionText = findViewById(R.id.appVersion);
        appVersionText.setText(String.format(getResources().getString(R.string.app_version), BuildConfig.VERSION_NAME));

        privacyPolicyCard.setOnClickListener(view -> {
            String url = privacyPolicyLink.getText().toString();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        developerAttrLink.setOnClickListener(view -> {
            String url = getString(R.string.url_adeekshith_twitter);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
    }
}