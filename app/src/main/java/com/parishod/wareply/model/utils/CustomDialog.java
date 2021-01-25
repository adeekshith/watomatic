package com.parishod.wareply.model.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.parishod.wareply.R;

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
}
