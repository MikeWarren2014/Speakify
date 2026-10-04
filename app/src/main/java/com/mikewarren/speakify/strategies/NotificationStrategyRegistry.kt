package com.mikewarren.speakify.strategies

import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Composable
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.utils.NotificationPermissionHelper
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object NotificationStrategyRegistry {
    private val strategyClasses: List<KClass<out BaseNotificationStrategy>> 
        get() = GeneratedStrategyRegistry.strategyClasses

    fun findStrategyClass(notification: StatusBarNotification, context: Context): KClass<out BaseNotificationStrategy> {
        val packageName = notification.packageName

        // Check for specific package name matches
        strategyClasses.find { kClass ->
            val annotation = kClass.findAnnotation<IsPackageName>()
            annotation?.packageName == packageName
        }?.let { return it }

        // Check for package list matches
        strategyClasses.find { kClass ->
            val annotation = kClass.findAnnotation<InPackageNameList>()
            if (annotation != null) {
                val list = PackageNames.GetListByName(annotation.listName)
                list.contains(packageName)
            } else {
                false
            }
        }?.let { return it }

        // Check for email app matches
        strategyClasses.find { kClass ->
            kClass.findAnnotation<IsEmailApp>() ?: return@find false

            return@find NotificationPermissionHelper(context).isEmailApp(packageName)
        }?.let { return it }

        // Fallback to default
        return strategyClasses.find { kClass ->
            kClass.findAnnotation<DefaultNotificationStrategy>() != null
        } ?: SimpleNotificationStrategy::class
    }

    fun findComponentClass(packageName: String, registryClassMap: Map<String, KClass<*>>, context: Context? = null): KClass<*>? {
        // 1. Direct package match
        registryClassMap["pkg:$packageName"]?.let { return it }

        // 2. List match
        registryClassMap.keys.filter { it.startsWith("list:") }.forEach { key ->
            val listName = key.substringAfter("list:")
            if (PackageNames.GetListByName(listName).contains(packageName)) {
                return registryClassMap[key]
            }
        }

        // 3. App type match
        if (context != null) {
            val permissionHelper = NotificationPermissionHelper(context)
            if (permissionHelper.isEmailApp(packageName)) {
                registryClassMap["appType:email"]?.let { return it }
            }
        }

        return null
    }

    fun findComponentView(packageName: String, registryViewMap: Map<String, @Composable (Any) -> Unit>, context: Context? = null): (@Composable (Any) -> Unit)? {
        // 1. Direct package match
        registryViewMap["pkg:$packageName"]?.let { return it }

        // 2. List match
        registryViewMap.keys.filter { it.startsWith("list:") }.forEach { key ->
            val listName = key.substringAfter("list:")
            if (PackageNames.GetListByName(listName).contains(packageName)) {
                return registryViewMap[key]
            }
        }

        // 3. App type match
        if (context != null) {
            val permissionHelper = NotificationPermissionHelper(context)
            if (permissionHelper.isEmailApp(packageName)) {
                registryViewMap["appType:email"]?.let { return it }
            }
        }

        return null
    }
}
