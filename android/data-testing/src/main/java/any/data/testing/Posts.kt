package any.data.testing

import any.data.entity.Post

fun List<Post>.markInFresh(): List<Post> {
    return mapIndexed { index, post -> post.copy(orderInFresh = index) }
}
