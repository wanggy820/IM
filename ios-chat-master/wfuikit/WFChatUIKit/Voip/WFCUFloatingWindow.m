//
//  WFCUFloatingWindow.m
//  WFDemo
//
//  Created by heavyrain on 17/9/27.
//  Copyright © 2017年 WildFireChat. All rights reserved.
//

#import "WFCUFloatingWindow.h"
#import <CoreTelephony/CTCall.h>
#import <CoreTelephony/CTCallCenter.h>
#import <UIKit/UIKit.h>
#import "WFCUConferenceManager.h"


@interface WFCUFloatingWindow ()

@property(nonatomic, strong) NSTimer *activeTimer;
@property(nonatomic, copy) void (^touchedBlock)(WFCCMessage *message);
@property(nonatomic, strong) CTCallCenter *callCenter;
@property(nonatomic, strong) NSString *focusUserId;

@property(nonatomic, strong) WFCCCallStartMessageContent *content;
@end

static WFCUFloatingWindow *staticWindow = nil;

static NSString *kFloatingWindowPosX = @"kFloatingWindowPosX";
static NSString *kFloatingWindowPosY = @"kFloatingWindowPosY";

@implementation WFCUFloatingWindow

+ (void)startCallFloatingWindow:(WFCCMessage *)message focusUser:(NSString *)focusUserId withTouchedBlock:(void (^)(WFCCMessage *))touchedBlock {
    staticWindow = [[WFCUFloatingWindow alloc] init];
    staticWindow.message = message;
    staticWindow.content = (WFCCCallStartMessageContent *)message.content;
    staticWindow.touchedBlock = touchedBlock;
    staticWindow.focusUserId = focusUserId;
    [staticWindow initWindow];
}

+ (void)stopCallFloatingWindow {
    [staticWindow hideCallFloatingWindow];
    [staticWindow clearCallFloatingWindow];
    staticWindow = nil;
}

- (void)initWindow {
    if ([TJIMSDK shareSDK].state == kTJAVEngineStateIdle) {
        [self performSelector:@selector(clearCallFloatingWindow) withObject:nil afterDelay:2];
    }
    [self updateActiveTimer];
    [self startActiveTimer];
    [self updateWindow];
    [self registerTelephonyEvent];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onDeviceOrientationDidChange:)
                                                 name:UIDeviceOrientationDidChangeNotification
                                               object:nil];
    [self addProximityMonitoringObserver];
}

- (void)registerTelephonyEvent {
    self.callCenter = [[CTCallCenter alloc] init];
    self.callCenter.callEventHandler = ^(CTCall *call) {
        if ([call.callState isEqualToString:CTCallStateConnected]) {
            [[TJIMSDK shareSDK] endCallWithReason:kTJAVCallEndReasonBusy];
        }
    };
}

- (void)onDeviceOrientationDidChange:(NSNotification *)notification {
    [self updateWindow];
}

- (void)startActiveTimer {
    self.activeTimer = [NSTimer scheduledTimerWithTimeInterval:1
                                                        target:self
                                                      selector:@selector(updateActiveTimer)
                                                      userInfo:nil
                                                       repeats:YES];
    [self.activeTimer fire];
}

- (void)stopActiveTimer {
    if (self.activeTimer) {
        [self.activeTimer invalidate];
        self.activeTimer = nil;
    }
}

- (void)updateActiveTimer {
    long sec = [[NSDate date] timeIntervalSince1970] - self.content.connectTime / 1000;
    if ([TJIMSDK shareSDK].state == kTJAVEngineStateConnected && ![self isVideoViewEnabled]) {
        NSString *timeStr;
        if (sec < 60 * 60) {
            timeStr = [NSString stringWithFormat:@"%02ld:%02ld", sec / 60, sec % 60];
        } else {
            timeStr = [NSString stringWithFormat:@"%02ld:%02ld:%02ld", sec / 60 / 60, (sec / 60) % 60, sec % 60];
        }
        
        BOOL showFloatingButton = NO;
        if (!self.floatingButton.titleLabel.text) {
            self.floatingButton.hidden = YES;
            showFloatingButton = YES;
        }
        [self.floatingButton setTitle:timeStr forState:UIControlStateNormal];
        [self layoutTimeText:self.floatingButton size:16];
        
        if (showFloatingButton) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.floatingButton setTitle:timeStr forState:UIControlStateNormal];
                [self layoutTimeText:self.floatingButton size:16];
                self.floatingButton.hidden = NO;
            });
        }
    }
}

//1.决定当前界面是否开启自动转屏，如果返回NO，后面两个方法也不会被调用，只是会支持默认的方向
- (BOOL)shouldAutorotate {
      return YES;
}

