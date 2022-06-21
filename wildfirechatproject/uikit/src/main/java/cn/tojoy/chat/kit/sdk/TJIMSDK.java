package cn.tojoy.chat.kit.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.tencent.imsdk.TIMLogLevel;
import com.tencent.imsdk.v2.V2TIMCallback;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMSDKConfig;
import com.tencent.liteav.beauty.TXBeautyManager;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.WfcIntent;
import cn.tojoy.chat.kit.WfcUIKit;
import cn.tojoy.chat.kit.voip.AsyncPlayer;
import cn.tojoy.chat.kit.voip.VoipBaseActivity;
import cn.tojoy.chat.kit.voip.VoipCallService;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TJCallAnswerTMessageContent;
import cn.wildfirechat.message.TJCallByeMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnReceiveMessageListener;
import cn.wildfirechat.remote.SendMessageCallback;


public class TJIMSDK implements OnReceiveMessageListener {
    private static  String TAG = "TJIMSDK";

    public static int TJ_MAX_PARTICIPANT_COUNT = 9;
    public boolean audioMuted;//是否静音
    public boolean videoMuted;//是否关闭视频
    public boolean handsFreeOn;//免提状态
    public boolean frontCamera;//前置摄像头
    public boolean isScreenSharing;//屏幕分享
    public Message message;
    public CallStartMessageContent content;
    public TJCallState state;

    private int sdkAppId;
    private String userId;
    private String token;
    private String userSig;
    private Application application;
    private TJAVCallMessageCallBack callBack;
    private AsyncPlayer ringPlayer;
    private TXCloudVideoView localVideoView;
    private ParticipantProfile myProfile;

    public List<ParticipantProfile> getParticipants() {
        return participants;
    }

    private long kTJAVCallTimeOut = 60 * 1000;
    private List<ParticipantProfile> participants;
    private TRTCCloud trtc;
    private WeakReference<Activity> sCurrentActivityWeakRef;

    public Activity getCurrentActivity() {
        return sCurrentActivityWeakRef.get();
    }

    public void setCurrentActivity(Activity activity) {
        sCurrentActivityWeakRef = new WeakReference<Activity>(activity);
    }

    private static TJIMSDK sdk = new TJIMSDK();
    public static TJIMSDK getSDK(){
        return sdk;
    }

    public boolean isConference() {
        return false;
    }
    /*
     * 初始化腾讯云im、音视频对话回调
     * */
    public static void init(Application application, int appId) {
        sdk.ringPlayer = new AsyncPlayer(null);
        sdk.setState(TJCallState.Idle);
        sdk.sdkAppId = appId;
        sdk.application = application;
        sdk.trtc = TRTCCloud.sharedInstance(application);
        V2TIMSDKConfig config = new V2TIMSDKConfig();
        config.setLogLevel(V2TIMSDKConfig.V2TIM_LOG_NONE);
        V2TIMManager.getInstance().initSDK(application, appId, config, null);
        TRTCCloud.setLogLevel(TIMLogLevel.OFF);

        if (isLogin()) {
            TJIMSDK.login(sdk.getUserId(), null);
        }
        ChatManager.Instance().addOnReceiveMessageListener(sdk);
    }

    public String getUserId() {
        if (userId == null) {
            SharedPreferences sharedPreferences = sdk.application.getSharedPreferences("user.config", Context.MODE_PRIVATE);
            userId = sharedPreferences.getString("userId", null);
        }
        return userId;
    }


    public String getToken() {
        if (token == null) {
            SharedPreferences sharedPreferences = sdk.application.getSharedPreferences("user.config", Context.MODE_PRIVATE);
            token = sharedPreferences.getString("token", null);
        }
        return token;
    }

    /*
     * 腾讯云Im登录
     * */
    public static void login(String userId, final TJUICallBack callback) {
        if (V2TIMManager.getInstance().getLoginStatus() == V2TIMManager.V2TIM_STATUS_LOGINED) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }
        sdk.userId = userId;
        sdk.userSig = GenerateUserSig.genTestUserSig(userId);
        V2TIMManager.getInstance().login(userId, sdk.userSig, new V2TIMCallback() {
            @Override
            public void onError(int code, String desc) {
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        });

    }

