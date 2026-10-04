package com.mikewarren.speakify.data

import android.content.Context
import androidx.room.Room
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.DbProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject

interface DbProviderMocker {
    val context: Context

    var db: AppDatabase

    fun setUpDatabaseDoubles() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        mockkObject(DbProvider)
        every { DbProvider.GetDb(any()) } returns db
    }

    fun tearDownDatabaseDoubles() {
        db.close()
        unmockkObject(DbProvider)
    }
}