import adapter.jgrapht.JGraphTObject
import extraction.JavaProcessor
import extraction.fillGraphFromExistingSources
import graph.GraphInterface
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

data class ExtractionConfig(val name: String, val root: String)

fun main(args: Array<String>) = runBlocking {

    Logger.disable()

    val configs = listOf(
        ExtractionConfig("vsa-exchange", "C:\\Users\\clement_obert\\IdeaProjects\\vsa-exchange"),
        ExtractionConfig("vsa-ticketing", "C:\\Users\\clement_obert\\IdeaProjects\\vsa-ticketing")
    )

    val outputFolder = Files.createDirectories(Path.of("output"))
    configs.forEach { (name, root) ->
        try {
            val projectFolder = Files.createDirectories(outputFolder.resolve(name))

            val graph = JGraphTObject<String>()
            with(graph) {
                fillGraphFromExistingSources(
                    root, setOf(
                        JavaProcessor(true)
                    )
                )
//            initFromGit("https://github.com/Eyoli/tactical.git", setOf(
//                JavaProcessor(true)
//            ))
                removeMatchingVertexes(".*(Cucumber|ApiCaller|Test|Stub|Fake).*".toRegex())
                tagVertexes("groups")
                findClusterBasedOnTags("group", "groups")

                useLabelPropagationClustering("cluster")

                vertexesToCsv(File(projectFolder.resolve("vertexes.csv").toUri()).bufferedWriter())
                edgesToCsv(File(projectFolder.resolve("edges.csv").toUri()).bufferedWriter())
            }

        } catch (e: Exception) {
            Logger.log(e.printStackTrace())
        }
    }
}

fun GraphInterface<String>.removeMatchingVertexes(regex: Regex) {
    removeVertexIf { value -> value.contains(regex) }
}

fun GraphInterface<String>.tagVertexes(targetKey: String) {
    val wordRegex = "[A-Z][a-z0-9]+".toRegex()
    forEachVertex { value: String, tags: MutableMap<String, String> ->
        tags[targetKey] = wordRegex.findAll(value).map { it.value }.joinToString(separator = ",")
    }
}

fun <T> Map<T, Map<String, String>>.countValuesOccurences(key: String) = run {
    values.mapNotNull { tags -> tags[key] }
        .flatMap { it.split(",") }
        .groupingBy { tag -> tag }
        .eachCount()
}

fun <T> GraphInterface<T>.findClusterBasedOnTags(targetKey: String, groupsKey: String) {
    val stats = vertexes().countValuesOccurences(groupsKey)

    forEachVertex { value: T, tags: MutableMap<String, String> ->
        val group = tags[groupsKey]?.split(",")?.maxByOrNull { tag -> stats[tag] ?: 0 }
        if (group != null) {
            tags[targetKey] = group
        }
    }
}

fun <T> GraphInterface<T>.vertexesToCsv(writer: BufferedWriter) {
    writer.use { out ->
        out.write("Id;Label;Group;Cluster")
        out.newLine()
        forEachVertex { value: T, tags: MutableMap<String, String> ->
            out.write("$value;$value;")
            out.write("${tags["group"] ?: ""};")
            out.write(tags["cluster"] ?: "")
            out.newLine()
        }
    }
}

fun <T> GraphInterface<T>.edgesToCsv(writer: BufferedWriter) {
    writer.use { out ->
        out.write("Source;Target")
        out.newLine()
        forEachEdge { source: T, target: T ->
            out.write("$source;$target")
            out.newLine()
        }
    }
}