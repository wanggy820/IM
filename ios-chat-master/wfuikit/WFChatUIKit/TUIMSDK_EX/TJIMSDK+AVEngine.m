//
//  TJIMSDK+AVEngine.m
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/6/8.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import "TJIMSDK+AVEngine.h"

@interface TJIMSDK (AVEngine)

@end

@implementation TJIMSDK (AVEngine)

+ (BOOL)isBluetoothSpeaker {
    AVAudioSessionRouteDescription* route = [[AVAudioSession sharedInstance] currentRoute];
    for (AVAudioSessionPortDescription* desc in [route outputs]) {
        if ([desc.portType isEqualToString:AVAudioSessionPortBluetoothA2DP]) {
            return YES;
        }
    }
    return NO;
}

+ (BOOL)isHeadsetPluggedIn {
    AVAudioSessionRouteDescription* route = [[AVAudioSession sharedInstance] currentRoute];
    for (AVAudioSessionPortDescription* desc in [route outputs]) {
        if ([[desc portType] isEqualToString:AVAudioSessionPortHeadphones]) {
            return YES;
        }
    }
    return NO;
}


#pragma mark  TRTCCloudDelegate
- (void)onEnterRoom:(NSInteger)result {
    if (result < 0) {
        [self endCallWithReason:kTJAVCallEndReasonRemoteNetworkError];
    } else {
        self.content.connectTime = [[NSDate date] timeIntervalSince1970] * 1000;
        self.myProfile.startTime = self.content.connectTime;
        self.state = kTJAVEngineStateConnected;
    }
}

- (void)onError:(TXLiteAVError)errCode errMsg:(nullable NSString *)errMsg
        extInfo:(nullable NSDictionary*)extInfo {
    [self endCallWithReason:kTJAVCallEndReasonRemoteNetworkError];
}

- (void)onRemoteUserEnterRoom:(NSString *)userID {
    // C2C curInvitingList 不要移除 userID，如果是自己邀请的对方，这里移除后，最后发结束信令的时候找不到人
    NSLog(@"%s", __func__);
    [self resetAVParticipantProfile:userID isEnterRoom:YES];
}

- (void)onRemoteUserLeaveRoom:(NSString *)userId reason:(NSInteger)reason {
    NSLog(@"%s", __func__);
    [self resetAVParticipantProfile:userId isEnterRoom:NO];
}

- (void)onExitRoom:(NSInteger)reason {
    NSLog(@"%s", __func__);
    [self endCallWithReason:kTJAVCallEndReasonRemoteNetworkError];
}

- (void)onUserVoiceVolume:(NSArray<TRTCVolumeInfo *> *)userVolumes totalVolume:(NSInteger)totalVolume {
    for (TRTCVolumeInfo *info in userVolumes) {
        [[NSNotificationCenter defaultCenter] postNotificationName:@"wfavVolumeUpdated" object:info.userId?:[WFCCNetworkService sharedInstance].userId userInfo:@{@"volume": @(info.volume)}];
    }
}

- (void)onUserAudioAvailable:(NSString *)userId available:(BOOL)available {
    for (TJAVParticipantProfile *profile in self.participants) {
        if ([profile.userId isEqualToString:userId]) {
            profile.audioMuted = !available;
            break;
        }
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:@"wfavVolumeUpdated" object:userId userInfo:@{@"volume": @(0)}];
}

- (void)onUserVideoAvailable:(NSString *)userId available:(BOOL)available {
    for (TJAVParticipantProfile *profile in self.participants) {
        if ([profile.userId isEqualToString:userId]) {
            profile.videoMuted = !available;
            break;
        }
    }
}

/**
 是否拦截收到的消息

 @param message 消息
 @return 是否拦截，如果拦截该消息，则ReceiveMessageDelegate回调不会再收到此消息
 */