//2.返回支持的旋转方向
//iPad设备上，默认返回值UIInterfaceOrientationMaskAllButUpSideDwon
//iPad设备上，默认返回值是UIInterfaceOrientationMaskAll
- (UIInterfaceOrientationMask)supportedInterfaceOrientations{
     return UIDeviceOrientationLandscapeLeft | UIDeviceOrientationLandscapeRight | UIDeviceOrientationPortrait;
}

//3.返回进入界面默认显示方向
- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
     return UIInterfaceOrientationPortrait;
}

- (void)updateWindow {
    CGFloat posX = [[[NSUserDefaults standardUserDefaults] objectForKey:kFloatingWindowPosX] floatValue];
    CGFloat posY = [[[NSUserDefaults standardUserDefaults] objectForKey:kFloatingWindowPosY] floatValue];
    posX = posX ? posX : 30;
    posY = posY ? posY : 30;
    CGRect screenBounds = [UIScreen mainScreen].bounds;
    posX = (posX + 30) > screenBounds.size.width ? (screenBounds.size.width - 30) : posX;
    posY = (posY + 48) > screenBounds.size.height ? (screenBounds.size.height - 48) : posY;

    if ([UIDevice currentDevice].orientation == UIDeviceOrientationLandscapeLeft) {
        self.window.transform = CGAffineTransformMakeRotation(M_PI / 2);
        self.window.frame = CGRectMake(posX, posY, 64, 96);
        self.floatingButton.frame = CGRectMake(0, 0, 96, 64);
        if ([self isVideoViewEnabled]) {
            self.videoView.frame = CGRectMake(0, 0, 96, 64);
        }
    } else if ([UIDevice currentDevice].orientation == UIDeviceOrientationLandscapeRight) {
        self.window.transform = CGAffineTransformMakeRotation(-M_PI / 2);
        self.window.frame = CGRectMake(posX, posY, 64, 96);
        self.floatingButton.frame = CGRectMake(0, 0, 96, 64);
        if ([self isVideoViewEnabled]) {
            self.videoView.frame = CGRectMake(0, 0, 96, 64);
        }
    } else {
        if ([UIDevice currentDevice].orientation == UIDeviceOrientationPortraitUpsideDown) {
            self.window.transform = CGAffineTransformMakeRotation(M_PI);
        } else {
            self.window.transform = CGAffineTransformMakeRotation(0);
        }

        self.window.frame = CGRectMake(posX, posY, 64, 96);
        self.floatingButton.frame = CGRectMake(0, 0, 64, 96);
        if ([self isVideoViewEnabled]) {
            self.videoView.frame = CGRectMake(0, 0, 64, 96);
        }
    }

    if ([self isVideoViewEnabled]) {
        if ([TJIMSDK shareSDK].state == kTJAVEngineStateIncomming) {
            [self.floatingButton setTitle:@"等待接听" forState:UIControlStateNormal];
            self.videoView.hidden = YES;
        } else if ([TJIMSDK shareSDK].state == kTJAVEngineStateOutgoing) {
            self.videoView.hidden = NO;
            [self.floatingButton setTitle:@"" forState:UIControlStateNormal];
            [[TJIMSDK shareSDK] startLocalPreview:self.videoView];
        } else if ([TJIMSDK shareSDK].state == kTJAVEngineStateConnected) {
            self.videoView.hidden = NO;
            [self.floatingButton setTitle:@"" forState:UIControlStateNormal];
            if ([self.focusUserId isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
                [[TJIMSDK shareSDK] startLocalPreview:self.videoView];
            } else {
                [[TJIMSDK shareSDK] startRemoteView:self.focusUserId view:self.videoView];
            }
        } else if ([TJIMSDK shareSDK].state == kTJAVEngineStateIdle) {
            UILabel *videoStopTips =
                [[UILabel alloc] initWithFrame:CGRectMake(0, self.videoView.frame.size.height / 2 - 10,
                                                          self.videoView.frame.size.width, 20)];
            videoStopTips.textAlignment = NSTextAlignmentCenter;
            videoStopTips.text = WFCString(@"Ended");
            videoStopTips.textColor = HEXCOLOR(0x0195ff);
            [self.videoView addSubview:videoStopTips];
        } else { //connecting...
            self.videoView.hidden = NO;
            [self.floatingButton setTitle:@"" forState:UIControlStateNormal];
        }
    } else {
        if ([TJIMSDK shareSDK].state == kTJAVEngineStateIdle) {
            [self.floatingButton setBackgroundColor:[UIColor clearColor]];
            [self.floatingButton setTitle:WFCString(@"Ended")
                                 forState:UIControlStateNormal];
        } else {
            if ([TJIMSDK shareSDK].state == kTJAVEngineStateOutgoing) {
                [self.floatingButton setTitle:@"等待接听" forState:UIControlStateNormal];
            } else if ([TJIMSDK shareSDK].state == kTJAVEngineStateIncomming) {
                [self.floatingButton setTitle:@"等待接听" forState:UIControlStateNormal];
            } else {
                [self.floatingButton setTitle:@"" forState:UIControlStateNormal];
            }
            [self.floatingButton setImage:[UIImage imageNamed:@"floatingaudio"] forState:UIControlStateNormal];
            [self layoutTimeText:self.floatingButton size:12];
        }
    }
}

- (UIWindow *)window {
    if (!_window) {
        CGFloat posX = [[[NSUserDefaults standardUserDefaults] objectForKey:kFloatingWindowPosX] floatValue];
        CGFloat posY = [[[NSUserDefaults standardUserDefaults] objectForKey:kFloatingWindowPosY] floatValue];
        posX = (posX - 30) ? posX : 30;
        posY = (posY - 48) ? posY : 48;
        CGRect screenBounds = [UIScreen mainScreen].bounds;
        posX = (posX + 30) > screenBounds.size.width ? (screenBounds.size.width - 30) : posX;
        posY = (posY + 48) > screenBounds.size.height ? (screenBounds.size.height - 48) : posY;
        _window = [[UIWindow alloc] initWithFrame:CGRectMake(posX, posY, 64, 96)];
        _window.backgroundColor = [UIColor whiteColor];
        _window.windowLevel = UIWindowLevelAlert + 1;
        _window.layer.cornerRadius = 4;
        _window.layer.masksToBounds = YES;
        _window.layer.borderWidth = 1;
        _window.layer.borderColor = [HEXCOLOR(0x0A88E1) CGColor];
        [_window makeKeyAndVisible]; //关键语句,显示window

        UIPanGestureRecognizer *panGestureRecognizer =
            [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handlePanGestures:)];
        panGestureRecognizer.minimumNumberOfTouches = 1;
        panGestureRecognizer.maximumNumberOfTouches = 1;
        [_window addGestureRecognizer:panGestureRecognizer];
    }
    return _window;
}

