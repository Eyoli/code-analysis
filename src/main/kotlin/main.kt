import extraction.JavaProcessor
import extraction.initFromExistingSources
import graph.Graph
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.File

fun main(args: Array<String>) = runBlocking {
    val root = "C:\\Users\\clement_obert\\IdeaProjects\\vsa-exchange"

    try {
        val graph = Graph<String>()
        with(graph) {
            initFromExistingSources(
                root, setOf(
                    JavaProcessor(true)
                )
            )
            removeMatchingVertexes(".*(Cucumber|ApiCaller|Test|Stub|Fake).*".toRegex())
            tagVertexes("groups")
            findMostProbableGroup("group", "groups")
            edgesToCsv(File("edges.csv").bufferedWriter())
            vertexesToCsv(File("vertexes.csv").bufferedWriter())
        }
    } catch (e: Exception) {
        println(e.printStackTrace())
    }
}

fun Graph<String>.removeMatchingVertexes(regex: Regex) {
    removeIf { value -> value.contains(regex) }
}

fun Graph<String>.tagVertexes(targetKey: String) {
    val wordRegex = "[A-Z][a-z0-9]+".toRegex()
    vertexes.iterable().forEach { vertex ->
        vertex.tags[targetKey] = wordRegex.findAll(vertex.value).map { it.value }.toSet()
    }
}

fun <T> Graph<T>.edgesToCsv(writer: BufferedWriter) {
    writer.use { out ->
        out.write("Source;Target;Tags")
        out.newLine()
        edges.iterable().forEach { edge ->
            out.write("${edge.start};${edge.end};")
            out.write(edge.tags.iterable<String>("tag").joinToString(","))
            out.newLine()
        }
    }
}

fun <T> Graph<T>.findMostProbableGroup(targetKey: String, groupsKey: String) {
    val stats = vertexes.iterable()
        .flatMap { vertex -> vertex.tags.iterable<String>(groupsKey) }
        .groupingBy { tag -> tag }
        .eachCount()

    vertexes.iterable()
        .forEach { vertex ->
            val group = vertex.tags.iterable<String>(groupsKey).maxByOrNull { tag -> stats[tag] ?: 0 }
            if (group != null) {
                vertex.tags[targetKey] = group
            }
        }
}

fun <T> Graph<T>.vertexesToCsv(writer: BufferedWriter) {
    writer.use { out ->
        out.write("Id;Label;Group")
        out.newLine()
        vertexes.iterable().forEach { vertex ->
            out.write("${vertex.value};${vertex.value};")
            out.write(vertex.tags.get<String>("group") ?: "")
            out.newLine()
        }
    }
}