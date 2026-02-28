# kotlinx.serialization — keep serializers, companions, and generated code
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializable classes and their companions
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep the companion's serializer()
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated $$serializer classes
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1>$$serializer {
    *** INSTANCE;
    static <fields>;
}

# Keep kotlinx serialization runtime
-keep class kotlinx.serialization.** { *; }

# Keep the message model classes
-keep class com.skidl.model.** { *; }
-keep class com.skidl.network.SkidlMessageSerializer { *; }

# Java-WebSocket
-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ZXing
-keep class com.google.zxing.** { *; }
