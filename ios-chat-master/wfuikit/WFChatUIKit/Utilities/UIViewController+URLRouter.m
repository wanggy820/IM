//
//  UIViewController+URLRouter.m
//  hybrid_stack_manager
//
//  Created by KyleWong on 2018/8/13.
//

#import "UIViewController+URLRouter.h"
#import <UIKit/UIKit.h>

@implementation UIViewController (URLRouter)


/// 获取当前控制器
+ (UIViewController*)currentViewController {
    //获得当前活动窗口的根视图
    UIViewController* vc = [self window].rootViewController;
    while (1) {
        //根据不同的页面切换方式，逐步取得最上层的viewController
        if ([vc isKindOfClass:[UITabBarController class]]) {
            vc = ((UITabBarController*)vc).selectedViewController;
        }
        if ([vc isKindOfClass:[UINavigationController class]]) {
            UIViewController * aVC =((UINavigationController*)vc).visibleViewController;
            if (![aVC isKindOfClass:UIAlertController.class]) {
                vc = ((UINavigationController*)vc).visibleViewController;
            }
            else
            {
                vc = ((UINavigationController*)vc).topViewController;
            }
        }
        if (vc.presentedViewController && ![vc.presentedViewController isKindOfClass:UIAlertController.class]) {
            vc = vc.presentingViewController;
        }
        else {
            break;
        }
    }
    return vc;
}

+ (UIWindow *)window {
    UIWindow *window = [UIApplication sharedApplication].keyWindow;
    //防止 UIAlertView 引起堆栈错乱
    if ([NSStringFromClass(window.class) hasPrefix:@"_"]) {
        window = [UIApplication sharedApplication].delegate.window;
    }
    return window;
}

/// 获取当前导航栏控制器
+ (UINavigationController*)currentNavigationController {
    UIViewController * aCurrentVC = [UIViewController currentViewController];
    UINavigationController * aCurrentNav = aCurrentVC.navigationController;
    if (!aCurrentNav) {
        UITabBarController * aTabbarVC = [self rootViewController];
        if ([aTabbarVC isKindOfClass:[UITabBarController class]]) {
            aCurrentNav = aTabbarVC.selectedViewController;
        } else {
            aCurrentNav = aCurrentVC.navigationController;
        }
    }
    return aCurrentNav;
}


/// 获取根控制器，UITabBarController
+ (UITabBarController *)currentTabbarController
{
    UITabBarController *tabbarVC = [self rootViewController];
    if ([tabbarVC isKindOfClass:[UITabBarController class]]) {
        return tabbarVC;
    }
    return nil;
}

+ (UITabBarController *)rootViewController
{
    UIViewController *aRootVC = [self window].rootViewController;
    UITabBarController *aRootViewController = (UITabBarController *)aRootVC;
    return aRootViewController;
}
@end
