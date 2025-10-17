# Keep Kotlin serialization generated classes
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class ** Companion { *; }
