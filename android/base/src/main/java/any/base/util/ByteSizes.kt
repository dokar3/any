package any.base.util

inline val Int.KB: Long get() = this * 1024L

inline val Int.MB: Long get() = this.KB * 1024L

inline val Int.GB: Long get() = this.MB * 1024L
