package any.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import any.data.db.converter.PostContentElementsConverter
import any.data.entity.PostContent

@Database(
    entities = [
        PostContent::class,
    ],
    version = 1,
)
@TypeConverters(
    value = [
        PostContentElementsConverter::class,
    ]
)
abstract class PostContentDatabase : RoomDatabase() {
    abstract fun postContentDao(): PostContentDao

    companion object {
        private const val DB_NAME = "post_content.db"

        @Volatile
        private var instance: PostContentDatabase? = null

        fun get(context: Context): PostContentDatabase {
            return instance ?: synchronized(PostContentDatabase::class) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        PostContentDatabase::class.java,
                        DB_NAME
                    )
                    .build()
                    .also { instance = it }
            }
        }
    }
}