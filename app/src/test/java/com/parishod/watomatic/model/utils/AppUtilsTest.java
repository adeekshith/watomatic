package com.parishod.watomatic.model.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {

    @Mock
    private Context mockContext;

    @Mock
    private PackageManager mockPackageManager;

    @Mock
    private Drawable mockIcon;

    private AppUtils appUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        AppUtils.resetInstance(); // Reset the singleton instance
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        appUtils = AppUtils.getInstance(mockContext);
    }

    @Test
    public void isPackageInstalled_shouldReturnTrue_whenPackageIsInstalled() throws PackageManager.NameNotFoundException {
        // Given
        String packageName = "com.example.app";
        when(mockPackageManager.getApplicationIcon(packageName)).thenReturn(mockIcon);

        // When
        boolean isInstalled = appUtils.isPackageInstalled(packageName);

        // Then
        assert(isInstalled);
    }

    @Test
    public void isPackageInstalled_shouldReturnFalse_whenPackageIsNotInstalled() throws PackageManager.NameNotFoundException {
        // Given
        String packageName = "com.example.app";
        when(mockPackageManager.getApplicationIcon(packageName)).thenThrow(new PackageManager.NameNotFoundException());

        // When
        boolean isInstalled = appUtils.isPackageInstalled(packageName);

        // Then
        assert(!isInstalled);
    }
}