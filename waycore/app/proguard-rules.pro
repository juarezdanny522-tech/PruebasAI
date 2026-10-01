# ── LiteRT-LM (IA local) ──
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class * implements com.google.ai.edge.litertlm.ToolSet { *; }
-keepclassmembers class * {
    @com.google.ai.edge.litertlm.Tool <methods>;
}
-dontwarn com.google.ai.edge.litertlm.**
-dontwarn sun.misc.Unsafe
-dontwarn org.slf4j.**

# ── Gson usado internamente por LiteRT-LM ──
-keep class com.google.gson.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# ── WayCore ──
-keep class com.wayhat.waycore.** { *; }
-keep class org.json.** { *; }
