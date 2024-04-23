package com.ibnux.smsgatewaymqtt;

/**
 * Created by Ibnu Maksum 2023
 */

import android.app.Application;

public class Aplikasi extends Application {

    public static Application app;

    @Override
    public void onCreate() {
        super.onCreate();
        this.app = this;
    }

}
