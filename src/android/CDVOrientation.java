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
import java.util.Locale;

import android.app.Activity;
import android.os.Build;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

        private static final String[] MAINLAND_OEM_TOKENS = new String[] {
            "oppo", "oneplus", "realme", "vivo", "iqoo",
            "xiaomi", "redmi", "huawei", "honor", "meizu"
        };
    
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
            final boolean useMainlandFallback = fullscreen && shouldUseMainlandCompatibilityFallback();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            if (fullscreen) {
                // 横屏全屏：默认四边铺满；大陆 ROM 命中降级策略时转为兼容模式
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                final int fullscreenBgColor = Color.TRANSPARENT;
                window.setBackgroundDrawable(new ColorDrawable(fullscreenBgColor));
                window.setStatusBarColor(fullscreenBgColor);
                window.setNavigationBarColor(fullscreenBgColor);
                WindowCompat.setDecorFitsSystemWindows(window, false);

                // 强制禁用对比度保护
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        window.setStatusBarContrastEnforced(false);
                        window.setNavigationBarContrastEnforced(false);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to disable contrast enforcement", e);
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                        window.setAttributes(lp);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to set layoutInDisplayCutoutMode", e);
                    }
                }

                // 激进的沉浸式：HIDE_NAVIGATION + FULLSCREEN + IMMERSIVE_STICKY
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decorView.setSystemUiVisibility(uiOptions);

                // 当系统栏被 ROM 或手势拉出时，立即重新隐藏虚拟按键
                decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0 ||
                                (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                            hideNavigationBar(decorView);
                        }
                    }
                });

                // API 30+ 也下发 WindowInsetsController 隐藏
                if (Build.VERSION.SDK_INT >= 30) {
                    final WindowInsetsControllerCompat insetsController =
                            WindowCompat.getInsetsController(window, decorView);
                    if (insetsController != null) {
                        insetsController.setSystemBarsBehavior(
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                        insetsController.hide(WindowInsetsCompat.Type.systemBars());
                    }
                }

                decorView.setBackgroundColor(fullscreenBgColor);
                View rootViewFullscreen = window.getDecorView().findViewById(android.R.id.content);
                if (rootViewFullscreen != null) {
                    rootViewFullscreen.setBackgroundColor(fullscreenBgColor);
                }
            } else {
                // 非全屏时使用 DRAWS_SYSTEM_BAR_BACKGROUNDS + 透明系统栏
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);
                WindowCompat.setDecorFitsSystemWindows(window, false);
                decorView.setOnSystemUiVisibilityChangeListener(null);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.setStatusBarContrastEnforced(false);
                    window.setNavigationBarContrastEnforced(false);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.setNavigationBarDividerColor(Color.TRANSPARENT);
                    try {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                        window.setAttributes(lp);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to restore layoutInDisplayCutoutMode", e);
                    }
                }

                if (Build.VERSION.SDK_INT >= 30) {
                    // 清理全屏残留 flag，恢复系统栏
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    decorView.setSystemUiVisibility(uiOptions);

                    final WindowInsetsControllerCompat insetsController =
                            WindowCompat.getInsetsController(window, decorView);
                    if (insetsController != null) {
                        insetsController.show(WindowInsetsCompat.Type.systemBars());
                        insetsController.setAppearanceLightStatusBars(true);
                    }
                } else {
                    int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    decorView.setSystemUiVisibility(uiOptions);
                }
            }
        }

        consumeInsetsForFullscreen(activity, fullscreen, fullscreen && shouldUseMainlandCompatibilityFallback());
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
                // 如果是全屏，立即再次隐藏虚拟导航栏以确保生效
                if (forceFullscreen) {
                    hideNavigationBar(decorView);
                }
            }
        });

        decorView.postDelayed(new Runnable() {
            @Override
            public void run() {
                setFullscreen(activity, forceFullscreen);
                if (forceFullscreen) {
                    hideNavigationBar(decorView);
                }
            }
        }, 250);

        decorView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (forceFullscreen) {
                    hideNavigationBar(decorView);
                }
            }
        }, 500);
    }

    private void hideNavigationBar(View decorView) {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void consumeInsetsForFullscreen(Activity activity, boolean fullscreen, boolean useMainlandFallback) {
        final View webViewView = (webView != null) ? webView.getView() : null;
        if (webViewView == null) {
            return;
        }

        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        if (fullscreen) {
            // 兼容模式下把左右安全区转为上下黑边；其他机型保持四边铺满
            webViewView.setFitsSystemWindows(false);
            webViewView.setBackgroundColor(useMainlandFallback ? Color.BLACK : Color.TRANSPARENT);
            webViewView.setPadding(0, 0, 0, 0);
            ViewCompat.setOnApplyWindowInsetsListener(webViewView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    if (useMainlandFallback) {
                        final androidx.core.graphics.Insets barsInsets =
                                insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                        final int sideInset = Math.max(barsInsets.left, barsInsets.right);
                        final int verticalInset = sideInset;
                        v.setPadding(0, verticalInset, 0, verticalInset);
                    } else {
                        v.setPadding(0, 0, 0, 0);
                    }
                    return WindowInsetsCompat.CONSUMED;
                }
            });

            if (rootView != null) {
                rootView.setFitsSystemWindows(false);
                rootView.setBackgroundColor(useMainlandFallback ? Color.BLACK : Color.TRANSPARENT);
                rootView.setPadding(0, 0, 0, 0);
                // 避免 root + webView 双层补边导致画面二次缩小
                ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
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

    private boolean shouldUseMainlandCompatibilityFallback() {
        final String manufacturer = Build.MANUFACTURER != null
                ? Build.MANUFACTURER.toLowerCase(Locale.ROOT)
                : "";
        final String brand = Build.BRAND != null
                ? Build.BRAND.toLowerCase(Locale.ROOT)
                : "";

        for (String token : MAINLAND_OEM_TOKENS) {
            if (manufacturer.contains(token) || brand.contains(token)) {
                Log.w(TAG, "Enable mainland fullscreen fallback for OEM: " + manufacturer + "/" + brand);
                return true;
            }
        }

        return false;
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