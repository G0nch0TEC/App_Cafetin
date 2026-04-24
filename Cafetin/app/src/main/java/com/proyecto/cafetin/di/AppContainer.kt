package com.proyecto.cafetin.di

import android.content.Context
import com.proyecto.cafetin.data.db.AppDatabase
import com.proyecto.cafetin.repository.CafetinRepository

class AppContainer(context: Context) {
    val database    by lazy { AppDatabase.getInstance(context) }
    val repository  by lazy { CafetinRepository(database) }
}