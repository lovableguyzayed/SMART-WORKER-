package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClosureDay
import com.example.data.model.CompanySetting
import com.example.data.model.Department
import com.example.data.model.LeaveAdjustment
import com.example.data.model.Notification
import com.example.data.model.PayrollRecord
import com.example.data.model.Project
import com.example.data.model.ProjectAssignment
import com.example.data.model.Site
import com.example.data.model.User
import com.example.data.model.WorkTask
import com.example.data.model.Worker
import com.example.data.model.WorkerModification
import com.example.data.model.WorkerTransaction

@Database(
    entities = [
        User::class,
        CompanySetting::class,
        Site::class,
        Department::class,
        Project::class,
        WorkTask::class,
        ProjectAssignment::class,
        WorkerModification::class,
        LeaveAdjustment::class,
        WorkerTransaction::class,
        Notification::class,
        Worker::class,
        AttendanceRecord::class,
        ClosureDay::class,
        PayrollRecord::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun companyDao(): CompanyDao
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun payrollDao(): PayrollDao
    abstract fun transactionDao(): TransactionDao
    abstract fun siteDao(): SiteDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun projectDao(): ProjectDao
    abstract fun workTaskDao(): WorkTaskDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun closureDao(): ClosureDao
    abstract fun notificationDao(): NotificationDao
    abstract fun modificationDao(): ModificationDao
    abstract fun leaveAdjustmentDao(): LeaveAdjustmentDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartworker.db",
                ).build().also { instance = it }
            }
    }
}
