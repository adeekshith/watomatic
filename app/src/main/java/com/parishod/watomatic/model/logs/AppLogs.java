package com.parishod.watomatic.model.logs;

import android.content.Context;

import com.parishod.watomatic.R;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class AppLogs {
    private static AppLogs _instance;
    private final Context thisContext;
    private final String logsFileName;

    public AppLogs(Context context) {
        thisContext = context;
        logsFileName = thisContext.getString(R.string.app_logs_file_name);
    }

    public static AppLogs getInstance(Context context) {
        if (_instance == null) {
            _instance = new AppLogs(context);
        }
        return _instance;
    }

    public void writeToSDFile(String msg) {
        FileOutputStream fos = null;
        try {
            fos = thisContext.openFileOutput(logsFileName, Context.MODE_APPEND);
            fos.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void readFile() {
        try {
            FileInputStream in = thisContext.openFileInput(logsFileName);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            inputStreamReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
