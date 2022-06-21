/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.contact.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import cn.tojoy.chat.kit.contact.model.UIUserInfo;
import cn.tojoy.chat.kit.contact.pick.viewholder.CheckableUserBlackViewHolder;
import cn.tojoy.chat.kit.contact.pick.viewholder.CheckableUserViewHolder;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;

public class CheckableUserListBlackAdapter extends CheckableUserListAdapter {

    public CheckableUserListBlackAdapter(Fragment fragment) {
        super(fragment);
    }

    @Override
    protected RecyclerView.ViewHolder onCreateContactViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact_black, parent, false);
        CheckableUserViewHolder viewHolder = new CheckableUserBlackViewHolder(fragment, this, itemView);

        itemView.setOnClickListener(v -> {
            UIUserInfo userInfo = viewHolder.getBindContact();
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(userInfo);
            }
        });
        return viewHolder;
    }
}
