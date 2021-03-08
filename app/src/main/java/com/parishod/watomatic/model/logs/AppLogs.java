package com.parishod.watomatic.model.logs;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AppLogs {
    private static AppLogs _instance;

    public static AppLogs getInstance(){
        if(_instance == null){
            _instance = new AppLogs();
        }
        return _instance;
    }

    public void writeToSDFile(String msg){
        // Find the root of the external storage.
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(folder, "WatomaticLogs.txt");
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file, true);
            fos.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fos!= null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
