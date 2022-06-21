/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.group.manage;

import android.view.View;

import androidx.fragment.app.Fragment;

import cn.tojoy.chat.kit.contact.UserListAdapter;
import cn.tojoy.chat.kit.contact.model.FooterValue;
import cn.tojoy.chat.kit.contact.viewholder.footer.FooterViewHolder;

public class AddGroupManagerViewHolder extends FooterViewHolder<FooterValue> {

    public AddGroupManagerViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(FooterValue footerValue) {
        // do nothing
    }
}
