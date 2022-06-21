//
//  VideoCell.m
//  WFChat UIKit
//
//  Created by WF Chat on 2017/9/2.
//  Copyright © 2017年 WildFireChat. All rights reserved.
//

#import "WFCUVideoCell.h"
#import <WFChatClient/WFCChatClient.h>

@interface WFCUVideoCell ()
@property(nonatomic, strong) UIImageView *shadowMaskView;
@end

@implementation WFCUVideoCell

+ (CGSize)sizeForClientArea:(WFCUMessageModel *)msgModel withViewWidth:(CGFloat)width {
    WFCCVideoMessageContent *imgContent = (WFCCVideoMessageContent *)msgModel.message.content;
    
    CGSize size = imgContent.thumbnail.size;
    
    if (size.height > width || size.width > width) {
        float scale = MIN(width/size.height, width/size.width);
        size = CGSizeMake(size.width * scale, size.height * scale);
    }
    return size;
}

- (void)setModel:(WFCUMessageModel *)model {
    [super setModel:model];
    
    WFCCVideoMessageContent *imgContent = (WFCCVideoMessageContent *)model.message.content;
    self.thumbnailView.frame = self.bubbleView.bounds;
    self.thumbnailView.image = imgContent.thumbnail;
    self.videoCoverView.frame = CGRectMake((self.bubbleView.bounds.size.width - 40)/2, (self.bubbleView.bounds.size.height - 40)/2, 40, 40);
    self.videoCoverView.image = [UIImage imageNamed:@"video_msg_cover"];
    long duration = imgContent.duration;
    NSString *time = [NSString stringWithFormat:@"%02ld:%02ld", (duration%(60*60))/60, duration%60];
    self.videoTimeLabel.text = time;
    self.videoTimeLabel.frame = CGRectMake(10, self.bubbleView.bounds.size.height - 20, self.bubbleView.bounds.size.width - 20, 20);
}

- (UIImageView *)thumbnailView {
    if (!_thumbnailView) {
        _thumbnailView = [[UIImageView alloc] init];
        [self.bubbleView addSubview:_thumbnailView];
    }
    return _thumbnailView;
}

- (UIImageView *)videoCoverView {
    if (!_videoCoverView) {
        _videoCoverView = [[UIImageView alloc] init];
        _videoCoverView.backgroundColor = [UIColor clearColor];
        [self.bubbleView addSubview:_videoCoverView];
    }
    return _videoCoverView;
}

- (UILabel *)videoTimeLabel {
    if (!_videoTimeLabel) {
        _videoTimeLabel = [[UILabel alloc] init];
        _videoTimeLabel.textColor = [UIColor whiteColor];
        _videoTimeLabel.textAlignment = NSTextAlignmentRight;
        _videoTimeLabel.font = [UIFont systemFontOfSize:14];
        [self.bubbleView addSubview:_videoTimeLabel];
    }
    return _videoTimeLabel;
}

- (void)setMaskImage:(UIImage *)maskImage{
    [super setMaskImage:maskImage];
    if (_shadowMaskView) {
        [_shadowMaskView removeFromSuperview];
    }
    _shadowMaskView = [[UIImageView alloc] initWithImage:maskImage];
    
    CGRect frame = CGRectMake(self.bubbleView.frame.origin.x - 1, self.bubbleView.frame.origin.y - 1, self.bubbleView.frame.size.width + 2, self.bubbleView.frame.size.height + 2);
    _shadowMaskView.frame = frame;
    [self.contentView addSubview:_shadowMaskView];
    [self.contentView bringSubviewToFront:self.bubbleView];
    
}

@end