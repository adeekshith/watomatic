package com.parishod.watomatic.model.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class ContextWrapper extends android.content.ContextWrapper {

    public ContextWrapper(Context base) {
        super(base);
    }

    //REF: https://medium.com/swlh/android-app-specific-language-change-programmatically-using-kotlin-d650a5392220
    public static ContextWrapper wrap(Context context, String lang){
        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();

        Locale newLocale = new Locale("en");
        if(lang.equalsIgnoreCase("pt")){
            newLocale = new Locale("pt", "Br");
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            configuration.setLocale(newLocale);

            LocaleList localeList = new LocaleList(newLocale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);

            context = context.createConfigurationContext(configuration);
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }else {
            configuration.locale = newLocale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }
        return new ContextWrapper(context);
    }
}
