package com.ibnux.smsgatewaymqtt.layanan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.ibnux.smsgatewaymqtt.Utils.Fungsi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class SmsListener extends BroadcastReceiver {

    SharedPreferences sp;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (sp == null) sp = context.getSharedPreferences("pref", 0);
        String url = sp.getString("urlPost", null);
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                String messageFrom = smsMessage.getOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();
                String messageTimestamp = String.valueOf(smsMessage.getTimestampMillis());
                Log.i("SMS From", messageFrom);
                Log.i("SMS Body", messageBody);
                Fungsi.writeLog("SMS: RECEIVED : " + messageFrom + " " + messageBody);
                if (url != null) {
                    if (sp.getBoolean("gateway_on", true)) {
                        sendPOST(url, messageFrom, messageBody, "received", messageTimestamp);
                    } else {
                        Fungsi.writeLog("GATEWAY OFF: SMS NOT POSTED TO SERVER");
                    }

                } else {
                    Log.i("SMS URL", "URL not SET");
                }
            }
        }
    }

    static class postDataTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... datas) {
            URL url;
            String response = "";
            try {
                try {
                    url = new URL(datas[0]);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(datas[1]);

                    writer.flush();
                    writer.close();
                    os.close();
                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null) {
                            response += line;
                        }
                    } else {
                        response = "";

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return "SMS: POST : " + datas[0] + " : " + response;
            } catch (Exception e) {
                e.printStackTrace();
                return "SMS: POST FAILED : " + datas[0] + " : " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String response) {
            Fungsi.writeLog(response);
        }
    }


    public static void sendPOST(String urlPost, String from, String msg, String tipe, String msgTimestamp) {
        if (urlPost == null) return;
        if (from.isEmpty()) return;
        if (!urlPost.startsWith("http")) return;
        try {
            new postDataTask().execute(urlPost,
                    "number=" + URLEncoder.encode(from, "UTF-8") +
                            "&message=" + URLEncoder.encode(msg, "UTF-8") +
                            "&type=" + URLEncoder.encode(tipe, "UTF-8") +
                            "&timestamp=" + URLEncoder.encode(msgTimestamp, "UTF-8")
            );
        } catch (Exception e) {
            e.printStackTrace();
            Fungsi.writeLog("SMS: POST FAILED : " + urlPost + " : " + e.getMessage());
        }
    }

}
