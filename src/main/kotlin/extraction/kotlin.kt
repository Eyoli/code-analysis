package extraction

import graph.Edge
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

private val project by lazy {
    KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        CompilerConfiguration(),
        EnvironmentConfigFiles.NATIVE_CONFIG_FILES //Can be JS/NATIVE_CONFIG_FILES for non JVM projects
    ).project
}

private val KOTLIN_BASIC_TYPES = setOf("List")

private const val TYPE_TAG = "type"

class KotlinProcessor(filterBasicTypes: Boolean) : LanguageProcessor(setOf("kt", "kts"), KOTLIN_BASIC_TYPES, filterBasicTypes) {

    override fun processFile(file: File): List<Edge<String>> {
        try {
            val fileContent = PsiManager.getInstance(project)
                .findFile(
                    LightVirtualFile(file.name, KotlinFileType.INSTANCE, file.bufferedReader().readText())
                ) as KtFile

            return fileContent.getChildrenOfType<KtClass>()
                .flatMap { ktClass ->
                    val edges = mutableListOf<Edge<String>>()

                    ktClass.getSuperNames().forEach { superName ->
                        with(Edge(ktClass.name!!, superName)) {
                            tags[TYPE_TAG] = "extends"
                            edges.add(this)
                        }
                    }
                    ktClass.primaryConstructor
                        ?.getChildOfType<KtParameterList>()?.parameters
                        ?.filter { ktParameter -> ktParameter.name != null && ktParameter.typeReference?.typeElement != null }
                        ?.forEach { ktParameter ->
                            with(
                                Edge(
                                    ktClass.name!!,
                                    (ktParameter.typeReference?.typeElement as KtUserType).referencedName!!
                                )
                            ) {
                                tags[TYPE_TAG] = "extends"
                                edges.add(this)
                            }
                        }

                    edges
                }
        } catch (e: Exception) {
            println("Error parsing file ${file.name}")
            return listOf()
        }
    }

}