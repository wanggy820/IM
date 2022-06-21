//
//  TJIMSDK.m
//  WFChatUIKit
//
//  Created by XiangWei on 2021/4/10.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import "TJIMSDK.h"
#import "GenerateTestUserSig.h"
#import "WFCUVideoViewController.h"
#import "WFCUMultiVideoViewController.h"
#import <WFChatUIKit/UIViewController+URLRouter.h>
#import <WFCUFloatingWindow.h>
#import <TJIMSDK+AVEngine.h>
#import "TJIMSDK+AudioPlayer.h"
#import "UIView+PlayerView.h"

#define kTJAVCallTimeOut  60
@interface TJIMSDK()

@property (nonatomic, assign) UInt32 sdkAppId;
@property (nonatomic, copy) NSString *userId;
@property (nonatomic, copy) NSString *userSig;

@end

@implementation TJIMSDK

+ (instancetype)shareSDK {
    static TJIMSDK *sdk = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sdk = [[TJIMSDK alloc] init];
        [[WFCCNetworkService sharedInstance] addReceiveMessageFilter:(id<ReceiveMessageFilter>)sdk];
    });
    return sdk;
}

/**
 *  设置sdkAppId，以便您能进一步接入IM SDK
 */
+ (void)initWithAppId:(UInt32)sdkAppId {
    [TJIMSDK shareSDK].sdkAppId = sdkAppId;
    V2TIMSDKConfig *config = [[V2TIMSDKConfig alloc] init];
    config.logLevel = V2TIM_LOG_NONE;
    [[V2TIMManager sharedInstance] initSDK:(int)sdkAppId config:config listener:nil];
    [TRTCCloud setLogLevel:TRTCLogLevelNone];
}

/**
 *  登录腾讯im
 */
+ (void)login:(NSString *)userID complete:(nonnull void (^)(NSError * _Nullable))complete {
    if ([[V2TIMManager sharedInstance] getLoginStatus] == V2TIM_STATUS_LOGINED) {
        if (complete) {
            complete(nil);
        }
        return;
    }
    NSString *userSig = [GenerateTestUserSig genTestUserSig:userID];
    [TJIMSDK shareSDK].userId = userID;
    [TJIMSDK shareSDK].userSig = userSig;
    [[V2TIMManager sharedInstance] login:userID userSig:userSig succ:^{
        id delegate = UIApplication.sharedApplication.delegate;
        [TJIMSDK sendeDeviceToken:[delegate valueForKey:@"deviceToken"]];
        if (complete) {
            complete(nil);
        }
    } fail:^(int code, NSString *desc) {
        if (complete) {
            complete([NSError errorWithDomain:desc code:code userInfo:nil]);
        }
    }];
}

+ (BOOL)isLogin {
    NSString *savedToken = [[NSUserDefaults standardUserDefaults] stringForKey:@"savedToken"];
    NSString *savedUserId = [[NSUserDefaults standardUserDefaults] stringForKey:@"savedUserId"];
    if (savedToken.length > 0 && savedUserId.length > 0) {
        return YES;
    }
    return NO;
}

+ (void)sendeDeviceToken:(NSData *)deviceToken {
    if (!deviceToken || [[V2TIMManager sharedInstance] getLoginStatus] != V2TIM_STATUS_LOGINED) {
        return;
    }
    
    V2TIMAPNSConfig *confg = [[V2TIMAPNSConfig alloc] init];
    confg.businessID = 26758;
    confg.token = deviceToken;
    [[V2TIMManager sharedInstance] setAPNS:confg succ:^{
         NSLog(@"-----> 设置 APNS 成功");
    } fail:^(int code, NSString *msg) {
         NSLog(@"-----> 设置 APNS 失败");
    }];
}

//发起对话
- (void)startCallWithConversation:(WFCCConversation *)conversation targets:(NSArray<NSString *> *)targetIds audioOnly:(BOOL)audioOnly {
    self.frontCamera = YES;
    self.audioMuted = NO;
    self.videoMuted = NO;
    self.handsFreeOn = YES;
    
#warning mark ---- 需要服务器下发roomId ----
    NSInteger roomId = self.userId.hash%1111111+1;
    self.content = [[WFCCCallStartMessageContent alloc] init];
    self.content.roomId = roomId;
    self.content.audioOnly = audioOnly;
    self.content.targetIds = targetIds;
    self.content.connectTime = [[NSDate date] timeIntervalSince1970] * 1000;
    
    self.message = [[WFCCIMService sharedWFCIMService] send:conversation content:self.content toUsers:self.content.targetIds expireDuration:0 success:^(long long messageUid, long long timestamp) {
        NSLog(@"消息发送成功:%lld", messageUid);
    } error:^(int error_code) {
        NSLog(@"消息发送失败：%d", error_code);
        self.state = kTJAVEngineStateIdle;
    }];
    [self initAVParticipantProfile];
    self.state = kTJAVEngineStateOutgoing;
}

