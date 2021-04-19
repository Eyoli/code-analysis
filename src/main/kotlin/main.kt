import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import java.io.File
import java.util.*

private val kotlinProject by lazy {
    KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.NATIVE_CONFIG_FILES //Can be JS/NATIVE_CONFIG_FILES for non JVM projects
    ).project
}

private val javascriptProject by lazy {
    KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.JS_CONFIG_FILES //Can be JS/NATIVE_CONFIG_FILES for non JVM projects
    ).project
}

fun createKtFile(codeString: String, fileName: String) =
    PsiManager.getInstance(kotlinProject)
        .findFile(
            LightVirtualFile(fileName, KotlinFileType.INSTANCE, codeString)
        ) as KtFile

data class Edge(val start: String?, val end: String?, val type: String)

class Graph() {
    val edges = mutableListOf<Edge>()
    fun add(edge: Edge) {
        edges.add(edge)
    }
}

fun main(args: Array<String>) = runBlocking {
    val rootKt = "F:\\Data\\IdeaProjects\\consoleTest\\src\\main\\kotlin\\domain"
    val rootJs = "F:\\Data\\Documents\\GitHub\\tactical\\build"

    try {
        graphCreator() {
            extractEdgesFromFilesRecursively(rootKt) {
                extractEdgesFromKtFile()
            }
//            extractEdgesFromFilesRecursively(rootJs) {
//                extractEdgesFromJsFile()
//            }
        }.toCsv()
    } catch (e: Exception) {
        println(e.printStackTrace())
    }
}

suspend fun graphCreator(process: suspend Graph.() -> Unit): Graph {
    val graph = Graph()
    graph.process()
    return graph
}

suspend fun Graph.extractEdgesFromFilesRecursively(root: String, extractEdges: File.() -> List<Edge>) {
    val jobs = ArrayDeque<Deferred<List<Edge>>>()

    val filesDeque = ArrayDeque<File>()
    filesDeque.addAll(File(root).listFiles())
    while (!filesDeque.isEmpty()) {
        val file = filesDeque.poll()
        if (file.isDirectory) {
            filesDeque.addAll(file.listFiles())
        } else {
            coroutineScope {
                jobs.add(async { file.extractEdges() })
            }
        }
    }

    // On attend que tous les fichiers soient traités
    while (!jobs.isEmpty()) {
        jobs.poll().await().forEach(this::add)
    }
}

fun File.extractEdgesFromJsFile(): List<Edge> {
    val jsFile = PsiManager.getInstance(javascriptProject)
        .findFile(
            LightVirtualFile("name", KotlinFileType.INSTANCE, bufferedReader().readText())
        )

    return listOf()
}

fun File.extractEdgesFromKtFile(): List<Edge> {
    val ktFile = createKtFile(bufferedReader().readText(), "xxx")

    return ktFile.getChildrenOfType<KtClass>()
        .flatMap { ktClass ->
            val edges = mutableListOf<Edge>()

            ktClass.getSuperNames().forEach { superName ->
                edges.add(Edge(ktClass.name, superName, "super"))
            }
            ktClass.primaryConstructor
                ?.getChildOfType<KtParameterList>()?.parameters
                ?.filter { ktParameter -> ktParameter.name != null }
                ?.forEach { ktParameter ->
                    edges.add(
                        Edge(
                            ktClass.name,
                            (ktParameter.typeReference?.typeElement as KtUserType).referencedName,
                            "parameter"
                        )
                    )
                }

            edges
        }
}

fun Graph.toCsv() {
    println("Source;Target;EdgeType")
    edges.forEach {
        println("${it.start};${it.end};${it.type}")
    }
}