//
//  TJCallByeMessageContent.m
//  WFChatClient
//
//  Created by wanggy820 on 2021/6/8.
//  Copyright © 2021 WildFireChat. All rights reserved.
//

#import "TJCallByeMessageContent.h"

@implementation TJCallByeMessageContent

- (WFCCMessagePayload *)encode {
    WFCCMessagePayload *payload = [super encode];
    payload.contentType = [self.class getContentType];
    payload.content = @(self.roomId).stringValue;
    
    NSMutableDictionary *dataDict = [NSMutableDictionary dictionary];
    if (self.inviteMsgUid > 0) {
        [dataDict setObject:@(self.inviteMsgUid) forKey:@"i"];
    }
    if (self.audioOnly) {
        [dataDict setObject:@(1) forKey:@"a"];
    }
    if (self.endReason > 0) {
        [dataDict setObject:@(self.endReason) forKey:@"e"];
    }
    payload.binaryContent = [NSJSONSerialization dataWithJSONObject:dataDict options:kNilOptions error:nil];
    return payload;
}

- (void)decode:(WFCCMessagePayload *)payload {
    [super decode:payload];
    self.roomId = payload.content.integerValue;
    NSError *__error = nil;
    NSDictionary *dictionary = [NSJSONSerialization JSONObjectWithData:payload.binaryContent
                                                               options:kNilOptions
                                                                 error:&__error];
    if (!__error) {
        self.inviteMsgUid = [dictionary[@"i"] longLongValue];
        self.audioOnly = [dictionary[@"a"] boolValue];
        self.endReason = [dictionary[@"e"] intValue];
    }
}

+ (int)getContentType {
    return VOIP_CONTENT_TYPE_END;
}

+ (int)getContentFlags {
    return WFCCPersistFlag_NOT_PERSIST;
}

+ (void)load {
    [[WFCCIMService sharedWFCIMService] registerMessageContent:self];
}

- (NSString *)digest:(WFCCMessage *)message {
    if (self.audioOnly) {
        return @"[语音对话]";
    }
    return @"[视频对话]";
}

@end
