# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\AndroidSoft\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

-dontshrink
-keep class org.webrtc.**  { *; }
-keepclasseswithmembernames class * { native <methods>; }

-keep class okhttp3.** {*;}
-keepclassmembers class okhttp3.** {
  *;
}

-keep class com.tencent.**{*;}
-keepclassmembers class com.tenncent.mars.** {
  *;
}

#-keep class !cn.wildfire.chat.moment.**,!cn.wildfirechat.moment.**, **{ *; }
-keep class cn.wildfirechat.moment.MomentClient {
    public void init(***);
}

-keep class cn.tojoy.chat.kit.net.base.** {*;}
-keepclassmembers class cn.tojoy.chat.kit.net.base.** {
  *;
}

-keep class cn.tojoy.chat.kit.group.GroupAnnouncement {*;}
-keepclassmembers class cn.tojoy.chat.kit.group.GroupAnnouncement {
  *;
}

-keep class cn.wildfirechat.model.** {*;}
-keepclassmembers class cn.wildfirechat.model.** {
  *;
}

-keepclassmembers class cn.wildfirechat.** {
    <init>(...);
}

-keepclassmembers class cn.tojoy.** {
    <init>(...);
}

-keep class net.sourceforge.pinyin4j.** { *;}

-keep class com.tencent.tencentmap.**{*;}
-keep class com.tencent.map.**{*;}
-keep class com.tencent.beacontmap.**{*;}
-keep class navsns.**{*;}
-dontwarn com.qq.**
-dontwarn com.tencent.**

