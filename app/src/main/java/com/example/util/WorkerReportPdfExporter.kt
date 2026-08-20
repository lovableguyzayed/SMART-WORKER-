package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CompanySetting
import com.example.data.model.PayTypes
import com.example.data.model.Worker
import com.example.data.repo.PayrollRepository
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Printable employee report — the app-side counterpart of the web app's
 * `worker_report.html` "Save PDF": company letterhead, worker identity block
 * with QR, personal/employment details, and the month-by-month payroll table.
 */
object WorkerReportPdfExporter {

    private const val PAGE_W = 595 // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    private const val NAVY = 0xFF0F172A.toInt()
    private const val SLATE = 0xFF64748B.toInt()
    private const val BORDER = 0xFFE2E8F0.toInt()
    private const val SUBTLE = 0xFFF8FAFC.toInt()
    private const val BLUE = 0xFF007BFF.toInt()
    private const val GREEN = 0xFF15803D.toInt()

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

    fun exportAndShare(
        context: Context,
        company: CompanySetting?,
        months: List<Pair<String, PayrollRepository.PayrollRow>>,
    ) {
        if (months.isEmpty()) return
        val worker = months.first().second.worker
        val file = render(context, company, worker, months)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Employee report — ${worker.fullName}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share report",
            ),
        )
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun stroke(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; strokeWidth = 1f; style = Paint.Style.STROKE
    }

    private fun render(
        context: Context,
        company: CompanySetting?,
        worker: Worker,
        months: List<Pair<String, PayrollRepository.PayrollRow>>,
    ): File {
        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var c: Canvas = page.canvas
        var y: Float

        val companyName = company?.name?.takeIf { it.isNotBlank() } ?: "Smart Worker"

        // ── Letterhead ──
        ImageStore.loadBitmap(company?.logo, 160)?.let {
            c.drawBitmap(it, null, RectF(MARGIN, 34f, MARGIN + 46f, 80f), null)
        }
        c.drawText(companyName, MARGIN + 58f, 52f, paint(NAVY, 16f, bold = true))
        company?.address?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, MARGIN + 58f, 68f, paint(SLATE, 9.5f))
        }
        val contact = listOfNotNull(
            company?.phone?.takeIf { it.isNotBlank() },
            company?.email?.takeIf { it.isNotBlank() },
            company?.gstNumber?.takeIf { it.isNotBlank() }?.let { "GST: $it" },
        ).joinToString("  •  ")
        if (contact.isNotBlank()) c.drawText(contact, MARGIN + 58f, 81f, paint(SLATE, 9f))

        c.drawText("EMPLOYEE REPORT", PAGE_W - MARGIN, 52f, paint(BLUE, 12f, bold = true, align = Paint.Align.RIGHT))
        c.drawText(LocalDate.now().format(dateFmt), PAGE_W - MARGIN, 68f, paint(SLATE, 9.5f, align = Paint.Align.RIGHT))
        c.drawLine(MARGIN, 94f, PAGE_W - MARGIN, 94f, stroke(BORDER))
        y = 116f

        // ── Worker identity block ──
        val blockH = 92f
        c.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + blockH), 12f, 12f, fill(SUBTLE))
        c.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + blockH), 12f, 12f, stroke(BORDER))

        val photo = ImageStore.loadBitmap(worker.profileImage, 256)
        val avatar = RectF(MARGIN + 14f, y + 14f, MARGIN + 78f, y + 78f)
        if (photo != null) {
            c.drawBitmap(photo, null, avatar, null)
        } else {
            c.drawRoundRect(avatar, 12f, 12f, fill(BLUE))
            c.drawText(
                worker.fullName.take(1).uppercase(Locale.US),
                avatar.centerX(), avatar.centerY() + 10f,
                paint(0xFFFFFFFF.toInt(), 26f, bold = true, align = Paint.Align.CENTER),
            )
        }
        val tx = avatar.right + 16f
        c.drawText(worker.fullName, tx, y + 34f, paint(NAVY, 16f, bold = true))
        c.drawText("${worker.position} • ${worker.department}", tx, y + 52f, paint(SLATE, 10.5f))
        c.drawText(
            "${worker.workerCode}   ·   ${worker.employeeType}   ·   ${worker.status.replaceFirstChar { it.uppercase() }}",
            tx, y + 68f, paint(SLATE, 10f),
        )

        // QR on the right of the identity block
        val qr = QrCodeGen.encode(QrCodeGen.workerPayload(worker.workerCode), 320)
        val qrSize = 64f
        c.drawBitmap(
            qr, null,
            RectF(PAGE_W - MARGIN - 14f - qrSize, y + 14f, PAGE_W - MARGIN - 14f, y + 14f + qrSize),
            null,
        )
        y += blockH + 22f

        // ── Personal & employment details ──
        c.drawText("PERSONAL & EMPLOYMENT DETAILS", MARGIN, y, paint(SLATE, 10f, bold = true).apply { letterSpacing = 0.08f })
        y += 14f

        val rate = when (worker.payType) {
            PayTypes.DAILY -> "₹${(worker.dailyRate ?: 0.0).toInt()} / day"
            PayTypes.MONTHLY -> "₹${(worker.monthlySalary ?: 0.0).toInt()} / month"
            PayTypes.HOURLY -> "₹${(worker.hourlyRate ?: 0.0).toInt()} / hour"
            else -> worker.projectRate?.let { "₹${it.toInt()} / project" } ?: "Project based"
        }
        val details = listOf(
            "Phone" to worker.phone.ifBlank { "—" },
            "Email" to worker.email.ifBlank { "—" },
            "Address" to worker.address.ifBlank { "—" },
            "Joined" to worker.joinDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            "Pay Type" to worker.payType.replaceFirstChar { it.uppercase() },
            "Pay Rate" to rate,
            "Overtime" to if (worker.overtimeEnabled) "Enabled" else "Disabled",
            "Leave Quota" to if (worker.leavePolicyEnabled) "${worker.allowedLeavesPerMonth} / month" else "Disabled",
        )
        val colW = (PAGE_W - MARGIN * 2) / 2
        details.forEachIndexed { i, (label, value) ->
            val col = i % 2
            val row = i / 2
            val x = MARGIN + col * colW
            val ry = y + row * 26f
            c.drawText(label, x, ry + 10f, paint(SLATE, 9f))
            c.drawText(value, x, ry + 22f, paint(NAVY, 11f, bold = true))
        }
        y += ((details.size + 1) / 2) * 26f + 18f

        // ── Month-by-month payroll table ──
        c.drawText("PAYROLL HISTORY", MARGIN, y, paint(SLATE, 10f, bold = true).apply { letterSpacing = 0.08f })
        y += 14f

        val cols = floatArrayOf(0f, 150f, 215f, 275f, 345f, 430f) // offsets from MARGIN
        val headers = listOf("Month", "Paid days", "OT (min)", "Gross", "Deductions", "Net pay")
        c.drawRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + 22f), fill(SUBTLE))
        headers.forEachIndexed { i, h ->
            val align = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT
            val x = if (i == 0) MARGIN + 8f else MARGIN + cols[i] + 50f
            c.drawText(h, x, y + 15f, paint(SLATE, 9f, bold = true, align = align))
        }
        y += 22f

        months.forEach { (label, row) ->
            if (y > PAGE_H - 90f) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                c = page.canvas
                y = MARGIN + 20f
            }
            c.drawText(label, MARGIN + 8f, y + 15f, paint(NAVY, 10f))
            val values = listOf(
                row.pay.paidDays.toString(),
                row.pay.overtimeMinutes.toString(),
                money(row.pay.grossPay),
                money(row.pay.totalDeductions),
                money(row.pay.estimatedPay),
            )
            values.forEachIndexed { i, v ->
                val bold = i == values.lastIndex
                c.drawText(
                    v, MARGIN + cols[i + 1] + 50f, y + 15f,
                    paint(if (bold) GREEN else NAVY, 10f, bold = bold, align = Paint.Align.RIGHT),
                )
            }
            c.drawLine(MARGIN, y + 22f, PAGE_W - MARGIN, y + 22f, stroke(BORDER))
            y += 22f
        }

        // ── Footer ──
        c.drawText(
            "Generated by $companyName · ${LocalDate.now().format(dateFmt)}",
            PAGE_W / 2f, PAGE_H - 34f, paint(SLATE, 9f, align = Paint.Align.CENTER),
        )

        doc.finishPage(page)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "worker_report_${worker.workerCode}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun money(v: Double): String = "₹" + String.format(Locale.forLanguageTag("en-IN"), "%,.0f", v)
}
