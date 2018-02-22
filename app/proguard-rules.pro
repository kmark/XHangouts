# Add project specific ProGuard rules here.

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Shared between Xposed and the Settings app
-keep class com.versobit.kmark.xhangouts.XApp {
    static boolean isActive();
    static com.versobit.kmark.xhangouts.TestedCompatibilityDefinition getTestedVersion();
    static com.versobit.kmark.xhangouts.BuildConfigWrapper getXBuildConfig();
}
-keepclassmembers public class com.versobit.kmark.xhangouts.TestedCompatibilityDefinition {
    <init>(...);
}
-keepclassmembers public class com.versobit.kmark.xhangouts.BuildConfigWrapper {
    <init>(...);
}

-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    public void handleLoadPackage(de.robv.android.xposed.callbacks.LoadPackageParam);
}

-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit {
    public void initZygote(de.robv.android.xposed.IXposedHookZygoteInit.StartupParam);
}

-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources {
    public void handleInitPackageResources(de.robv.android.xposed.IXposedHookInitPackageResources.InitPackageResourcesParam);
}

-keepclassmembers class * extends de.robv.android.xposed.XC_MethodHook {
    protected void beforeHookedMethod(de.robv.android.xposed.XC_MethodHook.MethodHookParam);
    protected void afterHookedMethod(de.robv.android.xposed.XC_MethodHook.MethodHookParam);
}

-keepclassmembers class * extends de.robv.android.xposed.XC_MethodReplacement {
    protected Object replaceHookedMethod(de.robv.android.xposed.XC_MethodHook.MethodHookParam);
}

# Probably not neccesary due to its inclusion in android.content.res but eh
-keepclassmembers class * extends android.content.res.XResources.DrawableLoader {
    public abstract Drawable newDrawable(android.content.res.XResources, int);
    public Drawable newDrawableForDensity(android.content.res.XResources, int, int);
}

-keepclassmembers class * extends de.robv.android.xposed.callbacks.XC_LayoutInflated {
    public abstract void handleLayoutInflated(de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam);
}
