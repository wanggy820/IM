/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import java.util.ArrayList;
import java.util.List;
import cn.tojoy.chat.kit.group.GroupViewModel;
import cn.tojoy.chat.kit.group.PickGroupMemberActivity;
import cn.tojoy.chat.kit.sdk.ParticipantProfile;
import cn.tojoy.chat.kit.sdk.TJCallEndReason;
import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallActivity extends VoipBaseActivity {

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 101;
    private String groupId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init() {
        if (TJIMSDK.getSDK().state == TJCallState.Idle) {
            finish();
            return;
        }
        groupId = TJIMSDK.getSDK().message.conversation.target;

        Fragment fragment;
        if (TJIMSDK.getSDK().state == TJCallState.Incoming) {
            fragment = new MultiCallIncomingFragment();
        } else if (TJIMSDK.getSDK().content.isAudioOnly()) {
            fragment = new MultiCallAudioFragment();
        } else {
            fragment = new MultiCallVideoFragment();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commit();
    }

    void hangup() {
        if (TJIMSDK.getSDK().state != TJCallState.Idle) {
            TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
        } else {
            finish();
        }
    }

    void accept() {
        if (TJIMSDK.getSDK().state == TJCallState.Idle) {
            finish();
            return;
        }

        Fragment fragment;
        if (TJIMSDK.getSDK().content.isAudioOnly()) {
            fragment = new MultiCallAudioFragment();
        } else {
            fragment = new MultiCallVideoFragment();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit();

        TJIMSDK.answerCall();
    }

    void addParticipant(int maxNewInviteParticipantCount) {
        isInvitingNewParticipant = true;
        Intent intent = new Intent(this, PickGroupMemberActivity.class);
        GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        GroupInfo groupInfo = groupViewModel.getGroupInfo(groupId, false);
        intent.putExtra(PickGroupMemberActivity.GROUP_INFO, groupInfo);
        List<String> participants =  new ArrayList<>();
        for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
            participants.add(profile.userId);
        }
        participants.add(ChatManager.Instance().getUserId());
        intent.putStringArrayListExtra(PickGroupMemberActivity.CHECKED_MEMBER_IDS, (ArrayList<String>) participants);
        intent.putStringArrayListExtra(PickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS, (ArrayList<String>) participants);
        intent.putExtra(PickGroupMemberActivity.MAX_COUNT, maxNewInviteParticipantCount);
        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            isInvitingNewParticipant = false;
            if (resultCode == RESULT_OK) {
                List<String> newParticipants = data.getStringArrayListExtra(PickGroupMemberActivity.EXTRA_RESULT);
                if (newParticipants != null && !newParticipants.isEmpty()) {
                    TJIMSDK.inviteNewParticipants(newParticipants);
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
