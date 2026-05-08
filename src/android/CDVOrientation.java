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

package cordova.plugins.screenorientation;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.os.Build;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class CDVOrientation extends CordovaPlugin {
    
    private static final String TAG = "YoikScreenOrientation"; 
    
    /**
     * Screen Orientation Constants
     */
    
    private static final String ANY = "any";
    private static final String PORTRAIT_PRIMARY = "portrait-primary";
    private static final String PORTRAIT_SECONDARY = "portrait-secondary";
    private static final String LANDSCAPE_PRIMARY = "landscape-primary";
    private static final String LANDSCAPE_SECONDARY = "landscape-secondary";
    private static final String PORTRAIT = "portrait";
    private static final String LANDSCAPE = "landscape";
    private boolean forceFullscreen;
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        
        Log.d(TAG, "execute action: " + action);
        
        // Route the Action
        if (action.equals("screenOrientation")) {
            return routeScreenOrientation(args, callbackContext);
        }
        
        // Action not found
        callbackContext.error("action not recognised");
        return false;
    }
    
    private boolean routeScreenOrientation(JSONArray args, CallbackContext callbackContext) {
        
        String action = args.optString(0);
        
        
        
        String orientation = args.optString(1);
        
        Log.d(TAG, "Requested ScreenOrientation: " + orientation);
        
        final Activity activity = cordova.getActivity();
        final int requestedOrientation;
        final boolean shouldFullscreen;

        if (orientation.equals(ANY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            shouldFullscreen = true;
        } else if (orientation.equals(LANDSCAPE_PRIMARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            shouldFullscreen = true;
        } else if (orientation.equals(PORTRAIT_PRIMARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            shouldFullscreen = true;
        } else if (orientation.equals(LANDSCAPE)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            shouldFullscreen = true;
        } else if (orientation.equals(PORTRAIT)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            shouldFullscreen = true;
        } else if (orientation.equals(LANDSCAPE_SECONDARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            shouldFullscreen = true;
        } else if (orientation.equals(PORTRAIT_SECONDARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            shouldFullscreen = true;
        } else {
            callbackContext.error("orientation not recognised");
            return false;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                forceFullscreen = shouldFullscreen;
                activity.setRequestedOrientation(requestedOrientation);
                setFullscreen(activity, shouldFullscreen);
                reapplyFullscreenAfterRotation(activity);
            }
        });
        
        callbackContext.success();
        return true;
    }

    private void setFullscreen(Activity activity, boolean fullscreen) {
        final Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        final View decorView = window.getDecorView();
        int uiOptions = (decorView != null) ? decorView.getSystemUiVisibility() : 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (fullscreen) {
                // 全屏模式：内容进入状态栏/刘海区域
                window.setStatusBarColor(Color.TRANSPARENT);
                uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                WindowCompat.setDecorFitsSystemWindows(window, false);
                setStatusBarViewVisible(false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                        window.setAttributes(lp);
                    } catch (Exception e) {
                        Log.w("CDVOrientation", "Failed to set layoutInDisplayCutoutMode", e);
                    }
                }
                
                // 隐藏导航栏
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            } else {
                // 非全屏模式：内容回到状态栏下方
                uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                WindowCompat.setDecorFitsSystemWindows(window, true);
                setStatusBarViewVisible(true);

                window.setStatusBarColor(Color.BLACK);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                        window.setAttributes(lp);
                    } catch (Exception e) {
                        Log.w("CDVOrientation", "Failed to restore layoutInDisplayCutoutMode", e);
                    }
                }
            }
        }

        if (decorView != null) {
            decorView.setSystemUiVisibility(uiOptions);
        }

        consumeInsetsForFullscreen(activity, fullscreen);

        // 处理状态栏文字颜色
        if (Build.VERSION.SDK_INT >= 30 && decorView != null) {
            final WindowInsetsControllerCompat insetsController =
                    WindowCompat.getInsetsController(window, decorView);
            if (insetsController != null) {
                insetsController.setAppearanceLightStatusBars(!fullscreen);
            }
        }
    }

    private void reapplyFullscreenAfterRotation(final Activity activity) {
        final Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        final View decorView = window.getDecorView();
        if (decorView == null) {
            return;
        }

        decorView.post(new Runnable() {
            @Override
            public void run() {
                setFullscreen(activity, forceFullscreen);
            }
        });

        decorView.postDelayed(new Runnable() {
            @Override
            public void run() {
                setFullscreen(activity, forceFullscreen);
            }
        }, 250);
    }

    private void consumeInsetsForFullscreen(Activity activity, boolean fullscreen) {
        final View webViewView = (webView != null) ? webView.getView() : null;
        if (webViewView == null) {
            return;
        }

        if (fullscreen) {
            webViewView.setFitsSystemWindows(false);
            webViewView.setPadding(0, 0, 0, 0);
            ViewCompat.setOnApplyWindowInsetsListener(webViewView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    if (forceFullscreen) {
                        v.setPadding(0, 0, 0, 0);
                        return WindowInsetsCompat.CONSUMED;
                    }
                    return insets;
                }
            });
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(webViewView, null);
        }

        ViewCompat.requestApplyInsets(webViewView);

        // 同时对根容器也做 Insets 消费
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
            rootView.setPadding(0, 0, 0, 0);
            if (fullscreen) {
                ViewCompat.setOnApplyWindowInsetsListener(rootView, new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                        if (forceFullscreen) {
                            v.setPadding(0, 0, 0, 0);
                            return WindowInsetsCompat.CONSUMED;
                        }
                        return insets;
                    }
                });
            } else {
                ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
            }
            ViewCompat.requestApplyInsets(rootView);
        }
    }

    private void setStatusBarViewVisible(boolean visible) {
        Activity activity = cordova.getActivity();
        if (activity == null) return;
        
        View root = activity.getWindow().getDecorView();
        if (root == null) return;

        View statusBarView = root.findViewWithTag("statusBarView");
        if (statusBarView == null) return;

        statusBarView.setVisibility(visible ? View.VISIBLE : View.GONE);

        View parent = (statusBarView.getParent() instanceof View) ? (View) statusBarView.getParent() : null;
        if (parent != null) {
            parent.requestApplyInsets();
        }
    }
}