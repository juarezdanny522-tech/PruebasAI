# Reglas de ProGuard/R8 para PruebasAI.

# LiteRT-LM usa JNI: conservar las clases que cargan las librerías nativas.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class com.google.ai.edge.litertlm.LiteRtLmJni { *; }
-dontwarn com.google.ai.edge.litertlm.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
