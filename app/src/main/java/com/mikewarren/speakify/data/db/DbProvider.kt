package com.mikewarren.speakify.data.db

import android.content.Context
import androidx.room.Room

object DbProvider {
    private var _db: AppDatabase? = null

    const val DbName = "speakify-db"

    fun GetDb(context: Context) : AppDatabase {
        if (this._db == null) {
            this._db = Room.databaseBuilder(
                context,
                AppDatabase::class.java, DbName
            )
                .build()

        }

        return this._db!!

    }

}