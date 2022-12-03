package any.base.util


inline fun <C> C?.ifNullOrEmpty(defaultValue: () -> C?): C? where C : Collection<*> =
    if (isNullOrEmpty()) defaultValue() else this

inline fun <E> MutableList<E>.updateIf(
    predicate: (E) -> Boolean,
    update: (E) -> E,
) {
    val idx = indexOfFirst(predicate)
    if (idx != -1) {
        val new = update(get(idx))
        set(idx, new)
    }
}

inline fun <E> MutableList<E>.findOrAdd(
    predicate: (E) -> Boolean,
    newEntry: () -> E,
): E {
    return find(predicate) ?: newEntry().also { add(it) }
}

/**
 * Replace old items in the current with new items in the latest list.
 *
 * @param latest The latest list.
 * @param key The key selector to find the target item in the original list.
 * @return The updated list if at least one item has been updated, or the original list if no.
 */
inline fun <E, K> List<E>.updateWith(latest: List<E>, key: (E) -> K): List<E> {
    val mutable = toMutableList()
    var listChanged = false
    latest.forEach { item ->
        val index = indexOfFirst { key(it) == key(item) }
        if (index != -1) {
            mutable[index] = item
            listChanged = true
        }
    }
    return if (listChanged) {
        mutable.toList()
    } else {
        this
    }
}
