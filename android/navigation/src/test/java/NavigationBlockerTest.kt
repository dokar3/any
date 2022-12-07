import any.navigation.NavigationBlocker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationBlockerTest {
    private val blocker = NavigationBlocker()

    @Test
    fun testBlock() {
        assertFalse(blocker.isBlocked("A"))
        assertFalse(blocker.isBlocked("B"))
        assertTrue(blocker.isAllowed("A"))
        assertTrue(blocker.isAllowed("B"))

        blocker.blockAll()

        assertTrue(blocker.isBlocked("A"))
        assertTrue(blocker.isBlocked("B"))
        assertFalse(blocker.isAllowed("A"))
        assertFalse(blocker.isAllowed("B"))

        blocker.block("A")
        assertTrue(blocker.isBlocked("A"))
        assertFalse(blocker.isAllowed("A"))

        blocker.allow("B")
        assertFalse(blocker.isBlocked("B"))
        assertTrue(blocker.isAllowed("B"))
    }
}