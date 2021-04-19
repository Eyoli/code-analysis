import java.io.File
import java.net.URI
import java.util.function.Consumer

private val project by lazy {
    KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES //Can be JS/NATIVE_CONFIG_FILES for non JVM projects
    ).project
}

data class Edge(val start: String, val end: String, val type: String)

class Graph() {
    val edges = mutableListOf<Edge>()
    fun add(edge: Edge) {
        edges.add(edge)
    }
}

fun Graph.toCsv() {
    edges.forEach({ println("${it.start};${it.end};${it.type}") })
}

fun clearContent(content: String): String {
    return content
        .replace("""\R""".toRegex(), "")
}

val graph = Graph()

val CLASS_NAME = """(data )?class [a-zA-Z]+"""
val ANY_CONTENT = """[a-zA-Z0-9:\,_<>\(\)\.\t= ]*"""
val ANY_BRAKET_CONTENT = """($ANY_CONTENT\{$ANY_CONTENT\}$ANY_CONTENT)*"""
val VARIABLE_PATTERN = """val [a-zA-Z]+: [a-zA-Z]+"""
val classNameRegex = Regex(CLASS_NAME)
val classRegex = Regex("""$CLASS_NAME\($ANY_CONTENT\)( )*(\{$ANY_BRAKET_CONTENT\})?""")
val variableRegex = Regex(VARIABLE_PATTERN)

val root = "src/main/kotlin/domain/world.kt"

var fileContent = File(root).bufferedReader().readText()
fileContent = clearContent(fileContent)

for (result in classRegex.findAll(fileContent)) {
    val classContent = result.value
    val className = classNameRegex.find(classContent)?.value?.split("(data )?class ".toRegex())?.get(1)

    className?.let {
        println("Class $it")
        println("$classContent")
        for (variable in variableRegex.findAll(classContent)) {
            val variableClassName = variable.value.split(": ").get(1)
            graph.add(Edge(it, variableClassName, "composition"))
        }
    }
}

graph.toCsv()