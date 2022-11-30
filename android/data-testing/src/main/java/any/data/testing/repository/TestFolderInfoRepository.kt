package any.data.testing.repository

import any.data.repository.FolderInfoRepository
import any.data.testing.dao.TestFolderInfoDao

fun createTestFolderInfoRepository() = FolderInfoRepository(
    folderInfoDao = TestFolderInfoDao()
)