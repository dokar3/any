package any.data.repository

import any.data.repository.ReactiveRepository.Change
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * A repository whose data changes can be notified and observed.
 *
 * [K] the key type for [Change.DeletedByKey] changes.
 *
 * [I] the item type for [Change.Inserted], [Change.Updated] and [Change.DeletedByItem] changes.
 */
abstract class ReactiveRepository<K, I> {
    private val _changesFlow = MutableSharedFlow<List<Change>>()

    val changes: Flow<List<Change>> = _changesFlow

    val insertions: Flow<List<Change.Inserted<I>>>
        get() = _changesFlow.filterByChangeType()

    val updates: Flow<List<Change.Updated<I>>>
        get() = _changesFlow.filterByChangeType()

    val deletions: Flow<List<Change>>
        get() = _changesFlow
            .mapNotNull { changes ->
                changes.filter {
                    it is Change.DeletedByKey<*> || it is Change.DeletedByItem<*>
                }
            }
            .filter { it.isNotEmpty() }

    suspend fun notifyInserted(item: I) {
        _changesFlow.emit(listOf(Change.Inserted(item)))
    }

    suspend fun notifyInserted(items: List<I>) {
        _changesFlow.emit(items.map { Change.Inserted(it) })
    }

    suspend fun notifyUpdated(item: I) {
        _changesFlow.emit(listOf(Change.Updated(item)))
    }

    suspend fun notifyUpdated(items: List<I>) {
        _changesFlow.emit(items.map { Change.Updated(it) })
    }

    suspend fun notifyDeletedByKey(key: K) {
        _changesFlow.emit(listOf(Change.DeletedByKey(key)))
    }

    suspend fun notifyDeletedByKey(keys: List<K>) {
        _changesFlow.emit(keys.map { Change.DeletedByKey(it) })
    }

    suspend fun notifyDeletedByItem(item: I) {
        _changesFlow.emit(listOf(Change.DeletedByItem(item)))
    }

    suspend fun notifyDeletedByItem(items: List<I>) {
        _changesFlow.emit(items.map { Change.DeletedByItem(it) })
    }

    sealed interface Change {
        class Inserted<T>(val item: T) : Change

        class Updated<T>(val item: T) : Change

        class DeletedByItem<T>(val item: T) : Change

        class DeletedByKey<K>(val key: K) : Change
    }

    companion object {
        inline fun <reified T : Change> Flow<List<Change>>.filterByChangeType():
                Flow<List<T>> {
            return map { it.filterIsInstance<T>() }
                .filter { it.isNotEmpty() }
        }
    }
}
