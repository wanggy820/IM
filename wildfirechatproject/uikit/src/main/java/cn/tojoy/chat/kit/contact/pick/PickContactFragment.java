/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.contact.pick;

import androidx.lifecycle.ViewModelProviders;

import cn.tojoy.chat.kit.contact.ContactViewModel;

public class PickContactFragment extends PickUserFragment {
    @Override
    protected void setupPickFromUsers() {
        ContactViewModel contactViewModel = ViewModelProviders.of(getActivity()).get(ContactViewModel.class);
        contactViewModel.contactListLiveData().observe(this, userInfos -> {
            showContent();
            pickUserViewModel.setUsers(userInfos);
            userListAdapter.setUsers(userInfos);
        });
    }
}
