//
//  WFCUFavoriteItem.m
//  WFChatUIKit
//
//  Created by Tom Lee on 2020/11/1.
//  Copyright © 2020 Tom Lee. All rights reserved.
//

#import "WFCUFavoriteItem.h"
#import <WFChatClient/WFCChatClient.h>


@implementation WFCUFavoriteItem
+ (WFCUFavoriteItem *)itemFromContent:(WFCCMessageContent *)content {
    WFCUFavoriteItem *item = [[WFCUFavoriteItem alloc] init];
    
    if ([content isKindOfClass:[WFCCTextMessageContent class]]) {
        WFCCTextMessageContent *textContent = (WFCCTextMessageContent *)content;
        
        item.favType = MESSAGE_CONTENT_TYPE_TEXT;
        item.title = textContent.text;
    } else if ([content isKindOfClass:[WFCCSoundMessageContent class]]) {
        WFCCSoundMessageContent *soundContent = (WFCCSoundMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_SOUND;
        item.url = soundContent.remoteUrl;
        NSDictionary *dict = @{@"duration":@(soundContent.duration)};
        NSData *data = [NSJSONSerialization dataWithJSONObject:dict
                                                                options:kNilOptions
                                                         error:nil];
        item.data = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    } else if ([content isKindOfClass:[WFCCImageMessageContent class]]) {
        WFCCImageMessageContent *imgContent = (WFCCImageMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_IMAGE;
        item.url = imgContent.remoteUrl;
        NSData *thumbData = UIImageJPEGRepresentation(imgContent.thumbnail, 0.45);
        NSDictionary *dict = @{@"thumb":[thumbData base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed]};
        NSData *data = [NSJSONSerialization dataWithJSONObject:dict
                                                                options:kNilOptions
                                                         error:nil];
        item.data = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    } else if ([content isKindOfClass:[WFCCVideoMessageContent class]]) {
        WFCCVideoMessageContent *imgContent = (WFCCVideoMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_VIDEO;
        item.url = imgContent.remoteUrl;
        NSData *thumbData = UIImageJPEGRepresentation(imgContent.thumbnail, 0.45);
        NSDictionary *dict = @{@"thumb":[thumbData base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed], @"duration":@(imgContent.duration)};
        NSData *data = [NSJSONSerialization dataWithJSONObject:dict
                                                                options:kNilOptions
                                                         error:nil];
        item.data = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    } else if ([content isKindOfClass:[WFCCLocationMessageContent class]]) {
        WFCCLocationMessageContent *locContent = (WFCCLocationMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_LOCATION;
        item.title = locContent.title;
        NSData *thumbData = UIImageJPEGRepresentation(locContent.thumbnail, 0.45);
        NSDictionary *dict = @{@"thumb":[thumbData base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed], @"long":@(locContent.coordinate.longitude), @"lat":@(locContent.coordinate.latitude)};
        NSData *data = [NSJSONSerialization dataWithJSONObject:dict
                                                                options:kNilOptions
                                                         error:nil];
        item.data = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    } else if ([content isKindOfClass:[WFCCLinkMessageContent class]]) {
        WFCCLinkMessageContent *linkContent = (WFCCLinkMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_LINK;
        item.title = linkContent.title;
        item.thumbUrl = linkContent.thumbnailUrl;
        item.url = linkContent.url;
    } else if ([content isKindOfClass:[WFCCCompositeMessageContent class]]) {
        WFCCCompositeMessageContent *compositeContent = (WFCCCompositeMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_COMPOSITE_MESSAGE;
        item.title = compositeContent.title;
        
        WFCCMessagePayload *payload = [compositeContent encode];
        item.data = [payload.binaryContent base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed];
    } else if ([content isKindOfClass:[WFCCFileMessageContent class]]) {
        WFCCFileMessageContent *fileContent = (WFCCFileMessageContent *)content;
        item.favType = MESSAGE_CONTENT_TYPE_FILE;
        item.title = fileContent.name;
        item.url = fileContent.remoteUrl;
        NSDictionary *dict = @{@"size":@(fileContent.size)};
        NSData *data = [NSJSONSerialization dataWithJSONObject:dict
                                                                options:kNilOptions
                                                         error:nil];
        item.data = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    } else {
        NSLog(@"Error, not implement!!!!");
        return nil;
    }
    
    return item;
}

- (WFCCMessageContent *)toContent {
    switch (self.favType) {
        case MESSAGE_CONTENT_TYPE_TEXT:
        {
            return [WFCCTextMessageContent contentWith:self.title];
        }
        case MESSAGE_CONTENT_TYPE_SOUND:
        {
            WFCCSoundMessageContent *soundCnt = [[WFCCSoundMessageContent alloc] init];
            soundCnt.remoteUrl = self.url;
            
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[self.data dataUsingEncoding:NSUTF8StringEncoding] options:kNilOptions error:nil];
            soundCnt.duration = [dict[@"duration"] intValue];
            
            return soundCnt;
        }
        case MESSAGE_CONTENT_TYPE_IMAGE:
        {
            WFCCImageMessageContent *imageCnt = [[WFCCImageMessageContent alloc] init];
            imageCnt.remoteUrl = self.url;
            
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[self.data dataUsingEncoding:NSUTF8StringEncoding] options:kNilOptions error:nil];
            
            NSString *thumbStr = dict[@"thumb"];
            NSData *thumbData = [[NSData alloc] initWithBase64EncodedString:thumbStr options:NSDataBase64DecodingIgnoreUnknownCharacters];
            imageCnt.thumbnail = [UIImage imageWithData:thumbData];
            
            return imageCnt;
        }
        case MESSAGE_CONTENT_TYPE_VIDEO:
        {
            WFCCVideoMessageContent *videoCnt = [[WFCCVideoMessageContent alloc] init];
            videoCnt.remoteUrl = self.url;
            
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[self.data dataUsingEncoding:NSUTF8StringEncoding] options:kNilOptions error:nil];
            
            NSString *thumbStr = dict[@"thumb"];
            NSData *thumbData = [[NSData alloc] initWithBase64EncodedString:thumbStr options:NSDataBase64DecodingIgnoreUnknownCharacters];
            videoCnt.thumbnail = [UIImage imageWithData:thumbData];
            videoCnt.duration = [dict[@"duration"] intValue];
            
            return videoCnt;
        }
        case MESSAGE_CONTENT_TYPE_LOCATION:
        {
            WFCCLocationMessageContent *locationCnt = [[WFCCLocationMessageContent alloc] init];
            locationCnt.title = self.title;
            
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[self.data dataUsingEncoding:NSUTF8StringEncoding] options:kNilOptions error:nil];
            
            NSString *thumbStr = dict[@"thumb"];
            NSData *thumbData = [[NSData alloc] initWithBase64EncodedString:thumbStr options:NSDataBase64DecodingIgnoreUnknownCharacters];
            locationCnt.thumbnail = [UIImage imageWithData:thumbData];
            
            double latitude = [dict[@"lat"] doubleValue];
            double longitude = [dict[@"long"] doubleValue];
            locationCnt.coordinate = CLLocationCoordinate2DMake(latitude, longitude);
            
            return locationCnt;
        }
        case MESSAGE_CONTENT_TYPE_LINK:
        {
            WFCCLinkMessageContent *linkCnt = [[WFCCLinkMessageContent alloc] init];
            linkCnt.url = self.url;
            linkCnt.thumbnailUrl = self.thumbUrl;
            linkCnt.title = self.title;

            return linkCnt;
        }
        case MESSAGE_CONTENT_TYPE_COMPOSITE_MESSAGE:
        {
            WFCCCompositeMessageContent *compositeCnt = [[WFCCCompositeMessageContent alloc] init];
            compositeCnt.title = self.title;
            
            NSData *binaryData = [[NSData alloc] initWithBase64EncodedString:self.data options:NSDataBase64DecodingIgnoreUnknownCharacters];
            WFCCMessagePayload *payload = [[WFCCMessagePayload alloc] init];
            payload.binaryContent = binaryData;
            [compositeCnt decode:payload];
            
            return compositeCnt;
        }
        case MESSAGE_CONTENT_TYPE_FILE:
        {
            WFCCFileMessageContent *fileCnt = [[WFCCFileMessageContent alloc] init];
            fileCnt.remoteUrl = self.url;
            fileCnt.name = self.title;
            
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[self.data dataUsingEncoding:NSUTF8StringEncoding] options:kNilOptions error:nil];
            fileCnt.size = [dict[@"size"] intValue];
            
            return fileCnt;
        }
        default:
            break;
    }
    return nil;
}
@end
