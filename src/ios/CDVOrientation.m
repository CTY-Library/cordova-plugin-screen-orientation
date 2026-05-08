/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

#import "CDVOrientation.h"
#import <Cordova/CDVViewController.h>
#import <objc/message.h>

@interface CDVOrientation () {}
@end

@implementation CDVOrientation

- (BOOL)applySupportedOrientations:(CDVViewController*)vc
                            result:(NSMutableArray*)result
                          selector:(SEL)selector
{
    if ([vc respondsToSelector:selector]) {
        ((void (*)(CDVViewController*, SEL, NSMutableArray*))objc_msgSend)(vc, selector, result);
        return YES;
    }

    // Fallback for Cordova iOS variants that no longer expose setSupportedOrientations:.
    @try {
        [vc setValue:result forKey:@"supportedOrientations"];
        return YES;
    } @catch (NSException *exception) {
        NSLog(@"CDVOrientation: unable to update supported orientations (%@)", exception.reason);
        return NO;
    }
}

- (UIWindowScene*)activeWindowScene API_AVAILABLE(ios(13.0))
{
    NSSet<UIScene*> *scenes = [UIApplication.sharedApplication connectedScenes];
    for (UIScene *scene in scenes) {
        if ([scene isKindOfClass:[UIWindowScene class]]
                && scene.activationState == UISceneActivationStateForegroundActive) {
            return (UIWindowScene*)scene;
        }
    }

    for (UIScene *scene in scenes) {
        if ([scene isKindOfClass:[UIWindowScene class]]) {
            return (UIWindowScene*)scene;
        }
    }

    return nil;
}


