//
//  TJAVParticipantProfile.h
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/6/9.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "TJIMSDKHeader.h"

NS_ASSUME_NONNULL_BEGIN

@interface TJAVParticipantProfile : NSObject

@property(nonatomic, strong) NSString *userId;
@property(nonatomic, assign) long long startTime;
@property(nonatomic, assign) TJAVEngineState state;

@property(nonatomic, assign) BOOL audioMuted;
@property(nonatomic, assign) BOOL videoMuted;
@property(nonatomic, assign) BOOL audience;//观众/被邀请者

@end

NS_ASSUME_NONNULL_END
