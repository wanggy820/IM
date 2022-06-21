//
//  WFCUFloatingWindow.h
//  WFDemo
//
//  Created by heavyrain on 17/9/27.
//  Copyright © 2017年 WildFireChat. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <WFChatClient/WFCChatClient.h>
#import <UIKit/UIKit.h>
#import "TJIMSDK.h"

/*!
 最小化显示的悬浮窗
 */
@interface WFCUFloatingWindow : NSObject

/*!
 悬浮窗的Window
 */
@property(nonatomic, strong) UIWindow *window;

/*!
 音频通话最小化时的Button
 */
@property(nonatomic, strong) UIButton *floatingButton;

/*!
 视频通话最小化时的视频View
 */
@property(nonatomic, strong) UIView *videoView;

/*!
 当前的通话实体
 */
@property(nonatomic, strong) WFCCMessage *message;

/*!
 开启悬浮窗

 @param message  通话实体
 @param focusUserId  焦点用户Id
 @param touchedBlock 悬浮窗点击的Block
 */
+ (void)startCallFloatingWindow:(WFCCMessage *)message focusUser:(NSString *)focusUserId
              withTouchedBlock:(void (^)(WFCCMessage *message))touchedBlock;

/*!
 关闭当前悬浮窗
 */
+ (void)stopCallFloatingWindow;

@end
