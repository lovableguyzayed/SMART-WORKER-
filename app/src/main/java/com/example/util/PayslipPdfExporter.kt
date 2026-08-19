package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.CompanySetting
import com.example.data.repo.PayrollRepository
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders the payslip as a designed A4 PDF using the framework PdfDocument API
 * (no external dependencies): gradient header, attendance summary tiles, a
 * colour-coded attendance calendar, pay breakdown, leave ledger, and a net
 * payable banner. Content flows across pages without clipping, and every page
 * carries the running header/footer.
 */
object PayslipPdfExporter {

    private const val PAGE_W = 595 // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val CONTENT_TOP = 92f
    private const val CONTENT_BOTTOM = 792f
    private val CONTENT_W = PAGE_W - MARGIN * 2

    // ── Palette ──
    private const val NAVY = 0xFF0F172A.toInt()
    private const val SLATE = 0xFF64748B.toInt()
    private const val BORDER = 0xFFE2E8F0.toInt()
    private const val SUBTLE = 0xFFF8FAFC.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val GRAD_A = 0xFF0A2A6B.toInt()
    private const val GRAD_B = 0xFF1565E0.toInt()
    private const val GREEN = 0xFF15803D.toInt()
    private const val GREEN_BG = 0xFFECFDF5.toInt()
    private const val RED = 0xFFDC2626.toInt()
    private const val RED_BG = 0xFFFEF2F2.toInt()
    private const val AMBER = 0xFFB45309.toInt()
    private const val AMBER_BG = 0xFFFEFCE8.toInt()
    private const val BLUE = 0xFF1D4ED8.toInt()
    private const val BLUE_BG = 0xFFEFF6FF.toInt()
    private const val GRAY_BG = 0xFFF1F5F9.toInt()

    private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    fun exportAndShare(
        context: Context,
        company: CompanySetting?,
        row: PayrollRepository.PayrollRow,
        period: YearMonth,
        records: List<AttendanceRecord> = emptyList(),
    ) {
        val file = render(context, company, row, period, records)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Payslip — ${row.worker.fullName} — ${period.format(monthFmt)}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share payslip"))
    }

