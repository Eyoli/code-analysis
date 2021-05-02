package graph

import java.util.function.Consumer

data class Tags(private val tags: MutableMap<String, Any> = mutableMapOf()) {

    operator fun set(key: String, value: Any) {
        tags[key] = value
    }

    operator fun <X> get(key: String): X? {
        return tags[key] as? X
    }

    fun <X> iterable(key: String): Iterable<X> {
        return tags[key] as? Iterable<X> ?: listOf()
    }
}

data class Edge<T>(val start: T, val end: T, val tags: Tags = Tags())

data class Vertex<T>(val value: T, val tags: Tags = Tags())

class Edges<T> {
    private val edges = mutableListOf<Edge<T>>()

    fun add(edge: Edge<T>): Boolean {
        return edges.add(edge)
    }

    fun removeIf(filter: (T) -> Boolean) {
        edges.removeIf { edge -> filter(edge.start) || filter(edge.end) }
    }

    fun iterable(): Iterable<Edge<T>> {
        return edges.asIterable()
    }
}

class Vertexes<T> {
    private val vertexes = mutableSetOf<Vertex<T>>()

    fun add(vertex: Vertex<T>): Boolean {
        return vertexes.add(vertex)
    }

    fun removeIf(filter: (T) -> Boolean) {
        vertexes.removeIf { vertex -> filter(vertex.value) }
    }

    fun iterable(): Iterable<Vertex<T>> {
        return vertexes.asIterable()
    }
}

class Graph<T> {
    val edges = Edges<T>()
    val vertexes = Vertexes<T>()

    fun add(edge: Edge<T>) {
        edges.add(edge)
        if (edge.start != null) vertexes.add(Vertex(edge.start))
        if (edge.end != null) vertexes.add(Vertex(edge.end))
    }

    fun removeIf(filter: (T) -> Boolean) {
        vertexes.removeIf(filter)
        edges.removeIf(filter)
    }
}