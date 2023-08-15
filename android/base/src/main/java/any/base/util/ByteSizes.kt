package any.base.util

inline val Double.KB: Long get() = (this * 1024).toLong()

inline val Double.MB: Long get() = this.KB * 1024

inline val Double.GB: Long get() = this.MB * 1024

inline val Int.KB: Long get() = this * 1024L

inline val Int.MB: Long get() = this.KB * 1024L

inline val Int.GB: Long get() = this.MB * 1024L
