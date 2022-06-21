//
//  TJIMSDKHeader.h
//  WFChatUIKit
//
//  Created by XiangWei on 2021/4/10.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#ifndef TJIMSDKHeader_h
#define TJIMSDKHeader_h
#import <Foundation/Foundation.h>
#import <WFChatClient/WFCChatClient.h>

/**
 通话状态

 - kTJAVEngineStateIdle: 无通话状态
 - kTJAVEngineStateOutgoing: 呼出中
 - kTJAVEngineStateIncomming: 呼入中
 - kTJAVEngineStateConnecting: 建立中
 - kTJAVEngineStateConnected: 通话中
 */
typedef NS_ENUM(NSInteger, TJAVEngineState) {
    kTJAVEngineStateIdle,
    kTJAVEngineStateOutgoing,
    kTJAVEngineStateIncomming,
    kTJAVEngineStateConnecting,
    kTJAVEngineStateConnected
};

/**
 通话结束原因
 - kTJAVCallEndReasonUnknown: 未知错误
 - kTJAVCallEndReasonBusy: 忙线
 - kTJAVCallEndReasonSignalError: 链路错误
 - kTJAVCallEndReasonHangup: 用户挂断
 - kTJAVCallEndReasonMediaError: 媒体错误
 - kTJAVCallEndReasonRemoteHangup: 对方挂断
 - kTJAVCallEndReasonOpenCameraFailure: 摄像头错误
 - kTJAVCallEndReasonTimeout: 未接听
 - kTJAVCallEndReasonAcceptByOtherClient: 被其它端接听
 - kTJAVCallEndReasonRemoteBusy: 对方忙线中
 - kTJAVCallEndReasonRemoteTimeout：对方未接听
 - kTJAVCallEndReasonRemoteNetworkError：对方网络错误
 - kTJAVCallEndReasonRoomDestroyed：会议室被销毁
 */
typedef NS_ENUM(NSInteger, TJAVCallEndReason) {
    kTJAVCallEndReasonUnknown = 0,//未接通
    kTJAVCallEndReasonBusy,
    kTJAVCallEndReasonSignalError,
    kTJAVCallEndReasonHangup,
    kTJAVCallEndReasonMediaError,
    kTJAVCallEndReasonRemoteHangup,
    kTJAVCallEndReasonOpenCameraFailure,
    kTJAVCallEndReasonTimeout,
    kTJAVCallEndReasonAcceptByOtherClient,
    kTJAVCallEndReasonAllLeft,//通话已结束
    kTJAVCallEndReasonRemoteBusy,
    kTJAVCallEndReasonRemoteTimeout,
    kTJAVCallEndReasonRemoteNetworkError,
    kTJAVCallEndReasonRoomDestroyed,
    kTJAVCallEndReasonRoomNotExist,
    kTJAVCallEndReasonRoomParticipantsFull
};


@protocol TJAVCallMessageDelegate <NSObject>

- (void)didChangeState:(TJAVEngineState)state;

@optional
- (void)didReceiveParticipantProfile:(NSString *)userId isEnterRoom:(BOOL)isEnter;
@end


/**
 * 腾讯云 SDKAppId，需要替换为您自己账号下的 SDKAppId。
 *
 * 进入腾讯云云通信[控制台](https://console.cloud.tencent.com/avc) 创建应用，即可看到 SDKAppId，
 * 它是腾讯云用于区分客户的唯一标识。
 */
static const int SDKAPPID = 1400518386;//1400501030;

/**
 *  签名过期时间，建议不要设置的过短
 *
 *  时间单位：秒
 *  默认时间：7 x 24 x 60 x 60 = 604800 = 7 天
 */
static const int EXPIRETIME = 604800;

/**
 * 计算签名用的加密密钥，获取步骤如下：
 *
 * step1. 进入腾讯云云通信[控制台](https://console.cloud.tencent.com/avc) ，如果还没有应用就创建一个，
 * step2. 单击“应用配置”进入基础配置页面，并进一步找到“帐号体系集成”部分。
 * step3. 点击“查看密钥”按钮，就可以看到计算 UserSig 使用的加密的密钥了，请将其拷贝并复制到如下的变量中
 *
 * 注意：该方案仅适用于调试Demo，正式上线前请将 UserSig 计算代码和密钥迁移到您的后台服务器上，以避免加密密钥泄露导致的流量盗用。
 * 文档：https://cloud.tencent.com/document/product/269/32688#Server
 */
#define SECRETKEY @"4d7cfcc198050009134b7e1152409688ca641bbc8dacb8132987009dfd951900"

#import "TJIMSDK.h"

#endif /* TJIMSDKHeader_h */
