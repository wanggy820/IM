/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package com.tojoy.chat.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;

import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cn.tojoy.chat.kit.Config;
import cn.tojoy.chat.kit.WfcUIKit;
import cn.tojoy.chat.kit.conversation.message.viewholder.MessageViewHolderManager;
import cn.tojoy.chat.kit.sdk.GenerateUserSig;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.third.location.viewholder.LocationMessageContentViewHolder;

import com.tojoy.chat.BuildConfig;
import com.tojoy.chat.R;

import cn.wildfirechat.push.PushService;


public class MyApp extends BaseApp {


    // 一定记得替换为你们自己的，ID请从BUGLY官网申请。关于BUGLY，可以从BUGLY官网了解，或者百度。
    public static String BUGLY_ID = "5247e6705e";

    @Override
    public void onCreate() {
        super.onCreate();
        AppService.validateConfig(this);

        // bugly，务必替换为你自己的!!!
//        if ("wildfirechat.cn".equals(Config.IM_SERVER_HOST)) {
            CrashReport.initCrashReport(getApplicationContext(), BUGLY_ID, false);
//        }
        // 只在主进程初始化，否则会导致重复收到消息
        if (getCurProcessName(this).equals(BuildConfig.APPLICATION_ID)) {
            // 如果uikit是以aar的方式引入 ，那么需要在此对Config里面的属性进行配置，如：
            // Config.IM_SERVER_HOST = "im.example.com";
            WfcUIKit wfcUIKit = WfcUIKit.getWfcUIKit();
            wfcUIKit.init(this);
            wfcUIKit.setAppServiceProvider(AppService.Instance());

            MessageViewHolderManager.getInstance().registerMessageViewHolder(LocationMessageContentViewHolder.class, R.layout.conversation_item_location_send, R.layout.conversation_item_location_send);
            setupWFCDirs();
            TJIMSDK.init(this, GenerateUserSig.SDKAPPID);

            //初始化推送
            PushService.init(this, BuildConfig.APPLICATION_ID);

            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {

                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    TJIMSDK.getSDK().setCurrentActivity(activity);
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {

                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {

                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {

                }
            });
        }
    }

    private void setupWFCDirs() {
        File file = new File(Config.VIDEO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(Config.AUDIO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(Config.FILE_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(Config.PHOTO_SAVE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String getCurProcessName(Context context) {

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) context
            .getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
            .getRunningAppProcesses()) {

            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }
}
