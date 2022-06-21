//
//  PCLoginConfirmViewController.m
//  WildFireChat
//
//  Created by heavyrain lee on 2019/3/2.
//  Copyright © 2019 WildFireChat. All rights reserved.
//

#import "PCLoginConfirmViewController.h"
#import <WFChatClient/WFCChatClient.h>
#import "MBProgressHUD.h"
#import "AppService.h"

@interface PCLoginConfirmViewController ()

@end

@implementation PCLoginConfirmViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self.view setBackgroundColor:[UIColor whiteColor]];
    CGFloat width = [UIScreen mainScreen].bounds.size.width;
    CGFloat height = [UIScreen mainScreen].bounds.size.height;
    UIImageView *pcView = [[UIImageView alloc] initWithFrame:CGRectMake((width - 200)/2, 120, 200, 200)];
    pcView.image = [UIImage imageNamed:@"pc"];
    [self.view addSubview:pcView];
    
    UILabel *label = [[UILabel alloc] initWithFrame:CGRectMake((width - 200)/2, 320, 200, 16)];
    switch (self.platform) {
        case PlatformType_Windows:
            [label setText:@"确认 Windows 登陆"];
            break;
        case PlatformType_OSX:
            [label setText:@"确认 Mac 登陆"];
            break;
        case PlatformType_WEB:
            [label setText:@"确认浏览器登陆"];
            break;
        case PlatformType_Linux:
            [label setText:@"确认 Linux 登陆"];
            break;
        default:
            [label setText:@"确认电脑登陆"];
            break;
    }
    
    [label setTextAlignment:NSTextAlignmentCenter];
    [self.view addSubview:label];
    
    UIButton *loginBtn = [[UIButton alloc] initWithFrame:CGRectMake(100, height - 150, width - 200, 40)];
    [loginBtn setBackgroundColor:[UIColor greenColor]];
    [loginBtn setTitle:@"登陆" forState:UIControlStateNormal];
    loginBtn.layer.masksToBounds = YES;
    loginBtn.layer.cornerRadius = 5.f;
    [loginBtn addTarget:self action:@selector(onLoginBtn:) forControlEvents:UIControlEventTouchUpInside];
    
    UIButton *cancelBtn = [[UIButton alloc] initWithFrame:CGRectMake(100, height - 90, width - 200, 40)];
    [cancelBtn setTitle:@"取消登陆" forState:UIControlStateNormal];
    [cancelBtn setTitleColor:[UIColor grayColor] forState:UIControlStateNormal];
    [cancelBtn addTarget:self action:@selector(onLoginCancel:) forControlEvents:UIControlEventTouchUpInside];
    
    UIButton *closeBtn = [[UIButton alloc] initWithFrame:CGRectMake(12, 12, 40, 40)];
    [closeBtn setTitle:@"关闭" forState:UIControlStateNormal];
    [closeBtn setTitleColor:[UIColor grayColor] forState:UIControlStateNormal];
    [closeBtn addTarget:self action:@selector(onClose:) forControlEvents:UIControlEventTouchUpInside];
    
    [self.view addSubview:loginBtn];
    [self.view addSubview:cancelBtn];
    [self.view addSubview:closeBtn];
    
    [self notifyScaned];
}

- (void)onLoginBtn:(id)sender {
    [self confirmLogin];
}

- (void)onLoginCancel:(id)sender {
    [[AppService sharedAppService] pcCancelLogin:self.sessionId success:nil error:nil];
    
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)onClose:(id)sender {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)notifyScaned {
    __weak typeof(self)ws = self;
    [[AppService sharedAppService] pcScaned:self.sessionId success:^{
        [ws sendCodeDone:YES isLogin:NO];
    } error:^(int errorCode, NSString * _Nonnull message) {
        [ws sendCodeDone:NO isLogin:NO];
    }];
}

- (void)confirmLogin {
    __weak typeof(self)ws = self;
    [[AppService sharedAppService] pcConfirmLogin:self.sessionId success:^{
        [ws sendCodeDone:YES isLogin:YES];
    } error:^(int errorCode, NSString * _Nonnull message) {
        [ws sendCodeDone:NO isLogin:YES];
    }];
}

- (void)sendCodeDone:(BOOL)result isLogin:(BOOL)isLogin {
    if (!result) {
        MBProgressHUD *hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
        hud.mode = MBProgressHUDModeText;
        hud.label.text = @"网络错误";
        hud.offset = CGPointMake(0.f, MBProgressMaxOffset);
        
        [hud hideAnimated:YES afterDelay:1.f];
    } else if(isLogin) {
        MBProgressHUD *hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
        hud.mode = MBProgressHUDModeText;
        hud.label.text = @"成功";
        hud.offset = CGPointMake(0.f, MBProgressMaxOffset);
        
        __weak typeof(self)ws = self;
        [hud setCompletionBlock:^{
            [ws dismissViewControllerAnimated:YES completion:nil];
        }];
        [hud hideAnimated:YES afterDelay:1.f];
    }
}
/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
