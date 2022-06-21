/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.voip.conference.ConferenceActivity;
import cn.wildfirechat.model.Conversation;

public class VoipDummyActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TJIMSDK.getSDK().state == TJCallState.Idle) {
            finish();
        } else if (TJIMSDK.getSDK().isConference()) {
            Intent intent = new Intent(this, ConferenceActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (TJIMSDK.getSDK().message.conversation == null) {
                finish();
                return;
            }
            Intent intent;
            if (TJIMSDK.getSDK().message.conversation.type == Conversation.ConversationType.Single) {
                intent = new Intent(this, SingleCallActivity.class);
            } else {
                intent = new Intent(this, MultiCallActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }
}
