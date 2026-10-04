package com.mikewarren.speakify.data

import android.content.Context
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.RecentEmailContactModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : EmailContactsRepository {
    private val dao = DbProvider.GetDb(context).recentEmailContactDao()

    override val recentContacts: Flow<List<RecentEmailContactModel>> = dao.getRecentContacts()

    override suspend fun insertContact(contact: RecentEmailContactModel) {
        dao.insertContact(contact)
        dao.pruneOldContacts()
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
