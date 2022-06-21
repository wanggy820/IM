//
//  TJCallByeMessageContent.h
//  WFChatClient
//
//  Created by wanggy820 on 2021/6/8.
//  Copyright © 2021 WildFireChat. All rights reserved.
//

#import <WFChatClient/WFCChatClient.h>

NS_ASSUME_NONNULL_BEGIN

@interface TJCallByeMessageContent : WFCCMessageContent

@property (nonatomic, assign) NSInteger roomId;
@property (nonatomic, assign) NSInteger inviteMsgUid;
@property (nonatomic, assign) BOOL audioOnly;
@property (nonatomic, assign) NSInteger endReason;

@end

NS_ASSUME_NONNULL_END
