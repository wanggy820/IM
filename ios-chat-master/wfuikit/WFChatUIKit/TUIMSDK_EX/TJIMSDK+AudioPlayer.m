//
//  TJIMSDK+AudioPlayer.m
//  WFChatUIKit
//
//  Created by wanggy820 on 2021/6/11.
//  Copyright © 2021 Tom Lee. All rights reserved.
//

#import "TJIMSDK+AudioPlayer.h"
#import <objc/runtime.h>

@implementation TJIMSDK (AudioPlayer)

- (void)setAudioPlayer:(AVAudioPlayer *)audioPlayer {
    objc_setAssociatedObject(self, _cmd, audioPlayer, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (AVAudioPlayer *)audioPlayer {
    return objc_getAssociatedObject(self, @selector(setAudioPlayer:));
}

- (void)shouldStartRing:(BOOL)isIncoming {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        if ([TJIMSDK shareSDK].state == kTJAVEngineStateIncomming || [TJIMSDK shareSDK].state == kTJAVEngineStateOutgoing) {
            if([UIApplication sharedApplication].applicationState == UIApplicationStateBackground) {
                AudioServicesAddSystemSoundCompletion(kSystemSoundID_Vibrate, NULL, NULL, systemAudioCallback, NULL);
                AudioServicesPlaySystemSound (kSystemSoundID_Vibrate);
            } else {
                AVAudioSession *audioSession = [AVAudioSession sharedInstance];
                //默认情况按静音或者锁屏键会静音
                [audioSession setCategory:AVAudioSessionCategorySoloAmbient error:nil];
                [audioSession setActive:YES error:nil];
                
                if (self.audioPlayer) {
                    [self shouldStopRing];
                }
                NSString *resource;
                if (isIncoming) {
                    resource = @"outgoing_call_ring";
                } else {
                    resource = @"incoming_call_ring";
                }
                NSURL *url = [[NSBundle mainBundle] URLForResource:resource withExtension:@"mp3"];
                NSError *error = nil;
                self.audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&error];
                if (!error) {
                    self.audioPlayer.numberOfLoops = -1;
                    self.audioPlayer.volume = 1.0;
                    [self.audioPlayer prepareToPlay];
                    [self.audioPlayer play];
                }
            }
        }
    });
}

void systemAudioCallback (SystemSoundID soundID, void* clientData) {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        if([UIApplication sharedApplication].applicationState == UIApplicationStateBackground) {
            if ([TJIMSDK shareSDK].state == kTJAVEngineStateIncomming) {
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
            }
        }
    });
}

- (void)shouldStopRing {
    if (self.audioPlayer) {
        [self.audioPlayer stop];
        self.audioPlayer = nil;
        [[AVAudioSession sharedInstance] setActive:NO withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
    }
}


@end
