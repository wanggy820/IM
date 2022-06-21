//
//  UIView+PlayerView.h
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/7/2.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIView (PlayerView)

@property (nonatomic, copy) NSString *userId;
@property (nonatomic, assign) BOOL isPlaying;

@end

@interface NSString (PlayerView)

@property (nonatomic, strong) UIView *_Nullable playerView;

@end

NS_ASSUME_NONNULL_END
