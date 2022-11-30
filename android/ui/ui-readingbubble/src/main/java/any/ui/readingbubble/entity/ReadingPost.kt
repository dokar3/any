package any.ui.readingbubble.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import any.data.entity.Post

@Immutable
data class ReadingPost(
    val serviceId: String,
    val url: String,
    val title: String,
    val source: String? = null,
    val author: String? = null,
    val summary: String? = null,
    val thumbnail: String? = null,
    val elementIndex: Int = 0,
    val elementScrollOffset: Int = 0,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        checkNotNull(parcel.readString()),
        checkNotNull(parcel.readString()),
        checkNotNull(parcel.readString()),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serviceId)
        parcel.writeString(url)
        parcel.writeString(title)
        parcel.writeString(source)
        parcel.writeString(author)
        parcel.writeString(summary)
        parcel.writeString(thumbnail)
        parcel.writeInt(elementScrollOffset)
        parcel.writeInt(elementScrollOffset)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ReadingPost> {
            override fun createFromParcel(parcel: Parcel): ReadingPost {
                return ReadingPost(parcel)
            }

            override fun newArray(size: Int): Array<ReadingPost?> {
                return arrayOfNulls(size)
            }
        }

        fun fromPost(
            post: Post,
            source: String? = null,
            elementIndex: Int = -1,
            elementScrollOffset: Int = 0,
        ): ReadingPost {
            return ReadingPost(
                serviceId = post.serviceId,
                url = post.url,
                title = post.title,
                source = source,
                author = post.author,
                summary = post.summary,
                thumbnail = post.media?.firstOrNull()?.thumbnailOrNull() ?: "",
                elementIndex = elementIndex,
                elementScrollOffset = elementScrollOffset,
            )
        }
    }
}
