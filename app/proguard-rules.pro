# Keep JavascriptInterface bridge methods used by the WebView.
-keepclassmembers class com.callcheck.app.MainActivity$Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
