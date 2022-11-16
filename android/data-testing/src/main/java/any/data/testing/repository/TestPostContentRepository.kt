package any.data.testing.repository

import any.data.repository.PostContentRepository
import any.data.testing.dao.TestPostContentDao

fun createTestPostContentRepository() = PostContentRepository(
    postContentDao = TestPostContentDao()
)