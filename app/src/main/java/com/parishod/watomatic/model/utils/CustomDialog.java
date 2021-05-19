package com.parishod.watomatic.model.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class CustomDialog {
    private final Context mContext;
    private List<AppCompatImageView> starViews;
    Dialog dialog;

    public CustomDialog(Context context){
        this.mContext = context;
    }

    public void showDialog(Bundle bundle, DialogInterface.OnClickListener onClickListener){
        if(bundle != null){
            MaterialAlertDialogBuilder materialAlertDialogBuilder;
            if(bundle.containsKey(Constants.PERMISSION_DIALOG_DENIED)
                    && bundle.getBoolean(Constants.PERMISSION_DIALOG_DENIED)) {
                 materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                    .setTitle(bundle.getString(Constants.PERMISSION_DIALOG_DENIED_TITLE))
                    .setIcon(mContext.getResources().getDrawable(R.drawable.ic_alert))
                    .setMessage(bundle.getString(Constants.PERMISSION_DIALOG_DENIED_MSG));
                materialAlertDialogBuilder
                        .setNegativeButton(mContext.getResources().getString(R.string.sure), onClickListener::onClick)
                        .setPositiveButton(mContext.getResources().getString(R.string.retry), onClickListener::onClick);
            }else{
                materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                    .setTitle(bundle.getString(Constants.PERMISSION_DIALOG_TITLE))
                    .setMessage(bundle.getString(Constants.PERMISSION_DIALOG_MSG));
                materialAlertDialogBuilder
                        .setNegativeButton(mContext.getResources().getString(R.string.decline), onClickListener::onClick)
                        .setPositiveButton(mContext.getResources().getString(R.string.accept), onClickListener::onClick);
            }
            materialAlertDialogBuilder
                .setCancelable(false)
                .show();
        }
    }

    public void showAppLocalRatingDialog(View.OnClickListener onClickListener){
        if(dialog != null){
            dialog.dismiss();
        }
        if(starViews != null) {
            starViews.clear();
        }
        starViews = new ArrayList<>();

        dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.app_rating_dialog);

        AppCompatButton laterButton = dialog.findViewById(R.id.rate_later_button);
        laterButton.setOnClickListener(v -> {
            PreferencesManager.getPreferencesInstance(mContext).setPlayStoreRatingStatus("LATER");
            dialog.dismiss();
        });

        View.OnClickListener starsClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onClick(v);
                dialog.dismiss();
            }
        };

        starViews.add(dialog.findViewById(R.id.star1));
        starViews.get(0).setTag(1);
        starViews.get(0).setOnClickListener(starsClickListener);

        starViews.add(dialog.findViewById(R.id.star2));
        starViews.get(1).setTag(2);
        starViews.get(1).setOnClickListener(starsClickListener);

        starViews.add(dialog.findViewById(R.id.star3));
        starViews.get(2).setTag(3);
        starViews.get(2).setOnClickListener(starsClickListener);

        starViews.add(dialog.findViewById(R.id.star4));
        starViews.get(3).setTag(4);
        starViews.get(3).setOnClickListener(starsClickListener);

        starViews.add(dialog.findViewById(R.id.star5));
        starViews.get(4).setTag(5);
        starViews.get(4).setOnClickListener(starsClickListener);

        dialog.show();
    }

    public void showAppRatingDialog(int rating, View.OnClickListener onClickListener){
        if(dialog != null){
            dialog.dismiss();
        }
        if(starViews != null) {
            starViews.clear();
        }
        starViews = new ArrayList<>();

        dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.app_rating_feedback_dialog);

        AppCompatTextView title = dialog.findViewById(R.id.title);
        AppCompatTextView message = dialog.findViewById(R.id.message);

        starViews.add(dialog.findViewById(R.id.star1));

        starViews.add(dialog.findViewById(R.id.star2));

        starViews.add(dialog.findViewById(R.id.star3));

        starViews.add(dialog.findViewById(R.id.star4));

        starViews.add(dialog.findViewById(R.id.star5));
        updateRating(rating);

        AppCompatButton button1 = dialog.findViewById(R.id.button1);
        button1.setOnClickListener(v -> {
            onClickListener.onClick(v);
            dialog.dismiss();
        });

        AppCompatButton button2 = dialog.findViewById(R.id.button2);

        button2.setOnClickListener(v -> {
            onClickListener.onClick(v);
            dialog.dismiss();
        });

        if(rating > 3){
            title.setText(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_title));
            message.setText(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_msg));

            button1.setText(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button1_title));
            button1.setTag(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button1_title));

            button2.setText(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button2_title));
            button2.setTag(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button2_title));
        }else{
            title.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_title));
            message.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_msg));

            button1.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_mail_button_title));
            button1.setTag(mContext.getResources().getString(R.string.app_rating_feedback_dialog_mail_button_title));

            button2.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_telegram_button_title));
            button2.setTag(mContext.getResources().getString(R.string.app_rating_feedback_dialog_telegram_button_title));

            if(!isTelegramAppInstalled()){
                button2.setVisibility(View.GONE);
            }
        }

        dialog.show();
    }

    private void updateRating(int rating){
        //Reset the stars
        for (AppCompatImageView starView: starViews
        ) {
            starView.setImageResource(R.drawable.ic_star_border);
        }

        //set selected num of stars
        for(int i = 0; i < rating; i++){
            if(starViews.get(i) != null){
                starViews.get(i).setImageResource(R.drawable.ic_star_filled);
            }
        }
    }

    private boolean isTelegramAppInstalled(){
        try {
            PackageManager packageManager = mContext.getPackageManager();
            packageManager.getPackageInfo("org.telegram.messenger", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
