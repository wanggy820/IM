//
//  WFCUParticipantCollectionViewCell.h
//  WFChatUIKit
//
//  Created by dali on 2020/1/20.
//  Copyright © 2020 WildFireChat. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <WFChatClient/WFCChatClient.h>
#import "TJAVParticipantProfile.h"

NS_ASSUME_NONNULL_BEGIN

@interface WFCUParticipantCollectionViewCell : UICollectionViewCell
@property (nonatomic, strong)UIImageView *portraitView;
- (void)setUserInfo:(WFCCUserInfo *)userInfo callProfile:(TJAVParticipantProfile *)profile;
@end

NS_ASSUME_NONNULL_END

