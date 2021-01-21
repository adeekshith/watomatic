package com.parishod.wareply;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.parishod.wareply.model.CustomRepliesData;

public class CustomReplyEditorActivity extends AppCompatActivity {
    TextInputEditText autoReplyText;
    Button saveAutoReplyTextBtn;
    CustomRepliesData customRepliesData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_reply_editor);

        customRepliesData = CustomRepliesData.getInstance(this);

        autoReplyText = findViewById(R.id.autoReplyTextInputEditText);
        saveAutoReplyTextBtn = findViewById(R.id.saveCustomReplyBtn);

        autoReplyText.setText(customRepliesData.get());
        autoReplyText.requestFocus();

        saveAutoReplyTextBtn.setOnClickListener(view -> {
            customRepliesData.set(autoReplyText.getText().toString());
            this.onNavigateUp();
        });
    }
}