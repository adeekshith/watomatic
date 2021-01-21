package com.parishod.wareply;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.parishod.wareply.model.CustomRepliesData;

public class MainActivity extends AppCompatActivity {
    CardView autoReplyTextPreviewCard;
    TextView autoReplyTextPreview;
    CustomRepliesData customRepliesData;
    String autoReplyTextPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customRepliesData = CustomRepliesData.getInstance(this);

        autoReplyTextPreviewCard = findViewById(R.id.mainAutoReplyTextCardView);
        autoReplyTextPreview = findViewById(R.id.textView4);

        autoReplyTextPlaceholder = getResources().getString(R.string.mainAutoReplyTextPlaceholder);

        autoReplyTextPreviewCard.setOnClickListener(this::openCustomReplyEditorActivity);
        autoReplyTextPreview.setText(customRepliesData.getOrElse(autoReplyTextPlaceholder));
    }

    private void openCustomReplyEditorActivity(View v) {
        Intent intent = new Intent(this, CustomReplyEditorActivity.class);
        startActivity(intent);
    }
}