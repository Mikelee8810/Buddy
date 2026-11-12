-keep class com.chaquo.python.** { *; }
-keep class com.scribe.app.api.PythonBridge { *; }

# Shizuku: newProcess is called by reflection
-keep class rikka.shizuku.Shizuku { *; }
