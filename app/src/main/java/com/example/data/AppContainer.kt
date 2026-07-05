package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.repo.AttendanceRepository
import com.example.data.repo.AuthRepository
import com.example.data.repo.CatalogRepository
import com.example.data.repo.PayrollRepository
import com.example.data.repo.WorkerRepository

/**
 * Lightweight manual dependency-injection container. A single instance is held
 * by [SmartWorkerApp] and shared with every ViewModel through the factory.
 */
class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.get(context)

    val authRepository = AuthRepository(db, context)
    val workerRepository = WorkerRepository(db)
    val attendanceRepository = AttendanceRepository(db)
    val payrollRepository = PayrollRepository(db)
    val catalogRepository = CatalogRepository(db)

    suspend fun seedIfEmpty() = DataSeeder(db).seedIfEmpty()
}