    public static boolean isLogin() {
        return !TextUtils.isEmpty(sdk.getUserId()) && !TextUtils.isEmpty(sdk.getToken());
    }

    public static void setCallBack(TJAVCallMessageCallBack messageCallBack) {
        sdk.callBack = messageCallBack;
    }
    //发起对话
    public static void startCall(Conversation conversation, List<String> targetIds, boolean audioOnly) {
        sdk.audioMuted = false;
        sdk.videoMuted = false;
        sdk.handsFreeOn = true;
        sdk.frontCamera = true;
        sdk.isScreenSharing = false;
        sdk.localVideoView = null;

        String userId = sdk.userId;
        int roomId = Math.abs(userId.hashCode() % 1111111) + 1;//需要服务器下发
        sdk.content = new CallStartMessageContent(roomId, targetIds, audioOnly);
        sdk.content.setConnectTime(System.currentTimeMillis());
        String[] toUsers = targetIds.toArray(new String[targetIds.size()]);

        sdk.message = ChatManager.Instance().sendMessage(conversation, sdk.content, toUsers, 0, new SendMessageCallback() {
            @Override
            public void onSuccess(long messageUid, long timestamp) {
                Log.v(TAG, "发送成功!!");
            }

            @Override
            public void onFail(int errorCode) {
                Log.v(TAG, "发送失败..");
            }

            @Override
            public void onPrepare(long messageId, long savedTime) {

            }
        });
        sdk.initAVParticipantProfile();
        sdk.setState(TJCallState.Outgoing);
    }

    //初始化参会人员
    private void initAVParticipantProfile() {
        if (myProfile != null) {
            return;
        }
        myProfile = new ParticipantProfile();
        myProfile.userId = this.userId;
        myProfile.state = this.state;
        myProfile.audience = !this.userId.equals(this.message.sender);
        myProfile.startTime = this.content.getConnectTime();

        participants = new ArrayList<>();
        for (String userId : this.content.getTargetIds()) {
            if (userId.equals(myProfile.userId)) {
                continue;
            }
            ParticipantProfile profile = new ParticipantProfile();
            profile.userId = userId;
            profile.state = TJCallState.Incoming;
            profile.audience = true;
            participants.add(profile);
        }

        //自己是观众,需要把邀请人加入进去
        if (myProfile.audience && !content.getTargetIds().contains(message.sender)) {
            ParticipantProfile profile = new ParticipantProfile();
            profile.userId = message.sender;
            profile.state = TJCallState.Incoming;
            profile.audience = false;
            participants.add(profile);
        }
    }
    public static void inviteNewParticipants(List<String> targetIds) {
        List<String> array = new ArrayList<>(targetIds);
        for (ParticipantProfile profile : sdk.participants) {
            array.add(profile.userId);
        }
        sdk.content.setTargetIds(array);
        String[] toUsers = array.toArray(new String[array.size()]);
        sdk.message = ChatManager.Instance().sendMessage(sdk.message.conversation, sdk.content, toUsers, 0, null);

        for (String userId : targetIds) {
            ParticipantProfile profile = new ParticipantProfile();
            profile.userId = userId;
            profile.state = TJCallState.Incoming;
            profile.audience = true;
            sdk.participants.add(profile);

            if (sdk.callBack != null) {
                sdk.callBack.didReceiveParticipantProfile(userId, true);
            }
        }
    }

