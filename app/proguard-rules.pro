# Strip verbose logging from release builds.
# Warn/error still go through so Crashlytics / user reports keep working.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}