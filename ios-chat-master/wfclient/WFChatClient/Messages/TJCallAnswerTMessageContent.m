//
//  TJCallAnswerTMessageContent.m
//  WFChatClient
//
//  Created by wanggy820 on 2021/6/8.
//  Copyright © 2021 WildFireChat. All rights reserved.
//

#import "TJCallAnswerTMessageContent.h"

@implementation TJCallAnswerTMessageContent

- (WFCCMessagePayload *)encode {
    WFCCMessagePayload *payload = [super encode];
    payload.contentType = [self.class getContentType];
    payload.content = @(self.roomId).stringValue;
    
    NSMutableDictionary *dataDict = [NSMutableDictionary dictionary];
    if (self.inviteMsgUid) {
        [dataDict setObject:@(self.inviteMsgUid) forKey:@"i"];
    }
    if (self.audioOnly) {
        [dataDict setObject:@(1) forKey:@"a"];
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
    }
}

+ (int)getContentType {
    return VOIP_CONTENT_TYPE_ACCEPT_T;
}

+ (int)getContentFlags {
    return WFCCPersistFlag_TRANSPARENT;
}

+ (void)load {
    [[WFCCIMService sharedWFCIMService] registerMessageContent:self];
}

- (NSString *)digest:(WFCCMessage *)message {
    return @"[通话中...]";
}

@end
