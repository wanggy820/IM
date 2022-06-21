//
//  ViewController.m
//  WFDemo
//
//  Created by heavyrain on 17/9/27.
//  Copyright © 2017年 WildFireChat. All rights reserved.
//


#import "WFCUMultiVideoViewController.h"
#import <AVFoundation/AVFoundation.h>
#import <AVKit/AVKit.h>
#import "WFCUFloatingWindow.h"
#import "WFCUParticipantCollectionViewCell.h"
#import <SDWebImage/SDWebImage.h>
#import <WFChatClient/WFCCConversation.h>
#import "WFCUPortraitCollectionViewCell.h"
#import "WFCUParticipantCollectionViewLayout.h"
#import "WFCUSeletedUserViewController.h"
#import "UIView+Toast.h"
#import <WFChatUIKit/UIViewController+URLRouter.h>
#import <WFChatUIKit/TJIMSDK+AVEngine.h>

@interface WFCUMultiVideoViewController () <TJAVCallMessageDelegate, UICollectionViewDataSource, UICollectionViewDelegate>

@property (nonatomic, strong) UIView *bigVideoView;
@property (nonatomic, strong) UICollectionView *smallCollectionView;

@property (nonatomic, strong) UICollectionView *portraitCollectionView;
@property (nonatomic, strong) UIButton *hangupButton;
@property (nonatomic, strong) UIButton *answerButton;
@property (nonatomic, strong) UIButton *switchCameraButton;
@property (nonatomic, strong) UIButton *audioButton;
@property (nonatomic, strong) UIButton *speakerButton;
@property (nonatomic, strong) UIButton *videoButton;

@property (nonatomic, strong) UIButton *minimizeButton;
@property (nonatomic, strong) UIButton *addParticipantButton;

@property (nonatomic, strong) UIImageView *portraitView;
@property (nonatomic, strong) UILabel *userNameLabel;
@property (nonatomic, strong) UILabel *stateLabel;
@property (nonatomic, strong) UILabel *connectTimeLabel;


@property (nonatomic, assign) CGPoint panStartPoint;
@property (nonatomic, assign) CGRect panStartVideoFrame;
@property (nonatomic, strong) NSTimer *connectedTimer;

/*
 participantIds把自己也加入列表，然后把发起者放到最后面。
 */
@property (nonatomic, strong) NSMutableArray<NSString *> *participantIds;

//视频时，大屏用户正在说话
@property (nonatomic, strong)UIImageView *speakingView;

@property (nonatomic, strong) WFCCMessage *message;
@property (nonatomic, strong) WFCCCallStartMessageContent *content;

@end

#define ButtonSize 60
#define BottomPadding 36
#define SmallVideoView 120
#define OperationTitleFont 10
#define OperationButtonSize 50

#define PortraitItemSize 48
#define PortraitLabelSize 16

@implementation WFCUMultiVideoViewController

- (instancetype)initWithMessage:(WFCCMessage *)message {
    if (self = [super init]) {
        self.message = message;
        self.content = (WFCCCallStartMessageContent *)message.content;
        [self rearrangeParticipants];
    }
    return self;
}

- (instancetype)initWithTargets:(NSArray<NSString *> *)targetIds conversation:(WFCCConversation *)conversation audioOnly:(BOOL)audioOnly {
    self = [super init];
    if (self) {
        [[TJIMSDK shareSDK] startCallWithConversation:conversation targets:targetIds audioOnly:audioOnly];
        self.message = [TJIMSDK shareSDK].message;
        self.content = [TJIMSDK shareSDK].content;
        [self rearrangeParticipants];
    }
    return self;
}

