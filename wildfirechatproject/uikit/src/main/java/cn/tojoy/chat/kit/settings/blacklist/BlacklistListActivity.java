/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.settings.blacklist;

import cn.tojoy.chat.kit.WfcBaseActivity;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;

public class BlacklistListActivity extends WfcBaseActivity {


    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, new BlacklistListFragment())
                .commit();
    }
}
