/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip.conference;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;
import cn.tojoy.chat.kit.WfcBaseActivity;
import cn.tojoy.chat.kit.widget.FixedTextInputEditText;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

public class CreateConferenceActivity extends WfcBaseActivity {
    @BindView(R2.id.conferenceTitleTextInputEditText)
    FixedTextInputEditText titleEditText;
    @BindView(R2.id.conferenceDescTextInputEditText)
    FixedTextInputEditText descEditText;
    @BindView((R2.id.videoSwitch))
    SwitchMaterial videoSwitch;
    @BindView((R2.id.audienceSwitch))
    SwitchMaterial audienceSwitch;
    @BindView(R2.id.createConferenceBtn)
    Button createButton;

    private String title;
    private String desc;

    @Override
    protected int contentLayout() {
        return R.layout.conference_create_activity;
    }

    @OnTextChanged(value = R2.id.conferenceTitleTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void conferenceTitleChannelName(Editable editable) {
        this.title = editable.toString();
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(desc)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R2.id.conferenceDescTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void conferenceDescChannelName(Editable editable) {
        this.desc = editable.toString();
        if (!TextUtils.isEmpty(desc) && !TextUtils.isEmpty(title)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnClick(R2.id.createConferenceBtn)
    public void onClickCreateBtn() {
        boolean audioOnly = !videoSwitch.isChecked();
        boolean audience = audienceSwitch.isChecked();
        String title = titleEditText.getText().toString();
        String desc = descEditText.getText().toString();
        AVEngineKit.CallSession session = AVEngineKit.Instance().startConference(null, audioOnly, null, ChatManager.Instance().getUserId(), title, desc, audience, null);
        if (session != null) {
            Intent intent = new Intent(this, ConferenceActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "创建会议失败", Toast.LENGTH_SHORT).show();
        }
    }
}