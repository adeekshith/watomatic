package com.parishod.wareply.activity.customreplyeditor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.parishod.wareply.R;
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
        autoReplyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable editable) {
                // Disable save button if text does not satisfy requirements
                saveAutoReplyTextBtn.setEnabled(CustomRepliesData.isValidCustomReply(editable));
            }
        });

        saveAutoReplyTextBtn.setOnClickListener(view -> {
            String setString = customRepliesData.set(autoReplyText.getText());
            if (setString != null) {
                this.onNavigateUp();
            }
        });
    }
}