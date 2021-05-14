package graph

interface GraphInterface<T> {
    fun addEdge(source: T, target: T, tags: Map<String, String>)
    fun forEachVertex(action: (value: T, tags: MutableMap<String, String>) -> Unit)
    fun forEachEdge(action: (source: T, target: T) -> Unit)
    fun removeVertexIf(filter: (T) -> Boolean)
    fun vertexes(): Map<T, Map<String, String>>
    fun vertexesTagValues(key: String): Map<T, String>
}