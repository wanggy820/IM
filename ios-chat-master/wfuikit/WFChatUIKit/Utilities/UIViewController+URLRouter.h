//
//  UIViewController+URLRouter.h
//  hybrid_stack_manager
//
//  Created by KyleWong on 2018/8/13.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIViewController (URLRouter)

/// 获取当前控制器
+ (UIViewController*)currentViewController;

/// 获取当前导航栏控制器
+ (UINavigationController*)currentNavigationController;

/// 获取根控制器，UITabBarController
+ (UITabBarController *)currentTabbarController;

@end

NS_ASSUME_NONNULL_END
