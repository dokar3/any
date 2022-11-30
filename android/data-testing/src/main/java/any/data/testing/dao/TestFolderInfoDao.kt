package any.data.testing.dao

import any.data.db.FolderInfoDao
import any.data.entity.FolderInfo

class TestFolderInfoDao : FolderInfoDao {
    override suspend fun get(path: String): FolderInfo? {
        return null
    }

    override suspend fun getAll(): List<FolderInfo> {
        return emptyList()
    }

    override suspend fun add(folderInfo: FolderInfo) {
    }

    override suspend fun add(list: List<FolderInfo>) {
    }

    override suspend fun update(folderInfo: FolderInfo) {
    }

    override suspend fun remove(folderInfo: FolderInfo) {
    }

    override suspend fun remove(path: String) {
    }

    override suspend fun clear() {
    }
}