package any.data.cleanable

interface CleanableProvider {
    fun get(type: Cleanable.Type): Cleanable
}