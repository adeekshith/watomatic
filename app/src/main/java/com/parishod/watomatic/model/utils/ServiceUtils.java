package com.parishod.watomatic.model.utils;

import android.content.Context;

public class ServiceUtils {
    private final Context appContext;
    private static ServiceUtils _INSTANCE;

    private ServiceUtils(Context context) {
        this.appContext = context;
    }

    public static ServiceUtils getInstance(Context context) {
        if (_INSTANCE == null) {
            _INSTANCE = new ServiceUtils(context);
        }
        return _INSTANCE;
    }
}
