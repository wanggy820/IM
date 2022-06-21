//
//  WFCCTextMessageContent.m
//  WFChatClient
//
//  Created by heavyrain on 2017/8/16.
//  Copyright © 2017年 WildFireChat. All rights reserved.
//

#import "WFCCRecallMessageContent.h"
#import "WFCCIMService.h"
#import "WFCCNetworkService.h"
#import "Common.h"


@implementation WFCCRecallMessageContent
- (WFCCMessagePayload *)encode {
    //注意：在proto层收到撤回命令或主动撤回成功会直接更新被撤回的消息，如果修改encode&decode，需要同步修改
    WFCCMessagePayload *payload = [super encode];
    payload.contentType = [self.class getContentType];
    payload.content = self.operatorId;
    payload.binaryContent = [[[NSNumber numberWithLongLong:self.messageUid] stringValue] dataUsingEncoding:NSUTF8StringEncoding];
    return payload;
}

- (void)decode:(WFCCMessagePayload *)payload {
    [super decode:payload];
    //注意：在proto层收到撤回命令或主动撤回成功会直接更新被撤回的消息，如果修改encode&decode，需要同步修改
    self.operatorId = payload.content;
    self.messageUid = [[[NSString alloc] initWithData:payload.binaryContent encoding:NSUTF8StringEncoding] longLongValue];
    if (self.extra.length) {
        NSError *__error = nil;
        NSDictionary *dictionary = [NSJSONSerialization JSONObjectWithData:[payload.extra dataUsingEncoding:NSUTF8StringEncoding]
                                                                   options:kNilOptions
                                                                     error:&__error];
        if (!__error) {
            self.originalSender = dictionary[@"s"];
            self.originalContentType = [dictionary[@"t"] intValue];
            self.originalSearchableContent = dictionary[@"sc"];
            self.originalContent = dictionary[@"c"];
            self.originalExtra = dictionary[@"e"];
            self.originalMessageTimestamp = [dictionary[@"ts"] longLongValue];
        }
    }
}

+ (int)getContentType {
    return MESSAGE_CONTENT_TYPE_RECALL;
}

+ (int)getContentFlags {
    return WFCCPersistFlag_PERSIST;
}


+ (void)load {
    [[WFCCIMService sharedWFCIMService] registerMessageContent:self];
}

- (NSString *)formatNotification:(WFCCMessage *)message {
    return [self digest:message];
}


- (NSString *)digest:(WFCCMessage *)message {
    if ([self.operatorId isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
        return WFCCString(@"你撤回了一条消息");
    } else {
        WFCCUserInfo *userInfo = [[WFCCIMService sharedWFCIMService] getUserInfo:self.operatorId refresh:NO];
        if (userInfo.friendAlias.length) {
            return [NSString stringWithFormat:WFCCString(@"%@撤回了一条消息"), userInfo.friendAlias];
        }
        if (userInfo.displayName != nil) {
            return [NSString stringWithFormat:WFCCString(@"%@撤回了一条消息"), userInfo.displayName];
        }
        return [NSString stringWithFormat:WFCCString(@"%@撤回了一条消息"), self.operatorId];
    }
}
@end
