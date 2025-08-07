package com.parishod.watomatic.model.utils;

import androidx.test.core.app.ApplicationProvider;


import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {

    

    

    

    

    private AppUtils appUtils;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        appUtils = AppUtils.getInstance(context);
    }

    @Test
    public void isPackageInstalled_shouldReturnTrue_whenPackageIsInstalled() {
        // Given
        String packageName = "com.example.app";
        ShadowPackageManager shadowPackageManager = shadowOf(ApplicationProvider.getApplicationContext().getPackageManager());
        shadowPackageManager.addPackage(packageName);
        
        

        // When
        boolean isInstalled = appUtils.isPackageInstalled(packageName);

        // Then
        assert(isInstalled);
    }

    @Test
    public void isPackageInstalled_shouldReturnFalse_whenPackageIsNotInstalled() {
        // Given
        String packageName = "com.example.app";
        ShadowPackageManager shadowPackageManager = shadowOf(ApplicationProvider.getApplicationContext().getPackageManager());
        shadowPackageManager.removePackage(packageName);

        // When
        boolean isInstalled = appUtils.isPackageInstalled(packageName);

        // Then
        assert(!isInstalled);
    }
}