/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
package com.example.gradlestudy;

import android.app.Application;

public class GradleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("111");
    }

}
