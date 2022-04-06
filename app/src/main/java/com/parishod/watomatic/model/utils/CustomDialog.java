package com.parishod.watomatic.model.utils;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.CATEGORY_BROWSABLE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT;
import static android.content.Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER;

public class CustomDialog {
    private final Context mContext;
    private List<AppCompatImageView> starViews;
    Dialog dialog;

    public CustomDialog(Context context) {
        this.mContext = context;
    }

    public void showDialog(Bundle bundle, String type, DialogInterface.OnClickListener onClickListener) {
        if (bundle != null) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder;
            if (type != null && type.equals("AutoStart")) {
                materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                        .setTitle(bundle.getString(Constants.PERMISSION_DIALOG_TITLE))
                        .setMessage(bundle.getString(Constants.PERMISSION_DIALOG_MSG));
                materialAlertDialogBuilder
                        .setNegativeButton(mContext.getResources().getString(R.string.decline_auto_start_setting), onClickListener)
                        .setPositiveButton(mContext.getResources().getString(R.string.enable_auto_start_setting), onClickListener);
            } else if (bundle.containsKey(Constants.PERMISSION_DIALOG_DENIED)
                    && bundle.getBoolean(Constants.PERMISSION_DIALOG_DENIED)) {
                materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                        .setTitle(bundle.getString(Constants.PERMISSION_DIALOG_DENIED_TITLE))
                        .setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_alert))
                        .setMessage(bundle.getString(Constants.PERMISSION_DIALOG_DENIED_MSG));
                materialAlertDialogBuilder
                        .setNegativeButton(mContext.getResources().getString(R.string.sure), onClickListener)
                        .setPositiveButton(mContext.getResources().getString(R.string.retry), onClickListener);
            } else if (bundle.containsKey(Constants.BETA_FEATURE_ALERT_DIALOG_TITLE)) {
                materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                        .setTitle(bundle.getString(Constants.BETA_FEATURE_ALERT_DIALOG_TITLE))
                        .setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_alert))
                        .setMessage(bundle.getString(Constants.BETA_FEATURE_ALERT_DIALOG_MSG));
                materialAlertDialogBuilder
                        .setNegativeButton(mContext.getResources().getString(R.string.decline_auto_start_setting), onClickListener)
                        .setPositiveButton(mContext.getResources().getString(R.string.enable_auto_start_setting), onClickListener);
            } else {
                materialAlertDialogBuilder = new MaterialAlertDialogBuilder(mContext)
                        .setTitle(bundle.getString(Constants.PERMISSION_DIALOG_TITLE))
                        .setMessage(bundle.getString(Constants.PERMISSION_DIALOG_MSG));
                materialAlertDialogBuilder
                        .setNegativeButton(mContext.getResources().getString(R.string.decline), onClickListener)
                        .setPositiveButton(mContext.getResources().getString(R.string.accept), onClickListener);
            }
            materialAlertDialogBuilder
                    .setCancelable(false)
                    .show();
        }
    }

    public void showAppLocalRatingDialog(View.OnClickListener onClickListener) {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (starViews != null) {
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

        View.OnClickListener starsClickListener = v -> {
            onClickListener.onClick(v);
            dialog.dismiss();
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

    public void showAppRatingDialog(int rating, View.OnClickListener onClickListener) {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (starViews != null) {
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

        if (rating > 3) {
            title.setText(String.format("%s%s",
                    mContext.getResources().getString(R.string.app_rating_goto_store_dialog_title),
                    mContext.getResources().getString(R.string.celebrate_emoji)));

            message.setText(String.format("%s\n%s",
                    mContext.getResources().getString(R.string.app_rating_pitch),
                    mContext.getResources().getString(R.string.app_rating_goto_store_dialog_msg)));

            button1.setText(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button1_title));
            button1.setTag(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button1_title));

            button2.setText(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button2_title));
            button2.setTag(mContext.getResources().getString(R.string.app_rating_goto_store_dialog_button2_title));
        } else {
            title.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_title));
            message.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_msg));

            button1.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_mail_button_title));
            button1.setTag(mContext.getResources().getString(R.string.app_rating_feedback_dialog_mail_button_title));

            button2.setText(mContext.getResources().getString(R.string.app_rating_feedback_dialog_telegram_button_title));
            button2.setTag(mContext.getResources().getString(R.string.app_rating_feedback_dialog_telegram_button_title));

            if (!isTelegramAppInstalled()) {
                button2.setVisibility(View.GONE);
            }
        }

        dialog.show();
    }

    private void updateRating(int rating) {
        //Reset the stars
        for (AppCompatImageView starView : starViews
        ) {
            starView.setImageResource(R.drawable.ic_star_border);
        }

        //set selected num of stars
        for (int i = 0; i < rating; i++) {
            if (starViews.get(i) != null) {
                starViews.get(i).setImageResource(R.drawable.ic_star_filled);
            }
        }
    }

    private boolean isTelegramAppInstalled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(Constants.TELEGRAM_URL));
            List<ResolveInfo> list = mContext.getPackageManager()
                    .queryIntentActivities(intent, 0);
            List<ResolveInfo> possibleBrowserIntents = mContext.getPackageManager()
                    .queryIntentActivities(new Intent(ACTION_VIEW, Uri.parse("http://www.deekshith.in/")), 0);
            Set<String> excludeIntents = new HashSet<>();
            for (ResolveInfo eachPossibleBrowserIntent : possibleBrowserIntents) {
                excludeIntents.add(eachPossibleBrowserIntent.activityInfo.name);
            }
            //Check for non browser application
            for (ResolveInfo resolveInfo : list) {
                if (!excludeIntents.contains(resolveInfo.activityInfo.name)) {
                    intent.setPackage(resolveInfo.activityInfo.packageName);
                    return true;
                }
            }
        } else {
            try {
                // In order for this intent to be invoked, the system must directly launch a non-browser app.
                // Ref: https://developer.android.com/training/package-visibility/use-cases#avoid-a-disambiguation-dialog
                Intent intent = new Intent(ACTION_VIEW, Uri.parse(Constants.TELEGRAM_URL))
                        .addCategory(CATEGORY_BROWSABLE)
                        .setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_REQUIRE_NON_BROWSER |
                                FLAG_ACTIVITY_REQUIRE_DEFAULT);
                if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                    return true;
                }
            } catch (ActivityNotFoundException e) {
                // This code executes in one of the following cases:
                // 1. Only browser apps can handle the intent.
                // 2. The user has set a browser app as the default app.
                // 3. The user hasn't set any app as the default for handling this URL.
                return false;
            }
        }
        return false;
    }
}
