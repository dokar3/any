// IReadingBubbleService.aidl
package any.ui.readingbubble;

// Declare any non-default types here with import statements
import any.ui.readingbubble.entity.ReadingPost;
import any.ui.readingbubble.INavigateToPostListener;

interface IReadingBubbleService {
    void addPost(in ReadingPost post);

    void removePost(in ReadingPost post);

    void clearPosts();

    void dismiss();

    void addNavigateListener(in INavigateToPostListener listener);

    void removeNavigateListener(in INavigateToPostListener listener);
}