//
//  UIView+PlayerView.m
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/7/2.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import "UIView+PlayerView.h"
#import <objc/runtime.h>

@implementation UIView (PlayerView)

- (void)setUserId:(NSString *)userId {
    objc_setAssociatedObject(self, _cmd, userId, OBJC_ASSOCIATION_COPY);
}

- (NSString *)userId {
    return objc_getAssociatedObject(self, @selector(setUserId:));
}

- (void)setIsPlaying:(BOOL)isPlaying {
    objc_setAssociatedObject(self, _cmd, @(isPlaying), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)isPlaying {
    return [objc_getAssociatedObject(self, @selector(setIsPlaying:)) boolValue];
}

@end

@implementation NSString (PlayerView)

- (void)setPlayerView:(UIView *)playerView {
    objc_setAssociatedObject(self, _cmd, playerView, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (UIView *)playerView {
    return objc_getAssociatedObject(self, @selector(setPlayerView:));
}

@end
