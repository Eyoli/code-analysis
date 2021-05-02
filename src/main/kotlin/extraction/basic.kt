package extraction

import graph.Edge
import graph.Graph
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File

abstract class LanguageProcessor(
    val extensions: Set<String>,
    val basicTypes: Set<String>,
    val filterBasicTypes: Boolean
) {
    abstract fun processFile(file: File): List<Edge<String>>
}

suspend fun Graph<String>.initFromExistingSources(
    root: String,
    processors: Set<LanguageProcessor>
) {

    val jobs = ArrayDeque<Deferred<List<Edge<String>>>>()

    val filesDeque = ArrayDeque<File>()
    filesDeque.add(File(root))
    while (!filesDeque.isEmpty()) {
        val file = filesDeque.removeFirst()
        if (file.isDirectory) {
            filesDeque.addAll(file.listFiles())
        } else {
            val firstProcessor = processors.firstOrNull { processor -> file.extension in processor.extensions }
            if (firstProcessor != null) {
                coroutineScope {
                    jobs.add(async {
                        val edges = firstProcessor.processFile(file)
                        if (firstProcessor.filterBasicTypes) {
                            edges.filter { edge ->
                                edge.start !in firstProcessor.basicTypes && edge.end !in firstProcessor.basicTypes
                            }
                        } else {
                            edges
                        }
                    })
                }
            }
        }
    }

    // On attend que tous les fichiers soient traités
    while (!jobs.isEmpty()) {
        jobs.removeFirst().await().forEach(this::add)
    }
}