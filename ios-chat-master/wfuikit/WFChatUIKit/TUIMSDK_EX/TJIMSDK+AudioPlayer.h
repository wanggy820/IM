//
//  TJIMSDK+AudioPlayer.h
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/6/11.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import <WFChatUIKit/WFChatUIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface TJIMSDK (AudioPlayer)

@property (nonatomic, strong) AVAudioPlayer *_Nullable audioPlayer;

/**
 播放铃声

 @param isIncoming 来电或去电
 */
- (void)shouldStartRing:(BOOL)isIncoming;

/**
 停止播放铃声
 */
- (void)shouldStopRing;

@end

NS_ASSUME_NONNULL_END
