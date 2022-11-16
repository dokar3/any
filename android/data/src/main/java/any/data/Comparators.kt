package any.data

import any.data.entity.Folder
import any.data.entity.HierarchicalFolder
import any.data.entity.Post
import any.data.entity.ServiceManifest
import java.text.Collator

object Comparators {
    private val collator = Collator.getInstance()

    val stringComparator = Comparator<String> { o1, o2 ->
        collator.compare(o1, o2)
    }

    val hierarchicalFolderNameComparator = Comparator<HierarchicalFolder> { o1, o2 ->
        collator.compare(o1.name, o2.name)
    }

    val folderNameComparator = Comparator<Folder> { o1, o2 ->
        collator.compare(o1.name, o2.name)
    }

    val postTitleComparator = Comparator<Post> { o1, o2 ->
        collator.compare(o1.title, o2.title)
    }

    val serviceManifestNameComparator = Comparator<ServiceManifest> { o1, o2 ->
        collator.compare(o1.name, o2.name)
    }
}