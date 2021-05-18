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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class CustomDialog {
    private final Context mContext;

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

    private List<AppCompatImageView> starViews;
    private int rating;
    public void showAppLocalRatingDialog(View.OnClickListener onClickListener){
        rating = -1;
        if(starViews != null) {
            starViews.clear();
        }
        starViews = new ArrayList<>();

        Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.app_rating_dialog);

        AppCompatButton rateButton = dialog.findViewById(R.id.rate_button);
        rateButton.setOnClickListener(v -> {
            if(rating == -1){
                Toast.makeText(mContext, "Please select the stars.", Toast.LENGTH_SHORT).show();
                return;
            }
            v.setTag(rating);
            onClickListener.onClick(v);
            dialog.dismiss();
        });

        AppCompatButton laterButton = dialog.findViewById(R.id.rate_later_button);
        laterButton.setOnClickListener(v -> {
            PreferencesManager.getPreferencesInstance(mContext).setPlayStoreRatingStatus("LATER");
            dialog.dismiss();
        });

        starViews.add(dialog.findViewById(R.id.star1));
        starViews.get(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rating = 1;
                updateRating(rating);
            }
        });

        starViews.add(dialog.findViewById(R.id.star2));
        starViews.get(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rating = 2;
                updateRating(rating);
            }
        });

        starViews.add(dialog.findViewById(R.id.star3));
        starViews.get(2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rating = 3;
                updateRating(rating);
            }
        });

        starViews.add(dialog.findViewById(R.id.star4));
        starViews.get(3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rating = 4;
                updateRating(rating);
            }
        });

        starViews.add(dialog.findViewById(R.id.star5));
        starViews.get(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rating = 5;
                updateRating(rating);
            }
        });

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

    public void showAppRatingDialog(View.OnClickListener onClickListener){
        Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.app_rating_feedback_dialog);

        AppCompatButton emailButton = dialog.findViewById(R.id.email_button);
        emailButton.setOnClickListener(v -> {
            onClickListener.onClick(v);
            dialog.dismiss();
        });

        AppCompatButton telegramButton = dialog.findViewById(R.id.telegram_button);
        telegramButton.setOnClickListener(v -> {
            onClickListener.onClick(v);
            dialog.dismiss();
        });
        if(!isTelegramAppInstalled()){
            telegramButton.setVisibility(View.GONE);
        }

        dialog.show();
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
