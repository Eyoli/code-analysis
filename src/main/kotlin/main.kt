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
        //ExtractionConfig("vsa-ticketing", "C:\\Users\\clement_obert\\IdeaProjects\\vsa-ticketing")
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
                findGroupBasedOnTags(
                    sourceTagKey = "groups",
                    sourceTagDelimiter = ",",
                    meaningfulGroupSize = 5,
                    targetTagKey = "group"
                )
                useLabelPropagationClustering("cluster")
                findGroupBasedOnTags(
                    sourceTagKey = "groups",
                    sourceTagDelimiter = ",",
                    meaningfulGroupSize = 5,
                    targetTagKey = "groupWithClustering",
                    clusterKey = "cluster"
                )

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

fun <T> GraphInterface<T>.findGroupBasedOnTags(
    sourceTagKey: String,
    sourceTagDelimiter: String,
    targetTagKey: String,
    meaningfulGroupSize: Int = 1,
    meaningfulGroupPercentage: Double = 0.0,
    clusterKey: String? = null
) {
    val tagOccurencies = vertexes().values
        .mapNotNull { tags -> tags[sourceTagKey]?.split(sourceTagDelimiter) }
        .flatten()
        .groupingBy { tag -> tag }
        .eachCount()
        .filter { it.value >= meaningfulGroupSize }
        .filter { it.value / vertexes().values.size.toDouble() >= meaningfulGroupPercentage }

    val defaultCluster = "DEFAULT_CLUSTER"
    val tagOccurenciesPerCluster = vertexes().values
        .map { it[clusterKey] ?: defaultCluster }
        .distinct()
        .associateWith { cluster ->
            vertexes().values
                .filter { tags -> (tags[clusterKey] ?: defaultCluster) == cluster }
                .mapNotNull { tags -> tags[sourceTagKey]?.split(sourceTagDelimiter) }
                .flatten()
                .groupingBy { tag -> tag }
                .eachCount()
        }

    forEachVertex { value: T, tags: MutableMap<String, String> ->
        val tagOccurenciesForCurrentCluster = tagOccurenciesPerCluster.getValue(tags[clusterKey] ?: defaultCluster)
        val group = tags[sourceTagKey]?.split(sourceTagDelimiter)
            ?.filter { tag -> tagOccurenciesForCurrentCluster[tag] != null }
            ?.maxByOrNull { tag -> tagOccurenciesForCurrentCluster.getValue(tag) }
        if (group != null && tagOccurencies.get<Any, Int>(group) != null) {
            tags[targetTagKey] = group
        }
    }
}

fun <T> GraphInterface<T>.vertexesToCsv(writer: BufferedWriter) {
    writer.use { out ->
        out.write("Id;Label;Cluster;Group;GroupWithClustering;Strange")
        out.newLine()
        forEachVertex { value: T, tags: MutableMap<String, String> ->
            val row = listOf(
                "$value",
                "$value",
                tags["cluster"] ?: "",
                tags["group"] ?: "",
                tags["groupWithClustering"] ?: tags["group"] ?: "",
                tags["group"] != tags["groupWithClustering"] && tags["groupWithClustering"] != null
            )
            out.write(row.joinToString(separator = ";"))
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