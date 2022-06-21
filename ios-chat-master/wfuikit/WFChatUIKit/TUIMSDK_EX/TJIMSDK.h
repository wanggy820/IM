//
//  TJIMSDK.h
//  WFChatUIKit
//
//  Created by XiangWei on 2021/4/10.
//  Copyright © 2021 Tom Lee. All rights reserved.
//


#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <WFChatClient/WFCChatClient.h>
#import <ImSDK/ImSDK.h>
#import <TXLiteAVSDK_TRTC/TXLiteAVSDK.h>
#import "TJAVParticipantProfile.h"

NS_ASSUME_NONNULL_BEGIN


@class TJAVParticipantProfile;
@interface TJIMSDK : NSObject

@property(nonatomic, assign) BOOL audioMuted;//是否静音
@property(nonatomic, assign) BOOL videoMuted;//是否关闭视频
@property(nonatomic, assign) BOOL handsFreeOn;//免提状态
@property(nonatomic, assign) BOOL frontCamera;//前置摄像头

@property (nonatomic, strong) WFCCMessage * _Nullable message;
@property (nonatomic, strong) WFCCCallStartMessageContent *_Nullable content;
@property (nonatomic, assign) TJAVEngineState state;

@property (nonatomic, weak) id<TJAVCallMessageDelegate> delegate;

/**
通话成员（不包含自己）
*/
@property(nonatomic, copy) NSArray<TJAVParticipantProfile *> * _Nullable participants;
@property(nonatomic, strong) TJAVParticipantProfile *_Nullable myProfile;

+ (instancetype)shareSDK;

/**
 *  设置sdkAppId，以便您能进一步接入IM SDK
 */
+ (void)initWithAppId:(UInt32)sdkAppId;

/**
 *  登录
 */
+ (void)login:(NSString *)userID complete:(void (^)(NSError *_Nullable error))complete;

+ (BOOL)isLogin;
+ (void)sendeDeviceToken:(NSData *)deviceToken;


/// 发起对话
/// @param conversation 会话
/// @param targetIds 目标人userId
/// @param audioOnly 是否为音频
- (void)startCallWithConversation:(WFCCConversation *)conversation targets:(NSArray<NSString *> *)targetIds audioOnly:(BOOL)audioOnly;
///进入房间
- (void)enterRoom;

///退出房间
- (void)quitRoom;

///开启远程用户视频渲染
- (void)startRemoteView:(NSString *)userId view:(UIView *)view;

///关闭远程用户视频渲染
- (void)stopRemoteView:(NSString *)userId;

///打开摄像头
- (void)startLocalPreview:(UIView *)view;

///关闭摄像头
- (void)stopLocalPreview;

///切换摄像头
- (void)switchCamera;

/**
 接听通话
 */
- (void)answerCall;

/**
 挂断通话
 */
- (void)endCallWithReason:(TJAVCallEndReason)reason;


@end



NS_ASSUME_NONNULL_END