- (void)rearrangeParticipants {
    self.participantIds = [NSMutableArray arrayWithArray:self.content.targetIds];
    if (![self.participantIds containsObject:[WFCCNetworkService sharedInstance].userId]) {
        [self.participantIds addObject:[WFCCNetworkService sharedInstance].userId];
    }
    if ([self.participantIds containsObject:self.message.fromUser]) {
        [self.participantIds removeObject:self.message.fromUser];
    }
    [self.participantIds addObject:self.message.fromUser];

    [self.participantIds removeObject:self.message.fromUser];
    [self.participantIds addObject:self.message.fromUser];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self.view setBackgroundColor:[UIColor blackColor]];
    
    if([TJIMSDK shareSDK].state == kTJAVEngineStateIdle) {
        [self didCallEndWithReason:kTJAVCallEndReasonUnknown];
        return;
    }
    [TJIMSDK shareSDK].delegate = self;
    
    self.bigVideoView = [[UIView alloc] initWithFrame:self.view.bounds];
    UITapGestureRecognizer *tapBigVideo = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(onClickedBigVideoView:)];
    [self.bigVideoView addGestureRecognizer:tapBigVideo];
    [self.view addSubview:self.bigVideoView];
    
    UICollectionViewFlowLayout *layout = [[UICollectionViewFlowLayout alloc] init];
    CGFloat itemWidth = (self.view.frame.size.width + layout.minimumLineSpacing)/3 - layout.minimumLineSpacing;
    layout.itemSize = CGSizeMake(itemWidth, itemWidth);
    layout.scrollDirection = UICollectionViewScrollDirectionHorizontal;
    NSInteger lines = (self.content.targetIds.count + 2) /3;
    self.smallCollectionView = [[UICollectionView alloc] initWithFrame:CGRectMake(0, kStatusBarAndNavigationBarHeight, self.view.frame.size.width, itemWidth*lines) collectionViewLayout:layout];
    
    self.smallCollectionView.dataSource = self;
    self.smallCollectionView.delegate = self;
    [self.smallCollectionView registerClass:[WFCUParticipantCollectionViewCell class] forCellWithReuseIdentifier:@"cell"];
    self.smallCollectionView.backgroundColor = [UIColor clearColor];
    
    [self.smallCollectionView addGestureRecognizer:[[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(onSmallVideoPan:)]];
    if (self.content.audioOnly) {
        self.smallCollectionView.hidden = YES;
    }
    [self.view addSubview:self.smallCollectionView];
    
    
    WFCUParticipantCollectionViewLayout *layout2 = [[WFCUParticipantCollectionViewLayout alloc] init];
    layout2.itemHeight = PortraitItemSize + PortraitLabelSize;
    layout2.itemWidth = PortraitItemSize;
    layout2.lineSpace = 6;
    layout2.itemSpace = 6;

    self.portraitCollectionView = [[UICollectionView alloc] initWithFrame:CGRectMake(16, self.view.frame.size.height - BottomPadding - ButtonSize - (PortraitItemSize + PortraitLabelSize)*3 - PortraitLabelSize, self.view.frame.size.width - 32, (PortraitItemSize + PortraitLabelSize)*3 + PortraitLabelSize) collectionViewLayout:layout2];
    self.portraitCollectionView.dataSource = self;
    self.portraitCollectionView.delegate = self;
    [self.portraitCollectionView registerClass:[WFCUPortraitCollectionViewCell class] forCellWithReuseIdentifier:@"cell2"];
    self.portraitCollectionView.backgroundColor = [UIColor clearColor];
    [self.view addSubview:self.portraitCollectionView];
    
    
    [self checkAVPermission];
    
    WFCCUserInfo *user = [[WFCCIMService sharedWFCIMService] getUserInfo:self.message.fromUser inGroup:self.message.conversation.target refresh:NO];
    
    self.portraitView = [[UIImageView alloc] init];
    [self.portraitView sd_setImageWithURL:[NSURL URLWithString:user.portrait] placeholderImage:[UIImage imageNamed:@"PersonalChat"]];
    self.portraitView.layer.masksToBounds = YES;
    self.portraitView.layer.cornerRadius = 8.f;
    [self.view addSubview:self.portraitView];
    
    self.userNameLabel = [[UILabel alloc] init];
    self.userNameLabel.font = [UIFont systemFontOfSize:26];
    self.userNameLabel.text = user.displayName;
    self.userNameLabel.textColor = [UIColor whiteColor];
    [self.view addSubview:self.userNameLabel];
    
    self.stateLabel = [[UILabel alloc] init];
    self.stateLabel.font = [UIFont systemFontOfSize:16];
    self.stateLabel.textColor = [UIColor whiteColor];
    [self.view addSubview:self.stateLabel];
    
    self.connectTimeLabel = [[UILabel alloc] init];
    self.connectTimeLabel.font = [UIFont systemFontOfSize:16];
    self.connectTimeLabel.textColor = [UIColor whiteColor];
    [self.view addSubview:self.connectTimeLabel];
    
    [self updateTopViewFrame];
    [self didChangeState:[TJIMSDK shareSDK].state];//update ui
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onDeviceOrientationDidChange) name:UIDeviceOrientationDidChangeNotification object:nil];
    [self onDeviceOrientationDidChange];

}