- (UIView *)videoView {
    if (!_videoView) {
        _videoView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 64, 96)];
        _videoView.backgroundColor = [UIColor blackColor];
        CGRect windowFrame = self.window.frame;
        windowFrame.size.width = _videoView.frame.size.width;
        windowFrame.size.height = _videoView.frame.size.height;
        self.window.frame = windowFrame;
        [self.window addSubview:_videoView];

        UITapGestureRecognizer *tap =
            [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(touchedWindow:)];
        [_videoView addGestureRecognizer:tap];
    }
    return _videoView;
}

- (UIButton *)floatingButton {
    if (!_floatingButton) {
        _floatingButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [_floatingButton setTitle:@"" forState:UIControlStateNormal];
        _floatingButton.backgroundColor = [UIColor clearColor];
        if ([UIDevice currentDevice].orientation == UIDeviceOrientationLandscapeLeft || [UIDevice currentDevice].orientation == UIDeviceOrientationLandscapeRight || [UIDevice currentDevice].orientation == UIDeviceOrientationUnknown) {
            _floatingButton.frame = CGRectMake(0, 0, 64, 96);
        } else {
            _floatingButton.frame = CGRectMake(0, 0, 96, 64);
        }
        
        [_floatingButton setTitleColor:[UIColor blueColor] forState:UIControlStateNormal];
        _floatingButton.titleLabel.font = [UIFont systemFontOfSize:14];
        CGRect windowFrame = self.window.frame;
        windowFrame.size.width = _floatingButton.frame.size.width;
        windowFrame.size.height = _floatingButton.frame.size.height;
        self.window.frame = windowFrame;

        [_floatingButton addTarget:self action:@selector(touchedWindow:) forControlEvents:UIControlEventTouchUpInside];
        [self.window addSubview:_floatingButton];
    }
    return _floatingButton;
}

