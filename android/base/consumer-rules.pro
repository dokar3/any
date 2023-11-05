-keepclasseswithmembers class com.jakewharton.disklrucache.DiskLruCache { *; }
-keepclasseswithmembers class com.jakewharton.disklrucache.DiskLruCache$Editor { *; }
-keepclasseswithmembers class com.jakewharton.disklrucache.DiskLruCache$Entry { *; }

# Fixes java.lang.NoClassDefFoundError: Failed resolution of: Landroid/graphics/ColorSpace
-keep public class com.facebook.imageutils.BitmapUtil {
   public *;
}
-keep public class com.facebook.imageutils.ImageMetaData {
   public *;
}
