package com.parishod.watomatic.model.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.parishod.watomatic.R;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

//Ref: https://stackoverflow.com/questions/44383983/how-to-programmatically-enable-auto-start-and-floating-window-permissions
public class AutoStartHelper {

    /***
     * Xiaomi
     */
    private final String BRAND_XIAOMI = "xiaomi";
    private String PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter";
    private String PACKAGE_XIAOMI_COMPONENT = "com.miui.permcenter.autostart.AutoStartManagementActivity";

    /***
     * Letv
     */
    private final String BRAND_LETV = "letv";
    private String PACKAGE_LETV_MAIN = "com.letv.android.letvsafe";
    private String PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.AutobootManageActivity";

    /***
     * ASUS ROG
     */
    private final String BRAND_ASUS = "asus";
    private String PACKAGE_ASUS_MAIN = "com.asus.mobilemanager";
    private String PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.powersaver.PowerSaverSettings";

    /***
     * Honor
     */
    private final String BRAND_HONOR = "honor";
    private String PACKAGE_HONOR_MAIN = "com.huawei.systemmanager";
    private String PACKAGE_HONOR_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity";

    /**
     * Oppo
     */
    private final String BRAND_OPPO = "oppo";
    private String PACKAGE_OPPO_MAIN = "com.coloros.safecenter";
    private String PACKAGE_OPPO_FALLBACK = "com.oppo.safe";
    private String PACKAGE_OPPO_COMPONENT = "com.coloros.safecenter.permission.startup.StartupAppListActivity";
    private String PACKAGE_OPPO_COMPONENT_FALLBACK = "com.oppo.safe.permission.startup.StartupAppListActivity";
    private String PACKAGE_OPPO_COMPONENT_FALLBACK_A = "com.coloros.safecenter.startupapp.StartupAppListActivity";

    /**
     * Vivo
     */

    private final String BRAND_VIVO = "vivo";
    private String PACKAGE_VIVO_MAIN = "com.iqoo.secure";
    private String PACKAGE_VIVO_FALLBACK = "com.vivo.perm;issionmanager";
    private String PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity";
    private String PACKAGE_VIVO_COMPONENT_FALLBACK = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity";
    private String PACKAGE_VIVO_COMPONENT_FALLBACK_A = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager";

    /**
     * Nokia
     */

    private final String BRAND_NOKIA = "nokia";
    private String PACKAGE_NOKIA_MAIN = "com.evenwell.powersaving.g3";
    private String PACKAGE_NOKIA_COMPONENT = "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity";

    /**
     * Samsung
     */
    private final String BRAND_SAMSUNG = "samsung";
    private String PACKAGE_SAMSUNG_MAIN1 = "com.samsung.android.lool";
    private String PACKAGE_SAMSUNG_MAIN2 = "com.samsung.android.sm";
    private String PACKAGE_SAMSUNG_COMPONENT1 = "com.samsung.android.sm.ui.battery.BatteryActivity";
    private String PACKAGE_SAMSUNG_COMPONENT2 = "com.samsung.android.sm.battery.ui.BatteryActivity";

    private AutoStartHelper() {
    }

    public static AutoStartHelper getInstance() {
        return new AutoStartHelper();
    }


    public void getAutoStartPermission(Context context) {

        String build_info = Build.BRAND.toLowerCase();
        switch (build_info) {
            case BRAND_ASUS:
                autoStartAsus(context);
                break;
            case BRAND_XIAOMI:
                autoStartXiaomi(context);
                break;
            case BRAND_LETV:
                autoStartLetv(context);
                break;
            case BRAND_HONOR:
                autoStartHonor(context);
                break;
            case BRAND_OPPO:
                autoStartOppo(context);
                break;
            case BRAND_VIVO:
                autoStartVivo(context);
                break;
            case BRAND_NOKIA:
                autoStartNokia(context);
                break;
            case BRAND_SAMSUNG:
                autoStartSamsung(context);
                break;
        }

    }

