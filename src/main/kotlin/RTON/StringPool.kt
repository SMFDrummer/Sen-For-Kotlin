package RTON

class StringPool(private val autoPool: Boolean = false) {
    private val stringPool: MutableMap<String, PoolInfo> = HashMap()
    private var position: Long = 0
    private var index: Int = 0

    val length: Int
        get() = index

    operator fun get(index: Int): PoolInfo? {
        return if (index > this.index) null else stringPool.values.elementAt(index)
    }

    operator fun get(id: String): PoolInfo? {
        var value = stringPool[id]
        if (value == null && autoPool) {
            value = throwInPool(id)
        }
        return value
    }

    fun exist(id: String): Boolean {
        return stringPool.containsKey(id)
    }

    fun clear() {
        stringPool.clear()
        position = 0
        index = 0
    }

    fun throwInPool(poolKey: String): PoolInfo {
        var value = stringPool[poolKey]
        if (value == null) {
            value = PoolInfo(position, index++, poolKey)
            stringPool[poolKey] = value
            position += poolKey.length + 1
        }
        return value
    }

    data class PoolInfo(var offset: Long, var index: Int, var value: String)
}