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
            shouldFullscreen = false;
        } else if (orientation.equals(LANDSCAPE_PRIMARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            shouldFullscreen = true;
        } else if (orientation.equals(PORTRAIT_PRIMARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            shouldFullscreen = false;
        } else if (orientation.equals(LANDSCAPE)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            shouldFullscreen = true;
        } else if (orientation.equals(PORTRAIT)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            shouldFullscreen = false;
        } else if (orientation.equals(LANDSCAPE_SECONDARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            shouldFullscreen = true;
        } else if (orientation.equals(PORTRAIT_SECONDARY)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            shouldFullscreen = false;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (fullscreen) {
                // 全屏模式：内容进入状态栏/刘海区域
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                window.setStatusBarColor(Color.TRANSPARENT);
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

                // 应用完整的沉浸式 UI flag
                int uiOptions = decorView.getSystemUiVisibility();
                uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decorView.setSystemUiVisibility(uiOptions);
                
                // 全屏时设置背景透明，避免导航栏区域显示白色
                decorView.setBackgroundColor(Color.TRANSPARENT);
                View rootViewFullscreen = window.getDecorView().findViewById(android.R.id.content);
                if (rootViewFullscreen != null) {
                    rootViewFullscreen.setBackgroundColor(Color.TRANSPARENT);
                }
            } else {
                // 非全屏模式：状态栏可见，但内容延伸到顶部
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                window.setStatusBarColor(Color.TRANSPARENT);
                WindowCompat.setDecorFitsSystemWindows(window, false);
                setStatusBarViewVisible(false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                        window.setAttributes(lp);
                    } catch (Exception e) {
                        Log.w("CDVOrientation", "Failed to restore layoutInDisplayCutoutMode", e);
                    }
                }

                // 保留布局进入状态栏区域，但不隐藏系统栏
                int uiOptions = decorView.getSystemUiVisibility();
                uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decorView.setSystemUiVisibility(uiOptions);
            }
        }

        consumeInsetsForFullscreen(activity, fullscreen);

        // 控制状态栏显隐与文字颜色
        if (Build.VERSION.SDK_INT >= 30 && decorView != null) {
            final WindowInsetsControllerCompat insetsController =
                    WindowCompat.getInsetsController(window, decorView);
            if (insetsController != null) {
                if (fullscreen) {
                    insetsController.hide(WindowInsetsCompat.Type.statusBars());
                } else {
                    insetsController.show(WindowInsetsCompat.Type.statusBars());
                }
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

        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        if (fullscreen) {
            // 全屏时强制消费所有 insets，让内容进入系统栏区域
            webViewView.setFitsSystemWindows(false);
            webViewView.setPadding(0, 0, 0, 0);
            webViewView.setBackgroundColor(Color.TRANSPARENT);
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

            if (rootView != null) {
                rootView.setFitsSystemWindows(false);
                rootView.setPadding(0, 0, 0, 0);
                rootView.setBackgroundColor(Color.TRANSPARENT);
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
            }
        } else {
            // 非全屏时保留状态栏可见，但不吃顶部 inset，内容顶到顶部
            webViewView.setFitsSystemWindows(false);
            ViewCompat.setOnApplyWindowInsetsListener(webViewView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    if (!forceFullscreen) {
                        final int left = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
                        final int right = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
                        final int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                        v.setPadding(left, 0, right, bottom);
                    }
                    return insets;
                }
            });

            if (rootView != null) {
                rootView.setFitsSystemWindows(false);
                rootView.setPadding(0, 0, 0, 0);
                ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
            }
        }

        ViewCompat.requestApplyInsets(webViewView);
        if (rootView != null) {
            ViewCompat.requestApplyInsets(rootView);
        }
        webViewView.requestLayout();
        if (rootView != null) {
            rootView.requestLayout();
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