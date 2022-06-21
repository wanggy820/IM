/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.group.manage;

import cn.tojoy.chat.kit.WfcBaseActivity;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;
import cn.wildfirechat.model.GroupInfo;

public class GroupMemberPermissionActivity extends WfcBaseActivity {

    @Override
    protected void afterViews() {
        GroupInfo groupInfo = getIntent().getParcelableExtra("groupInfo");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, GroupMemberPermissionFragment.newInstance(groupInfo))
                .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
}