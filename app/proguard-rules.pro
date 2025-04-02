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

# Правила для исключения конфликтов XML-парсера
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.**
-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }
-keep class org.kxml2.** { *; }
-keepclassmembers class org.kxml2.** { *; }

# Правила для OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keep class org.osmdroid.views.MapView { *; }
-keep class org.osmdroid.tileprovider.** { *; }

# Правила для обработки конфликта XmlResourceParser
-keep class android.content.res.XmlResourceParser { *; }
-keep class * implements org.xmlpull.v1.XmlPullParser { *; }
-keepclassmembers class * implements org.xmlpull.v1.XmlPullParser { *; }

# Дополнительные правила для R8
-dontwarn javax.annotation.**
-dontwarn org.xmlpull.**
-dontnote org.xmlpull.v1.**

# Полное исключение XmlPullParser из обработки
-keep class org.xmlpull.v1.** { *; }
-keepnames class org.xmlpull.** { *; }
-keepnames interface org.xmlpull.** { *; }

# Исключение конфликта с Android XmlResourceParser
-keep class * extends android.content.res.XmlResourceParser { *; }