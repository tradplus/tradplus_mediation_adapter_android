# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-verbose
-optimizationpasses 5
-dontusemixedcaseclassnames
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-dontwarn android.support.**
-keepattributes *Annotation*
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault


-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-keep class com.kidoz.sdk.api.**{
   *;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}


-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


-keepclassmembers class fqcn.of.javascript.interface.for.webview {
    public *;
 }

-keepclassmembers class * {
     @android.webkit.JavascriptInterface <methods>;
 }
-keepattributes JavascriptInterface

-dontwarn okio.**
-keep class com.squareup.okhttp3.** {*;}
-dontwarn com.squareup.okhttp3.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keep public class pl.droidsonroids.gif.GifIOException{<init>(int);}
-keep class pl.droidsonroids.gif.GifInfoHandle{<init>(long,int,int,int);}

-dontwarn android.webkit.**
-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient
-keep public class android.webkit.WebView