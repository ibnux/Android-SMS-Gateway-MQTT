package com.ibnux.smsgatewaymqtt.layanan;

/**
 * Created by Ibnu Maksum 2020
 */


import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ibnux.smsgatewaymqtt.Aplikasi;
import com.ibnux.smsgatewaymqtt.MainActivity;
import com.ibnux.smsgatewaymqtt.R;
import com.ibnux.smsgatewaymqtt.Utils.Fungsi;
import com.ibnux.smsgatewaymqtt.Utils.SimUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class BackgroundService extends Service {
    public static MqttAndroidClient mqttAndroidClient;

    public static final String ACTION_STOP = "com.ibnux.smsgatewaymqtt.action.STOP";
    WifiManager.WifiLock wifiLock=null;
    NotificationManager mNotificationManager = null;
    public static PendingIntent contentIntent;
    BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String msg = null;
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    msg = "success";
                    break;
                case Activity.RESULT_CANCELED:
                    msg = "failed";
                    break;
            }
            if (msg != null) {
                Fungsi.writeLog("DELIVERED: " + msg + " : " + arg1.getStringExtra("number"));
                SmsListener.sendPOST(
                        getSharedPreferences("pref", 0).getString("urlPost", null),
                        arg1.getStringExtra("number"),
                        msg,
                        "delivered",
                        String.valueOf(System.currentTimeMillis())
                );
            }
        }
    };

    BroadcastReceiver sentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String msg = null;
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    msg = "success";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    msg = "Generic failure";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    msg = "No service";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    msg = "Null PDU";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    msg = "Radio off";
                    break;
            }

            // RETRY AFTER 10 SECOND IF FAILED UNTIL 3 TIMES
            if(msg!=null && !msg.equals("success")){
                int retry = arg1.getIntExtra("retry",0);
                if(retry<3){
                    Fungsi.writeLog("SENT FAILED: " + msg);
                    Fungsi.writeLog("RETRY SEND SMS in 10s #" + (retry+1));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String number = arg1.getStringExtra("number");
                            int simID = arg1.getIntExtra("simID",0);
                            String centerNum = arg1.getStringExtra("centerNum");
                            String smsText = arg1.getStringExtra("smsText");
                            int retry = arg1.getIntExtra("retry",0);
                            retry++;
                            SimUtil.sendSMS(Aplikasi.app, simID,number,centerNum,smsText,retry);
                        }
                    }, 10000);

                    return;
                }
            }

            if (msg != null) {
                Calendar cal = Calendar.getInstance();
                Fungsi.writeLog("SENT: " + msg + " : " + arg1.getStringExtra("number"));
                SmsListener.sendPOST(getSharedPreferences("pref", 0).getString("urlPost", null),
                        arg1.getStringExtra("number"), msg, "sent", String.valueOf(System.currentTimeMillis()));
            }
        }
    };

    public BackgroundService() {

    }

    @Override
    public void onCreate() {
        Fungsi.log("BackgroundService onCreate");
        registerReceiver(sentReceiver, new IntentFilter(SimUtil.SENT));
        registerReceiver(deliveredReceiver, new IntentFilter(SimUtil.DELIVERED));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,new IntentFilter("BackgroundService"));
    }

    @Override
    public void onDestroy() {
        Fungsi.log("BackgroundService onDestroy");
        unregisterReceiver(sentReceiver);
        unregisterReceiver(deliveredReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Fungsi.log("BackgroundService onStartCommand");
        mNotificationManager = getNotificationServices();
        if (intent!=null && intent.getAction()!=null && intent.getAction().equals(ACTION_STOP)) {
            lockWifi(false);
            mNotificationManager.cancelAll();
            mNotificationManager = null;
            try {
                mqttAndroidClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            stopForeground(true);
            stopSelf();
        }else {
            Fungsi.writeLog("Background Services Started");


            if (Build.VERSION.SDK_INT > 25) {
                NotificationChannel androidChannel = new NotificationChannel("MQTT Listener",
                        "Background", NotificationManager.IMPORTANCE_LOW);
                androidChannel.enableLights(false);
                androidChannel.enableVibration(false);
                androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                mNotificationManager.createNotificationChannel(androidChannel);
            }

            contentIntent = PendingIntent.getActivity(Aplikasi.app, 0, new Intent(Aplikasi.app, MainActivity.class), PendingIntent.FLAG_MUTABLE);

            setNotification(Aplikasi.app.getText(R.string.app_name).toString(), Aplikasi.app.getSharedPreferences("pref", 0).getString("mqtt_server", "tcp://broker.hivemq.com:1883"), "Connecting");
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("BackgroundService"));
            mqttService();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public NotificationManager getNotificationServices(){
        if(mNotificationManager==null)
            return (NotificationManager) Aplikasi.app.getSystemService(Context.NOTIFICATION_SERVICE);
        return mNotificationManager;
    }

    public void setNotification(String title, String content, String subtext ){
        mNotificationManager = getNotificationServices();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(Aplikasi.app, "MQTT Listener")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setSubText(subtext)
                .setOngoing(true)
                .setContentText(content)
                .setAutoCancel(false);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(34, mBuilder.build());
    }

    public void setSubtext(String subtext){

        setNotification(Aplikasi.app.getText(R.string.app_name).toString(), Aplikasi.app.getSharedPreferences("pref", 0).getString("mqtt_server", "tcp://broker.hivemq.com:1883"), subtext);
    }

    public void mqttService() {
        SharedPreferences sp = Aplikasi.app.getSharedPreferences("pref", 0);
        if (sp.getString("mqtt_server", null) == null) {
            Fungsi.writeLog("MQTT Server not found");
            return;
        }
        if (mqttAndroidClient == null){
                mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), sp.getString("mqtt_server", null), UUID.randomUUID().toString());
        }
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectionLost(Throwable cause) {
                if(cause!=null){
                    cause.printStackTrace();
                    Fungsi.writeLog("ERROR: NODATA : push received without data\n" + cause.getMessage());
                }
                Fungsi.writeLog("MQTT Disconnected");
                if(mNotificationManager!=null)
                    setSubtext("Connection Lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                try {
                    String msg = new String(message.getPayload());
                    if (!msg.isEmpty()) {

                        JSONObject json = new JSONObject(msg);
                        String to = json.getString("to");
                        String sim = "0";
                        if (json.has("sim")) {
                            sim = json.getString("sim");
                        }
                        String text = json.getString("message");
                        SharedPreferences sp = getSharedPreferences("pref", 0);

                        Fungsi.log("To " + to + "\n" +
                                "SIM " + sim + "\n" +
                                "Message " + text);

                        if (!TextUtils.isEmpty(to) && !TextUtils.isEmpty(text)) {
                            Fungsi.writeLog("MQTT Received: " + topic + "\n" + msg);
                            if (sp.getBoolean("gateway_on", true)) {
                                sim = sim.replaceAll( "[^\\d]", "" );
                                if(sim.isEmpty()){
                                    sim = "0";
                                }
                                String finalSim = sim;
                                // low bandwidth will have duplicate message
                                String md5 = Fungsi.md5(msg);
                                File fileMd5 = new File(Aplikasi.app.getCacheDir(), md5+".txt");
                                // after 15 seconds, can resend, sometimes mqtt get double message
                                if(!fileMd5.exists() || System.currentTimeMillis() - fileMd5.lastModified() > 15000) {
                                    fileMd5.setLastModified(System.currentTimeMillis());
                                    Fungsi.writeToFile(String.valueOf(System.currentTimeMillis()), fileMd5);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendSMSorUSSD(to, text, Integer.parseInt(finalSim));
                                        }
                                    }).start();
                                    Fungsi.deleteCache();
                                }else{
                                    Fungsi.log("Duplicate");
                                }
                            } else {
                                Fungsi.writeLog("GATEWAY OFF: " + to + " " + message);
                            }
                        } else {
                            Fungsi.writeLog("ERROR: TO MESSAGE AND SECRET REQUIRED : " + to + " " + message);
                        }
                        mqttAndroidClient.publish(topic, "".getBytes(), 0, true);
                    } else {
                        Fungsi.log("MQTT Received empty: " + topic);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Fungsi.writeLog("MQTT Message Exception: " + e.getMessage());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    Fungsi.log("deliveryComplete" + token.getMessage().getPayload().toString());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Fungsi.writeLog("MQTT Reconnect");
                    setSubtext("Reconnect");
                    subscribeToTopic();
                } else {
                    setSubtext("Connected");
                    Fungsi.writeLog("MQTT Connected");
                }
            }
        });
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        if(!sp.getString("mqtt_user","").isEmpty())
            mqttConnectOptions.setUserName(sp.getString("mqtt_user",""));
        if(!sp.getString("mqtt_pass","").isEmpty())
            mqttConnectOptions.setPassword(sp.getString("mqtt_pass","").toCharArray());
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, Aplikasi.app, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(true);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();

                    setSubtext("Connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                    setSubtext("Connection Failure");
                    Fungsi.writeLog("Failed to connect to " + Aplikasi.app.getSharedPreferences("pref",0).getString("mqtt_server", "") + "\n" + exception.getMessage());
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
            Fungsi.writeLog("MQTT connect Exception: " + ex.getMessage());
        }
    }

    public void subscribeToTopic() {
        String deviceID = Aplikasi.app.getSharedPreferences("pref",0).getString("deviceID","");
        try {
            mqttAndroidClient.subscribe(deviceID, 0, Aplikasi.app, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    lockWifi(true);
                    Fungsi.writeLog("MQTT Subscribed: " + deviceID);
                    setSubtext("Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                    setSubtext("Subscribe Failure");
                    Fungsi.writeLog("MQTT Subscribe Failed: " + deviceID + "\n" + exception.getMessage());
                }
            });
        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Fungsi.log("BackgroundService onBind");
        return null;
    }

    public void lockWifi(boolean lock){
        try {
            if (wifiLock == null)
                wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "Gateway lock Wifi");
            if (lock) {
                wifiLock.acquire();
            } else {
                wifiLock.release();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void sendSMSorUSSD(String to, String message, int simNumber) {
        if (simNumber > 2) simNumber = 2;
        if (to.startsWith("*")) {
            if (to.trim().endsWith("#")) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Fungsi.log("CALL_PHONE not granted");
                    return;
                }
                Fungsi.log("USSD to " + to + " sim " + simNumber);
                Fungsi.writeLog("QUEUE USSD: " + to + " SIM " + simNumber);

                SimUtil.queueUssd(to,(simNumber == 0) ? 1 : simNumber);
            } else {
                Fungsi.log("not end with #");
                Fungsi.writeLog("USSD not end with # : " + to);

            }
        } else {
            Fungsi.log("send SMS " + to);
            Fungsi.writeLog("SEND SMS: " + to + " SIM " + simNumber + "\n" + message);

            if (simNumber > 0) {
                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> messageList = smsManager.divideMessage(message);
                if (messageList.size() > 1) {
                    SimUtil.sendMultipartTextSMS(this, simNumber - 1, to, null, messageList);
                } else {
                    SimUtil.sendSMS(this, simNumber - 1, to, null, message, 0);
                }
            } else {
                SimUtil.sendSMS(to, message, this);
            }
        }
    }






    public static void tellMainActivity(){
        Intent i = new Intent("MainActivity");
        i.putExtra("newMessage", "newMessage");
        LocalBroadcastManager.getInstance(Aplikasi.app).sendBroadcast(i);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Fungsi.log("BackgroundService BroadcastReceiver received");
            if(intent.hasExtra("kill") && intent.getBooleanExtra("kill",false)){
                Fungsi.log("BackgroundService KILL");
                try {
                    mqttAndroidClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                ((NotificationManager) Aplikasi.app.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
                intent = new Intent("MainActivity");
                intent.putExtra("kill",true);
                LocalBroadcastManager.getInstance(BackgroundService.this).sendBroadcast(intent);
                lockWifi(false);
                stopForeground(true);
                stopSelf();
            }else {
                LocalBroadcastManager.getInstance(BackgroundService.this).sendBroadcast(new Intent("MainActivity"));
            }
        }
    };
}