- (void)enterRoom {
    if (self.state == kTJAVEngineStateConnected) {
        return;
    }
    
    TRTCParams *param = [[TRTCParams alloc] init];
    param.sdkAppId = self.sdkAppId;
    param.userId = self.userId;
    param.userSig = self.userSig;
    param.roomId = (UInt32)self.content.roomId;
    
    if (!self.content.audioOnly) {
        TXBeautyManager *beauty = [[TRTCCloud sharedInstance] getBeautyManager];
        [beauty setBeautyStyle:TXBeautyStyleNature];
        [beauty setBeautyLevel:6];
        TRTCVideoEncParam *videoEncParam = [[TRTCVideoEncParam alloc] init];
        videoEncParam.videoResolution = TRTCVideoResolution_960_540;
        videoEncParam.videoFps = 15;
        videoEncParam.videoBitrate = 1000;
        videoEncParam.resMode = TRTCVideoResolutionModePortrait;
        videoEncParam.enableAdjustRes = true;
        [[TRTCCloud sharedInstance] setVideoEncoderParam:videoEncParam];
    }
    [[TRTCCloud sharedInstance] setDelegate:(id<TRTCCloudDelegate>)self];
    [[TRTCCloud sharedInstance] enterRoom:param appScene:TRTCAppSceneVideoCall];
    [[TRTCCloud sharedInstance] startLocalAudio:TRTCAudioQualitySpeech];
    [[TRTCCloud sharedInstance] enableAudioVolumeEvaluation:300];
    
    [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
    
    self.state = kTJAVEngineStateConnecting;
}

- (void)quitRoom {
    UIViewController *vc = [UIViewController currentViewController];
    if (self.message.conversation.type == Single_Type && [vc isKindOfClass:WFCUVideoViewController.class]) {
        [vc dismissViewControllerAnimated:YES completion:nil];
    } else if (self.message.conversation.type == Group_Type && [vc isKindOfClass:WFCUMultiVideoViewController.class]) {
        [vc dismissViewControllerAnimated:YES completion:nil];
    }
    
    [WFCUFloatingWindow stopCallFloatingWindow];
    
    [[TRTCCloud sharedInstance] stopLocalAudio];
    [[TRTCCloud sharedInstance] stopLocalPreview];
    [[TRTCCloud sharedInstance] exitRoom];
    [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
    
    self.audioMuted = NO;
    self.videoMuted = NO;
    self.handsFreeOn = YES;
    self.frontCamera = YES;
    self.message = nil;
    self.content = nil;
    self.myProfile = nil;
    for (TJAVParticipantProfile *profile in self.participants) {
        profile.userId.playerView = nil;
    }
    self.userId.playerView = nil;
    self.participants = nil;
    self.state = kTJAVEngineStateIdle;
}

- (void)startRemoteView:(NSString *)userId view:(UIView *)view {
    if (self.state == kTJAVEngineStateIdle || !view || !userId.length) {
        return;
    }
    if (userId.playerView == view && userId.playerView.isPlaying && [view.userId isEqualToString:userId]) {
        return;
    }
    
    if (userId.playerView.isPlaying) {
        [[TRTCCloud sharedInstance] updateRemoteView:view streamType:TRTCVideoStreamTypeBig forUser:userId];
    } else {
        [[TRTCCloud sharedInstance] startRemoteView:userId streamType:TRTCVideoStreamTypeBig view:view];
    }
    userId.playerView = view;
    view.isPlaying = YES;
    view.userId = userId;
}

- (void)stopRemoteView:(NSString *)userId {
    [[TRTCCloud sharedInstance] stopRemoteView:userId streamType:TRTCVideoStreamTypeBig];
    userId.playerView.isPlaying = NO;
}

- (void)startLocalPreview:(UIView *)view {
    if (self.state == kTJAVEngineStateIdle || !view) {
        return;
    }
    if (self.userId.playerView == view && self.userId.playerView.isPlaying && [view.userId isEqualToString:self.userId]) {
        return;
    }
    
    if (self.userId.playerView.isPlaying) {
        [[TRTCCloud sharedInstance] updateLocalView:view];
    } else {
        [[TRTCCloud sharedInstance] startLocalPreview:self.frontCamera view:view];
    }
    self.userId.playerView = view;
    view.isPlaying = YES;
    view.userId = self.userId;
}

- (void)stopLocalPreview {
    [[TRTCCloud sharedInstance] stopLocalPreview];
    self.userId.playerView.isPlaying = NO;
}

- (void)switchCamera {
    if (self.state == kTJAVEngineStateIdle) {
        return;
    }
    self.frontCamera = !self.frontCamera;
    [[[TRTCCloud sharedInstance] getDeviceManager] switchCamera:self.frontCamera];
}

- (void)setHandsFreeOn:(BOOL)handsFreeOn {
    if (_handsFreeOn == handsFreeOn) {
        return;
    }
    _handsFreeOn = handsFreeOn;
    [[TRTCCloud sharedInstance] setAudioRoute:handsFreeOn ? TRTCAudioModeSpeakerphone : TRTCAudioModeEarpiece];
}

- (void)setAudioMuted:(BOOL)audioMuted {
    if (_audioMuted == audioMuted) {
        return;
    }
    _audioMuted = audioMuted;
    [[TRTCCloud sharedInstance] muteLocalAudio:audioMuted];
    self.myProfile.audioMuted = audioMuted;
    [[NSNotificationCenter defaultCenter] postNotificationName:@"wfavVolumeUpdated" object:self.userId userInfo:@{@"volume": @(0)}];
}

- (void)setVideoMuted:(BOOL)videoMuted {
    if (_videoMuted == videoMuted) {
        return;
    }
    _videoMuted = videoMuted;
//    mute YES：暂停；NO：恢复
    [[TRTCCloud sharedInstance] muteLocalVideo:videoMuted];
    
    self.myProfile.videoMuted = videoMuted;
}


/**
 接听通话
 */
- (void)answerCall {
    NSLog(@"接听通话");
    [self enterRoom];
    //发消息
    TJCallAnswerTMessageContent *answer = [TJCallAnswerTMessageContent new];
    answer.roomId = self.content.roomId;
    answer.audioOnly = self.content.audioOnly;
    answer.inviteMsgUid = self.message.messageUid;
    
    NSMutableArray *targetIds = [NSMutableArray array];
    for (TJAVParticipantProfile *profile in self.participants) {
        [targetIds addObject:profile.userId];
    }
    
    [[WFCCIMService sharedWFCIMService] send:self.message.conversation content:answer toUsers:targetIds expireDuration:0  success:^(long long messageUid, long long timestamp) {
            
    } error:^(int error_code) {
            
    }];
    
    //设置消息已读
    [[WFCCIMService sharedWFCIMService] clearUnreadStatus:self.message.conversation];
}

/**
 挂断通话
 */
- (void)endCallWithReason:(TJAVCallEndReason)reason {
    if (self.state == kTJAVEngineStateIdle) {
        return;
    }
    NSLog(@"挂断电话：%ld", (long)reason);
    if (reason == kTJAVCallEndReasonRemoteHangup) {
        self.content.status = kTJAVCallEndReasonHangup;
    } else if (reason == kTJAVCallEndReasonTimeout) {
        self.content.status = kTJAVCallEndReasonRemoteTimeout;
    } else {
        self.content.status = (int)reason;
    }

    if (self.state == kTJAVEngineStateConnected) {
        self.content.endTime = [[NSDate date] timeIntervalSince1970] * 1000;
    }
    
    [[WFCCIMService sharedWFCIMService] updateMessage:self.message.messageId content:self.content];
    //发消息
    TJCallByeMessageContent *byeMessage = [[TJCallByeMessageContent alloc] init];
    byeMessage.roomId = self.content.roomId;
    byeMessage.inviteMsgUid = self.message.messageUid;
    byeMessage.endReason = reason;
    byeMessage.audioOnly = self.content.audioOnly;
    NSMutableArray *targetIds = [NSMutableArray array];
    for (TJAVParticipantProfile *profile in self.participants) {
        [targetIds addObject:profile.userId];
    }
    [[WFCCIMService sharedWFCIMService] send:self.message.conversation content:byeMessage toUsers:targetIds expireDuration:0 success:^(long long messageUid, long long timestamp) {
        
    } error:^(int error_code) {
            
    }];

    [self quitRoom];
}

- (void)setState:(TJAVEngineState)state {
    _state = state;
    self.myProfile.state = state;
    if ([self.delegate respondsToSelector:@selector(didChangeState:)]) {
        [self.delegate didChangeState:state];
    }
    if (state == kTJAVEngineStateIncomming) {
        [self shouldStartRing:YES];
    } else if (state == kTJAVEngineStateOutgoing) {
        [self shouldStartRing:NO];
    } else {
        [self shouldStopRing];
    }
    
    if (state == kTJAVEngineStateOutgoing || state == kTJAVEngineStateIncomming) {
        [self performSelector:@selector(endCallWithReason:) withObject:@(kTJAVCallEndReasonTimeout) afterDelay:kTJAVCallTimeOut];
    } else {
        [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(endCallWithReason:) object:@(kTJAVCallEndReasonTimeout)];
    }
}

@end
