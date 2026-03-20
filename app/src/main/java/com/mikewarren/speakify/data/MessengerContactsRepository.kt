package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.RecentMessengerContactModel
import kotlinx.coroutines.flow.Flow

interface MessengerContactsRepository {
    val recentContacts: Flow<List<RecentMessengerContactModel>>
    suspend fun insertContact(contact: RecentMessengerContactModel)
    suspend fun clearAll()
}
