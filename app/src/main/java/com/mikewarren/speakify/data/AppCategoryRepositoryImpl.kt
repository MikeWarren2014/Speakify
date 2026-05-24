package com.mikewarren.speakify.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.AppCategoryDao
import com.mikewarren.speakify.data.db.firestore.AppCategoryFirestoreRepository
import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.AppCategoryModel
import com.mikewarren.speakify.utils.AppCategoryService
import com.mikewarren.speakify.utils.RawAppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCategoryDao: AppCategoryDao,
    private val firestoreRepository: AppCategoryFirestoreRepository
) : AppCategoryRepository {

    override suspend fun getCategoryForPackage(packageName: String): AppCategory {
        val localCategory = appCategoryDao.getCategoryForPackage(packageName)
        if (localCategory != null) return localCategory
        
        // Try to discover via System ApplicationInfo
        val systemCategory = try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Communication
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.BusinessProductivity
                    else -> null
                }
            } else null
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        if (systemCategory != null) {
            addCategory(packageName, systemCategory)
            return systemCategory
        }


        val scrapedAppCategory = AppCategoryService().fetchCategory(packageName)
            .let { rawAppCategory: RawAppCategory ->
                if (rawAppCategory in listOf(RawAppCategory.BUSINESS, RawAppCategory.PRODUCTIVITY))
                    return@let AppCategory.BusinessProductivity
                if (rawAppCategory == RawAppCategory.COMMUNICATION)
                    return@let AppCategory.Communication
                if (rawAppCategory in listOf(RawAppCategory.SHOPPING, RawAppCategory.FOOD_AND_DRINK))
                    return@let AppCategory.Shopping

                return@let AppCategory.Other
            }

        if (scrapedAppCategory != AppCategory.Other)
            addCategory(packageName, scrapedAppCategory)

        return scrapedAppCategory
    }

    override suspend fun getCategoriesForPackages(packageNames: List<String>): Map<String, AppCategory> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, AppCategory>()
        val remainingPackages = mutableListOf<String>()

        // 1. Bulk check local cache first
        packageNames.forEach { pkg ->
            val local = appCategoryDao.getCategoryForPackage(pkg)
            if (local != null) {
                results[pkg] = local
            } else {
                remainingPackages.add(pkg)
            }
        }

        if (remainingPackages.isEmpty()) return@withContext results

        // 2. Parallel discovery for the rest, limited to 5 at a time to be polite to the system and web
        val semaphore = Semaphore(5)
        remainingPackages.map { pkg ->
            async {
                semaphore.withPermit {
                    pkg to getCategoryForPackage(pkg)
                }
            }
        }.awaitAll().forEach { (pkg, cat) ->
            results[pkg] = cat
        }

        return@withContext results
    }

    override suspend fun initializeCategories() {
        val categories = mutableListOf<AppCategoryModel>()
        
        // Add hardcoded defaults
        PackageNames.CommunicationAppList.forEach { 
            categories.add(AppCategoryModel(packageName = it, appCategory = AppCategory.Communication))
        }
        PackageNames.BusinessProductivityAppList.forEach { 
            categories.add(AppCategoryModel(packageName = it, appCategory = AppCategory.BusinessProductivity))
        }
        PackageNames.ShoppingAppList.forEach { 
            categories.add(AppCategoryModel(packageName = it, appCategory = AppCategory.Shopping))
        }

        // Fetch from Firestore (the single source of truth)
        val remoteCategories = firestoreRepository.fetchAllCategories()

        // any categories we don't yet have in Firestore, we write there.
        categories.filter { it !in remoteCategories }
            .forEach { firestoreRepository.uploadCategory(it) }

        categories.addAll(remoteCategories)

        // Save all to local DB (duplicates are handled by REPLACE strategy in DAO)
        appCategoryDao.insertAll(categories)
    }

    override suspend fun addCategory(packageName: String, category: AppCategory) {
        val model = AppCategoryModel(packageName = packageName, appCategory = category)
        appCategoryDao.insertAll(listOf(model))
        firestoreRepository.uploadCategory(model)
    }
}
