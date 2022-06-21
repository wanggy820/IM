/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package cn.tojoy.chat.kit.voip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class SingleCallActivity extends VoipBaseActivity {
    private static final String TAG = "SingleCallActivity";

    public static final String EXTRA_FROM_FLOATING_VIEW = "fromFloatingView";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
    }

    private void init() {
        if (TJCallState.Idle == TJIMSDK.getSDK().state) {
            finishFadeout();
            return;
        }

        Fragment fragment;
        if (content.isAudioOnly()) {
            fragment = new SingleAudioFragment();
        } else {
            fragment = new SingleVideoFragment();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commit();
    }

    public void audioAccept() {
        SingleAudioFragment fragment = new SingleAudioFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit();

        if (TJIMSDK.getSDK().state == TJCallState.Outgoing) {
            TJIMSDK.answerCall();
        } else if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            TJIMSDK.getSDK().content.setAudioOnly(true);
        } else if (TJIMSDK.getSDK().state == TJCallState.Incoming) {
            TJIMSDK.getSDK().content.setAudioOnly(true);
            TJIMSDK.answerCall();
        }
    }

    public void audioCall() {
        audioAccept();
        TJIMSDK.answerCall();
    }

}
