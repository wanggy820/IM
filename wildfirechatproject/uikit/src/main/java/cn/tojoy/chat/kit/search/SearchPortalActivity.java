/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.search;

import java.util.List;

import cn.tojoy.chat.kit.search.module.ChannelSearchModule;
import cn.tojoy.chat.kit.search.module.ContactSearchModule;
import cn.tojoy.chat.kit.search.module.ConversationSearchModule;
import cn.tojoy.chat.kit.search.module.GroupSearchViewModule;

public class SearchPortalActivity extends SearchActivity {
    @Override
    protected void initSearchModule(List<SearchableModule> modules) {

        SearchableModule module = new ContactSearchModule();
        modules.add(module);

        module = new GroupSearchViewModule();
        modules.add(module);

        module = new ConversationSearchModule();
        modules.add(module);
        modules.add(new ChannelSearchModule());
    }
}
