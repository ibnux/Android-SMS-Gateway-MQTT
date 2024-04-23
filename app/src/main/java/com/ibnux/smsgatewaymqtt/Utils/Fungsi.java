package com.ibnux.smsgatewaymqtt.Utils;

/**
 * Created by Ibnu Maksum 2023
 */


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ibnux.smsgatewaymqtt.Aplikasi;
import com.ibnux.smsgatewaymqtt.MainActivity;
import com.ibnux.smsgatewaymqtt.R;
import com.ibnux.smsgatewaymqtt.layanan.BackgroundService;
import com.ibnux.smsgatewaymqtt.layanan.UssdService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Fungsi {
    private static NotificationManager mNotificationManager;

    public static void writeToFile(String data, File file) {
        try {
            FileOutputStream outputStreamWriter = new FileOutputStream(file);
            outputStreamWriter.write(data.getBytes());
            outputStreamWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFromFile(File file, Context context) {

        String data = "";
        try (FileInputStream stream = new FileInputStream(file)) {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            data = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }



    public static void sendNotification(String to, String msg) {
        if(mNotificationManager==null)
            mNotificationManager = (NotificationManager) Aplikasi.app.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>25) {

            NotificationChannel androidChannel = new NotificationChannel("com.ibnux.smsgatewaymqtt",
                    "SMS Notifikasi", NotificationManager.IMPORTANCE_LOW);
            androidChannel.enableLights(false);
            androidChannel.enableVibration(false);
            androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            mNotificationManager.createNotificationChannel(androidChannel);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(Aplikasi.app, 0, new Intent(Aplikasi.app, MainActivity.class), PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(Aplikasi.app,"com.ibnux.smsgatewaymqtt")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(Aplikasi.app.getText(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .setContentText("sent to "+to).setAutoCancel(true);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(1, mBuilder.build());
    }

    public static void log(String txt){
        Log.d("SMSin","-------------------------------");
        Log.d("SMSin",txt+"");
        Log.d("SMSin","-------------------------------");
    }

    public static void log(String tag, String txt){
        Log.d(tag,"-------------------------------");
        Log.d(tag,txt+"");
        Log.d(tag,"-------------------------------");
    }

    static public void writeLog(String message){
        File file = new File(Aplikasi.app.getFilesDir(), "log.txt");
        String lastlog = "";
        if(file.exists()) {
            lastlog = readFromFile(file, Aplikasi.app);
        }
        String[] lines = lastlog.split("\r\n|\r|\n");
        if(lines.length>110) {
            String newLog = "";
            for (int i = 0; i < 100; i++) {
                newLog += lines[i] + "\r\n";
            }
            lastlog = newLog;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        String newLog = cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH) + " " +
                cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)+"\n"+message;
        writeToFile(newLog+"\n"+lastlog,file);
        BackgroundService.tellMainActivity();
    }

    public static void deleteCache(){
        File dir = Aplikasi.app.getCacheDir();
        if (dir!= null && dir.isDirectory())
        {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++){
                if(files[i].isFile() && System.currentTimeMillis() - files[i].lastModified() > 15000){
                    files[i].delete();
                }
            }
        }
    }

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + UssdService.class.getCanonicalName();
        log("USSD",service);
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v("USSD", "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("USSD", "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v("USSD", "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v("USSD", "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v("USSD", "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v("USSD", "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }

    public static Uri ussdToCallableUri(String ussd) {

        String uriString = "";

        if(!ussd.startsWith("tel:"))
            uriString += "tel:";

        for(char c : ussd.toCharArray()) {

            if(c == '#')
                uriString += Uri.encode("#");
            else
                uriString += c;
        }

        return Uri.parse(uriString);
    }

    public static List<SimInfo> getSIMInfo(Context context) {
        List<SimInfo> simInfoList = new ArrayList<>();
        Uri URI_TELEPHONY = Uri.parse("content://telephony/siminfo/");
        Cursor c = context.getContentResolver().query(URI_TELEPHONY, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(c.getColumnIndex("_id"));
                int slot = c.getInt(c.getColumnIndex("slot"));
                String display_name = c.getString(c.getColumnIndex("display_name"));
                String icc_id = c.getString(c.getColumnIndex("icc_id"));
                SimInfo simInfo = new SimInfo(id, display_name, icc_id, slot);
                Log.d("apipas_sim_info", simInfo.toString());
                simInfoList.add(simInfo);
            } while (c.moveToNext());
        }
        c.close();

        return simInfoList;
    }


}
