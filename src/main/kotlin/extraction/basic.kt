package extraction

import Logger
import graph.GraphInterface
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

abstract class LanguageProcessor(
    val extensions: Set<String>,
    private val basicTypes: Set<String>,
    private val filterBasicTypes: Boolean,
    private val mutex: Mutex = Mutex()
) {
    abstract suspend fun processFile(file: File, graph: GraphInterface<String>)

    protected suspend fun GraphInterface<String>.addEdgeIfValid(start: String, end: String, tags: Map<String, String>) {
        if (!filterBasicTypes || (start !in basicTypes && end !in basicTypes)) {
            mutex.withLock {
                addEdge(start, end, tags)
            }
        }
    }
}

suspend fun GraphInterface<String>.getGraphFromGitProject(rawUrl: String, processors: Set<JavaProcessor>) = coroutineScope {
    val gitClone = async {
        File("checkout").deleteRecursively()
        val exec = Runtime.getRuntime().exec("git clone $rawUrl checkout")
        exec.waitFor()
        exec
    }

    val process = gitClone.await()
    if (process.exitValue() == 0) {
        Logger.log("Project is checked out")
        fillGraphFromExistingSources("checkout", processors)
    } else {
        Logger.log(String(process.errorStream.readAllBytes()))
    }
}

suspend fun GraphInterface<String>.fillGraphFromExistingSources(root: String, processors: Set<LanguageProcessor>) {

    val jobs = ArrayDeque<Job>()

    val filesDeque = ArrayDeque<File>()
    filesDeque.add(File(root))

    coroutineScope {
        while (!filesDeque.isEmpty()) {
            val file = filesDeque.removeFirst()
            if (file.isDirectory) {
                filesDeque.addAll(file.listFiles())
            } else {
                val firstProcessor = processors.firstOrNull { processor -> file.extension in processor.extensions }
                if (firstProcessor != null) {
                    Logger.log("Add task for ${file.name}")
                    jobs.add(launch {
                        Logger.log("Processing ${file.name}")

                        try {
                            firstProcessor.processFile(file, this@fillGraphFromExistingSources)
                        } catch (e: Exception) {
                            Logger.log("Error parsing file ${file.name}")
                        }
                    })
                }
            }
        }
    }

    // On attend que tous les fichiers soient traités
    while (!jobs.isEmpty()) {
        jobs.removeFirst().join()
    }
}