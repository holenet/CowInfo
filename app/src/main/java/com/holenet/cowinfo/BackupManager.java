package com.holenet.cowinfo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BackupManager {
    static boolean backupToDevice(Context context) {
        String backupDir = Environment.getExternalStorageDirectory()+ File.separator+
                "cowinfo"+File.separator+"database";
        String backupFilePath = backupDir+File.separator+new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime())+".db";
        Log.e("backupDir", backupDir);
        Log.e("backupFilePath", backupFilePath);
        File inFile = new File(context.getExternalFilesDir(null).toString() + File.separator + "cows.db");
        File outFile = new File(backupDir);
        if(!outFile.exists()) {
            if(!outFile.mkdirs()) {
                Log.e("outFile.mkdirs Error", "");
                return false;
            }
        }
        outFile = new File(backupFilePath);
        Log.e("file", "in and out prepared");
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            int data;
            while(true) {
                data = in.read();
                if(data==-1)
                    break;
                out.write(data);
            }
            in.close();
            out.close();
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static boolean backupToServer(Context context) {
        String url = NetworkManager.MAIN_DOMAIN+"cowinfo/upload/";
        File file = new File(context.getExternalFilesDir(null).toString() + File.separator + "cows.db");
        String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime())+".db";
        return NetworkManager.upload(url, file, fileName)==HttpURLConnection.HTTP_OK;
    }

    static boolean restoreFromDevice(Context context, String filePath) {
        return false;
    }

    static boolean restoreFromServer(Context context, String filePath) {
        return false;
    }
}
