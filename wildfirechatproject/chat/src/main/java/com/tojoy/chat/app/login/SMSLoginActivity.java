/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package com.tojoy.chat.app.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.tojoy.chat.BuildConfig;
import com.tojoy.chat.app.AppService;
import com.tojoy.chat.app.login.model.LoginResult;
import com.tojoy.chat.app.main.MainActivity;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.tojoy.chat.kit.ChatManagerHolder;
import cn.tojoy.chat.kit.WfcBaseNoToolbarActivity;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.sdk.TJUICallBack;

import com.tojoy.chat.R;


public class SMSLoginActivity extends WfcBaseNoToolbarActivity {

    private static final String TAG = "SMSLoginActivity";

    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.phoneNumberEditText)
    EditText phoneNumberEditText;
    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText;
    @BindView(R.id.requestAuthCodeButton)
    TextView requestAuthCodeButton;



    @Override
    protected int contentLayout() {
        return R.layout.login_activity_sms;
    }

    @Override
    protected void afterViews() {
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray14);

        phoneNumberEditText.setText(getSharedPreferences("globel.config", MODE_PRIVATE).getString("phoneNumber", ""));
        loginButton.setEnabled(true);

        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            authCodeEditText.setText("66666");
        }
    }



    @OnTextChanged(value = R.id.phoneNumberEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputPhoneNumber(Editable editable) {
        String phone = editable.toString().trim();
        if (phone.length() == 11) {
            requestAuthCodeButton.setEnabled(true);
        } else {
            requestAuthCodeButton.setEnabled(false);
        }
        loginButton.setEnabled(phone.length() == 11 && authCodeEditText.getText().length() > 2);
    }

    @OnTextChanged(value = R.id.authCodeEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputAuthCode(Editable editable) {
        if (editable.toString().length() > 2) {
            loginButton.setEnabled(true);
        }
    }



    @OnClick(R.id.loginButton)
    void login() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String authCode = authCodeEditText.getText().toString().trim();

        loginButton.setEnabled(false);
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("登录中...")
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();


        AppService.Instance().smsLogin(phoneNumber, authCode, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                //需要登录腾讯云im，用于音视频聊天

                TJIMSDK.login(loginResult.getUserId(), new TJUICallBack() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing()) {
                            return;
                        }

                        Log.v(TAG, "phoneNumber:"+phoneNumber + ", userid:" + loginResult.getUserId());
                        dialog.dismiss();
                        //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                        ChatManagerHolder.gChatManager.connect(loginResult.getUserId(), loginResult.getToken());
                        SharedPreferences sp = getSharedPreferences("user.config", Context.MODE_PRIVATE);
                        sp.edit()
                                .putString("userId", loginResult.getUserId())
                                .putString("token", loginResult.getToken())
                                .apply();

                        getSharedPreferences("globel.config", MODE_PRIVATE)
                                .edit().putString("phoneNumber", phoneNumber).apply();
                        Intent intent = new Intent(SMSLoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(int errCode, String errMsg) {
                        if (isFinishing()) {
                            return;
                        }
                        Toast.makeText(SMSLoginActivity.this, "登录失败：" + errCode + " " + errMsg, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loginButton.setEnabled(true);
                    }
                });
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(SMSLoginActivity.this, "登录失败：" + code + " " + msg, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loginButton.setEnabled(true);
            }
        });
    }

    private Handler handler = new Handler();

    @OnClick(R.id.requestAuthCodeButton)
    void requestAuthCode() {
        requestAuthCodeButton.setEnabled(false);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    requestAuthCodeButton.setEnabled(true);
                }
            }
        }, 60 * 1000);

        Toast.makeText(this, "请求验证码...", Toast.LENGTH_SHORT).show();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        AppService.Instance().requestAuthCode(phoneNumber, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(SMSLoginActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(SMSLoginActivity.this, "发送验证码失败: " + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