- (BOOL)onReceiveMessage:(WFCCMessage *)message {
    if ([message.content isKindOfClass:WFCCCallStartMessageContent.class]) {
        WFCCCallStartMessageContent *content = (WFCCCallStartMessageContent *)message.content;
        if (self.state != kTJAVEngineStateIdle) {
            //忙线中
            if (self.content.roomId == content.roomId){
                if (message.conversation.type == Group_Type) {
                    //新邀请的人
                    for (NSString *userId in content.targetIds) {
                        if (![self containsUserId:userId]) {
                            [self resetAVParticipantProfile:userId isEnterRoom:YES];
                        }
                    }
                }
                
                return YES;
            }
            WFCCConversation *conversation = [WFCCConversation conversationWithType:message.conversation.type target:message.fromUser line:message.conversation.line];
            TJCallByeMessageContent *byeMessage = [TJCallByeMessageContent new];
            byeMessage.endReason = kTJAVCallEndReasonRemoteBusy;
            byeMessage.inviteMsgUid = message.messageUid;
            byeMessage.roomId = content.roomId;
            byeMessage.audioOnly = content.audioOnly;
            
            NSMutableArray *toUsers = [NSMutableArray arrayWithArray:message.toUsers];
            [toUsers removeObject:self.myProfile.userId];
            if (![toUsers containsObject:message.fromUser]) {
                [toUsers addObject:message.fromUser];
            }
            [[WFCCIMService sharedWFCIMService] send:conversation content:byeMessage toUsers:toUsers expireDuration:0 success:nil error:nil];
            return YES;
        }
        self.frontCamera = YES;
        self.audioMuted = NO;
        self.videoMuted = NO;
        self.handsFreeOn = YES;
        self.state = kTJAVEngineStateIncomming;
        self.message = message;
        self.content = content;
        [self initAVParticipantProfile];
        
        //收到来电通知后等待200毫秒，检查message有效后再弹出通知。原因是当当前用户不在线时如果有人来电并挂断，当前用户再连接后，会出现先弹来电界面，再消失的画面。
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (self.state != kTJAVEngineStateIncomming) {
                return;
            }
            UIViewController *videoVC;
            if (message.conversation.type == Group_Type) {
                videoVC = [[WFCUMultiVideoViewController alloc] initWithMessage:message];
            } else {
                videoVC = [[WFCUVideoViewController alloc] initWithMessage:message];
            }
            
            videoVC.modalPresentationStyle = UIModalPresentationFullScreen;
            [[UIViewController currentNavigationController] presentViewController:videoVC animated:YES completion:nil];
        });

        return YES;
    } else if ([message.content isKindOfClass:TJCallByeMessageContent.class]) {
        TJCallByeMessageContent *content = (TJCallByeMessageContent *)message.content;
        if (self.content.roomId == content.roomId) {
            //单聊或者群聊发起者结束视频
            if (self.message.conversation.type == Single_Type || [message.fromUser isEqualToString:self.message.fromUser]) {
                self.content.status = (int)content.endReason;
                if (self.state == kTJAVEngineStateConnected) {
                    self.content.endTime = [[NSDate date] timeIntervalSince1970] * 1000;
                }
                [[WFCCIMService sharedWFCIMService] updateMessage:self.message.messageId content:self.content];
                [self quitRoom];
            } else {
                //群聊有人退出，刷新ui
                [self resetAVParticipantProfile:message.fromUser isEnterRoom:NO];
            }
        }
        return YES;
    } else if ([message.content isKindOfClass:TJCallAnswerTMessageContent.class]) {
        if (self.state == kTJAVEngineStateIdle) {
            [self endCallWithReason:kTJAVCallEndReasonSignalError];
            return YES;
        }
        TJCallAnswerTMessageContent *content = (TJCallAnswerTMessageContent *)message.content;
        if (self.content.roomId == content.roomId) {
            if (self.message.conversation.type == Single_Type) {
                [self enterRoom];
                self.content.audioOnly = content.audioOnly;
            } else {
                //由于只能收到别人的消息
                [self resetAVParticipantProfile:message.fromUser isEnterRoom:YES];
                if (!self.myProfile.audience) {
                    [self enterRoom];
                }
            }

            if ([self.delegate respondsToSelector:@selector(didChangeState:)]) {
                [self.delegate didChangeState:self.state];
            }
        }
        return YES;
    }
    return NO;
}

