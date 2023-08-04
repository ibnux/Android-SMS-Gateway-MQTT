package com.ibnux.smsgatewaymqtt;

/**
 * Created by Ibnu Maksum 2023
 */

import android.app.Application;
import android.content.SharedPreferences;

import java.util.UUID;

public class Aplikasi extends Application {

    public static Application app;

    @Override
    public void onCreate() {
        super.onCreate();
        ObjectBox.init(this);
        this.app = this;
    }

}
