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

# Keep crash line numbers readable in this test build.
-keepattributes SourceFile,LineNumberTable

# --- JNI: native methods are matched by exact class/method name from C, so keep
#     any class that declares native methods plus those method names verbatim. ---
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# --- WorkManager instantiates our workers by class name via reflection. ---
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- youtubedl-android: reflection, Jackson JSON mappers, bundled python payload. ---
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*,Signature,EnclosingMethod,InnerClasses
-dontwarn com.fasterxml.jackson.**
# yt-dlp unpacks its Python payload on first run via commons-compress + XZ, which
# instantiate (de)compressor classes reflectively — keep them concrete or the unpack
# throws "class ... is not a concrete class" (only on a fresh install).
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-keep class org.tukaani.xz.** { *; }
-dontwarn org.tukaani.xz.**
-keep class org.brotli.** { *; }
-dontwarn org.brotli.**

# --- sherpa-onnx JNI bindings (libs loaded alongside our own native code). ---
-keep class com.k2fsa.** { *; }
-dontwarn com.k2fsa.**