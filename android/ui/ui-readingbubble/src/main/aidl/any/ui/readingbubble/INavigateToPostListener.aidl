// INavigateToPostListener.aidl
package any.ui.readingbubble;

// Declare any non-default types here with import statements
import any.ui.readingbubble.entity.ReadingPost;

interface INavigateToPostListener {
     void onNavigate(in ReadingPost post);
}