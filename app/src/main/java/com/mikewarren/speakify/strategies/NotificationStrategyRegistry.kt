package com.mikewarren.speakify.strategies

import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Composable
import com.mikewarren.speakify.data.constants.PackageNames
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object NotificationStrategyRegistry {
    private val strategyClasses: List<KClass<out BaseNotificationStrategy>> 
        get() = GeneratedStrategyRegistry.strategyClasses

    fun findStrategyClass(notification: StatusBarNotification): KClass<out BaseNotificationStrategy> {
        val packageName = notification.packageName

        // 1. Check for specific package name matches
        strategyClasses.find { kClass ->
            val annotation = kClass.findAnnotation<IsPackageName>()
            annotation?.packageName == packageName
        }?.let { return it }

        // 2. Check for package list matches
        strategyClasses.find { kClass ->
            val annotation = kClass.findAnnotation<InPackageNameList>()
            if (annotation != null) {
                val list = PackageNames.GetListByName(annotation.listName)
                list.contains(packageName)
            } else {
                false
            }
        }?.let { return it }

        // 3. Fallback to default
        return strategyClasses.find { kClass ->
            kClass.findAnnotation<DefaultNotificationStrategy>() != null
        } ?: SimpleNotificationStrategy::class
    }

    fun findComponentClass(packageName: String, registryClassMap: Map<String, KClass<*>>): KClass<*>? {
        // 1. Direct package match
        registryClassMap["pkg:$packageName"]?.let { return it }

        // 2. List match
        registryClassMap.keys.filter { it.startsWith("list:") }.forEach { key ->
            val listName = key.substringAfter("list:")
            if (PackageNames.GetListByName(listName).contains(packageName)) {
                return registryClassMap[key]
            }
        }

        return null
    }

    fun findComponentView(packageName: String, registryViewMap: Map<String, @Composable (Any) -> Unit>): (@Composable (Any) -> Unit)? {
        // 1. Direct package match
        registryViewMap["pkg:$packageName"]?.let { return it }

        // 2. List match
        registryViewMap.keys.filter { it.startsWith("list:") }.forEach { key ->
            val listName = key.substringAfter("list:")
            if (PackageNames.GetListByName(listName).contains(packageName)) {
                return registryViewMap[key]
            }
        }

        return null
    }
}
