package extraction

import graph.GraphInterface
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.PsiType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.PsiJavaFileImpl
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.util.*

private val JAVA_PROJECT by lazy {
    KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.NATIVE_CONFIG_FILES //Can be JS/NATIVE_CONFIG_FILES for non JVM projects
    ).project
}

private val JAVA_BASIC_TYPES = setOf(
    "String", "List", "Set", "long", "LocalDate", "Boolean", "Double", "Integer",
    "boolean", "int", "Map", "LocalDateTime", "BigDecimal", "double", "YearMonth",
    "Serializable", "LinkedList", "Date"
)

private const val TYPE_TAG = "type"

class JavaProcessor(filterBasicTypes: Boolean) : LanguageProcessor(setOf("java"), JAVA_BASIC_TYPES, filterBasicTypes) {

    override suspend fun processFile(file: File, graph: GraphInterface<String>) {
        val fileContent = PsiManager.getInstance(JAVA_PROJECT)
            .findFile(
                LightVirtualFile(file.name, JavaFileType.INSTANCE, file.bufferedReader().readText())
            ) as PsiJavaFileImpl

        fileContent.classes
            .filter { psiClass -> !psiClass.isEnum }
            .forEach { javaClass ->
                val start = javaClass.name!!

                javaClass.extendsListTypes.forEach { superClass ->
                    graph.addEdgeIfValid(start, superClass.className, mapOf(Pair(TYPE_TAG, "extends")))
                }
                javaClass.implementsListTypes.forEach { superClass ->
                    graph.addEdgeIfValid(start, superClass.className, mapOf(Pair(TYPE_TAG, "implements")))
                }

                val parametersToProcess = ArrayDeque<PsiType>()
                javaClass.fields.forEach { field -> parametersToProcess.add(field.type) }
                while (!parametersToProcess.isEmpty()) {
                    val type = parametersToProcess.removeFirst()
                    if (type is PsiClassReferenceType) {
                        type.parameters.forEach(parametersToProcess::add)
                        graph.addEdgeIfValid(start, type.name, mapOf(Pair(TYPE_TAG, "field")))

                    } else {
                        graph.addEdgeIfValid(start, type.presentableText, mapOf(Pair(TYPE_TAG, "field")))
                    }
                }
            }
    }


}