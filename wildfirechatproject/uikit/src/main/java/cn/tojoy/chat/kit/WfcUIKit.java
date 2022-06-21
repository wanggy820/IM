/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.lqr.emoji.LQREmotionKit;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import cn.tojoy.chat.kit.common.AppScopeViewModel;
import cn.tojoy.chat.kit.net.OKHttpHelper;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.third.utils.UIUtils;
import cn.tojoy.chat.kit.voip.MultiCallActivity;
import cn.tojoy.chat.kit.voip.SingleCallActivity;
import cn.tojoy.chat.kit.voip.VoipCallService;
import cn.tojoy.chat.kit.voip.conference.message.ConferenceChangeModelContent;
import cn.tojoy.chat.kit.voip.conference.message.ConferenceKickoffMemberContent;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.PCLoginRequestMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnFriendUpdateListener;
import cn.wildfirechat.remote.OnRecallMessageListener;
import cn.wildfirechat.remote.OnReceiveMessageListener;


public class WfcUIKit implements OnReceiveMessageListener, OnRecallMessageListener, OnFriendUpdateListener {

    private boolean isBackground = true;
    private Application application;
    private static ViewModelProvider viewModelProvider;
    private ViewModelStore viewModelStore;
    private AppServiceProvider appServiceProvider;
    private static WfcUIKit wfcUIKit;
    private boolean isSupportMoment = false;

    private WfcUIKit() {
    }

    public static WfcUIKit getWfcUIKit() {
        if (wfcUIKit == null) {
            wfcUIKit = new WfcUIKit();
        }
        return wfcUIKit;
    }

    public void init(Application application) {
        this.application = application;
        UIUtils.application = application;
        initWFClient(application);
        initMomentClient(application);
        //初始化表情控件
        LQREmotionKit.init(application, (context, path, imageView) -> Glide.with(context).load(path).apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate()).into(imageView));

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                WfcNotificationManager.getInstance().clearAllNotification(application);
                isBackground = false;

                // 处理没有后台弹出界面权限
                if (TJIMSDK.getSDK().message != null) {
                    TJIMSDK.getSDK().receiveCall();
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
                isBackground = true;
            }
        });

        viewModelStore = new ViewModelStore();
        ViewModelProvider.Factory factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
        viewModelProvider = new ViewModelProvider(viewModelStore, factory);
        OKHttpHelper.init(application.getApplicationContext());

        Log.d("WfcUIKit", "init end");
    }

    public boolean isSupportMoment() {
        return isSupportMoment;
    }

    public Application getApplication() {
        return application;
    }

    private void initWFClient(Application application) {
        ChatManager.init(application, Config.IM_SERVER_HOST);
        try {
            ChatManagerHolder.gChatManager = ChatManager.Instance();
            ChatManagerHolder.gChatManager.startLog();
            ChatManagerHolder.gChatManager.addOnReceiveMessageListener(this);
            ChatManagerHolder.gChatManager.addRecallMessageListener(this);
            ChatManagerHolder.gChatManager.addFriendUpdateListener(this);

            ChatManager.Instance().registerMessageContent(ConferenceChangeModelContent.class);
            ChatManager.Instance().registerMessageContent(ConferenceKickoffMemberContent.class);

            SharedPreferences sp = application.getSharedPreferences("user.config", Context.MODE_PRIVATE);
            String userId = sp.getString("userId", null);
            String token = sp.getString("token", null);
            if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(token)) {
                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                //另外不能多次connect，如果需要切换用户请先disconnect，然后3秒钟之后再connect（如果是用户手动登录可以不用等，因为用户操作很难3秒完成，如果程序自动切换请等3秒）
                ChatManagerHolder.gChatManager.connect(userId, token);
            }
        } catch (NotInitializedExecption notInitializedExecption) {
            notInitializedExecption.printStackTrace();
        }
    }

    private void initMomentClient(Application application) {
        String momentClientClassName = "cn.wildfirechat.moment.MomentClient";
        try {
            Class clazz = Class.forName(momentClientClassName);
            Constructor constructor = clazz.getConstructor();
            Object o = constructor.newInstance();
            Method method = clazz.getMethod("init", Context.class);
            method.invoke(o, application);
            isSupportMoment = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 当{@link androidx.lifecycle.ViewModel} 需要跨{@link android.app.Activity} 共享数据时使用
     */
    public static <T extends ViewModel> T getAppScopeViewModel(@NonNull Class<T> modelClass) {
        if (!AppScopeViewModel.class.isAssignableFrom(modelClass)) {
            throw new IllegalArgumentException("the model class should be subclass of AppScopeViewModel");
        }
        return viewModelProvider.get(modelClass);
    }

    // pls refer to https://stackoverflow.com/questions/11124119/android-starting-new-activity-from-application-class
    public static void singleCall(Context context, String targetId, boolean isAudioOnly) {
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, targetId);
        TJIMSDK.startCall(conversation, Collections.singletonList(targetId), isAudioOnly);

        Intent voip = new Intent(context, SingleCallActivity.class);
        startActivity(context, voip);

        VoipCallService.start(context, false);
    }

    public static void multiCall(Context context, String groupId, List<String> participants, boolean isAudioOnly) {
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId);
        TJIMSDK.startCall(conversation, participants, isAudioOnly);

        Intent intent = new Intent(context, MultiCallActivity.class);
        startActivity(context, intent);
    }

    public static void startActivity(Context context, Intent intent) {
        if (context instanceof Activity) {
            context.startActivity(intent);
            ((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Intent main = new Intent(context.getPackageName() + ".main");
            PendingIntent pendingIntent = PendingIntent.getActivities(context, 100, new Intent[]{main, intent}, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        long now = System.currentTimeMillis();
        long delta = ChatManager.Instance().getServerDeltaTime();
        if (messages != null) {
            for (Message msg : messages) {
                if (msg.content instanceof PCLoginRequestMessageContent && (now - (msg.serverTime - delta)) < 60 * 1000) {
                    PCLoginRequestMessageContent content = ((PCLoginRequestMessageContent) msg.content);
                    appServiceProvider.showPCLoginActivity(ChatManager.Instance().getUserId(), content.getSessionId(), content.getPlatform());
                    break;
                }
            }
        }

        if (isBackground && !ChatManager.Instance().isMuteNotificationWhenPcOnline()) {
            if (messages == null) {
                return;
            }

            List<Message> msgs = new ArrayList<>(messages);
            Iterator<Message> iterator = msgs.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (message.content.getPersistFlag() == PersistFlag.No_Persist
                    || now - (message.serverTime - delta) > 10 * 1000) {
                    iterator.remove();
                }
            }
            WfcNotificationManager.getInstance().handleReceiveMessage(application, msgs);
        } else {
            // do nothing
        }
    }

    @Override
    public void onRecallMessage(Message message) {
        if (isBackground) {
            WfcNotificationManager.getInstance().handleRecallMessage(application, message);
        }
    }

    @Override
    public void onFriendListUpdate(List<String> updateFriendList) {
        // do nothing

    }

    @Override
    public void onFriendRequestUpdate(List<String> newRequests) {
        if (isBackground) {
            if (newRequests == null || newRequests.isEmpty()) {
                return;
            }
            WfcNotificationManager.getInstance().handleFriendRequest(application, newRequests);
        }
    }

    public AppServiceProvider getAppServiceProvider() {
        return this.appServiceProvider;
    }


    public void setAppServiceProvider(AppServiceProvider appServiceProvider) {
        this.appServiceProvider = appServiceProvider;
    }
}