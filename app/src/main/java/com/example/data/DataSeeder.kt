package com.example.data

import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CompanySetting
import com.example.data.model.Department
import com.example.data.model.PayTypes
import com.example.data.model.Project
import com.example.data.model.ProjectAssignment
import com.example.data.model.Roles
import com.example.data.model.Site
import com.example.data.model.User
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.data.repo.AuthRepository
import java.time.LocalDate
import java.time.LocalTime

/** Seeds a realistic first-run dataset so the app is usable immediately. */
class DataSeeder(private val db: AppDatabase) {

    suspend fun seedIfEmpty() {
        if (db.userDao().count() > 0) return

        db.companyDao().upsert(
            CompanySetting(
                name = "Smart Worker Constructions",
                address = "Green Valley Tower, Lucknow, Uttar Pradesh",
                phone = "+91 98765 43210",
                email = "hr@smartworker.in",
                website = "www.smartworker.in",
                gstNumber = "09ABCDE1234F1Z5",
            )
        )

        db.userDao().upsert(
            User(
                username = "admin",
                email = "admin@smartworker.in",
                fullName = "Ramesh Kumar",
                passwordHash = AuthRepository.hashPassword("admin123"),
                role = Roles.ADMIN,
            )
        )

        val siteId = db.siteDao().upsert(
            Site(name = "Green Valley Tower", address = "Sector 12, Lucknow", contactPerson = "Ravi Kumar", contactPhone = "+91 90000 11111")
        )
        db.siteDao().upsert(Site(name = "Sunrise Residency", address = "Gomti Nagar, Lucknow"))

        for (d in listOf("Masonry", "Electrical", "Carpentry", "Plumbing", "Painting", "Steel Fixing", "Helper")) {
            db.departmentDao().upsert(Department(name = d))
        }

        val projectId = db.projectDao().upsert(
            Project(
                name = "Tower A – Structure", description = "Main tower structural work", siteId = siteId,
                startDate = LocalDate.now().minusMonths(3), deadline = LocalDate.now().plusMonths(2), status = "active",
            )
        )

        db.userDao().upsert(
            User(
                username = "supervisor",
                email = "sup@smartworker.in",
                fullName = "Suresh Yadav",
                passwordHash = AuthRepository.hashPassword("super123"),
                role = Roles.ATTENDANCE,
                assignedSiteIds = siteId.toString(),
            )
        )

        data class Seed(val name: String, val phone: String, val dept: String, val role: String, val pay: String, val rate: Double)
        val seeds = listOf(
            Seed("Ramesh Kumar", "9876500001", "Masonry", "Mason", PayTypes.DAILY, 750.0),
            Seed("Suresh Yadav", "9876500002", "Helper", "Helper", PayTypes.DAILY, 500.0),
            Seed("Arun Kumar", "9876500003", "Electrical", "Electrician", PayTypes.MONTHLY, 22000.0),
            Seed("Vijay Singh", "9876500004", "Carpentry", "Carpenter", PayTypes.DAILY, 800.0),
            Seed("Deepak Verma", "9876500005", "Plumbing", "Plumber", PayTypes.DAILY, 700.0),
            Seed("Mohammad Ali", "9876500006", "Masonry", "Mason", PayTypes.DAILY, 750.0),
            Seed("Rohit Sharma", "9876500007", "Steel Fixing", "Steel Fixer", PayTypes.HOURLY, 120.0),
            Seed("Sanjay Patel", "9876500008", "Painting", "Painter", PayTypes.DAILY, 650.0),
        )

        val today = LocalDate.now()
        seeds.forEachIndexed { index, s ->
            val prefix = s.dept.take(2).uppercase()
            val code = "%s%03d".format(prefix, index + 1)
            val worker = Worker(
                workerCode = code,
                fullName = s.name,
                phone = s.phone,
                position = s.role,
                department = s.dept,
                employeeType = if (s.pay == PayTypes.MONTHLY) "Full Time" else "Daily Wage",
                joinDate = today.minusMonths(6),
                payType = s.pay,
                dailyRate = if (s.pay == PayTypes.DAILY) s.rate else null,
                monthlySalary = if (s.pay == PayTypes.MONTHLY) s.rate else null,
                hourlyRate = if (s.pay == PayTypes.HOURLY) s.rate else null,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(18, 0),
                overtimeEnabled = true,
                overtimeRate = if (s.pay == PayTypes.HOURLY) s.rate * 1.5 else 100.0,
                overtimeType = "hour",
                leavePolicyEnabled = s.pay == PayTypes.MONTHLY,
                leaveDeductionPerDay = if (s.pay == PayTypes.MONTHLY) s.rate / 26.0 else null,
            )
            val workerId = db.workerDao().insert(worker)
            db.assignmentDao().upsert(
                ProjectAssignment(workerId = workerId, projectId = projectId, siteId = siteId, startDate = today.minusMonths(3))
            )

            // A fortnight of attendance so dashboards/payroll have real data.
            for (offset in 0..13) {
                val d = today.minusDays(offset.toLong())
                if (d.dayOfWeek.value == 7) continue // Sundays off
                val status = when {
                    offset % 9 == 4 -> AttendanceStatus.ABSENT
                    offset % 7 == 3 -> AttendanceStatus.LATE
                    else -> AttendanceStatus.PRESENT
                }
                val checkIn = if (status == AttendanceStatus.LATE) d.atTime(9, 35) else d.atTime(9, 0)
                val checkOut = if (status != AttendanceStatus.ABSENT) d.atTime(18, if (offset % 5 == 0) 45 else 0) else null
                val worked = if (checkOut != null) java.time.Duration.between(checkIn, checkOut).toMinutes().toInt() else 0
                db.attendanceDao().upsert(
                    AttendanceRecord(
                        workerId = workerId,
                        date = d,
                        status = status,
                        checkInTime = if (status != AttendanceStatus.ABSENT) checkIn else null,
                        checkOutTime = checkOut,
                        overtimeMinutes = maxOf(worked - 9 * 60, 0),
                        lateMinutes = if (status == AttendanceStatus.LATE) 35 else 0,
                        markedBy = 1,
                        siteId = siteId,
                    )
                )
            }

            if (index == 0) {
                db.transactionDao().upsert(
                    WorkerTransaction(workerId = workerId, txnType = "advance", amount = 2000.0, date = today.minusDays(5), description = "Festival advance")
                )
            }
        }
    }
}
