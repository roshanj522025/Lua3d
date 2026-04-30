# LuaJ – keep all reflection-accessed classes
-keep class org.luaj.** { *; }
-dontwarn org.luaj.**

# Keep game framework for Lua reflection
-keep class com.luagame.framework.** { *; }

# JOML math library
-keep class org.joml.** { *; }
