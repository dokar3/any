package any.data.repository

import android.content.Context
import any.data.db.AppDatabase
import any.data.db.FolderInfoDao
import any.data.entity.FolderInfo

class FolderInfoRepository(
    private val folderInfoDao: FolderInfoDao,
): ReactiveRepository<String, FolderInfo>() {
    suspend fun get(path: String): FolderInfo? {
        return folderInfoDao.get(path)
    }

    suspend fun add(folderInfo: FolderInfo) {
        folderInfoDao.add(folderInfo)
    }

    suspend fun update(folderInfo: FolderInfo) {
        folderInfoDao.update(folderInfo)
        notifyUpdated(folderInfo)
    }

    suspend fun remove(path: String) {
        folderInfoDao.remove(path)
        notifyDeletedByKey(path)
    }

    suspend fun remove(folderInfo: FolderInfo) {
        folderInfoDao.remove(folderInfo)
        notifyDeletedByItem(folderInfo)
    }

    companion object {
        @Volatile
        private var instance: FolderInfoRepository? = null

        fun getDefault(context: Context): FolderInfoRepository {
            return instance ?: synchronized(FolderInfoRepository::class) {
                instance ?: FolderInfoRepository(
                    folderInfoDao = AppDatabase.get(context).folderInfoDao(),
                ).also { instance = it }
            }
        }
    }
}