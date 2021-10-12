package com.parishod.watomatic.model.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.parishod.watomatic.R;

import java.util.List;
import java.util.Locale;

//Ref: https://stackoverflow.com/q/44383983
public class AutoStartHelper {

    /***
     * Xiaomi
     */
    private final String BRAND_XIAOMI = "xiaomi";
    private final String BRAND_XIAOMI_POCO = "poco";
    private final String BRAND_XIAOMI_REDMI = "redmi";
    private final String PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter";
    private final String PACKAGE_XIAOMI_COMPONENT = "com.miui.permcenter.autostart.AutoStartManagementActivity";

    /***
     * Letv
     */
    private final String BRAND_LETV = "letv";
    private final String PACKAGE_LETV_MAIN = "com.letv.android.letvsafe";
    private final String PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.AutobootManageActivity";

    /***
     * ASUS ROG
     */
    private final String BRAND_ASUS = "asus";
    private final String PACKAGE_ASUS_MAIN = "com.asus.mobilemanager";
    private final String PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.powersaver.PowerSaverSettings";
    private final String PACKAGE_ASUS_COMPONENT_FALLBACK = "com.asus.mobilemanager.autostart.AutoStartActivity";

    /***
     * Honor
     */
    private final String BRAND_HONOR = "honor";
    private final String PACKAGE_HONOR_MAIN = "com.huawei.systemmanager";
    private final String PACKAGE_HONOR_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity";

    /***
     * Huawei
     */
    private final String BRAND_HUAWEI = "huawei";
    private final String PACKAGE_HUAWEI_MAIN = "com.huawei.systemmanager";
    private final String PACKAGE_HUAWEI_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity";
    private final String PACKAGE_HUAWEI_COMPONENT_FALLBACK = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity";

    /**
     * Oppo
     */
    private final String BRAND_OPPO = "oppo";
    private final String PACKAGE_OPPO_MAIN = "com.coloros.safecenter";
    private final String PACKAGE_OPPO_FALLBACK = "com.oppo.safe";
    private final String PACKAGE_OPPO_COMPONENT = "com.coloros.safecenter.permission.startup.StartupAppListActivity";
    private final String PACKAGE_OPPO_COMPONENT_FALLBACK = "com.oppo.safe.permission.startup.StartupAppListActivity";
    private final String PACKAGE_OPPO_COMPONENT_FALLBACK_A = "com.coloros.safecenter.startupapp.StartupAppListActivity";

    /**
     * Vivo
     */

    private final String BRAND_VIVO = "vivo";
    private final String PACKAGE_VIVO_MAIN = "com.iqoo.secure";
    private final String PACKAGE_VIVO_FALLBACK = "com.vivo.perm;issionmanager";
    private final String PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity";
    private final String PACKAGE_VIVO_COMPONENT_FALLBACK = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity";
    private final String PACKAGE_VIVO_COMPONENT_FALLBACK_A = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager";

    /**
     * Nokia
     */

    private final String BRAND_NOKIA = "nokia";
    private final String PACKAGE_NOKIA_MAIN = "com.evenwell.powersaving.g3";
    private final String PACKAGE_NOKIA_COMPONENT = "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity";

    /**
     * Samsung
     */
    private final String BRAND_SAMSUNG = "samsung";
    private final String PACKAGE_SAMSUNG_MAIN1 = "com.samsung.android.lool";
    private final String PACKAGE_SAMSUNG_MAIN2 = "com.samsung.android.sm";
    private final String PACKAGE_SAMSUNG_COMPONENT1 = "com.samsung.android.sm.ui.battery.BatteryActivity";
    private final String PACKAGE_SAMSUNG_COMPONENT2 = "com.samsung.android.sm.battery.ui.BatteryActivity";

    /***
     * One plus
     */
    private final String BRAND_ONE_PLUS = "oneplus";
    private final String PACKAGE_ONE_PLUS_MAIN = "com.oneplus.security";
    private final String PACKAGE_ONE_PLUS_COMPONENT = "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity";

