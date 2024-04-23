package com.ibnux.smsgatewaymqtt.Utils;

import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import com.ibnux.smsgatewaymqtt.Aplikasi;
import com.ibnux.smsgatewaymqtt.data.UssdData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SimUtil {
    public static String SENT = "SMS_SENT";
    public static String DELIVERED = "SMS_DELIVERED";
    private final static String simSlotName[] = {
            "extra_asus_dial_use_dualsim",
            "com.android.phone.extra.slot",
            "slot",
            "simslot",
            "sim_slot",
            "subscription",
            "Subscription",
            "phone",
            "com.android.phone.DialingMode",
            "simSlot",
            "slot_id",
            "simId",
            "simnum",
            "phone_type",
            "slotId",
            "slotIdx"
    };

    public static List<UssdData> ussdDataList = new ArrayList<>();
    public static boolean isRun = false;
    public static UssdData current;
    public static long lastUssd = 0L;

    public static boolean sendSMS(Context ctx, int simID, String toNum, String centerNum, String smsText, int retry) {
        String name;

        try {
            if (simID == 0) {
                name = "isms";
                // for model : "Philips T939" name = "isms0"
            } else if (simID == 1) {
                name = "isms2";
            } else {
                Fungsi.writeLog("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                return false;
            }
            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            method.setAccessible(true);
            Object param = method.invoke(null, name);

            int time = (int) System.currentTimeMillis()/1000;

            Intent is = new Intent(SENT);
            is.putExtra("number",toNum);
            is.putExtra("centerNum",centerNum);
            is.putExtra("simID",simID);
            is.putExtra("smsText",smsText);
            is.putExtra("retry",retry);
            PendingIntent sentPI = PendingIntent.getBroadcast(ctx, time,
                    is, PendingIntent.FLAG_MUTABLE);
            Intent id = new Intent(DELIVERED);
            is.putExtra("number",toNum);
            is.putExtra("centerNum",centerNum);
            is.putExtra("simID",simID);
            is.putExtra("smsText",smsText);
            is.putExtra("retry",retry);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(ctx, time,
                    id, PendingIntent.FLAG_MUTABLE);

            method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object stubObj = method.invoke(null, param);
            try {
                if (stubObj != null) {
                    if (Build.VERSION.SDK_INT < 18) {
                        method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                        method.invoke(stubObj, toNum, centerNum, smsText, sentPI, deliveredPI);
                    } else {
                        method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                        method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsText, sentPI, deliveredPI);
                    }
                } else {
                    SubscriptionManager localSubscriptionManager = SubscriptionManager.from(ctx);
                    if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                        List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                        SubscriptionInfo simInfo = (SubscriptionInfo) localList.get(simID);
                        SmsManager
                                .getSmsManagerForSubscriptionId(simInfo.getSubscriptionId())
                                .sendTextMessage(toNum, null, smsText, sentPI, deliveredPI);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                SubscriptionManager localSubscriptionManager = SubscriptionManager.from(ctx);
                if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                    List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                    SubscriptionInfo simInfo = (SubscriptionInfo) localList.get(simID);
                    SmsManager
                            .getSmsManagerForSubscriptionId(simInfo.getSubscriptionId())
                            .sendTextMessage(toNum, null, smsText, sentPI, deliveredPI);
                }
            }

            Fungsi.writeLog("SUBMIT SMS SUCCESS: " + toNum + " SIM" + (simID+1));

            return true;
        } catch (ClassNotFoundException e) {
            Fungsi.writeLog("ClassNotFoundException:" + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Fungsi.writeLog("NoSuchMethodException:" + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Fungsi.writeLog("InvocationTargetException:" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Fungsi.writeLog("IllegalAccessException:" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Fungsi.writeLog("Exception:" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    public static boolean sendMultipartTextSMS(Context ctx, int simID, String toNum, String centerNum, ArrayList<String> smsTextlist) {
        String name;
        try {
            if (simID == 0) {
                name = "isms";
                // for model : "Philips T939" name = "isms0"
            } else if (simID == 1) {
                name = "isms2";
            } else {
                Fungsi.writeLog("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                return false;
            }
            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            method.setAccessible(true);
            Object param = method.invoke(null, name);

            ArrayList<PendingIntent> sentIntentList = new ArrayList<>();
            ArrayList<PendingIntent> deliveryIntentList = new ArrayList<>();
            String sms = "";
            for(int n=0;n<smsTextlist.size();n++) {
                int time = (int) System.currentTimeMillis()/1000;
                sms += smsTextlist.get(n);

                Intent is = new Intent(SENT);
                is.putExtra("number",toNum);
                is.putExtra("centerNum",centerNum);
                is.putExtra("simID",simID);
                is.putExtra("smsText",sms);
                is.putExtra("retry",0);
                sentIntentList.add(PendingIntent.getBroadcast(ctx, time+n,
                        is, PendingIntent.FLAG_MUTABLE));

                Intent id = new Intent(DELIVERED);
                is.putExtra("number",toNum);
                is.putExtra("centerNum",centerNum);
                is.putExtra("simID",simID);
                is.putExtra("smsText",sms);
                is.putExtra("retry",0);
                deliveryIntentList.add(PendingIntent.getBroadcast(ctx, time+n,
                        id, PendingIntent.FLAG_MUTABLE));
            }

            method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object stubObj = method.invoke(null, param);
            try {
                if (stubObj != null) {
                    if (Build.VERSION.SDK_INT < 18) {
                        method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, List.class, List.class, List.class);
                        method.invoke(stubObj, toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
                    } else {
                        method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, String.class, List.class, List.class, List.class);
                        method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
                    }
                } else {
                    SubscriptionManager localSubscriptionManager = SubscriptionManager.from(ctx);
                    if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                        List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                        SubscriptionInfo simInfo = (SubscriptionInfo) localList.get(simID);
                        for(int n=0;n<smsTextlist.size();n++) {
                            SmsManager
                                    .getSmsManagerForSubscriptionId(simInfo.getSubscriptionId())
                                    .sendTextMessage(toNum, null,
                                            smsTextlist.get(n),
                                            sentIntentList.get(n),
                                            deliveryIntentList.get(n));
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                SubscriptionManager localSubscriptionManager = SubscriptionManager.from(ctx);
                if (localSubscriptionManager.getActiveSubscriptionInfoCount() > 1) {
                    List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
                    SubscriptionInfo simInfo = (SubscriptionInfo) localList.get(simID);
                    for(int n=0;n<smsTextlist.size();n++) {
                        SmsManager
                                .getSmsManagerForSubscriptionId(simInfo.getSubscriptionId())
                                .sendTextMessage(toNum, null,
                                        smsTextlist.get(n),
                                        sentIntentList.get(n),
                                        deliveryIntentList.get(n));
                    }
                }
            }

            Fungsi.writeLog("SUBMIT SMS SUCCESS: " + toNum + " SIM" + (simID+1));
            return true;
        } catch (ClassNotFoundException e) {
            Fungsi.writeLog("ClassNotFoundException:" + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Fungsi.writeLog("NoSuchMethodException:" + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Fungsi.writeLog("InvocationTargetException:" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Fungsi.writeLog("IllegalAccessException:" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Fungsi.writeLog("Exception:" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static void queueUssd(String to, int simNumber){
        Fungsi.log("queueUssd "+to+" "+simNumber);
        UssdData data = new UssdData();
        data.to = to;
        data.sim = simNumber;
        ussdDataList.add(data);
        if(!isRun){
            runUssd();
        }
    }

    public static boolean isCheck = false;
    private static Runnable runnable = () -> {
        Fungsi.log("check is Timeout");
        isCheck = false;
        if(isRun){
            long sisa = (System.currentTimeMillis()-lastUssd)/1000L;

            Fungsi.log("check is Timeout sisa "+sisa);
            if(sisa>=60){
                runUssd();
            }else{
                checkTimeout();
            }
        }
    };
    public static void checkTimeout(){
        Fungsi.log("checkTimeout");
        if(isCheck)return;
        isCheck = true;
        Fungsi.log("checkTimeout 5 second");
        new Handler(Looper.getMainLooper()).postDelayed(runnable, 5000);
    }

    public static void runUssd(){
        Fungsi.log("runUssd");
        isRun = true;
        if(current!=null){
            Fungsi.log("current!=null");
            ussdDataList.remove(0);
            current = null;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runUssd();
                }
            }, 10000);
            return;
        }
        if(ussdDataList.size()>0) {
            lastUssd = System.currentTimeMillis();
            current = ussdDataList.get(0);
            sendUSSD(current.to, current.sim);
            checkTimeout();
        }else{
            Fungsi.log("runUssd Finished");
            isRun = false;
        }
    }

    public static void sendUSSD(String to, int simNumber) {
        Fungsi.log("sendUSSD");
        if (simNumber == 0) {
            Fungsi.log("send ussd " + Fungsi.ussdToCallableUri(to));
            Fungsi.writeLog("CALLING USSD: " + to);
            Intent i = new Intent("android.intent.action.CALL", Fungsi.ussdToCallableUri(to));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Aplikasi.app.startActivity(i);
        } else {
            Fungsi.log("USSD to " + to + " sim " + simNumber);
            Fungsi.writeLog("CALLING USSD: " + to + " SIM " + simNumber);
            Intent intent = new Intent(Intent.ACTION_CALL, Fungsi.ussdToCallableUri(to));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("com.android.phone.force.slot", true);
            intent.putExtra("Cdma_Supp", true);
            //Add all slots here, according to device.. (different device require different key so put all together)
            for (String s : simSlotName)
                intent.putExtra(s, simNumber - 1); //0 or 1 according to sim.......
            //works only for API >= 21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    TelecomManager telecomManager = (TelecomManager) Aplikasi.app.getSystemService(Context.TELECOM_SERVICE);
                    List<PhoneAccountHandle> phoneAccountHandleList = telecomManager.getCallCapablePhoneAccounts();
                    intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandleList.get(simNumber - 1));
                } catch (Exception e) {
                    e.printStackTrace();
                    //writeLog("No Sim card? at slot " + simNumber+"\n\n"+e.getMessage(), this);
                }
            }
            Aplikasi.app.startActivity(intent);
        }

    }



    public static void sendSMS(final String number, String message, final Context cx){

        if (!TextUtils.isEmpty(number) && !TextUtils.isEmpty(message))
        {
            int time = (int) System.currentTimeMillis()/1000;
            Intent is = new Intent(SENT);
            is.putExtra("number",number);
            PendingIntent sentPI = PendingIntent.getBroadcast(cx, time,
                    is, PendingIntent.FLAG_MUTABLE);
            Intent id = new Intent(DELIVERED);
            id.putExtra("number",number);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(cx, time,
                    id, PendingIntent.FLAG_MUTABLE);

            try
            {
                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> parts = smsManager.divideMessage(message);
                if (parts.size() > 1)
                {
                    try {
                        ArrayList<PendingIntent> spi = new ArrayList<>();
                        ArrayList<PendingIntent> dpi = new ArrayList<>();
                        for (int n = 0; n < parts.size(); n++) {
                            spi.add(sentPI);
                            dpi.add(deliveredPI);
                        }
                        smsManager.sendMultipartTextMessage(number, null, parts, spi, dpi);
                    }catch (Exception e){
                        smsManager.sendTextMessage(number, null, message, sentPI, deliveredPI);
                    }
                }
                else
                {
                    smsManager.sendTextMessage(number, null, message, sentPI, deliveredPI);
                }

                String result = number + ": " + message;
                Log.i(TAG, result);

                Fungsi.sendNotification(number, message);

                ContentValues values = new ContentValues();
                values.put("address", number);
                values.put("body", message);
                Aplikasi.app.getContentResolver()
                        .insert(Uri.parse("content://sms/sent"), values);
                Fungsi.writeLog("SUBMIT SMS SUCCESS: " + number);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                Fungsi.writeLog("SEND FAILED: " + number + " " + message+"\n\n"+ex.getMessage());
            }
        }
    }

}