    private boolean containsUserId(String userId) {
        for (ParticipantProfile profile : participants) {
            if (profile.userId.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private void resetAVParticipantProfile(String userId, boolean isEnterRoom) {
        if (TextUtils.isEmpty(userId) || participants == null) {
            return;
        }
        if (!isEnterRoom) {
            for (ParticipantProfile profile : participants) {
                if (profile.userId.equals(userId)) {
                    participants.remove(profile);
                    break;
                }
            }
        } else if (!containsUserId(userId) && !message.sender.equals(userId)) {
            ParticipantProfile profile = new ParticipantProfile();
            profile.userId = userId;
            profile.state = TJCallState.Connected;
            profile.startTime = System.currentTimeMillis();
            profile.audience = true;
            participants.add(profile);
        } else  {
            //自己不用管
            for (ParticipantProfile profile : participants) {
                if (profile.userId.equals(userId)) {
                    profile.state = TJCallState.Connected;
                    profile.startTime = System.currentTimeMillis();
                    break;
                }
            }
        }
        if (callBack != null) {
            callBack.didReceiveParticipantProfile(userId, isEnterRoom);
        }
    }

    //进入房间
    public static void enterRoom() {
        if (sdk.state == TJCallState.Connected) {
            return;
        }

        TRTCCloudDef.TRTCParams params = new TRTCCloudDef.TRTCParams();
        params.sdkAppId = sdk.sdkAppId;
        params.userId = sdk.userId;
        params.userSig = sdk.userSig;
        params.roomId = sdk.content.getRoomId();
        params.role = TRTCCloudDef.TRTCRoleAnchor;

        if (!sdk.content.isAudioOnly()) {
            // 开启基础美颜
            TXBeautyManager txBeautyManager = sdk.trtc.getBeautyManager();
            // 自然美颜
            txBeautyManager.setBeautyStyle(1);
            txBeautyManager.setBeautyLevel(6);
            // 进房前需要设置一下关键参数
            TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
            encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540;
            encParam.videoFps = 15;
            encParam.videoBitrate = 1000;
            encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
            encParam.enableAdjustRes = true;
            sdk.trtc.setVideoEncoderParam(encParam);
        }

        sdk.trtc.enableAudioVolumeEvaluation(300);
        sdk.trtc.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
        sdk.trtc.startLocalAudio(1);
        // 进房前，开始监听trtc的消息
        sdk.trtc.setListener(sdk.mTRTCCloudListener);
        sdk.trtc.enterRoom(params, TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL);
        sdk.setState(TJCallState.Connecting);
    }
    //退出房间
    public static void quitRoom() {
        //判断当前的activity是否是会话页面
        if(sdk.getCurrentActivity() instanceof VoipBaseActivity) {
            VoipBaseActivity activity = (VoipBaseActivity) sdk.getCurrentActivity();
            activity.finish();
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        VoipCallService.stop(sdk.application);
        sdk.trtc.stopLocalAudio();
        sdk.trtc.stopLocalPreview();
        sdk.trtc.exitRoom();

        sdk.setState(TJCallState.Idle);
        sdk.audioMuted = false;
        sdk.videoMuted = false;
        sdk.handsFreeOn = true;
        sdk.frontCamera = true;
        sdk.isScreenSharing = false;
        sdk.localVideoView = null;

        sdk.message = null;
        sdk.content = null;
        sdk.myProfile = null;
        sdk.participants = null;

        if (sdk.callBack != null) {
            sdk.callBack = null;
        }
    }
    //开启远程用户视频渲染
    public static void startRemoteView(String userId, TXCloudVideoView view) {
        if (sdk.state == TJCallState.Idle || view == null) {
            return;
        }
        sdk.trtc.startRemoteView(userId,0, view);
    }
    //关闭远程用户视频渲染
    public static void stopRemoteView(String userId) {
        sdk.trtc.stopRemoteView(userId);
    }
    //打开摄像头
    public static void startLocalPreview(TXCloudVideoView view) {
        if (sdk.state == TJCallState.Idle || view == null) {
            return;
        }
        sdk.localVideoView = view;
        sdk.trtc.startLocalPreview(sdk.frontCamera, view);
    }

    public static void startScreenCapture() {
        if (sdk.state != TJCallState.Connected) {
            return;
        }
        stopLocalPreview();
        sdk.isScreenSharing = true;

        TRTCCloudDef.TRTCVideoEncParam encParams = new TRTCCloudDef.TRTCVideoEncParam();
        encParams.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720;
        encParams.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        encParams.videoFps = 10;
        encParams.enableAdjustRes = false;
        encParams.videoBitrate = 1200;

        TRTCCloudDef.TRTCScreenShareParams shareParams = new TRTCCloudDef.TRTCScreenShareParams();
        sdk.trtc.startScreenCapture(encParams, shareParams);
    }

    public static void stopScreenCapture() {
        sdk.isScreenSharing = false;
        sdk.trtc.stopScreenCapture();

//        根据条件回复视频对话
        if (!sdk.content.isAudioOnly() ) {
            startLocalPreview(sdk.localVideoView);
            if (sdk.videoMuted) {
                sdk.trtc.muteLocalVideo(true);
            }
        }
    }

    public static void stopLocalPreview() {
        sdk.trtc.stopLocalPreview();
    }
    //切换摄像头
    public static void switchCamera() {
        if (sdk.state == TJCallState.Idle) {
            return;
        }
        sdk.frontCamera = !sdk.frontCamera;
        sdk.trtc.getDeviceManager().switchCamera(sdk.frontCamera);
    }

    public static void setAudioMuted(boolean muted) {
        if (sdk.audioMuted == muted) {
            return;
        }
        sdk.audioMuted = muted;
        sdk.trtc.muteLocalAudio(muted);
        sdk.myProfile.audioMuted = muted;
        //发送音量变化通知
        if (sdk.callBack != null) {
            sdk.callBack.didReportAudioVolume(sdk.userId, 0);
        }
    }

    public static void setVideoMuted(boolean muted) {
        if (sdk.videoMuted == muted) {
            return;
        }
        sdk.videoMuted = muted;
        sdk.trtc.muteLocalVideo(muted);
        sdk.myProfile.videoMuted = muted;
    }

    public static void setHandsFreeOn(boolean freeOn) {
        if (sdk.handsFreeOn == freeOn) {
            return;
        }
        sdk.handsFreeOn = freeOn;
        sdk.trtc.setAudioRoute(freeOn ? TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER : TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
    }

    public static boolean isHandsFreeOn() {
        return sdk.handsFreeOn;
    }
    //接听通话
    public static void answerCall() {
        enterRoom();

        TJCallAnswerTMessageContent content = new TJCallAnswerTMessageContent();
        content.setRoomId(sdk.content.getRoomId());
        content.setAudioOnly(sdk.content.isAudioOnly());
        content.setInviteMsgUid(sdk.message.messageUid);

        String[] toUsers = new String[sdk.participants.size()];
        int i = 0;
        for (ParticipantProfile profile : sdk.participants) {
            toUsers[i++] = profile.userId;
        }
        ChatManager.Instance().sendMessage(sdk.message.conversation, content, toUsers, 0, null);
        ChatManager.Instance().clearMessages(sdk.message.conversation);
    }
    //挂断通话
    public static void endCallWithReason(TJCallEndReason reason) {
        if (sdk.state == TJCallState.Idle) {
            return;
        }
        Log.v(TAG, "结束：" + reason);

        if (reason == TJCallEndReason.RemoteHangup) {
            sdk.content.setStatus(TJCallEndReason.Hangup.ordinal());
        } else if (reason == TJCallEndReason.Timeout) {
            sdk.content.setStatus(TJCallEndReason.RemoteTimeout.ordinal());
        } else  {
            sdk.content.setStatus(reason.ordinal());
        }
        if (sdk.state == TJCallState.Connected) {
            sdk.content.setEndTime(System.currentTimeMillis());
        }
        ChatManager.Instance().updateMessage(sdk.message.messageId, sdk.content);
        //发送结束对话消息
        TJCallByeMessageContent messageContent = new TJCallByeMessageContent();
        messageContent.setRoomId(sdk.content.getRoomId());
        messageContent.setInviteMsgUid(sdk.message.messageUid);
        messageContent.setAudioOnly(sdk.content.isAudioOnly());
        messageContent.setEndReason(reason.ordinal());

        String[] toUsers = new String[sdk.participants.size()];
        int i = 0;
        for (ParticipantProfile profile : sdk.participants) {
            toUsers[i++] = profile.userId;
        }
        ChatManager.Instance().sendMessage(sdk.message.conversation, messageContent, toUsers, 0, null);
        quitRoom();
    }

    private void setState(TJCallState state) {
        this.state = state;
        if (callBack != null) {
            callBack.didChangeState(state);
        }
        if (myProfile != null) {
            myProfile.state = this.state;
        }
        if (this.state == TJCallState.Outgoing) {
            shouldStartRing(false);
        } else if(this.state == TJCallState.Incoming) {
            shouldStartRing(true);
        } else  {
            shouldStopRing();
        }

        if (this.state == TJCallState.Outgoing || this.state == TJCallState.Incoming) {
            ChatManager.Instance().getMainHandler().postDelayed(runnable, kTJAVCallTimeOut);
        } else  {
            ChatManager.Instance().getMainHandler().removeCallbacks(runnable);
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (state == TJCallState.Outgoing || state == TJCallState.Incoming) {
                endCallWithReason(TJCallEndReason.Timeout);
            }
        }
    };

    private void shouldStartRing(boolean isIncoming) {
        ChatManager.Instance().getMainHandler().postDelayed(() -> {

            if (state != TJCallState.Incoming && state != TJCallState.Outgoing) {
                shouldStopRing();
                return;
            }
            if (isIncoming) {
                ringPlayer.play(application, R.raw.incoming_call_ring, true, AudioManager.STREAM_MUSIC);
            } else {
                ringPlayer.play(application, R.raw.outgoing_call_ring, true, AudioManager.STREAM_MUSIC);
            }
        }, 200);
    }


    private void shouldStopRing() {
        Log.d("wfcUIKit", "showStopRing");
        ringPlayer.stop();
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        for (Message message : messages) {
            if (message.content instanceof CallStartMessageContent) {
                //收到对话邀请
                CallStartMessageContent messageContent = (CallStartMessageContent)message.content;
                if (state != TJCallState.Idle) {
                    if (this.content != null && messageContent.getRoomId() == this.content.getRoomId()) {
                        if (message.conversation.type == Conversation.ConversationType.Group) {
                            for (String userId : messageContent.getTargetIds()) {
                                if (!containsUserId(userId)) {
                                    resetAVParticipantProfile(userId, true);
                                }
                            }
                        }
                        return;
                    }
                    //忙线中
                    Conversation conversation = new Conversation(message.conversation.type, message.sender, message.conversation.line);
                    TJCallByeMessageContent byeMessageContent = new TJCallByeMessageContent();
                    byeMessageContent.setEndReason(TJCallEndReason.Busy.ordinal());
                    byeMessageContent.setInviteMsgUid(message.messageUid);
                    byeMessageContent.setRoomId(messageContent.getRoomId());
                    byeMessageContent.setAudioOnly(messageContent.isAudioOnly());


                    String toUsers[] = new String[message.toUsers == null ? 1 : message.toUsers.length];
                    int i = 0;
                    if (message.toUsers != null) {
                        for (String userId : message.toUsers) {
                            if (userId.equals(this.userId)) {
                                break;
                            }
                            toUsers[i++] = userId;
                        }
                    }
                    toUsers[i] = message.sender;
                    ChatManager.Instance().sendMessage(conversation, byeMessageContent, toUsers, 0,null);
                    return;
                }

                sdk.audioMuted = false;
                sdk.videoMuted = false;
                sdk.handsFreeOn = true;
                sdk.frontCamera = true;
                sdk.isScreenSharing = false;
                sdk.localVideoView = null;

                setState(TJCallState.Incoming);
                this.message = message;
                this.content = messageContent;
                initAVParticipantProfile();
                ChatManager.Instance().getMainHandler().postDelayed(() -> {
                    if (state != TJCallState.Incoming) {
                        return;
                    }

                    receiveCall();
                }, 200);
            } else if (message.content instanceof TJCallByeMessageContent) {
                //结束对话
                TJCallByeMessageContent messageContent = (TJCallByeMessageContent)message.content;
                if (this.content != null && messageContent.getRoomId() == this.content.getRoomId()) {
                    //单聊或者群聊发起者结束视频
                    if (message.conversation.type == Conversation.ConversationType.Single || this.message.sender.equals(message.sender)) {
                        this.content.setStatus(messageContent.getEndReason());
                        if (state == TJCallState.Connected) {
                            this.content.setEndTime(System.currentTimeMillis());
                        }
                        ChatManager.Instance().updateMessage(this.message.messageId, this.content);
                        quitRoom();
                    } else  {
                        resetAVParticipantProfile(message.sender, false);
                    }
                }
                return;
            } else if (message.content instanceof TJCallAnswerTMessageContent) {
                //被邀请人上线
                if (state == TJCallState.Idle) {
                    endCallWithReason(TJCallEndReason.SignalError);
                    return;
                }
                TJCallAnswerTMessageContent messageContent = (TJCallAnswerTMessageContent)message.content;
                if (content.getRoomId() == messageContent.getRoomId()) {
                    if (message.conversation.type == Conversation.ConversationType.Single) {
                        enterRoom();
                        content.setAudioOnly(messageContent.isAudioOnly());
                    } else  {
                        //由于只能收到别人的消息
                        resetAVParticipantProfile(message.sender, true);
                        if (!myProfile.audience) {
                            enterRoom();
                        }
                    }
                    if (callBack != null) {
                        callBack.didChangeState(state);
                    }
                }
                return;
            }
        }
    }

    public void receiveCall() {
        if (participants == null || participants.isEmpty()) {
            return;
        }

        handsFreeOn = message.conversation.type != Conversation.ConversationType.Single || !content.isAudioOnly();

        Conversation conversation = message.conversation;
        if (conversation.type == Conversation.ConversationType.Single) {
            Intent intent = new Intent(WfcIntent.ACTION_VOIP_SINGLE);
            WfcUIKit.startActivity(getCurrentActivity(), intent);
        } else {
            Intent intent = new Intent(WfcIntent.ACTION_VOIP_MULTI);
            WfcUIKit.startActivity(getCurrentActivity(), intent);
        }
        VoipCallService.start(application, false);
    }

    /**
     * TRTC的监听器
     */
    private TRTCCloudListener mTRTCCloudListener = new TRTCCloudListener() {
        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            endCallWithReason(TJCallEndReason.RemoteNetworkError);
        }

        @Override
        public void onEnterRoom(long result) {
//            TUIKitLog.d(TAG, "onEnterRoom result:" + result);
            if (result < 0) {
                endCallWithReason(TJCallEndReason.RemoteNetworkError);
            } else {
                content.setConnectTime(System.currentTimeMillis());
                myProfile.startTime = content.getConnectTime();
                setState(TJCallState.Connected);
            }
        }

        @Override
        public void onExitRoom(int reason) {
            endCallWithReason(TJCallEndReason.RemoteNetworkError);
        }

        @Override
        public void onRemoteUserEnterRoom(String userId) {
            resetAVParticipantProfile(userId, true);
        }

        @Override
        public void onRemoteUserLeaveRoom(String userId, int reason) {
            resetAVParticipantProfile(userId, false);
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
            if (participants == null) {
                return;
            }
            for (ParticipantProfile profile : participants) {
                if (profile.userId.equals(userId)) {
                    profile.videoMuted = !available;
                    break;
                }
            }
        }

        @Override
        public void onUserAudioAvailable(String userId, boolean available) {
            if (participants == null) {
                return;
            }
            for (ParticipantProfile profile : participants) {
                if (profile.userId.equals(userId)) {
                    profile.audioMuted = !available;
                    break;
                }
            }
            if (callBack != null) {
                callBack.didReportAudioVolume(userId,0);
            }
        }

        @Override
        public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
            if (callBack == null) {
                return;
            }
            for (TRTCCloudDef.TRTCVolumeInfo info : userVolumes) {
                callBack.didReportAudioVolume(info.userId == null ? userId : info.userId, info.volume);
            }
        }

        @Override
        public void onUserSubStreamAvailable(String s, boolean b) {
            super.onUserSubStreamAvailable(s, b);
            resetAVParticipantProfile(s, b);
        }
    };
}