    private void autoStartSamsung(Context context) {
        String packageName;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N)
            packageName = PACKAGE_SAMSUNG_MAIN1;
        else
            packageName = PACKAGE_SAMSUNG_MAIN2;

        if (isPackageExists(context, packageName)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, packageName, PACKAGE_SAMSUNG_COMPONENT1, PACKAGE_SAMSUNG_COMPONENT2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            });
        }
    }

    private void autoStartAsus(final Context context) {
        if (isPackageExists(context, PACKAGE_ASUS_MAIN)) {

            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            });

        }
    }

    private void showAlert(Context context, DialogInterface.OnClickListener onClickListener) {
        CustomDialog customDialog = new CustomDialog(context);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_TITLE, context.getString(R.string.auto_start_permission_dialog_title));
        bundle.putString(Constants.PERMISSION_DIALOG_MSG, context.getString(R.string.auto_start_permission_dialog_message));
        customDialog.showDialog(bundle, "AutoStart", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == -2){
                    //Decline
                }else{
                    //Accept
                    onClickListener.onClick(dialog, which);
                }
            }
        });
    }

    private void autoStartXiaomi(final Context context) {
        if (isPackageExists(context, PACKAGE_XIAOMI_MAIN)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        }
    }

    private void autoStartLetv(final Context context) {
        if (isPackageExists(context, PACKAGE_LETV_MAIN)) {
            showAlert(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        startIntent(context, PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


        }
    }


    private void autoStartHonor(final Context context) {
        if (isPackageExists(context, PACKAGE_HONOR_MAIN)) {
            showAlert(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        startIntent(context, PACKAGE_HONOR_MAIN, PACKAGE_HONOR_COMPONENT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


        }
    }

    private void autoStartOppo(final Context context) {
        if (isPackageExists(context, PACKAGE_OPPO_MAIN) || isPackageExists(context, PACKAGE_OPPO_FALLBACK)) {
            showAlert(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        startIntent(context, PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            startIntent(context, PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            try {
                                startIntent(context, PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT_FALLBACK_A);
                            } catch (Exception exx) {
                                exx.printStackTrace();
                            }

                        }

                    }
                }
            });


        }
    }

    private void autoStartVivo(final Context context) {
        if (isPackageExists(context, PACKAGE_VIVO_MAIN) || isPackageExists(context, PACKAGE_VIVO_FALLBACK)) {
            showAlert(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        startIntent(context, PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            startIntent(context, PACKAGE_VIVO_FALLBACK, PACKAGE_VIVO_COMPONENT_FALLBACK);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            try {
                                startIntent(context, PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT_FALLBACK_A);
                            } catch (Exception exx) {
                                exx.printStackTrace();
                            }

                        }

                    }

                }
            });
        }
    }

    private void autoStartNokia(final Context context) {
        if (isPackageExists(context, PACKAGE_NOKIA_MAIN)) {
            showAlert(context, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try {
                        startIntent(context, PACKAGE_NOKIA_MAIN, PACKAGE_NOKIA_COMPONENT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void startIntent(Context context, String packageName, String componentName, String componentName2) throws Exception {
        List<Intent> possibleIntents = new ArrayList<>();
        possibleIntents.add(new Intent().setComponent(new ComponentName(packageName, componentName)));
        possibleIntents.add(new Intent().setComponent(new ComponentName(packageName, componentName2)));
        possibleIntents.add(new Intent(Settings.ACTION_SETTINGS));
        for (Intent intent:possibleIntents
             ) {
            try {
                context.startActivity(intent);
                PreferencesManager.getPreferencesInstance(context).setAutoStartPermissionPref(true);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startIntent(Context context, String packageName, String componentName) throws Exception {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, componentName));
            context.startActivity(intent);
            PreferencesManager.getPreferencesInstance(context).setAutoStartPermissionPref(true);
        } catch (Exception var5) {
            var5.printStackTrace();
            throw var5;
        }
    }

    private Boolean isPackageExists(Context context, String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo :
                packages) {
            if (packageInfo.packageName.equals(targetPackage)) {
                return true;
            }
        }

        return false;
    }
}
