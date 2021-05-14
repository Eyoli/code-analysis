package adapter.jgrapht

import graph.GraphInterface
import org.jgrapht.alg.clustering.LabelPropagationClustering
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph

class JGraphTObject<T> : GraphInterface<T> {

    private val jGraphTObject: org.jgrapht.Graph<T, DefaultEdge> = SimpleGraph(DefaultEdge::class.java)
    private val vertexesTags: MutableMap<T, MutableMap<String, String>> = mutableMapOf()

    fun useLabelPropagationClustering(tagKey: String) {
        val alg = LabelPropagationClustering(jGraphTObject)
        val clustering = alg.clustering
        var i = 1
        clustering.clusters.forEach { cluster ->
            val clusterName = "Cluster$i"
            cluster.forEach { v ->
                vertexesTags[v]?.set(tagKey, clusterName)
            }
            i++
        }
    }

    override fun addEdge(source: T, target: T, tags: Map<String, String>) {
        if (source != target) {
            // We add source vertex if necessary
            if (!jGraphTObject.containsVertex(source)) {
                jGraphTObject.addVertex(source)
            }
            // We add target vertex if necessary
            if (!jGraphTObject.containsVertex(target)) {
                jGraphTObject.addVertex(target)
            }
            // We add the edge
            jGraphTObject.addEdge(source, target)
        }
    }

    override fun forEachVertex(action: (T, MutableMap<String, String>) -> Unit) =
        jGraphTObject.vertexSet().forEach {
            action(it, vertexesTags.getOrPut(it) { mutableMapOf() })
        }

    override fun forEachEdge(action: (source: T, target: T) -> Unit) =
        jGraphTObject.edgeSet().forEach { action(jGraphTObject.getEdgeSource(it), jGraphTObject.getEdgeTarget(it)) }

    override fun removeVertexIf(filter: (T) -> Boolean) {
        jGraphTObject.removeAllVertices(jGraphTObject.vertexSet().filter(filter))
    }

    override fun vertexes(): Map<T, Map<String, String>> =
        jGraphTObject.vertexSet().associateWith { vertexesTags.getOrDefault(it, mapOf()) }

    override fun vertexesTagValues(key: String) = vertexesTags
        .filterValues { it.containsKey(key) }
        .mapValues { it.value.getValue(key) }
}