- (void)handlePanGestures:(UIPanGestureRecognizer *)paramSender {
    if (paramSender.state != UIGestureRecognizerStateEnded && paramSender.state != UIGestureRecognizerStateFailed) {
        CGPoint location = [paramSender locationInView:[UIApplication sharedApplication].windows[0]];

        if ([UIDevice currentDevice].orientation == UIDeviceOrientationLandscapeLeft) {
            CGFloat tmp = location.x;
            location.x = [UIScreen mainScreen].bounds.size.height - location.y;
            location.y = tmp;
        } else if ([UIDevice currentDevice].orientation == UIDeviceOrientationLandscapeRight) {
            CGFloat tmp = location.x;
            location.x = location.y;
            location.y = [UIScreen mainScreen].bounds.size.width - tmp;
        } else if ([UIDevice currentDevice].orientation == UIDeviceOrientationPortraitUpsideDown) {
            CGFloat tmp = location.x;
            location.x = [UIScreen mainScreen].bounds.size.height - location.y;
            location.y = [UIScreen mainScreen].bounds.size.width - tmp;
        }

        CGRect frame = self.window.frame;
        frame.origin.x = location.x - frame.size.width / 2;
        frame.origin.y = location.y - frame.size.height / 2;

        if (frame.origin.x < 0) {
            frame.origin.x = 2;
        }
        if (frame.origin.y < 0) {
            frame.origin.y = 2;
        }

        CGRect screenBounds = [UIScreen mainScreen].bounds;
        BOOL isLandscape = screenBounds.size.width > screenBounds.size.height;
        if (isLandscape) {
            if (frame.origin.y + frame.size.height > [UIScreen mainScreen].bounds.size.width) {
                frame.origin.y = [UIScreen mainScreen].bounds.size.width - 2 - frame.size.height;
            }

            if (frame.origin.x + frame.size.width > [UIScreen mainScreen].bounds.size.height) {
                frame.origin.x = [UIScreen mainScreen].bounds.size.height - 2 - frame.size.width;
            }
        } else {
            if (frame.origin.x + frame.size.width > [UIScreen mainScreen].bounds.size.width) {
                frame.origin.x = [UIScreen mainScreen].bounds.size.width - 2 - frame.size.width;
            }

            if (frame.origin.y + frame.size.height > [UIScreen mainScreen].bounds.size.height) {
                frame.origin.y = [UIScreen mainScreen].bounds.size.height - 2 - frame.size.height;
            }
        }
        self.window.frame = frame;
    } else if (paramSender.state == UIGestureRecognizerStateEnded) {
        CGRect frame = self.window.frame;
        [[NSUserDefaults standardUserDefaults] setObject:@(frame.origin.x) forKey:kFloatingWindowPosX];
        [[NSUserDefaults standardUserDefaults] setObject:@(frame.origin.y) forKey:kFloatingWindowPosY];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }
}

- (void)touchedWindow:(id)sender {
    [self hideCallFloatingWindow];
    if (self.touchedBlock) {
        self.touchedBlock(self.message);
    }
    [self clearCallFloatingWindow];
}

- (void)hideCallFloatingWindow {
    [self stopActiveTimer];

    if (_videoView) {
        [_videoView removeFromSuperview];
        _videoView = nil;
    }
    if (_floatingButton) {
        [_floatingButton removeFromSuperview];
        _floatingButton = nil;
    }
    [_window setHidden:YES];
}

- (void)clearCallFloatingWindow {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    _activeTimer = nil;
    _message = nil;
    _touchedBlock = nil;
    _floatingButton = nil;
    _videoView = nil;
    _window = nil;
    staticWindow = nil;
}

- (void)layoutTimeText:(UIButton *)button size:(int)size {
    [button.titleLabel setFont:[UIFont systemFontOfSize:size]];
    [button setTitleColor:HEXCOLOR(0x0195ff) forState:UIControlStateNormal];

    //top left buttom right
    button.titleEdgeInsets = UIEdgeInsetsMake(button.imageView.frame.size.height / 2, -button.imageView.frame.size.width,
                                              -button.imageView.frame.size.height, 0);

    button.imageEdgeInsets = UIEdgeInsetsMake(0,
                                              0, button.imageView.frame.size.height / 2, -button.titleLabel.bounds.size.width);
}

- (BOOL)isVideoViewEnabled {
    return !self.content.isAudioOnly;
}

#pragma mark - WFAVCallSessionDelegate
- (void)didChangeState:(TJAVEngineState)state {
    switch (state) {
        case kTJAVEngineStateIdle:
            [self updateWindow];
            [self performSelector:@selector(clearCallFloatingWindow) withObject:nil afterDelay:2];
            [self removeProximityMonitoringObserver];
            break;
            
        case kTJAVEngineStateConnected:
            [self updateWindow];
            break;
            
        default:
            [self updateWindow];
            break;
    }
}


- (void)didChangeMode:(BOOL)isAudioOnly {
    [self.videoView removeFromSuperview];
    [self initWindow];
}

- (void)addProximityMonitoringObserver {
    [UIDevice currentDevice].proximityMonitoringEnabled = YES;

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(proximityStatueChanged:)
                                                 name:UIDeviceProximityStateDidChangeNotification
                                               object:nil];
}

- (void)removeProximityMonitoringObserver {
    [UIDevice currentDevice].proximityMonitoringEnabled = NO;

    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:UIDeviceProximityStateDidChangeNotification
                                                  object:nil];
}

- (void)proximityStatueChanged:(NSNotificationCenter *)notification {
    
}

@end

