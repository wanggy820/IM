/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package com.tojoy.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.tojoy.chat.R;
import com.tojoy.chat.app.AppService;

import butterknife.BindView;
import butterknife.OnClick;

import com.tojoy.chat.app.main.SplashActivity;
import cn.tojoy.chat.kit.ChatManagerHolder;
import cn.tojoy.chat.kit.Config;
import cn.tojoy.chat.kit.WfcBaseActivity;
import cn.tojoy.chat.kit.net.OKHttpHelper;
import cn.tojoy.chat.kit.net.SimpleCallback;
import cn.tojoy.chat.kit.settings.PrivacySettingActivity;
import cn.tojoy.chat.kit.widget.OptionItemView;

public class SettingActivity extends WfcBaseActivity {
    @BindView(R.id.diagnoseOptionItemView)
    OptionItemView diagnoseOptionItemView;

    @Override
    protected int contentLayout() {
        return R.layout.setting_activity;
    }

    @OnClick(R.id.exitOptionItemView)
    void exit() {
        //不要清除session，这样再次登录时能够保留历史记录。如果需要清除掉本地历史记录和服务器信息这里使用true
        ChatManagerHolder.gChatManager.disconnect(true, false);
        SharedPreferences sp = getSharedPreferences("user.config", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        sp = getSharedPreferences("moment", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        OKHttpHelper.clearCookies();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @OnClick(R.id.privacySettingOptionItemView)
    void privacySetting() {
        Intent intent = new Intent(this, PrivacySettingActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.diagnoseOptionItemView)
    void diagnose() {
        long start = System.currentTimeMillis();
        OKHttpHelper.get("http://" + Config.IM_SERVER_HOST + "/api/version", null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                long duration = (System.currentTimeMillis() - start) / 2;
                diagnoseOptionItemView.setDesc(duration + "ms");
                Toast.makeText(SettingActivity.this, "服务器延迟为：" + duration + "ms", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseOptionItemView.setDesc("test failed");
                Toast.makeText(SettingActivity.this, "访问IM Server失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick(R.id.uploadLogOptionItemView)
    void uploadLog() {
        AppService.Instance().uploadLog(new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String path) {
                if (!isFinishing()) {
                    Toast.makeText(SettingActivity.this, "上传日志" + path + "成功", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isFinishing()) {
                    Toast.makeText(SettingActivity.this, "上传日志失败" + code + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @OnClick(R.id.aboutOptionItemView)
    void about() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }
}