package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CompanySetting
import com.example.data.repo.PayrollRepository
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Generates a single-page A4 payslip PDF with the Android framework
 * PdfDocument API (no external dependencies) and opens a share sheet.
 */
object PayslipPdfExporter {

    private const val PAGE_W = 595 // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    fun exportAndShare(
        context: Context,
        company: CompanySetting?,
        row: PayrollRepository.PayrollRow,
        period: YearMonth,
    ) {
        val file = render(context, company, row, period)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Payslip — ${row.worker.fullName} — $period")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share payslip"))
    }

    fun render(
        context: Context,
        company: CompanySetting?,
        row: PayrollRepository.PayrollRow,
        period: YearMonth,
    ): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42); textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headPaint = Paint().apply {
            color = Color.rgb(13, 91, 255); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint().apply { color = Color.rgb(100, 116, 139); textSize = 11f }
        val valuePaint = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 11f }
        val valueBold = Paint(valuePaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val linePaint = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 1f }

        var y = MARGIN + 10f
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")

        // Header
        canvas.drawText(company?.name ?: "Smart Worker", MARGIN, y, titlePaint)
        y += 16f
        company?.address?.takeIf { it.isNotBlank() }?.let { canvas.drawText(it, MARGIN, y, labelPaint); y += 14f }
        company?.phone?.takeIf { it.isNotBlank() }?.let { canvas.drawText("Phone: $it", MARGIN, y, labelPaint); y += 14f }
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 24f

        canvas.drawText("PAYSLIP — ${period.format(fmt)}", MARGIN, y, headPaint)
        y += 22f

        // Worker block
        fun kv(label: String, value: String, bold: Boolean = false) {
            canvas.drawText(label, MARGIN, y, labelPaint)
            canvas.drawText(value, MARGIN + 180f, y, if (bold) valueBold else valuePaint)
            y += 16f
        }
        val w = row.worker
        kv("Worker", "${w.fullName} (${w.workerCode})", bold = true)
        kv("Designation / Department", "${w.position} / ${w.department}")
        kv("Pay Type", w.payType.replaceFirstChar { it.uppercase() })
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 20f

        // Attendance block
        canvas.drawText("ATTENDANCE", MARGIN, y, headPaint); y += 18f
        val att = row.attendance
        kv("Days Marked", att.totalMarkedDays.toString())
        kv("Paid Days (present + late)", row.pay.paidDays.toString())
        kv("Absent / Leave", "${att.absentDays} / ${att.leaveDays}")
        if (row.pay.halfDayDays > 0 || row.pay.proratedDays > 0) {
            kv("Half-day / Pro-rata days", "${row.pay.halfDayDays} / ${row.pay.proratedDays}")
        }
        kv("Overtime", "${row.pay.overtimeMinutes} min")
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 20f

        // Earnings / deductions
        canvas.drawText("EARNINGS", MARGIN, y, headPaint); y += 18f
        kv("Base Pay", money(row.pay.basePay))
        if (row.pay.overtimePay > 0) kv("Overtime Pay", money(row.pay.overtimePay))
        if (row.pay.closureExtraPay > 0) kv("Closure Day Extra", money(row.pay.closureExtraPay))
        if (row.pay.transactionEarnings > 0) kv("Bonus / Incentives", money(row.pay.transactionEarnings))
        kv("Gross Pay", money(row.pay.grossPay), bold = true)
        y += 8f

        canvas.drawText("DEDUCTIONS", MARGIN, y, headPaint); y += 18f
        if (row.pay.leaveDeductions > 0) kv("Leave Deduction", money(row.pay.leaveDeductions))
        if (row.pay.lateDeductions > 0) kv("Late Deduction", money(row.pay.lateDeductions))
        if (row.pay.delayPenalty > 0) kv("Project Delay Penalty", money(row.pay.delayPenalty))
        if (row.pay.transactionDeductions > 0) kv("Advances / Recoveries", money(row.pay.transactionDeductions))
        kv("Total Deductions", money(row.pay.totalDeductions), bold = true)
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 24f

        // Net pay
        val netPaint = Paint().apply {
            color = Color.rgb(22, 163, 74); textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("NET PAY: ${money(row.pay.estimatedPay)}", MARGIN, y, netPaint)
        y += 28f

        if (row.pay.policyNotes.isNotEmpty()) {
            canvas.drawText("Notes:", MARGIN, y, labelPaint); y += 14f
            for (note in row.pay.policyNotes.take(8)) {
                canvas.drawText("• $note", MARGIN, y, labelPaint)
                y += 13f
            }
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "payslips").apply { mkdirs() }
        val file = File(dir, "payslip_${w.workerCode}_${period}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun money(v: Double): String = "\u20B9%,.2f".format(v)
}
