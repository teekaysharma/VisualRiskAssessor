# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }

# Keep model classes
-keep class com.hse.visualriskassessor.model.** { *; }
-keep class com.hse.visualriskassessor.data.** { *; }
