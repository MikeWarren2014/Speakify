package com.mikewarren.speakify.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

class StrategyProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val strategySymbols = resolver.getSymbolsWithAnnotation("com.mikewarren.speakify.strategies.IsPackageName") +
                              resolver.getSymbolsWithAnnotation("com.mikewarren.speakify.strategies.InPackageNameList") +
                              resolver.getSymbolsWithAnnotation("com.mikewarren.speakify.strategies.DefaultNotificationStrategy")

        val additionalSettingsSymbols = resolver.getSymbolsWithAnnotation("com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent")
        val notificationListSymbols = resolver.getSymbolsWithAnnotation("com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.NotificationListComponent")

        val allSymbols = strategySymbols + additionalSettingsSymbols + notificationListSymbols
        val ret = allSymbols.filter { !it.validate() }.toList()
        
        val strategyClasses = strategySymbols
            .filter { it is KSClassDeclaration && it.validate() }
            .map { it as KSClassDeclaration }
            .toList()

        if (strategyClasses.isNotEmpty()) {
            generateStrategyCode(strategyClasses)
        }

        val additionalSettingsClasses = additionalSettingsSymbols
            .filter { it is KSClassDeclaration && it.validate() }
            .map { it as KSClassDeclaration }
            .toList()
        
        val additionalSettingsFunctions = additionalSettingsSymbols
            .filter { it is KSFunctionDeclaration && it.validate() }
            .map { it as KSFunctionDeclaration }
            .toList()

        if (additionalSettingsClasses.isNotEmpty() || additionalSettingsFunctions.isNotEmpty()) {
            generateComponentCode(
                "GeneratedAdditionalSettingsRegistry",
                "AdditionalSettingsComponent",
                additionalSettingsClasses,
                additionalSettingsFunctions
            )
        }

        val notificationListClasses = notificationListSymbols
            .filter { it is KSClassDeclaration && it.validate() }
            .map { it as KSClassDeclaration }
            .toList()
        
        val notificationListFunctions = notificationListSymbols
            .filter { it is KSFunctionDeclaration && it.validate() }
            .map { it as KSFunctionDeclaration }
            .toList()

        if (notificationListClasses.isNotEmpty() || notificationListFunctions.isNotEmpty()) {
            generateComponentCode(
                "GeneratedNotificationListRegistry",
                "NotificationListComponent",
                notificationListClasses,
                notificationListFunctions
            )
        }

        return ret
    }

    private fun generateStrategyCode(classes: List<KSClassDeclaration>) {
        val packageName = "com.mikewarren.speakify.strategies"
        val fileName = "GeneratedStrategyRegistry"
        val file: OutputStream = codeGenerator.createNewFile(
            Dependencies(false, *classes.map { it.containingFile!! }.toTypedArray()),
            packageName,
            fileName
        )

        file.writer().use { writer ->
            writer.write("package $packageName\n\n")
            writer.write("import kotlin.reflect.KClass\n")
            classes.forEach { 
                writer.write("import ${it.qualifiedName?.asString()}\n")
            }
            writer.write("\n")
            writer.write("object $fileName {\n")
            writer.write("    val strategyClasses: List<KClass<out BaseNotificationStrategy>> = listOf(\n")
            classes.forEach { 
                writer.write("        ${it.simpleName.asString()}::class,\n")
            }
            writer.write("    )\n")
            writer.write("}\n")
        }
    }

    private fun generateComponentCode(
        fileName: String,
        annotationName: String,
        classes: List<KSClassDeclaration>,
        functions: List<KSFunctionDeclaration>
    ) {
        val packageName = "com.mikewarren.speakify.strategies"
        val dependencies = Dependencies(
            false,
            *(classes.mapNotNull { it.containingFile } + functions.mapNotNull { it.containingFile }).toTypedArray()
        )
        val file: OutputStream = codeGenerator.createNewFile(dependencies, packageName, fileName)

        file.writer().use { writer ->
            writer.write("package $packageName\n\n")
            writer.write("import kotlin.reflect.KClass\n")
            writer.write("import androidx.compose.runtime.Composable\n")
            
            val allImports = (classes.mapNotNull { it.qualifiedName?.asString() } + 
                              functions.mapNotNull { it.qualifiedName?.asString() }).toSet()
            
            allImports.forEach { writer.write("import $it\n") }
            
            writer.write("\nobject $fileName {\n")
            
            // Generate Class Map
            writer.write("    val classMap: Map<String, KClass<*>> = mapOf(\n")
            classes.forEach { cls ->
                cls.annotations.filter { it.shortName.asString() == annotationName }.forEach { annotation ->
                    val pkg = annotation.arguments.find { it.name?.asString() == "packageName" }?.value as? String
                    val list = annotation.arguments.find { it.name?.asString() == "listName" }?.value as? String
                    
                    if (!pkg.isNullOrEmpty()) {
                        writer.write("        \"pkg:$pkg\" to ${cls.simpleName.asString()}::class,\n")
                    }
                    if (!list.isNullOrEmpty()) {
                        writer.write("        \"list:$list\" to ${cls.simpleName.asString()}::class,\n")
                    }
                }
            }
            writer.write("    )\n\n")

            // For functions (Composables), we need a way to reference them. 
            // We'll generate a map of package names to a lambda that invokes the Composable.
            writer.write("    val viewMap: Map<String, @Composable (Any) -> Unit> = mapOf(\n")
            functions.forEach { func ->
                func.annotations.filter { it.shortName.asString() == annotationName }.forEach { annotation ->
                    val pkg = annotation.arguments.find { it.name?.asString() == "packageName" }?.value as? String
                    val list = annotation.arguments.find { it.name?.asString() == "listName" }?.value as? String
                    
                    val paramType = func.parameters.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString() ?: "Any"

                    if (!pkg.isNullOrEmpty()) {
                        writer.write("        \"pkg:$pkg\" to { vm: Any -> ${func.simpleName.asString()}(vm as $paramType) },\n")
                    }
                    if (!list.isNullOrEmpty()) {
                        writer.write("        \"list:$list\" to { vm: Any -> ${func.simpleName.asString()}(vm as $paramType) },\n")
                    }
                }
            }
            writer.write("    )\n")
            writer.write("}\n")
        }
    }
}