- (BOOL)containsUserId:(NSString *)userId {
    for (TJAVParticipantProfile *profile in self.participants) {
        if ([profile.userId isEqualToString:userId]) {
            return YES;
        }
    }
    return NO;
}

- (void)resetAVParticipantProfile:(NSString *)userId isEnterRoom:(BOOL)isEnter {
    NSMutableArray *participants = [NSMutableArray arrayWithArray:self.participants];
    if (!isEnter) {
        for (TJAVParticipantProfile *profile in self.participants) {
            if ([userId isEqualToString:profile.userId]) {
                [participants removeObject:profile];
                break;
            }
        }
    } else if (![self containsUserId:userId] && ![self.message.fromUser isEqualToString:userId]){
        TJAVParticipantProfile *profile = [TJAVParticipantProfile new];
        profile.userId = userId;
        profile.state = kTJAVEngineStateIncomming;
        profile.startTime = [[NSDate date] timeIntervalSince1970] * 1000;
        profile.audience = YES;
        [participants addObject:profile];
    } else {//自己不用管
        for (TJAVParticipantProfile *profile in self.participants) {
            if ([profile.userId isEqualToString:userId]) {
                profile.state = kTJAVEngineStateConnected;
                profile.startTime = [[NSDate date] timeIntervalSince1970] * 1000;
                break;
            }
        }
    }
    self.participants = participants;
    
    if ([self.delegate respondsToSelector:@selector(didReceiveParticipantProfile:isEnterRoom:)]) {
        [self.delegate didReceiveParticipantProfile:userId isEnterRoom:isEnter];
    }
}


- (void)initAVParticipantProfile {
    if (self.myProfile) {
        return;
    }
    self.myProfile = [TJAVParticipantProfile new];
    self.myProfile.userId = [WFCCNetworkService sharedInstance].userId;
    self.myProfile.state = self.state;
    self.myProfile.audience = ![self.myProfile.userId isEqualToString:self.message.fromUser];
    self.myProfile.startTime = self.content.connectTime;
    
    NSMutableArray *participants = [NSMutableArray array];
    for (NSString *userId in self.content.targetIds) {
        if ([userId isEqualToString:self.myProfile.userId]) {
            continue;
        }
        TJAVParticipantProfile *profile = [TJAVParticipantProfile new];
        profile.userId = userId;
        profile.state = kTJAVEngineStateIncomming;
        profile.audience = YES;
        [participants addObject:profile];
    }
    //自己是观众,需要把邀请人加入进去
    if (self.myProfile.audience && ![self.content.targetIds containsObject:self.message.fromUser]) {
        TJAVParticipantProfile *profile = [TJAVParticipantProfile new];
        profile.userId = self.message.fromUser;
        profile.state = kTJAVEngineStateIncomming;
        profile.audience = NO;
        [participants addObject:profile];
    }
    self.participants = participants.copy;
}

- (void)inviteNewParticipants:(NSArray<NSString *>*)targetIds {
    NSMutableArray *array = [NSMutableArray arrayWithArray:targetIds];
    for (TJAVParticipantProfile *profile in self.participants) {
        [array addObject:profile.userId];
    }
    self.content.targetIds = array;
    //重新发送消息给新加入的人员
    self.message = [[WFCCIMService sharedWFCIMService] send:self.message.conversation content:self.content toUsers:array expireDuration:0 success:^(long long messageUid, long long timestamp) {
        NSLog(@"消息发送成功:%lld", messageUid);
    } error:^(int error_code) {
        NSLog(@"消息发送失败：%d", error_code);
    }];
    NSMutableArray *participants = [NSMutableArray arrayWithArray:self.participants];
    for (NSString *userId in targetIds) {
        TJAVParticipantProfile *profile = [TJAVParticipantProfile new];
        profile.userId = userId;
        profile.state = kTJAVEngineStateIncomming;
        profile.audience = YES;
        [participants addObject:profile];
        
        if ([self.delegate respondsToSelector:@selector(didReceiveParticipantProfile:isEnterRoom:)]) {
            [self.delegate didReceiveParticipantProfile:profile.userId isEnterRoom:YES];
        }
    }
    self.participants = participants.copy;
}

@end
