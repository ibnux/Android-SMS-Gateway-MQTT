package com.ibnux.smsgatewaymqtt;

/**
 * Created by Ibnu Maksum 2020
 */

import static com.ibnux.smsgatewaymqtt.layanan.BackgroundService.ACTION_STOP;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ibnux.smsgatewaymqtt.Utils.Fungsi;
import com.ibnux.smsgatewaymqtt.Utils.SimUtil;
import com.ibnux.smsgatewaymqtt.layanan.BackgroundService;
import com.ibnux.smsgatewaymqtt.layanan.UssdService;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private boolean serviceActive = false;
    TextView info, txtLog;
    String infoTxt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = findViewById(R.id.text);
        txtLog = findViewById(R.id.txtLog);
        info.setText("Click Me to Show Device ID");
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.setText(infoTxt);
            }
        });
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.RECEIVE_BOOT_COMPLETED,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Manifest.permission.CALL_PHONE
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Fungsi.log("All Permission granted");
                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            Fungsi.log("Some Permission not granted");
                        }
                        Dexter.withContext(MainActivity.this)
                                .withPermissions(
                                        Manifest.permission.SEND_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                ).withListener(new MultiplePermissionsListener() {
                                    @Override
                                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                                        if (report.areAllPermissionsGranted()) {
                                            Fungsi.log("All SMS Permission granted");
                                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                                            Fungsi.log("Some Permission not granted");
                                        }
                                    }

                                    @Override
                                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
                                }).check();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
                }).check();
        updateInfo();

        if (getSharedPreferences("pref", 0).getBoolean("gateway_on", true))
            checkServices();


        startService(new Intent(this, UssdService.class));
        txtLog.setText(Fungsi.readFromFile(new File(Aplikasi.app.getFilesDir(), "log.txt"), Aplikasi.app));

    }

    public void updateInfo() {
        SharedPreferences sp = getSharedPreferences("pref", 0);
        String deviceID = sp.getString("deviceID", null);
        String mqtt_server = sp.getString("mqtt_server", null);
        if (deviceID == null) {
            deviceID = String.valueOf(UUID.randomUUID());
            sp.edit().putString("deviceID", deviceID).apply();
        }
        if (mqtt_server == null) {
            mqtt_server = "tcp://broker.hivemq.com:1883";
            sp.edit().putString("mqtt_server", mqtt_server).apply();
        }
        infoTxt = "Your Device ID \n" + deviceID + "\n\n"+"MQTT Server \n" + mqtt_server + "\n";
    }

    public void checkServices() {
        Fungsi.log("checkServices");
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("BackgroundService"));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Fungsi.log("checkServices " + serviceActive);
                if (!serviceActive) {
                    startService(new Intent(MainActivity.this, BackgroundService.class));
                }
            }
        }, 3000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_gateway_switch);
        View view = MenuItemCompat.getActionView(menuItem);
        Switch switcha = view.findViewById(R.id.switchForActionBar);
        switcha.setChecked(getSharedPreferences("pref", 0).getBoolean("gateway_on", true));
        switcha.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences("pref", 0).edit().putBoolean("gateway_on", isChecked).apply();
                if (!isChecked) {
                    Intent i = new Intent(getApplicationContext(), BackgroundService.class);
                    i.setAction(ACTION_STOP);
                    startService(i);
                    Toast.makeText(MainActivity.this, "Gateway OFF", Toast.LENGTH_LONG).show();
                } else {
                    checkServices();
                    Toast.makeText(MainActivity.this, "Gateway ON", Toast.LENGTH_LONG).show();
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_change_device_id) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setTitle("Change DeviceID");
            builder1.setMessage("Device ID used for Topic ID");
            final EditText input1 = new EditText(this);
            input1.setText(getSharedPreferences("pref", 0).getString("deviceID", ""));
            input1.setHint("UUID or Phone Number");
            input1.setMaxLines(1);
            input1.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT);
            builder1.setView(input1);
            builder1.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String deviceID = input1.getText().toString().trim();
                    getSharedPreferences("pref", 0).edit().putString("deviceID", deviceID).commit();
                    Toast.makeText(MainActivity.this, "deviceID changed", Toast.LENGTH_LONG).show();
                }
            });
            builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder1.show();
            return true;
        } else if (id == R.id.menu_set_mqtt_server) {
            setMqttServer();
            return true;
        } else if (id == R.id.menu_set_url) {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setTitle("Change URL for receiving SMS");
            builder2.setMessage("Data will send using POST with parameter number and message and type=received/sent/delivered/ussd");
            final EditText input2 = new EditText(this);
            input2.setText(getSharedPreferences("pref", 0).getString("urlPost", ""));
            input2.setHint("https://sms.domain.tld/callback.php");
            input2.setMaxLines(1);
            input2.setInputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT);
            builder2.setView(input2);
            builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String urlPost = input2.getText().toString();
                    getSharedPreferences("pref", 0).edit().putString("urlPost", urlPost).commit();
                    Toast.makeText(MainActivity.this, "SERVER URL changed", Toast.LENGTH_LONG).show();
                }
            });
            builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder2.show();
            return true;
        } else if (id == R.id.menu_php_script) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ibnux/Android-SMS-Gateway-MQTT/tree/main/php-gateway")));
            return true;
        } else if (id == R.id.menu_clear_logs) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Logs")
                    .setMessage("Are you sure?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            File file = new File(Aplikasi.app.getFilesDir(), "log.txt");
                            Fungsi.writeToFile("", file);
                            txtLog.setText(Fungsi.readFromFile(file, Aplikasi.app));
                            Fungsi.deleteCache();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        } else if (id == R.id.menu_ussd_set) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_ussd_test) {
            callUssd();
            return true;
        } else if (id == R.id.menu_battery_optimization) {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:com.ibnux.smsgatewaymqtt")));
            return true;
        }
        return false;
    }

    public void setMqttServer(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MQTT Server");
        builder.setMessage("Your Mqtt Server");
        final EditText input = new EditText(this);
        input.setText(getSharedPreferences("pref", 0).getString("mqtt_server", "tcp://broker.hivemq.com:1883"));
        input.setHint("tcp://broker.hivemq.com:1883");
        input.setMaxLines(1);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String data = input.getText().toString();
                getSharedPreferences("pref", 0).edit().putString("mqtt_server", data).commit();
                Toast.makeText(MainActivity.this, "MQTT Server saved", Toast.LENGTH_LONG).show();
                setMqttUsername();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void setMqttUsername(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("MQTT Username");
        builder.setMessage("Your Mqtt Server Username");
        final EditText input = new EditText(this);
        input.setText(getSharedPreferences("pref", 0).getString("mqtt_user", ""));
        input.setHint("Username");
        input.setMaxLines(1);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String data = input.getText().toString();
                getSharedPreferences("pref", 0).edit().putString("mqtt_user", data).commit();
                Toast.makeText(MainActivity.this, "MQTT Username saved", Toast.LENGTH_LONG).show();
                setMqttPassword();
            }
        });
        builder.show();
    }

    public void setMqttPassword(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MQTT Password");
        builder.setCancelable(false);
        builder.setMessage("Your Mqtt Server Password");
        final EditText input = new EditText(this);
        input.setText(getSharedPreferences("pref", 0).getString("mqtt_pass", ""));
        input.setHint("Password");
        input.setMaxLines(1);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String data = input.getText().toString();
                SharedPreferences sp = getSharedPreferences("pref", 0);
                sp.edit().putString("mqtt_pass", data).commit();
                Toast.makeText(MainActivity.this, "MQTT Password saved", Toast.LENGTH_LONG).show();

                // Reset Services
                if (sp.getBoolean("gateway_on", true)) {
                    Intent i = new Intent(getApplicationContext(), BackgroundService.class);
                    i.setAction(ACTION_STOP);
                    startService(i);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkServices();
                        }
                    }, 2000);
                }
            }
        });
        builder.show();
    }

    public void callUssd() {
        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setTitle("SEND USSD");
        final EditText input2 = new EditText(this);
        input2.setText("*888#");
        input2.setHint("*888#");
        input2.setMaxLines(1);
        builder2.setView(input2);
        builder2.setPositiveButton("Call USSD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ussd = input2.getText().toString();
                Log.d("ussd", "tel:" + ussd);
                SimUtil.queueUssd(ussd, 1);
            }
        });
        builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder2.show();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("MainActivity"));
        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Fungsi.log("BroadcastReceiver received");
            if (intent.hasExtra("newMessage"))
                txtLog.setText(Fungsi.readFromFile(new File(Aplikasi.app.getFilesDir(), "log.txt"), Aplikasi.app));
            else if (intent.hasExtra("kill") && intent.getBooleanExtra("kill", false)) {
                Fungsi.log("BackgroundService KILLED");
                serviceActive = false;
            } else
                serviceActive = true;

        }
    };
}
