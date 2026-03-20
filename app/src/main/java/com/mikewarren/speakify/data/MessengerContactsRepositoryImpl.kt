package com.mikewarren.speakify.data

import android.content.Context
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessengerContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MessengerContactsRepository {
    private val dao = DbProvider.GetDb(context).recentMessengerContactDao()

    override val recentContacts: Flow<List<RecentMessengerContactModel>> = dao.getRecentContacts()

    override suspend fun insertContact(contact: RecentMessengerContactModel) {
        dao.insertContact(contact)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