- (UIButton *)hangupButton {
    if (!_hangupButton) {
        _hangupButton = [[UIButton alloc] init];
        [_hangupButton setImage:[UIImage imageNamed:@"hangup"] forState:UIControlStateNormal];
        [_hangupButton setImage:[UIImage imageNamed:@"hangup_hover"] forState:UIControlStateHighlighted];
        [_hangupButton setImage:[UIImage imageNamed:@"hangup_hover"] forState:UIControlStateSelected];
        _hangupButton.backgroundColor = [UIColor clearColor];
        [_hangupButton addTarget:self action:@selector(hanupButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _hangupButton.hidden = YES;
        [self.view addSubview:_hangupButton];
    }
    return _hangupButton;
}

- (UIButton *)answerButton {
    if (!_answerButton) {
        _answerButton = [[UIButton alloc] init];
        
        if (self.content.audioOnly) {
            [_answerButton setImage:[UIImage imageNamed:@"answer"] forState:UIControlStateNormal];
            [_answerButton setImage:[UIImage imageNamed:@"answer_hover"] forState:UIControlStateHighlighted];
            [_answerButton setImage:[UIImage imageNamed:@"answer_hover"] forState:UIControlStateSelected];
        } else {
            [_answerButton setImage:[UIImage imageNamed:@"video_answer"] forState:UIControlStateNormal];
            [_answerButton setImage:[UIImage imageNamed:@"video_answer_hover"] forState:UIControlStateHighlighted];
            [_answerButton setImage:[UIImage imageNamed:@"video_answer_hover"] forState:UIControlStateSelected];
        }
        
        _answerButton.backgroundColor = [UIColor clearColor];
        [_answerButton addTarget:self action:@selector(answerButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _answerButton.hidden = YES;
        [self.view addSubview:_answerButton];
    }
    return _answerButton;
}

- (UIButton *)minimizeButton {
    if (!_minimizeButton) {
        _minimizeButton = [[UIButton alloc] initWithFrame:CGRectMake(6, 16 + (kIs_iPhoneX ? 24 : 0), 50, 50)];
        
        [_minimizeButton setImage:[UIImage imageNamed:@"minimize"] forState:UIControlStateNormal];
        [_minimizeButton setImage:[UIImage imageNamed:@"minimize_hover"] forState:UIControlStateHighlighted];
        [_minimizeButton setImage:[UIImage imageNamed:@"minimize_hover"] forState:UIControlStateSelected];
        
        _minimizeButton.backgroundColor = [UIColor clearColor];
        [_minimizeButton addTarget:self action:@selector(minimizeButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _minimizeButton.hidden = NO;
        [self.view addSubview:_minimizeButton];
    }
    return _minimizeButton;
}

- (UIButton *)addParticipantButton {
    if (!_addParticipantButton) {
        _addParticipantButton = [[UIButton alloc] initWithFrame:CGRectMake(self.view.frame.size.width - 6 - 50, 16 + (kIs_iPhoneX ? 24 : 0), 50, 50)];
        
        [_addParticipantButton setImage:[UIImage imageNamed:@"plus-circle"] forState:UIControlStateNormal];
        [_addParticipantButton setImage:[UIImage imageNamed:@"plus-circle"] forState:UIControlStateHighlighted];
        [_addParticipantButton setImage:[UIImage imageNamed:@"plus-circle"] forState:UIControlStateSelected];
        
        _addParticipantButton.backgroundColor = [UIColor clearColor];
        [_addParticipantButton addTarget:self action:@selector(addParticipantButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _addParticipantButton.hidden = YES;
        [self.view addSubview:_addParticipantButton];
    }
    return _addParticipantButton;
}

- (UIButton *)switchCameraButton {
    if (!_switchCameraButton) {
        _switchCameraButton = [[UIButton alloc] init];
        [_switchCameraButton setImage:[UIImage imageNamed:@"switchcamera"] forState:UIControlStateNormal];
        [_switchCameraButton setImage:[UIImage imageNamed:@"switchcamera_hover"] forState:UIControlStateHighlighted];
        [_switchCameraButton setImage:[UIImage imageNamed:@"switchcamera_hover"] forState:UIControlStateSelected];
        _switchCameraButton.backgroundColor = [UIColor clearColor];
        [_switchCameraButton addTarget:self action:@selector(switchCameraButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _switchCameraButton.hidden = YES;
        [self.view addSubview:_switchCameraButton];
    }
    return _switchCameraButton;
}

- (UIButton *)audioButton {
    if (!_audioButton) {
        _audioButton = [[UIButton alloc] initWithFrame:CGRectMake(self.view.frame.size.width/2-ButtonSize/2, self.view.frame.size.height-10-ButtonSize, ButtonSize, ButtonSize)];
        [_audioButton setImage:[UIImage imageNamed:@"mute"] forState:UIControlStateNormal];
        [_audioButton setImage:[UIImage imageNamed:@"mute_hover"] forState:UIControlStateHighlighted];
        [_audioButton setImage:[UIImage imageNamed:@"mute_hover"] forState:UIControlStateSelected];
        _audioButton.backgroundColor = [UIColor clearColor];
        [_audioButton addTarget:self action:@selector(audioButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _audioButton.hidden = YES;
        [self updateAudioButton];
        [self.view addSubview:_audioButton];
    }
    return _audioButton;
}
- (UIButton *)speakerButton {
    if (!_speakerButton) {
        _speakerButton = [[UIButton alloc] initWithFrame:CGRectMake(self.view.frame.size.width/2-ButtonSize/2, self.view.frame.size.height-10-ButtonSize, ButtonSize, ButtonSize)];
        [_speakerButton setImage:[UIImage imageNamed:@"speaker"] forState:UIControlStateNormal];
        [_speakerButton setImage:[UIImage imageNamed:@"speaker_hover"] forState:UIControlStateHighlighted];
        [_speakerButton setImage:[UIImage imageNamed:@"speaker_hover"] forState:UIControlStateSelected];
        _speakerButton.backgroundColor = [UIColor clearColor];
        [_speakerButton addTarget:self action:@selector(speakerButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _speakerButton.hidden = YES;
        [self.view addSubview:_speakerButton];
    }
    return _speakerButton;
}

- (UIButton *)videoButton {
    if (!_videoButton) {
        _videoButton = [[UIButton alloc] initWithFrame:CGRectMake(self.view.frame.size.width*3/4-ButtonSize/4, self.view.frame.size.height-45-ButtonSize-ButtonSize/2-2, ButtonSize/2, ButtonSize/2)];
        
        [_videoButton setImage:[UIImage imageNamed:@"enable_video"] forState:UIControlStateNormal];
        _videoButton.backgroundColor = [UIColor clearColor];
        [_videoButton addTarget:self action:@selector(videoButtonDidTap:) forControlEvents:UIControlEventTouchDown];
        _videoButton.hidden = YES;
        [self updateVideoButton];
        [self.view addSubview:_videoButton];
    }
    return _videoButton;
}

- (UIImageView *)speakingView {
    if (!_speakingView) {
        _speakingView = [[UIImageView alloc] initWithFrame:CGRectMake(0, self.bigVideoView.bounds.size.height - 20, 20, 20)];

        _speakingView.layer.masksToBounds = YES;
        _speakingView.layer.cornerRadius = 2.f;
        _speakingView.image = [UIImage imageNamed:@"speaking"];
        _speakingView.hidden = YES;
        [self.bigVideoView addSubview:_speakingView];
    }
    return _speakingView;
}

- (void)startConnectedTimer {
    [self stopConnectedTimer];
    self.connectedTimer = [NSTimer scheduledTimerWithTimeInterval:1
                                                        target:self
                                                      selector:@selector(updateConnectedTimeLabel)
                                                      userInfo:nil
                                                       repeats:YES];
    [self.connectedTimer fire];
}

- (void)stopConnectedTimer {
    if (self.connectedTimer) {
        [self.connectedTimer invalidate];
        self.connectedTimer = nil;
    }
}

- (void)setFocusUser:(NSString *)userId {
    if (userId) {
        [self.participantIds removeObject:userId];
        [self.participantIds addObject:userId];
        [self reloadVideoUI];
    }
}

- (void)updateConnectedTimeLabel {
    long sec = [[NSDate date] timeIntervalSince1970] - self.content.connectTime / 1000;
    if (sec < 60 * 60) {
        self.connectTimeLabel.text = [NSString stringWithFormat:@"%02ld:%02ld", sec / 60, sec % 60];
    } else {
        self.connectTimeLabel.text = [NSString stringWithFormat:@"%02ld:%02ld:%02ld", sec / 60 / 60, (sec / 60) % 60, sec % 60];
    }
}

- (void)hanupButtonDidTap:(UIButton *)button {
    if([TJIMSDK shareSDK].state != kTJAVEngineStateIdle) {
        TJAVCallEndReason reason = [TJIMSDK shareSDK].state == kTJAVEngineStateConnected ? kTJAVCallEndReasonAllLeft : kTJAVCallEndReasonRemoteHangup;
        [[TJIMSDK shareSDK] endCallWithReason:reason];
    }
}

- (void)answerButtonDidTap:(UIButton *)button {
    if ([TJIMSDK shareSDK].state == kTJAVEngineStateIncomming) {
        [[TJIMSDK shareSDK] answerCall];
    }
}

- (void)minimizeButtonDidTap:(UIButton *)button {
    __block NSString *focusUser = [self.participantIds lastObject];
    [WFCUFloatingWindow startCallFloatingWindow:self.message focusUser:focusUser withTouchedBlock:^(WFCCMessage *message) {
        WFCUMultiVideoViewController *vc = [[WFCUMultiVideoViewController alloc] initWithMessage:message];
        [vc setFocusUser:focusUser];
        vc.modalPresentationStyle = UIModalPresentationFullScreen;
        [[UIViewController currentNavigationController] presentViewController:vc animated:YES completion:nil];
    }];
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)addParticipantButtonDidTap:(UIButton *)button {
    WFCUSeletedUserViewController *pvc = [[WFCUSeletedUserViewController alloc] init];
    
    NSMutableArray *disabledUser = [[NSMutableArray alloc] init];
    [disabledUser addObjectsFromArray:self.participantIds];
    pvc.disableUserIds = disabledUser;
    
    pvc.groupId = self.message.conversation.target;
    
    NSMutableArray *candidateUser = [[NSMutableArray alloc] init];
    NSArray<WFCCGroupMember *> *members = [[WFCCIMService sharedWFCIMService] getGroupMembers:self.message.conversation.target forceUpdate:NO];
    for (WFCCGroupMember *member in members) {
      [candidateUser addObject:member.memberId];
    }
    pvc.candidateUsers = candidateUser;
    pvc.type = Vertical;
    pvc.maxSelectCount = (int)(candidateUser.count > 9 ? 9 : candidateUser.count);
    pvc.selectResult = ^(NSArray<NSString *> *contacts) {
        if (contacts.count) {
            [[TJIMSDK shareSDK] inviteNewParticipants:contacts];
        }
    };
        
    UINavigationController *navi = [[UINavigationController alloc] initWithRootViewController:pvc];
    navi.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:navi animated:YES completion:nil];
}

- (void)switchCameraButtonDidTap:(UIButton *)button {
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateIdle) {
        [[TJIMSDK shareSDK] switchCamera];
    }
}

- (void)audioButtonDidTap:(UIButton *)button {
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateIdle) {
        [TJIMSDK shareSDK].audioMuted = ![TJIMSDK shareSDK].audioMuted;
        [self updateAudioButton];
    }
}

- (void)updateAudioButton {
    if (TJIMSDK.shareSDK.audioMuted) {
        [self.audioButton setImage:[UIImage imageNamed:@"mute_hover"] forState:UIControlStateNormal];
    } else {
        [self.audioButton setImage:[UIImage imageNamed:@"mute"] forState:UIControlStateNormal];
    }
}

- (void)speakerButtonDidTap:(UIButton *)button {
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateIdle) {
        [TJIMSDK shareSDK].handsFreeOn = ![TJIMSDK shareSDK].handsFreeOn;
        [self updateSpeakerButton];
    }
}

- (void)updateSpeakerButton {
    if([TJIMSDK isHeadsetPluggedIn] || [TJIMSDK isBluetoothSpeaker]) {
        self.speakerButton.enabled = NO;
    } else {
        self.speakerButton.enabled = YES;
    }
    
    if (![TJIMSDK shareSDK].handsFreeOn) {
        [self.speakerButton setImage:[UIImage imageNamed:@"speaker"] forState:UIControlStateNormal];
    } else {
        [self.speakerButton setImage:[UIImage imageNamed:@"speaker_hover"] forState:UIControlStateNormal];
    }
}

- (void)updateVideoButton {
    if (![TJIMSDK shareSDK].videoMuted) {
        [self.videoButton setImage:[UIImage imageNamed:@"disable_video"] forState:UIControlStateNormal];
    } else {
        [self.videoButton setImage:[UIImage imageNamed:@"enable_video"] forState:UIControlStateNormal];
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

- (BOOL)onDeviceOrientationDidChange {
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateConnected) {
        return YES;
    }
    //获取当前设备Device
    UIDevice *device = [UIDevice currentDevice] ;
    NSString *lastUser = nil;
    switch (device.orientation) {
        case UIDeviceOrientationFaceUp:
            break;

        case UIDeviceOrientationFaceDown:
            break;

        case UIDeviceOrientationUnknown:
            //系统当前无法识别设备朝向，可能是倾斜
            break;

        case UIDeviceOrientationLandscapeLeft:
            self.bigVideoView.transform = CGAffineTransformMakeRotation(M_PI_2);
            self.bigVideoView.frame = self.view.bounds;
            lastUser = [self.participantIds lastObject];
            if ([lastUser isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
                [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
            } else {
                [[TJIMSDK shareSDK] startRemoteView:lastUser view:self.bigVideoView];
            }
            break;

        case UIDeviceOrientationLandscapeRight:
            self.bigVideoView.transform = CGAffineTransformMakeRotation(-M_PI_2);
            self.bigVideoView.frame = self.view.bounds;
            if (!self.content.audioOnly) {
                lastUser = [self.participantIds lastObject];
                if ([lastUser isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
                    [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
                } else {
                    [[TJIMSDK shareSDK] startRemoteView:lastUser view:self.bigVideoView];
                }
            }
            break;

        case UIDeviceOrientationPortrait:
            self.bigVideoView.transform = CGAffineTransformMakeRotation(0);
            self.bigVideoView.frame = self.view.bounds;
            if (!self.content.isAudioOnly) {
                lastUser = [self.participantIds lastObject];
                if ([lastUser isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
                    [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
                } else {
                    [[TJIMSDK shareSDK] startRemoteView:lastUser view:self.bigVideoView];
                }
            }
            break;

        case UIDeviceOrientationPortraitUpsideDown:
            break;

        default:
            NSLog(@"無法识别");
            break;
    }
    
    if (!self.smallCollectionView.hidden) {
        [self.smallCollectionView reloadData];
    }
    return YES;
}


- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    
    if ([TJIMSDK shareSDK].state == kTJAVEngineStateConnected) {
        [self updateConnectedTimeLabel];
        [self startConnectedTimer];
    }
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    
    [self stopConnectedTimer];
}

- (void)setPanStartPoint:(CGPoint)panStartPoint {
    _panStartPoint = panStartPoint;
    _panStartVideoFrame = self.smallCollectionView.frame;
}

- (void)moveToPanPoint:(CGPoint)panPoint {
    CGRect frame = self.panStartVideoFrame;
    CGSize moveSize = CGSizeMake(panPoint.x - self.panStartPoint.x, panPoint.y - self.panStartPoint.y);
    
    frame.origin.x += moveSize.width;
    frame.origin.y += moveSize.height;
    self.smallCollectionView.frame = frame;
}

- (void)onSmallVideoPan:(UIPanGestureRecognizer *)recognize {
    switch (recognize.state) {
        case UIGestureRecognizerStateBegan:
            self.panStartPoint = [recognize translationInView:self.view];
            break;
        case UIGestureRecognizerStateChanged: {
            CGPoint currentPoint = [recognize translationInView:self.view];
            [self moveToPanPoint:currentPoint];
            break;
        }
        case UIGestureRecognizerStateEnded: {
            CGPoint endPoint = [recognize translationInView:self.view];
            [self moveToPanPoint:endPoint];
            break;
        }
        case UIGestureRecognizerStateCancelled:
        case UIGestureRecognizerStateFailed:
        default:
            break;
        }
}

- (void)videoButtonDidTap:(UIButton *)button {
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateIdle) {
        [TJIMSDK shareSDK].videoMuted = ![TJIMSDK shareSDK].videoMuted;
        [self updateVideoButton];
        [self reloadVideoUI];
    }
}

- (CGRect)getButtomCenterButtonFrame {
    return CGRectMake(self.view.frame.size.width/2-ButtonSize/2, self.view.frame.size.height-BottomPadding-ButtonSize, ButtonSize, ButtonSize);
}

- (CGRect)getButtomLeftButtonFrame {
    return CGRectMake(self.view.frame.size.width/4-ButtonSize/2, self.view.frame.size.height-BottomPadding-ButtonSize, ButtonSize, ButtonSize);
}

- (CGRect)getButtomRightButtonFrame {
    return CGRectMake(self.view.frame.size.width*3/4-ButtonSize/2, self.view.frame.size.height-BottomPadding-ButtonSize, ButtonSize, ButtonSize);
}

- (CGRect)getToAudioButtonFrame {
    return CGRectMake(self.view.frame.size.width*3/4-ButtonSize/2, self.view.frame.size.height-BottomPadding-ButtonSize-ButtonSize-2, ButtonSize, ButtonSize);
}

- (void)updateTopViewFrame {
    CGFloat containerWidth = self.view.bounds.size.width;
    
    self.portraitView.frame = CGRectMake((containerWidth-64)/2, kStatusBarAndNavigationBarHeight, 64, 64);;
    
    self.userNameLabel.frame = CGRectMake((containerWidth - 240)/2, kStatusBarAndNavigationBarHeight + 64 + 8, 240, 26);
    self.userNameLabel.textAlignment = NSTextAlignmentCenter;
    
    self.connectTimeLabel.frame = CGRectMake((containerWidth - 240)/2, self.smallCollectionView.frame.origin.y + self.smallCollectionView.frame.size.height + 8, 240, 16);
    self.connectTimeLabel.textAlignment = NSTextAlignmentCenter;

    self.stateLabel.frame = CGRectMake((containerWidth - 240)/2, self.smallCollectionView.frame.origin.y + self.smallCollectionView.frame.size.height + 30, 240, 16);
    self.stateLabel.textAlignment = NSTextAlignmentCenter;
}

- (void)onClickedBigVideoView:(id)sender {
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateConnected) {
        return;
    }
    
    if (self.content.audioOnly) {
        return;
    }
    
    if (self.smallCollectionView.hidden) {
        if (self.hangupButton.hidden) {
            self.hangupButton.hidden = NO;
            self.audioButton.hidden = NO;
            if (self.content.audioOnly) {
                self.videoButton.hidden = YES;
            } else {
                self.videoButton.hidden = NO;
            }
            self.switchCameraButton.hidden = NO;
            self.smallCollectionView.hidden = NO;
            self.minimizeButton.hidden = NO;
            self.addParticipantButton.hidden = NO;
        } else {
            self.hangupButton.hidden = YES;
            self.audioButton.hidden = YES;
            self.videoButton.hidden = YES;
            self.switchCameraButton.hidden = YES;
            self.minimizeButton.hidden = YES;
            self.addParticipantButton.hidden = YES;
        }
    } else {
        self.smallCollectionView.hidden = YES;
    }
}

#pragma mark - TJAVEngineDelegate
- (void)didChangeState:(TJAVEngineState)state {
    if (!self.viewLoaded) {
        return;
    }
    switch (state) {
        case kTJAVEngineStateIdle:
            self.answerButton.hidden = YES;
            self.hangupButton.hidden = YES;
            self.switchCameraButton.hidden = YES;
            self.audioButton.hidden = YES;
            self.videoButton.hidden = YES;
            [self stopConnectedTimer];
            self.userNameLabel.hidden = YES;
            self.portraitView.hidden = YES;
            self.stateLabel.text = WFCString(@"CallEnded");
            self.smallCollectionView.hidden = YES;
            self.portraitCollectionView.hidden = YES;
            self.bigVideoView.hidden = YES;
            self.minimizeButton.hidden = YES;
            self.speakerButton.hidden = YES;
            self.addParticipantButton.hidden = YES;
            [self updateTopViewFrame];
            break;
        case kTJAVEngineStateOutgoing:
            self.answerButton.hidden = YES;
            self.connectTimeLabel.hidden = YES;
            self.hangupButton.hidden = NO;
            self.hangupButton.frame = [self getButtomCenterButtonFrame];
            
            self.audioButton.frame = [self getButtomLeftButtonFrame];
            self.audioButton.hidden = NO;
            if (self.content.isAudioOnly) {
                self.speakerButton.hidden = NO;
                self.switchCameraButton.hidden = YES;
                [self updateSpeakerButton];
                self.speakerButton.frame = [self getButtomRightButtonFrame];
            } else {
                self.speakerButton.hidden = YES;
                self.switchCameraButton.hidden = NO;
                self.switchCameraButton.frame = [self getButtomRightButtonFrame];
                [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
            }
            self.videoButton.hidden = YES;
            
            self.stateLabel.text = WFCString(@"WaitingAccept");
            self.smallCollectionView.hidden = YES;
            self.portraitCollectionView.hidden = NO;
            [self.portraitCollectionView reloadData];
            
            self.userNameLabel.hidden = YES;
            self.portraitView.hidden = YES;
            self.minimizeButton.hidden = NO;
            [self updateTopViewFrame];
            
            break;
        case kTJAVEngineStateConnecting:
            self.answerButton.hidden = YES;
            self.hangupButton.hidden = NO;
            self.hangupButton.frame = [self getButtomCenterButtonFrame];
            self.videoButton.hidden = YES;
            self.audioButton.frame = [self getButtomLeftButtonFrame];
            self.audioButton.hidden = NO;
            self.audioButton.enabled = NO;
            if (self.content.isAudioOnly) {
                self.speakerButton.hidden = NO;
                self.speakerButton.enabled = NO;
                self.switchCameraButton.hidden = YES;
                self.speakerButton.frame = [self getButtomRightButtonFrame];
            } else {
                self.speakerButton.hidden = YES;
                self.switchCameraButton.hidden = NO;
                self.switchCameraButton.enabled = NO;
                self.switchCameraButton.frame = [self getButtomRightButtonFrame];
            }
            
            if (self.content.audioOnly) {
                self.smallCollectionView.hidden = YES;
                self.portraitCollectionView.hidden = NO;
                [self.portraitCollectionView reloadData];
                
                self.portraitCollectionView.center = self.view.center;
            } else {
                self.smallCollectionView.hidden = NO;
                [self.smallCollectionView reloadData];
                self.portraitCollectionView.hidden = YES;
            }
            
            
            self.stateLabel.text = WFCString(@"CallConnecting");
            self.portraitView.hidden = YES;
            self.userNameLabel.hidden = YES;
            break;
        case kTJAVEngineStateConnected:
            self.answerButton.hidden = YES;
            self.hangupButton.hidden = NO;
            self.connectTimeLabel.hidden = NO;
            self.stateLabel.hidden = YES;
            self.hangupButton.frame = [self getButtomCenterButtonFrame];
            self.audioButton.hidden = NO;
            self.audioButton.enabled = YES;
            self.audioButton.frame = [self getButtomLeftButtonFrame];
            if (self.content.isAudioOnly) {
                self.speakerButton.hidden = NO;
                self.speakerButton.enabled = YES;
                self.speakerButton.frame = [self getButtomRightButtonFrame];
                [self updateSpeakerButton];
                self.switchCameraButton.hidden = YES;
                self.videoButton.hidden = YES;
            } else {
                self.speakerButton.hidden = YES;
                [TJIMSDK shareSDK].handsFreeOn = YES;
                [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
                self.switchCameraButton.hidden = NO;
                self.switchCameraButton.enabled = YES;
                self.switchCameraButton.frame = [self getButtomRightButtonFrame];
                self.videoButton.hidden = NO;
            }
            
            self.minimizeButton.hidden = NO;
            self.addParticipantButton.hidden = NO;
            
            if (self.content.isAudioOnly) {
                [[TJIMSDK shareSDK] stopLocalPreview];
                self.smallCollectionView.hidden = YES;
                self.bigVideoView.hidden = YES;
                
                self.portraitCollectionView.hidden = NO;
                [self.portraitCollectionView reloadData];
            } else {
                NSString *lastUser = [self.participantIds lastObject];
                if ([lastUser isEqualToString:[WFCCNetworkService sharedInstance].userId]) {

                    [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
                } else {
                    [[TJIMSDK shareSDK] startRemoteView:lastUser view:self.bigVideoView];
                }
                
                self.smallCollectionView.hidden = NO;
                [self.smallCollectionView reloadData];
                self.bigVideoView.hidden = NO;
                
                self.portraitCollectionView.hidden = YES;
            }
            
            self.userNameLabel.hidden = YES;
            self.portraitView.hidden = YES;
            [self updateConnectedTimeLabel];
            [self startConnectedTimer];
            [self updateTopViewFrame];
            break;
        case kTJAVEngineStateIncomming:
            self.connectTimeLabel.hidden = YES;
            self.answerButton.hidden = NO;
            self.answerButton.frame = [self getButtomRightButtonFrame];
            self.hangupButton.hidden = NO;
            self.hangupButton.frame = [self getButtomLeftButtonFrame];
            self.switchCameraButton.hidden = YES;
            self.audioButton.hidden = YES;
            self.videoButton.hidden = YES;
            self.minimizeButton.hidden = NO;
            
            self.stateLabel.text = WFCString(@"InvitingYou");
            self.smallCollectionView.hidden = YES;
            self.portraitCollectionView.hidden = NO;
            [self.portraitCollectionView reloadData];
            break;
        default:
            break;
    }
}

- (void)didReceiveParticipantProfile:(NSString *)userId isEnterRoom:(BOOL)isEnter {
    if (!userId.length) {
        return;
    }
    if ([self.participantIds containsObject:userId] && self.participantIds.count <= 2 && !isEnter) {
        [[TJIMSDK shareSDK] endCallWithReason:kTJAVCallEndReasonRemoteHangup];
        return;
    }
    if (!isEnter) {
        [self.participantIds removeObject:userId];
    } else if (![self.participantIds containsObject:userId]) {
        [self.participantIds removeObject:self.message.fromUser];
        [self.participantIds addObject:userId];
        [self.participantIds addObject:self.message.fromUser];
    }
    [self didChangeState:[TJIMSDK shareSDK].state];
}

- (void)didCallEndWithReason:(TJAVCallEndReason)reason {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [[TJIMSDK shareSDK] endCallWithReason:reason];
    });
}


- (void)checkAVPermission {
    [self checkCapturePermission:nil];
    [self checkRecordPermission:nil];
}

- (void)checkCapturePermission:(void (^)(BOOL granted))complete {
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (authStatus == AVAuthorizationStatusDenied || authStatus == AVAuthorizationStatusRestricted) {
        if (complete) {
            complete(NO);
        }
    } else if (authStatus == AVAuthorizationStatusNotDetermined) {
        [AVCaptureDevice
         requestAccessForMediaType:AVMediaTypeVideo
         completionHandler:^(BOOL granted) {
             if (complete) {
                 complete(granted);
             }
         }];
    } else {
        if (complete) {
            complete(YES);
        }
    }
}

- (void)checkRecordPermission:(void (^)(BOOL granted))complete {
    if ([[AVAudioSession sharedInstance] respondsToSelector:@selector(requestRecordPermission:)]) {
        [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted) {
            if (complete) {
                complete(granted);
            }
        }];
    }
}
- (void)reloadVideoUI {
    if (self.content.audioOnly) {
        [self.portraitCollectionView reloadData];
        return;
    }
    if ([TJIMSDK shareSDK].state != kTJAVEngineStateConnecting && [TJIMSDK shareSDK].state != kTJAVEngineStateConnected) {
        [self.portraitCollectionView reloadData];
        return;
    }
    UICollectionViewFlowLayout *layout = [[UICollectionViewFlowLayout alloc] init];
    CGFloat itemWidth = (self.view.frame.size.width + layout.minimumLineSpacing)/3 - layout.minimumLineSpacing;
    
    if (self.participantIds.count - 1 > 3) {
        self.smallCollectionView.frame = CGRectMake(0, kStatusBarAndNavigationBarHeight, self.view.frame.size.width, itemWidth * 2 + layout.minimumLineSpacing);
    } else {
        self.smallCollectionView.frame = CGRectMake(0, kStatusBarAndNavigationBarHeight, self.view.frame.size.width, itemWidth);
    }
    
    self.speakingView.hidden = YES;
    NSString *userId = [self.participantIds lastObject];
    
    //大屏-->自己-->本地视频
    if ([userId isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
        BOOL videoMuted = [TJIMSDK shareSDK].myProfile.videoMuted;
        if (videoMuted) {
            self.stateLabel.text = WFCString(@"VideoClosed");
            self.stateLabel.hidden = NO;
        } else {
            self.stateLabel.text = nil;
            self.stateLabel.hidden = YES;
        }
        [[TJIMSDK shareSDK] startLocalPreview:self.bigVideoView];
        [self.smallCollectionView reloadData];
        return;
    }
    //他人-->远程视频
    for (TJAVParticipantProfile *profile in [TJIMSDK shareSDK].participants) {
        if ([profile.userId isEqualToString:userId]) {
            if (profile.videoMuted) {
                self.stateLabel.text = WFCString(@"VideoClosed");
                self.stateLabel.hidden = NO;
            } else {
                self.stateLabel.text = nil;
                self.stateLabel.hidden = YES;
            }
            [[TJIMSDK shareSDK] startRemoteView:userId view:self.bigVideoView];
            break;
        }
    }
    
    [self.smallCollectionView reloadData];
}

#pragma mark - UICollectionViewDataSource
- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section {
    if (collectionView == self.portraitCollectionView) {
        if (self.content.audioOnly && ([TJIMSDK shareSDK].state == kTJAVEngineStateConnecting || [TJIMSDK shareSDK].state == kTJAVEngineStateConnected)) {
            return self.participantIds.count;
        }
    }
    return self.participantIds.count - 1;
}

// The cell that is returned must be retrieved from a call to -dequeueReusableCellWithReuseIdentifier:forIndexPath:
- (__kindof UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath {
    NSString *userId = self.participantIds[indexPath.row];
    if (collectionView == self.smallCollectionView) {
        WFCUParticipantCollectionViewCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"cell" forIndexPath:indexPath];

        WFCCUserInfo *userInfo = [[WFCCIMService sharedWFCIMService] getUserInfo:userId inGroup:self.message.conversation.target refresh:NO];
        
        
        UIDevice *device = [UIDevice currentDevice] ;
        if (device.orientation == UIDeviceOrientationLandscapeLeft) {
            cell.transform = CGAffineTransformMakeRotation(M_PI_2);
        } else if (device.orientation == UIDeviceOrientationLandscapeRight) {
            cell.transform = CGAffineTransformMakeRotation(-M_PI_2);
        } else {
            cell.transform = CGAffineTransformMakeRotation(0);
        }
        
        
        if ([userId isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
            TJAVParticipantProfile *profile = [TJIMSDK shareSDK].myProfile;
            [cell setUserInfo:userInfo callProfile:profile];
            [[TJIMSDK shareSDK] startLocalPreview:cell.portraitView];
        } else {
            for (TJAVParticipantProfile *profile in [TJIMSDK shareSDK].participants) {
                if ([profile.userId isEqualToString:userId]) {
                    [cell setUserInfo:userInfo callProfile:profile];
                    [[TJIMSDK shareSDK] startRemoteView:userId view:cell.portraitView];
                    break;
                }
            }
        }

        return cell;
    } else {
        WFCUPortraitCollectionViewCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"cell2" forIndexPath:indexPath];
        
        cell.itemSize = PortraitItemSize;
        cell.labelSize = PortraitLabelSize;
        
        WFCCUserInfo *userInfo = [[WFCCIMService sharedWFCIMService] getUserInfo:userId inGroup:self.message.conversation.target refresh:NO];
        cell.userInfo = userInfo;
        
        if ([userId isEqualToString:[WFCCNetworkService sharedInstance].userId]) {
            cell.profile = [TJIMSDK shareSDK].myProfile;
        } else {
            for (TJAVParticipantProfile *profile in [TJIMSDK shareSDK].participants) {
                if ([profile.userId isEqualToString:userId]) {
                    cell.profile = profile;
                    break;
                }
            }
        }
        
        return cell;
    }
    
}

#pragma mark - UICollectionViewDelegate
- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath {
    if (collectionView == self.smallCollectionView) {
        [self.participantIds exchangeObjectAtIndex:indexPath.row withObjectAtIndex:self.participantIds.count - 1];
        [self reloadVideoUI];
    }
}

@end
