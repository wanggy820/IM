/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.favorite;

import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.WfcBaseActivity;

public class FavoriteListActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, new FavoriteListFragment())
            .commit();
    }
}
