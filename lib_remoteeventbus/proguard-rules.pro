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

# ================== basic pameter =====================

-keepattributes Exceptions,InnerClasses,Signature #保留内部接口或内部类、内部类、泛型签名类型
-keepattributes SourceFile, LineNumberTable #崩溃抛出异常时,保留源码文件名和源码行号
-renamesourcefileattribute SourceFile
-keepattributes *Annotation* #保留注释

#未混淆的类和成员
-printseeds   build/outputs/aar/proguard/seeds.txt
#列出删除的代码
-printusage   build/outputs/aar/proguard/unused.txt
#混淆前后的映射
-printmapping build/outputs/aar/proguard/mapping.txt
#字典文件
-obfuscationdictionary build/outputs/aar/proguard/dictionary.txt
-classobfuscationdictionary build/outputs/aar/proguard/dictionary.txt
-packageobfuscationdictionary build/outputs/aar/proguard/dictionary.txt

# ================== common prolicy =====================

#eventBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keep class * implements android.os.Parcelable{
    public static final android.os.Parcelable$Creator *;
}

#android framework
-keep class android.app.**  {*;}
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Appliction
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService

# 保留AndroidX下的所有类及其内部类
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
# 保留support下的所有类及其内部类
-keep class android.support.** {*;}
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**
# 保留R下面的资源
-keep class **.R$* {*;}
# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

#不混淆反射用到的类
 -keepattributes Signature
 -keepattributes EnclosingMethod
# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

#disable debug log
-assumenosideeffects class java.lang.System {
    public static *** out(...);
}

# ================== other =====================

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    # public static *** i(...);
    # public static *** w(...);
    # public static *** e(...);
}

-keep public interface kuyou.common.ipc.basic.IRemoteConfig{ *; }
-keep public interface * extends kuyou.common.ipc.basic.IRemoteConfig { public <fields>; }

-keep public class kuyou.common.ipc.event.RemoteEvent{ 
    public <methods>;
    protected <methods>;
}
-keep public class kuyou.common.ipc.event.EventFrame

-keep public class kuyou.common.ipc.RemoteEventBus{
    public static kuyou.common.ipc.RemoteEventBus getInstance();
    public void binder(kuyou.common.ipc.RemoteEventBus$IRegisterConfig);
}
-keep public class kuyou.common.ipc.EventDispatcherImpl{
    public static kuyou.common.ipc.EventDispatcherImpl getInstance();
    public kuyou.common.ipc.EventDispatcherImpl setEventReceiveList(java.util.List);
    public kuyou.common.ipc.EventDispatcherImpl setLocalModulePackageName(java.lang.String);
}
-keep public class kuyou.common.assist.BasicLocalModuleApplication{
    protected void initFrame();
    protected java.lang.String getIpcFramePackageName();
}