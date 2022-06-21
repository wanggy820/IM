//
//  TJIMSDK+AVEngine.h
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/6/8.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import <WFChatUIKit/WFChatUIKit.h>

NS_ASSUME_NONNULL_BEGIN



@interface TJIMSDK (AVEngine)<ReceiveMessageFilter, TRTCCloudDelegate>


/**
 是否是蓝牙设备连接

 @return 是否是蓝牙设备连接
 */
+ (BOOL)isBluetoothSpeaker;

/**
 是否是耳机连接

 @return 是否是耳机连接
 */
+ (BOOL)isHeadsetPluggedIn;

- (void)initAVParticipantProfile;
- (void)inviteNewParticipants:(NSArray<NSString *>*)targetIds;

@end

NS_ASSUME_NONNULL_END
