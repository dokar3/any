package any.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import any.data.db.converter.ChecksumsConverter
import any.data.db.converter.JsPageKeyConverter
import any.data.db.converter.PostMediaConverter
import any.data.db.converter.PostReferenceConverter
import any.data.db.converter.ServiceConfigsConverter
import any.data.db.converter.ServiceResourcesConverter
import any.data.db.converter.ServiceViewTypeConverter
import any.data.db.converter.StringListConverter
import any.data.entity.Bookmark
import any.data.entity.FolderInfo
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.entity.User

@Database(
    entities = [
        User::class,
        Post::class,
        Bookmark::class,
        ServiceManifest::class,
        FolderInfo::class,
    ],
    version = 1,
)
@TypeConverters(
    value = [
        StringListConverter::class,
        ServiceConfigsConverter::class,
        ServiceViewTypeConverter::class,
        ServiceResourcesConverter::class,
        JsPageKeyConverter::class,
        PostMediaConverter::class,
        PostReferenceConverter::class,
        ChecksumsConverter::class,
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun postDao(): PostDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun serviceDao(): ServiceDao

    abstract fun folderInfoDao(): FolderInfoDao

    companion object {
        private const val DB_NAME = "any.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(AppDatabase::class.java) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .build()
                    .also { instance = it }
            }
        }
    }
}