-(void)handleBelowEqualIos15WithOrientationMask:(NSInteger) orientationMask viewController: (CDVViewController*) vc result:(NSMutableArray*) result selector:(SEL) selector
{
    NSValue *value;
    if (orientationMask != 15) {
        if (!_isLocked) {
            _lastOrientation = [UIApplication sharedApplication].statusBarOrientation;
        }
        UIInterfaceOrientation deviceOrientation = [UIApplication sharedApplication].statusBarOrientation;
        if(orientationMask == 8  || (orientationMask == 12  && !UIInterfaceOrientationIsLandscape(deviceOrientation))) {
            value = [NSNumber numberWithInt:UIInterfaceOrientationLandscapeLeft];
        } else if (orientationMask == 4){
            value = [NSNumber numberWithInt:UIInterfaceOrientationLandscapeRight];
        } else if (orientationMask == 1 || (orientationMask == 3 && !UIInterfaceOrientationIsPortrait(deviceOrientation))) {
            value = [NSNumber numberWithInt:UIInterfaceOrientationPortrait];
        } else if (orientationMask == 2) {
            value = [NSNumber numberWithInt:UIInterfaceOrientationPortraitUpsideDown];
        }
    } else {
        if (_lastOrientation != UIInterfaceOrientationUnknown) {
            [[UIDevice currentDevice] setValue:[NSNumber numberWithInt:_lastOrientation] forKey:@"orientation"];
            [self applySupportedOrientations:vc result:result selector:selector];
            [UINavigationController attemptRotationToDeviceOrientation];
        }
    }
    if (value != nil) {
        _isLocked = true;
        [[UIDevice currentDevice] setValue:value forKey:@"orientation"];
        [UIViewController attemptRotationToDeviceOrientation];
    } else {
        _isLocked = false;
    }
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 160000
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunguarded-availability-new"
// this will stop it complaining about new iOS16 APIs being used.
-(void)handleAboveEqualIos16WithOrientationMask:(NSInteger) orientationMask viewController: (CDVViewController*) vc result:(NSMutableArray*) result selector:(SEL) selector
{
    NSObject *value;
    // oritentationMask 15 is "unlock" the orientation lock.
    if (orientationMask != 15) {
        if (!_isLocked) {
            _lastOrientation = [UIApplication sharedApplication].statusBarOrientation;
        }
        UIInterfaceOrientation deviceOrientation = [UIApplication sharedApplication].statusBarOrientation;
        if(orientationMask == 8  || (orientationMask == 12  && !UIInterfaceOrientationIsLandscape(deviceOrientation))) {
            value = [[UIWindowSceneGeometryPreferencesIOS alloc] initWithInterfaceOrientations:UIInterfaceOrientationMaskLandscapeLeft];
        } else if (orientationMask == 4){
            value = [[UIWindowSceneGeometryPreferencesIOS alloc] initWithInterfaceOrientations:UIInterfaceOrientationMaskLandscapeRight];
        } else if (orientationMask == 1 || (orientationMask == 3 && !UIInterfaceOrientationIsPortrait(deviceOrientation))) {
            value = [[UIWindowSceneGeometryPreferencesIOS alloc] initWithInterfaceOrientations:UIInterfaceOrientationMaskPortrait];
        } else if (orientationMask == 2) {
            value = [[UIWindowSceneGeometryPreferencesIOS alloc] initWithInterfaceOrientations:UIInterfaceOrientationMaskPortraitUpsideDown];
        }
    } else {
        [self applySupportedOrientations:vc result:result selector:selector];
    }
    if (value != nil) {
        _isLocked = true;
        UIWindowScene *scene = [self activeWindowScene];
        if (scene != nil) {
            [scene requestGeometryUpdateWithPreferences:(UIWindowSceneGeometryPreferencesIOS*)value errorHandler:^(NSError * _Nonnull error) {
                NSLog(@"Failed to change orientation  %@ %@", error, [error userInfo]);
            }];
        } else {
            NSLog(@"Failed to change orientation: no active UIWindowScene found");
        }
    } else {
        _isLocked = false;
    }
}
#pragma clang diagnostic pop

-(void)handleWithOrientationMask:(NSInteger) orientationMask viewController: (CDVViewController*) vc result:(NSMutableArray*) result selector:(SEL) selector
{
    if (@available(iOS 16.0, *)) {
        [self handleAboveEqualIos16WithOrientationMask:orientationMask viewController:vc result:result selector:selector];
        // always double check the supported interfaces, so we update if needed
        // but do it right at the end here to avoid the "double" rotation issue reported in
        // https://github.com/apache/cordova-plugin-screen-orientation/pull/107
        [self.viewController setNeedsUpdateOfSupportedInterfaceOrientations];
    } else {
        [self handleBelowEqualIos15WithOrientationMask:orientationMask viewController:vc result:result selector:selector];
    }

}
#else
-(void)handleWithOrientationMask:(NSInteger) orientationMask viewController: (CDVViewController*) vc result:(NSMutableArray*) result selector:(SEL) selector
{
    [self handleBelowEqualIos15WithOrientationMask:orientationMask viewController:vc result:result selector:selector];
}
#endif


-(void)screenOrientation:(CDVInvokedUrlCommand *)command
{
    __weak CDVOrientation *weakSelf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        CDVOrientation *strongSelf = weakSelf;
        if (strongSelf == nil) {
            return;
        }

        CDVPluginResult* pluginResult;
        NSInteger orientationMask = [[command argumentAtIndex:0] integerValue];
        CDVViewController* vc = (CDVViewController*)strongSelf.viewController;
        NSMutableArray* result = [[NSMutableArray alloc] init];

        if(orientationMask & 1) {
            [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationPortrait]];
        }
        if(orientationMask & 2) {
            [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationPortraitUpsideDown]];
        }
        if(orientationMask & 4) {
            [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationLandscapeRight]];
        }
        if(orientationMask & 8) {
            [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationLandscapeLeft]];
        }
        SEL selector = NSSelectorFromString(@"setSupportedOrientations:");

        if (orientationMask != 15 || [UIDevice currentDevice] == nil) {
            [strongSelf applySupportedOrientations:vc result:result selector:selector];
        }

        if ([UIDevice currentDevice] != nil){
            [strongSelf handleWithOrientationMask:orientationMask viewController:vc result:result selector:selector];
        }

        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];

        [strongSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
}

@end
