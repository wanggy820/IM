//
//  V2TIMManager+Badge.m
//  老板云IM
//
//  Created by wanggy820 on 2021/5/12.
//  Copyright © 2021 WildFireChat. All rights reserved.
//

#import "V2TIMManager+Badge.h"
#import <objc/runtime.h>
#import <UIKit/UIKit.h>

@implementation V2TIMManager (badge)

+ (void)load {
    //不使用腾讯的未读消息计数
    Method originMethod = class_getInstanceMethod(self, NSSelectorFromString(@"enterBackground"));
    Method myMethod = class_getInstanceMethod(self, @selector(tj_enterBackground));
    method_exchangeImplementations(originMethod, myMethod);
}

- (void)tj_enterBackground {
    [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
                    
    }];
    TIMBackgroundParam *p = [[TIMBackgroundParam alloc] init];
    [[TIMManager sharedInstance] doBackground:p succ:nil fail:nil];
}

@end
