package any.base.test

import any.base.image.PostImageSources
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostImageSourcesTest {
    @Test
    fun test_contains() {
        val source = PostImageSources.diskCache()
        assertTrue(source.contains(PostImageSources.diskCache()))

        val all = PostImageSources.all()
        assertTrue(all.contains(PostImageSources.memory()))
        assertTrue(all.contains(PostImageSources.diskCache()))
        assertTrue(all.contains(PostImageSources.downloadDir()))
        assertTrue(all.contains(PostImageSources.subsamplingCacheDir()))
        assertTrue(all.contains(PostImageSources.network()))
    }

    @Test
    fun test_plus() {
        val source = PostImageSources.diskCache() + PostImageSources.downloadDir()
        assertTrue(source.contains(PostImageSources.diskCache()))
        assertTrue(source.contains(PostImageSources.downloadDir()))
    }

    @Test
    fun test_minus() {
        val source = PostImageSources.diskCache() - PostImageSources.all()
        assertTrue(source == PostImageSources.none())

        val source2 = PostImageSources.all() - PostImageSources.memory()

        assertFalse(source2.contains(PostImageSources.memory()))
    }
}