    private AutoStartHelper() {
    }

    public static AutoStartHelper getInstance() {
        return new AutoStartHelper();
    }


    public void getAutoStartPermission(Context context) {

        String build_info = Build.BRAND.toLowerCase(Locale.getDefault());
        switch (build_info) {
            case BRAND_ASUS:
                autoStartAsus(context);
                break;
            case BRAND_XIAOMI:
            case BRAND_XIAOMI_POCO:
            case BRAND_XIAOMI_REDMI:
                autoStartXiaomi(context);
                break;
            case BRAND_LETV:
                autoStartLetv(context);
                break;
            case BRAND_HONOR:
                autoStartHonor(context);
                break;
            case BRAND_HUAWEI:
                autoStartHuawei(context);
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
            case BRAND_ONE_PLUS:
                autoStartOnePlus(context);
                break;
            default:
                Toast.makeText(context, context.getString(R.string.setting_not_available_for_device),
                        Toast.LENGTH_SHORT).show();
        }

    }

    private void autoStartSamsung(final Context context) {
        String packageName;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N)
            packageName = PACKAGE_SAMSUNG_MAIN1;
        else
            packageName = PACKAGE_SAMSUNG_MAIN2;

        if (isPackageExists(context, packageName)) {

            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, packageName, PACKAGE_SAMSUNG_COMPONENT1);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        startIntent(context, packageName, PACKAGE_SAMSUNG_COMPONENT2);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
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
                    try {
                        startIntent(context, PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT_FALLBACK);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                dialog.dismiss();
            });

        }
    }

    private void showAlert(Context context, DialogInterface.OnClickListener onClickListener) {
        CustomDialog customDialog = new CustomDialog(context);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PERMISSION_DIALOG_TITLE, context.getString(R.string.auto_start_permission_dialog_title));
        bundle.putString(Constants.PERMISSION_DIALOG_MSG,
                context.getString(R.string.auto_start_permission_dialog_message) +
                        "\n\n" +
                        context.getString(R.string.device_based_settings_message));
        customDialog.showDialog(bundle, "AutoStart", (dialog, which) -> {
            if (which != -2) {
                //Decline
                onClickListener.onClick(dialog, which);
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
            showAlert(context, (dialog, which) -> {

                try {
                    startIntent(context, PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        }
    }


    private void autoStartHonor(final Context context) {
        if (isPackageExists(context, PACKAGE_HONOR_MAIN)) {
            showAlert(context, (dialog, which) -> {

                try {
                    startIntent(context, PACKAGE_HONOR_MAIN, PACKAGE_HONOR_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        }
    }

    private void autoStartHuawei(final Context context) {
        if (isPackageExists(context, PACKAGE_HUAWEI_MAIN)) {

            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        startIntent(context, PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT_FALLBACK);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                dialog.dismiss();
            });

        }
    }

    private void autoStartOppo(final Context context) {
        if (isPackageExists(context, PACKAGE_OPPO_MAIN) || isPackageExists(context, PACKAGE_OPPO_FALLBACK)) {
            showAlert(context, (dialog, which) -> {

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
            });


        }
    }

    private void autoStartVivo(final Context context) {
        if (isPackageExists(context, PACKAGE_VIVO_MAIN) || isPackageExists(context, PACKAGE_VIVO_FALLBACK)) {
            showAlert(context, (dialog, which) -> {

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

            });
        }
    }

    private void autoStartNokia(final Context context) {
        if (isPackageExists(context, PACKAGE_NOKIA_MAIN)) {
            showAlert(context, (dialog, which) -> {

                try {
                    startIntent(context, PACKAGE_NOKIA_MAIN, PACKAGE_NOKIA_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void autoStartOnePlus(final Context context) {
        if (isPackageExists(context, PACKAGE_ONE_PLUS_MAIN)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_ONE_PLUS_MAIN, PACKAGE_ONE_PLUS_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        }
    }

    private void startIntent(Context context, String packageName, String componentName) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, componentName));
            context.startActivity(intent);
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
