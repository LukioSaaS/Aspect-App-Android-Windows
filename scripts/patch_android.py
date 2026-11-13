"""Ajustes no projeto Android gerado pelo Capacitor:
- libera servidores HTTP (cleartext)
- aparece na tela inicial da Android TV (leanback + banner) e funciona sem toque
- tela cheia imersiva e tela sempre ligada
- versionCode sobe a cada build (para instalar por cima)
"""
import os
import re
import shutil

APP = "android/app/src/main"
MANIFEST = APP + "/AndroidManifest.xml"
GRADLE = "android/app/build.gradle"

# ---------- AndroidManifest ----------
m = open(MANIFEST, encoding="utf-8").read()
PKG = "app.aspect.player"
if "OPTIONS_PROVIDER_CLASS_NAME" not in m:
    m = m.replace(
        "</application>",
        '    <meta-data android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME" android:value="' + PKG + '.CastOptionsProvider" />\n    </application>',
        1,
    )
if "usesCleartextTraffic" not in m:
    m = m.replace("<application", '<application\n        android:usesCleartextTraffic="true"\n        android:largeHeap="true"\n        android:hardwareAccelerated="true"\n        android:banner="@drawable/banner"', 1)
if "LEANBACK_LAUNCHER" not in m:
    m = m.replace(
        '<category android:name="android.intent.category.LAUNCHER" />',
        '<category android:name="android.intent.category.LAUNCHER" />\n                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />',
        1,
    )
if "android.software.leanback" not in m:
    m = m.replace(
        "</application>",
        '</application>\n\n    <uses-feature android:name="android.software.leanback" android:required="false" />\n    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />',
        1,
    )
open(MANIFEST, "w", encoding="utf-8").write(m)

# ---------- banner da Android TV ----------
os.makedirs(APP + "/res/drawable", exist_ok=True)
shutil.copy("assets/banner.png", APP + "/res/drawable/banner.png")

# ---------- MainActivity: tela cheia imersiva + tela ligada ----------
java_dir = None
for root, _dirs, files in os.walk(APP + "/java"):
    if "MainActivity.java" in files:
        java_dir = root
        break
if java_dir is None:
    raise SystemExit("MainActivity.java nao encontrado")
pkg = java_dir.split("/java/", 1)[1].replace("/", ".")
JAVA = """package %s;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.webkit.WebView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private String insetsJs = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AspectCastPlugin.class);
        registerPlugin(AspectPlayerPlugin.class);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#050505"));

        // Ocupa a tela toda, inclusive ao redor da camera (notch / furo)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // Envia as margens seguras (camera e barras) para o app usar no CSS
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, windowInsets) -> {
            Insets i = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            float d = getResources().getDisplayMetrics().density;
            insetsJs = "(function(){var s=document.documentElement.style;"
                + "s.setProperty('--ins-t','" + (i.top / d) + "px');"
                + "s.setProperty('--ins-b','" + (i.bottom / d) + "px');"
                + "s.setProperty('--ins-l','" + (i.left / d) + "px');"
                + "s.setProperty('--ins-r','" + (i.right / d) + "px');})();";
            pushInsets();
            return windowInsets;
        });

        hideBars();
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(this::pushInsets, 800);
        h.postDelayed(this::pushInsets, 2000);
        h.postDelayed(this::pushInsets, 4500);
    }

    @Override
    public void onResume() {
        super.onResume();
        pushInsets();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideBars();
            pushInsets();
        }
    }

    private void pushInsets() {
        if (insetsJs.isEmpty() || getBridge() == null || getBridge().getWebView() == null) return;
        final WebView w = getBridge().getWebView();
        final String js = insetsJs;
        w.post(() -> w.evaluateJavascript(js, null));
    }

    private void hideBars() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
"""
open(java_dir + "/MainActivity.java", "w", encoding="utf-8").write(JAVA % pkg)

# ---------- Google Cast (classes nativas + dependências) ----------
for name in ("AspectCastPlugin.java", "CastOptionsProvider.java", "AspectPlayerPlugin.java"):
    src = open("scripts/native/" + name, encoding="utf-8").read().replace("__PKG__", pkg)
    open(java_dir + "/" + name, "w", encoding="utf-8").write(src)

g0 = open(GRADLE, encoding="utf-8").read()
if "play-services-cast-framework" not in g0:
    g0 = g0.replace(
        "dependencies {",
        "dependencies {\n    implementation 'com.google.android.gms:play-services-cast-framework:21.5.0'\n    implementation 'androidx.mediarouter:mediarouter:1.7.0'\n    implementation 'androidx.media3:media3-exoplayer:1.4.1'\n    implementation 'androidx.media3:media3-exoplayer-hls:1.4.1'\n    implementation 'androidx.media3:media3-ui:1.4.1'",
        1,
    )
    open(GRADLE, "w", encoding="utf-8").write(g0)

VARS = "android/variables.gradle"
if os.path.exists(VARS):
    v = open(VARS, encoding="utf-8").read()
    v = re.sub(r"minSdkVersion\s*=\s*\d+", "minSdkVersion = 24", v, count=1)
    open(VARS, "w", encoding="utf-8").write(v)

# ---------- versionCode ----------
g = open(GRADLE, encoding="utf-8").read()
n = os.environ.get("BUILD_NUMBER", "1")
g = re.sub(r"versionCode\s+\d+", "versionCode " + n, g, count=1)
g = re.sub(r'versionName\s+"[^"]*"', 'versionName "1.' + n + '"', g, count=1)
open(GRADLE, "w", encoding="utf-8").write(g)
print("ajustes aplicados, pacote:", pkg)
