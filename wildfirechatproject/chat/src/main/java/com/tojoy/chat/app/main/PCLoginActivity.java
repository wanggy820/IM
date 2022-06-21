/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package com.tojoy.chat.app.main;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.tojoy.chat.app.AppService;
import com.tojoy.chat.app.login.model.PCSession;

import butterknife.BindView;
import butterknife.OnClick;
import cn.tojoy.chat.kit.WfcBaseActivity;
import cn.tojoy.chat.kit.user.UserViewModel;
import com.tojoy.chat.R;
import cn.wildfirechat.client.Platform;

public class PCLoginActivity extends WfcBaseActivity {
    private String token;
    private boolean isConfirmPcLogin = false;
    @BindView(R.id.confirmButton)
    Button confirmButton;
    @BindView(R.id.descTextView)
    TextView descTextView;

    private Platform platform;

    @Override
    protected void beforeViews() {
        token = getIntent().getStringExtra("token");
        isConfirmPcLogin = getIntent().getBooleanExtra("isConfirmPcLogin", false);
        int tmp = getIntent().getIntExtra("platform", 0);
        platform = Platform.platform(tmp);
        if (!isConfirmPcLogin && TextUtils.isEmpty(token)) {
            finish();
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.pc_login_activity;
    }

    @Override
    protected void afterViews() {
        descTextView.setText("允许 " + platform.getPlatFormName() + " 登录");
        if (isConfirmPcLogin) {
            confirmButton.setEnabled(true);
        } else {
            scanPCLogin(token);
        }
    }

    @OnClick(R.id.confirmButton)
    void confirmPCLogin() {
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        confirmPCLogin(token, userViewModel.getUserId());
    }

    @OnClick(R.id.cancelButton)
    void cancelPCLogin() {
        AppService.Instance().cancelPCLogin(token, new AppService.PCLoginCallback() {
            @Override
            public void onUiSuccess() {
                if (isFinishing()) {
                    return;
                }
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                finish();
            }
        });
    }

    private void scanPCLogin(String token) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("处理中")
            .progress(true, 100)
            .build();
        dialog.show();

        AppService.Instance().scanPCLogin(token, new AppService.ScanPCCallback() {
            @Override
            public void onUiSuccess(PCSession pcSession) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                if (pcSession.getStatus() == 1) {
                    confirmButton.setEnabled(true);
                } else {
                    Toast.makeText(PCLoginActivity.this, "status: " + pcSession.getStatus(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCLoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void confirmPCLogin(String token, String userId) {
        AppService.Instance().confirmPCLogin(token, userId, new AppService.PCLoginCallback() {
            @Override
            public void onUiSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCLoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCLoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
