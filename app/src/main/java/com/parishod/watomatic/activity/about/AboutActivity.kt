package com.parishod.watomatic.activity.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.parishod.watomatic.BuildConfig
import com.parishod.watomatic.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val privacyPolicyCard: CardView = findViewById(R.id.privacyPolicyCardView)
        val developerAttrLink: TextView = findViewById(R.id.developerLink)
        val appVersionText: TextView = findViewById(R.id.appVersion)

        appVersionText.text = String.format(resources.getString(R.string.app_version), BuildConfig.VERSION_NAME)
        privacyPolicyCard.setOnClickListener {
            val url = getString(R.string.url_privacy_policy);
            val i = Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            startActivity(i)
        }
        developerAttrLink.setOnClickListener {
            val url = getString(R.string.url_adeekshith_twitter)
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }
}