    fun render(
        context: Context,
        company: CompanySetting?,
        row: PayrollRepository.PayrollRow,
        period: YearMonth,
        records: List<AttendanceRecord> = emptyList(),
    ): File {
        // Pass 1 counts pages so the footer can print "Page X of Y"; the throwaway
        // document is never written to disk.
        val probe = PdfDocument()
        val total = draw(probe, company, row, period, records, totalPages = 0)
        probe.close()

        val doc = PdfDocument()
        draw(doc, company, row, period, records, totalPages = total)

        val dir = File(context.cacheDir, "payslips").apply { mkdirs() }
        val file = File(dir, "payslip_${row.worker.workerCode}_$period.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ── Paints ────────────────────────────────────────────────────────────────
    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun stroke(color: Int, width: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; strokeWidth = width; style = Paint.Style.STROKE
    }

    /** Uppercase section label with the wide letter-spacing used in the design. */
    private fun Canvas.sectionLabel(text: String, y: Float) {
        val p = paint(SLATE, 10.5f, bold = true).apply { letterSpacing = 0.12f }
        drawText(text.uppercase(Locale.US), MARGIN, y, p)
    }

    // ── Renderer ──────────────────────────────────────────────────────────────
    /** Draws the whole payslip; returns the number of pages produced. */
    private fun draw(
        doc: PdfDocument,
        company: CompanySetting?,
        row: PayrollRepository.PayrollRow,
        period: YearMonth,
        records: List<AttendanceRecord>,
        totalPages: Int,
    ): Int {
        val w = row.worker
        val pay = row.pay
        val att = row.attendance
        val companyName = company?.name?.takeIf { it.isNotBlank() } ?: "Smart Worker"
        val periodLabel = period.format(monthFmt)

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas = Canvas()
        var y = 0f

        fun drawRunningHeader(c: Canvas) {
            c.drawText(w.fullName.uppercase(Locale.US), MARGIN, 48f, paint(NAVY, 11.5f, bold = true))
            c.drawText(
                "ID ${w.workerCode}  ·  Payslip $periodLabel",
                PAGE_W - MARGIN, 48f, paint(SLATE, 9.5f, align = Paint.Align.RIGHT),
            )
            c.drawLine(MARGIN, 60f, PAGE_W - MARGIN, 60f, stroke(BORDER))
        }

        fun drawFooter(c: Canvas, n: Int) {
            c.drawText(
                "Generated by $companyName · ${LocalDate.now().format(dateFmt)}",
                PAGE_W / 2f, 812f, paint(SLATE, 9f, align = Paint.Align.CENTER),
            )
            val label = if (totalPages > 0) "Page $n of $totalPages" else "Page $n"
            c.drawText(label, PAGE_W / 2f, 826f, paint(SLATE, 8f, align = Paint.Align.CENTER))
        }

        fun newPage() {
            page?.let { drawFooter(it.canvas, pageNo); doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            drawRunningHeader(canvas)
            y = CONTENT_TOP
        }

        /** Starts a new page when [needed] height would overflow the content area. */
        fun ensure(needed: Float) {
            if (y + needed > CONTENT_BOTTOM) newPage()
        }

        newPage()

        // ── 1. Gradient hero card ────────────────────────────────────────────
        run {
            val h = 150f
            ensure(h + 16f)
            val rect = RectF(MARGIN, y, PAGE_W - MARGIN, y + h)
            val grad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, GRAD_A, GRAD_B, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(rect, 16f, 16f, grad)

            canvas.drawText(companyName, MARGIN + 22f, y + 38f, paint(WHITE, 19f, bold = true))
            company?.address?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, MARGIN + 22f, y + 55f, paint(0xCCFFFFFF.toInt(), 10.5f))
            }
            canvas.drawText("PAYSLIP", PAGE_W - MARGIN - 22f, y + 36f, paint(WHITE, 15f, bold = true, align = Paint.Align.RIGHT))
            canvas.drawText(periodLabel, PAGE_W - MARGIN - 22f, y + 54f, paint(0xE6FFFFFF.toInt(), 12f, align = Paint.Align.RIGHT))

            // Inner translucent worker card
            val inner = RectF(MARGIN + 18f, y + 70f, PAGE_W - MARGIN - 18f, y + 132f)
            canvas.drawRoundRect(inner, 12f, 12f, fill(0x33FFFFFF))
            val av = RectF(inner.left + 12f, inner.top + 10f, inner.left + 54f, inner.top + 52f)
            canvas.drawRoundRect(av, 10f, 10f, fill(0x40FFFFFF))
            canvas.drawText(
                w.fullName.take(1).uppercase(Locale.US),
                av.centerX(), av.centerY() + 8f, paint(WHITE, 22f, bold = true, align = Paint.Align.CENTER),
            )
            val tx = av.right + 14f
            canvas.drawText(w.fullName.uppercase(Locale.US), tx, inner.top + 22f, paint(WHITE, 13.5f, bold = true))
            canvas.drawText("${w.position} · ${w.department}", tx, inner.top + 37f, paint(0xCCFFFFFF.toInt(), 10f))
            canvas.drawText(
                "ID: ${w.workerCode} · ${w.payType.replaceFirstChar { it.uppercase() }}",
                tx, inner.top + 51f, paint(0xCCFFFFFF.toInt(), 10f),
            )

            // Status pill
            val statusText = when (row.status) {
                "paid" -> "PAID"
                "pending" -> "PENDING"
                else -> "DRAFT"
            }
            val sp = paint(WHITE, 10f, bold = true, align = Paint.Align.CENTER)
            val pw = sp.measureText(statusText) + 28f
            val pill = RectF(inner.right - 12f - pw, inner.top + 18f, inner.right - 12f, inner.top + 44f)
            canvas.drawRoundRect(pill, 13f, 13f, fill(0x40FFFFFF))
            canvas.drawText(statusText, pill.centerX(), pill.centerY() + 3.5f, sp)

            y += h + 22f
        }

        // ── 2. Attendance summary tiles ──────────────────────────────────────
        run {
            ensure(90f)
            canvas.sectionLabel("Attendance Summary", y)
            y += 14f
            val tiles = listOf(
                Triple("Present", att.presentDays.toString(), GREEN to GREEN_BG),
                Triple("Absent", att.absentDays.toString(), RED to RED_BG),
                Triple("Late", att.lateDays.toString(), AMBER to AMBER_BG),
                Triple("Leave", att.leaveDays.toString(), BLUE to BLUE_BG),
                Triple("Paid Days", pay.paidDays.toString(), NAVY to GRAY_BG),
            )
            val gap = 9f
            val tw = (CONTENT_W - gap * (tiles.size - 1)) / tiles.size
            tiles.forEachIndexed { i, (label, value, colors) ->
                val left = MARGIN + i * (tw + gap)
                val r = RectF(left, y, left + tw, y + 56f)
                canvas.drawRoundRect(r, 10f, 10f, fill(colors.second))
                canvas.drawRoundRect(r, 10f, 10f, stroke(BORDER))
                canvas.drawText(value, r.centerX(), y + 28f, paint(colors.first, 17f, bold = true, align = Paint.Align.CENTER))
                canvas.drawText(label, r.centerX(), y + 45f, paint(colors.first, 9.5f, align = Paint.Align.CENTER))
            }
            y += 56f + 22f
        }

        // ── 3. Attendance calendar ───────────────────────────────────────────
        run {
            val byDay = records.associateBy { it.date.dayOfMonth }
            val first = period.atDay(1)
            // Grid starts on Sunday, like the design.
            val lead = first.dayOfWeek.value % 7 // Sun=0 … Sat=6
            val daysInMonth = period.lengthOfMonth()
            val rows = Math.ceil((lead + daysInMonth) / 7.0).toInt()
            val cellH = 26f
            val gridH = cellH * (rows + 1)

            ensure(gridH + 46f)
            canvas.sectionLabel("Attendance Calendar — $periodLabel", y)
            y += 14f

            val cw = CONTENT_W / 7f
            val top = y
            // Header row
            canvas.drawRoundRect(RectF(MARGIN, top, PAGE_W - MARGIN, top + cellH), 8f, 8f, fill(SUBTLE))
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEachIndexed { i, d ->
                canvas.drawText(
                    d, MARGIN + cw * i + cw / 2, top + 17f,
                    paint(SLATE, 8.5f, bold = true, align = Paint.Align.CENTER),
                )
            }
            // Day cells
            for (idx in 0 until rows * 7) {
                val col = idx % 7
                val rowI = idx / 7
                val cellL = MARGIN + col * cw
                val cellT = top + cellH * (rowI + 1)
                val cell = RectF(cellL, cellT, cellL + cw, cellT + cellH)
                val dayNum = idx - lead + 1
                val inMonth = dayNum in 1..daysInMonth

                val status = if (inMonth) byDay[dayNum]?.status else null
                val bg = when (status) {
                    AttendanceStatus.PRESENT -> GREEN_BG
                    AttendanceStatus.ABSENT -> RED_BG
                    AttendanceStatus.LATE -> AMBER_BG
                    AttendanceStatus.LEAVE -> BLUE_BG
                    else -> if (inMonth) WHITE else SUBTLE
                }
                val fg = when (status) {
                    AttendanceStatus.PRESENT -> GREEN
                    AttendanceStatus.ABSENT -> RED
                    AttendanceStatus.LATE -> AMBER
                    AttendanceStatus.LEAVE -> BLUE
                    else -> if (inMonth) NAVY else 0xFFCBD5E1.toInt()
                }
                canvas.drawRect(cell, fill(bg))
                canvas.drawRect(cell, stroke(BORDER))
                val label = if (inMonth) dayNum.toString() else {
                    val d = if (dayNum < 1) first.minusDays((1 - dayNum).toLong())
                    else period.atEndOfMonth().plusDays((dayNum - daysInMonth).toLong())
                    d.dayOfMonth.toString()
                }
                canvas.drawText(label, cell.centerX(), cellT + 17f, paint(fg, 9.5f, align = Paint.Align.CENTER))
            }
            y = top + gridH + 16f

            // Legend
            val legend = listOf("Present" to 0xFF22C55E.toInt(), "Absent" to 0xFFEF4444.toInt(), "Late" to 0xFFF59E0B.toInt(), "Leave" to 0xFF3B82F6.toInt())
            val lp = paint(SLATE, 9.5f)
            val totalLegendW = legend.sumOf { (lp.measureText(it.first) + 26f).toDouble() }.toFloat()
            var lx = MARGIN + (CONTENT_W - totalLegendW) / 2
            legend.forEach { (label, color) ->
                canvas.drawCircle(lx + 4f, y - 3.5f, 3.5f, fill(color))
                canvas.drawText(label, lx + 13f, y, lp)
                lx += lp.measureText(label) + 26f
            }
            y += 24f
        }

        // ── 4. Pay breakdown ─────────────────────────────────────────────────
        run {
            data class Line(val label: String, val value: String, val bold: Boolean = false, val muted: String? = null)
            val lines = buildList {
                when (pay.payType) {
                    "monthly" -> add(Line("Monthly Salary", money(w.monthlySalary ?: 0.0)))
                    "daily" -> add(Line("Daily Rate × ${pay.paidDays} paid days", money(w.dailyRate ?: 0.0)))
                    "hourly" -> add(Line("Hourly Rate", money(w.hourlyRate ?: 0.0)))
                    else -> add(Line("Project Rate", money(w.projectRate ?: 0.0)))
                }
                if (pay.leaveDays > 0 || pay.extraLeaveDays > 0) {
                    add(Line("Leave Days", pay.leaveDays.toString(), muted = "(${pay.extraLeaveDays} extra)"))
                }
                add(Line("Base Pay", money(pay.basePay), bold = true))
                if (pay.overtimePay > 0) add(Line("Overtime Pay (${pay.overtimeMinutes} min)", money(pay.overtimePay)))
                if (pay.closureExtraPay > 0) add(Line("Closure Day Extra", money(pay.closureExtraPay)))
                if (pay.transactionEarnings > 0) add(Line("Bonus / Incentives", money(pay.transactionEarnings)))
                if (pay.leaveDeductions > 0) add(Line("Leave Deduction", "− " + money(pay.leaveDeductions)))
                if (pay.lateDeductions > 0) add(Line("Late Deduction (${pay.lateMinutes} min)", "− " + money(pay.lateDeductions)))
                if (pay.delayPenalty > 0) add(Line("Project Delay Penalty", "− " + money(pay.delayPenalty)))
                if (pay.transactionDeductions > 0) add(Line("Advances / Recoveries", "− " + money(pay.transactionDeductions)))
                add(Line("Gross Pay", money(pay.grossPay), bold = true))
                add(Line("Total Deductions", "− " + money(pay.totalDeductions), bold = true))
            }
            val rowH = 26f
            val cardH = lines.size * rowH + 16f
            ensure(cardH + 30f)
            canvas.sectionLabel("Pay Breakdown", y)
            y += 14f
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + cardH), 12f, 12f, fill(SUBTLE))
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + cardH), 12f, 12f, stroke(BORDER))
            var ry = y + 8f
            lines.forEachIndexed { i, line ->
                val baseline = ry + 17f
                val lblPaint = paint(NAVY, if (line.bold) 11.5f else 11f, bold = line.bold)
                canvas.drawText(line.label, MARGIN + 16f, baseline, lblPaint)
                line.muted?.let {
                    canvas.drawText(it, MARGIN + 16f + lblPaint.measureText(line.label) + 6f, baseline, paint(SLATE, 10f))
                }
                canvas.drawText(
                    line.value, PAGE_W - MARGIN - 16f, baseline,
                    paint(NAVY, if (line.bold) 12f else 11f, bold = line.bold, align = Paint.Align.RIGHT),
                )
                if (i < lines.lastIndex) {
                    canvas.drawLine(MARGIN + 16f, ry + rowH, PAGE_W - MARGIN - 16f, ry + rowH, stroke(BORDER))
                }
                ry += rowH
            }
            y += cardH + 20f
        }

        // ── 5. Leave balance ─────────────────────────────────────────────────
        pay.leaveBalance?.let { lb ->
            val h = 68f
            ensure(h + 16f)
            val rect = RectF(MARGIN, y, PAGE_W - MARGIN, y + h)
            canvas.drawRoundRect(rect, 12f, 12f, fill(BLUE_BG))
            canvas.drawRoundRect(rect, 12f, 12f, stroke(0xFFBFDBFE.toInt()))
            canvas.drawText("LEAVE BALANCE (SINCE JOINING)", MARGIN + 16f, y + 20f, paint(BLUE, 10f, bold = true).apply { letterSpacing = 0.06f })
            val stats = listOf(
                lb.monthlyQuota.toString() to "Quota/mo",
                oneDec(lb.balanceBefore) to "Carried",
                lb.usedThisMonth.toString() to "Used",
                oneDec(lb.balanceAfter) to "Remaining",
            )
            val colW = CONTENT_W / stats.size
            stats.forEachIndexed { i, (value, label) ->
                val cx = MARGIN + colW * i + colW / 2
                canvas.drawText(value, cx, y + 45f, paint(BLUE, 14f, bold = true, align = Paint.Align.CENTER))
                canvas.drawText(label, cx, y + 59f, paint(SLATE, 9f, align = Paint.Align.CENTER))
            }
            y += h + 16f
        }

        // ── 6. Net payable banner ────────────────────────────────────────────
        run {
            val h = 74f
            ensure(h + 20f)
            val rect = RectF(MARGIN, y, PAGE_W - MARGIN, y + h)
            val grad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, GRAD_A, GRAD_B, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(rect, 14f, 14f, grad)
            canvas.drawText("NET PAYABLE", MARGIN + 22f, y + 28f, paint(0xCCFFFFFF.toInt(), 10.5f, bold = true).apply { letterSpacing = 0.1f })
            canvas.drawText(money(pay.estimatedPay), MARGIN + 22f, y + 57f, paint(WHITE, 24f, bold = true))

            val paid = row.status == "paid"
            val badgeCx = PAGE_W - MARGIN - 46f
            canvas.drawCircle(badgeCx, y + 30f, 15f, fill(if (paid) 0xFF22C55E.toInt() else 0xFFFACC15.toInt()))
            canvas.drawText(
                if (paid) "✓" else "!",
                badgeCx, y + 36f, paint(if (paid) WHITE else NAVY, 16f, bold = true, align = Paint.Align.CENTER),
            )
            canvas.drawText(
                if (paid) "Paid" else "Pending",
                badgeCx, y + 60f, paint(WHITE, 10f, align = Paint.Align.CENTER),
            )
            y += h + 14f
        }

        // ── 7. Policy note ───────────────────────────────────────────────────
        pay.policyNotes.firstOrNull()?.let { note ->
            ensure(20f)
            canvas.drawText(note, MARGIN, y + 10f, paint(SLATE, 9.5f))
            y += 24f
        }

        // ── 8. Signatures + closing ──────────────────────────────────────────
        run {
            ensure(60f)
            y += 22f
            val halfW = (CONTENT_W - 40f) / 2
            canvas.drawLine(MARGIN, y, MARGIN + halfW, y, stroke(BORDER))
            canvas.drawLine(PAGE_W - MARGIN - halfW, y, PAGE_W - MARGIN, y, stroke(BORDER))
            canvas.drawText("Employer Signature", MARGIN + halfW / 2, y + 16f, paint(SLATE, 10f, align = Paint.Align.CENTER))
            canvas.drawText("Employee Signature", PAGE_W - MARGIN - halfW / 2, y + 16f, paint(SLATE, 10f, align = Paint.Align.CENTER))
            y += 26f
        }

        page?.let { drawFooter(it.canvas, pageNo); doc.finishPage(it) }
        return pageNo
    }

    private fun money(v: Double): String = "₹" + String.format(Locale.forLanguageTag("en-IN"), "%,.2f", v)
    private fun oneDec(v: Double): String = String.format(Locale.US, "%.1f", v)
}
