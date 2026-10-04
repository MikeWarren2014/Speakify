package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.RecentEmailContactModel
import kotlinx.coroutines.flow.Flow

interface EmailContactsRepository {
    val recentContacts: Flow<List<RecentEmailContactModel>>
    suspend fun insertContact(contact: RecentEmailContactModel)
    suspend fun clearAll()
}
