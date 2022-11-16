package any.data.testing.repository

import any.data.repository.PostRepository
import any.data.testing.source.post.TestLocalPostDataSource
import any.data.testing.source.post.TestServiceBridge
import kotlinx.coroutines.CoroutineDispatcher

fun createTestPostRepository(
    ioDispatcher: CoroutineDispatcher,
    localSource: TestLocalPostDataSource,
    serviceBridge: TestServiceBridge,
) = PostRepository(
    localDataSource = localSource,
    serviceBridge = serviceBridge,
    postContentRepository = createTestPostContentRepository(),
    ioDispatcher = ioDispatcher,
)