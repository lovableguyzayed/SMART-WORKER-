package com.example.screens

import android.content.Context
import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CompanySetting
import com.example.data.model.Worker
import com.example.ui.LocalAppContainer
import com.example.ui.SwTopBar
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.util.ImageStore
import com.example.util.LocalImage
import com.example.util.QrCodeGen
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── ID-card palette (matches the design) ──
private val CardTop = Color(0xFF0A1C46)
private val CardMid = Color(0xFF15448C)
private val CardBottom = Color(0xFF1E74EA)
private val Glass = Color(0x24FFFFFF)      // translucent white fill
private val GlassStrong = Color(0x33FFFFFF)
private val GlassBorder = Color(0x33FFFFFF)
private val OnCard = Color(0xFFFFFFFF)
private val OnCardDim = Color(0xB3FFFFFF)   // ~70% white
private val Verified = Color(0xFF22C55E)

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

/**
 * Digital employee ID card with a scannable QR (payload SMARTWORKER:<code>,
 * read by the Quick Mark scanner). Rendered as a blue gradient badge and
 * exportable to a matching PDF.
 */
@Composable
fun IdCardScreen(workerId: Long, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val worker by remember(workerId) { container.workerRepository.worker(workerId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val company by remember { container.catalogRepository.company }
        .collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SwTopBar(
                title = "ID Card",
                leading = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack),
                    )
                },
            )
        },
    ) { padding ->
        val w = worker
        if (w == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IdCard(w, company)

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { exportIdCardPdf(context, company, w) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export ID Card PDF", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun IdCard(w: Worker, company: CompanySetting?) {
    val qr = remember(w.workerCode) { QrCodeGen.encode(QrCodeGen.workerPayload(w.workerCode), 480) }
    val qrId = "${w.workerCode}QR${w.joinDate.year}"

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(CardTop, CardMid, CardBottom)))
            .padding(22.dp),
    ) {
        Column {
            // ── Header: logo (center) + building icon (right) ──
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.align(Alignment.TopCenter).size(64.dp).clip(RoundedCornerShape(12.dp))
                        .background(CardTop.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LocalImage(company?.logo, "Logo", Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                        Icon(Icons.Filled.Business, null, tint = OnCardDim, modifier = Modifier.size(30.dp))
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).size(48.dp).clip(RoundedCornerShape(14.dp)).background(Glass),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Business, null, tint = OnCard, modifier = Modifier.size(24.dp)) }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                company?.name ?: "Smart Worker",
                color = OnCard, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Pill("Employee ID Card", fontSize = 15.sp)
            }

            Spacer(Modifier.height(22.dp))
            // ── Photo + name/role/status ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(120.dp)) {
                    Box(
                        Modifier.size(120.dp).clip(RoundedCornerShape(18.dp)).background(GlassStrong),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalImage(w.profileImage, "Photo of ${w.fullName}", Modifier.size(120.dp).clip(RoundedCornerShape(18.dp))) {
                            Text(w.fullName.take(1).uppercase(), color = OnCard, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (w.status == "active") {
                        Box(
                            Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp).size(32.dp)
                                .clip(CircleShape).background(OnCard),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(Verified), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Check, "Active", tint = OnCard, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(w.fullName, color = OnCard, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(w.position, color = OnCardDim, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("ID: ${w.workerCode}")
                        Pill(if (w.status == "active") "Active" else "Inactive")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            // ── Phone + joined date ──
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoField("Phone Number", w.phone.ifBlank { "—" }, Modifier.weight(1f))
                InfoField("Joined Date", w.joinDate.format(dateFmt), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            // ── QR row ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("QR Code for Attendance", color = OnCardDim, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Glass)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text("QR ID: $qrId", color = OnCard, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Box(Modifier.clip(RoundedCornerShape(14.dp)).background(OnCard).padding(10.dp)) {
                    Image(qr.asImageBitmap(), "QR code for ${w.workerCode}", modifier = Modifier.size(96.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.HorizontalDivider(color = GlassBorder)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Valid for attendance marking", color = OnCardDim, fontSize = 12.sp)
                Text("Generated: ${LocalDate.now().format(dateFmt)}", color = OnCardDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Pill(text: String, fontSize: androidx.compose.ui.unit.TextUnit = 14.sp) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(Glass).padding(horizontal = 14.dp, vertical = 6.dp),
    ) { Text(text, color = OnCard, fontSize = fontSize, fontWeight = FontWeight.Medium) }
}

@Composable
private fun InfoField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(14.dp),
    ) {
        Text(label, color = OnCardDim, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = OnCard, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── PDF export — mirrors the gradient card design ──────────────────────────────
private fun exportIdCardPdf(context: Context, company: CompanySetting?, w: Worker) {
    val pw = 420
    val ph = 640
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create())
    val c = page.canvas
    val fw = pw.toFloat(); val fh = ph.toFloat()

    fun px(dp: Int) = dp.toFloat()

    // Gradient background
    val bg = Paint().apply {
        shader = LinearGradient(0f, 0f, fw, fh, intArrayOf(0xFF0A1C46.toInt(), 0xFF15448C.toInt(), 0xFF1E74EA.toInt()), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    }
    c.drawRoundRect(RectF(0f, 0f, fw, fh), 28f, 28f, bg)

    val white = android.graphics.Color.WHITE
    val dim = 0xB3FFFFFF.toInt()
    val glass = 0x24FFFFFF
    fun bold(size: Float, color: Int = white, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = align
    }
    fun reg(size: Float, color: Int = white, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; textAlign = align
    }
    val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = glass }

    // Company logo box (top-center)
    c.drawRoundRect(RectF(fw / 2 - 30, 26f, fw / 2 + 30, 86f), 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8C0A1C46.toInt() })
    ImageStore.loadBitmap(company?.logo, 128)?.let { c.drawBitmap(it, null, RectF(fw / 2 - 30, 26f, fw / 2 + 30, 86f), null) }

    // Company name (wrap up to 2 lines, centered)
    val name = bold(24f, white, Paint.Align.CENTER)
    val companyName = company?.name ?: "Smart Worker"
    val words = companyName.split(" ")
    val lines = mutableListOf<String>()
    var cur = ""
    for (word in words) {
        val test = if (cur.isEmpty()) word else "$cur $word"
        if (name.measureText(test) > fw - 48 && cur.isNotEmpty()) { lines.add(cur); cur = word } else cur = test
    }
    if (cur.isNotEmpty()) lines.add(cur)
    var y = 116f
    for (line in lines.take(2)) { c.drawText(line, fw / 2, y, name); y += 28f }

    // "Employee ID Card" pill
    val pillW = 190f
    c.drawRoundRect(RectF(fw / 2 - pillW / 2, y - 6, fw / 2 + pillW / 2, y + 22), 16f, 16f, glassPaint)
    c.drawText("Employee ID Card", fw / 2, y + 14, reg(15f, dim, Paint.Align.CENTER))
    y += 46f

    // Photo + green check
    val photoL = 24f; val photoT = y; val photoSz = 120f
    c.drawRoundRect(RectF(photoL, photoT, photoL + photoSz, photoT + photoSz), 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF })
    val photo = ImageStore.loadBitmap(w.profileImage, 256)
    if (photo != null) {
        c.drawBitmap(photo, null, RectF(photoL, photoT, photoL + photoSz, photoT + photoSz), null)
    } else {
        c.drawText(w.fullName.take(1).uppercase(), photoL + photoSz / 2, photoT + photoSz / 2 + 16, bold(42f, white, Paint.Align.CENTER))
    }
    if (w.status == "active") {
        val bx = photoL + photoSz - 6; val by = photoT + photoSz - 6
        c.drawCircle(bx, by, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white })
        c.drawCircle(bx, by, 13f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF22C55E.toInt() })
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; strokeWidth = 2.5f; style = Paint.Style.STROKE }
        c.drawLines(floatArrayOf(bx - 6, by, bx - 2, by + 4, bx - 2, by + 4, bx + 6, by - 5), tick)
    }

    // Name / role / pills (right of photo)
    val rx = photoL + photoSz + 16
    c.drawText(w.fullName, rx, photoT + 26, bold(21f, white, Paint.Align.LEFT))
    c.drawText(w.position, rx, photoT + 50, reg(16f, dim, Paint.Align.LEFT))
    fun chip(text: String, x: Float, top: Float): Float {
        val p = reg(13f, white, Paint.Align.LEFT); val tw = p.measureText(text)
        c.drawRoundRect(RectF(x, top, x + tw + 22, top + 26), 13f, 13f, glassPaint)
        c.drawText(text, x + 11, top + 18, p); return x + tw + 22 + 8
    }
    val chipTop = photoT + 66
    val next = chip("ID: ${w.workerCode}", rx, chipTop)
    chip(if (w.status == "active") "Active" else "Inactive", next, chipTop)
    y = photoT + photoSz + 22

    // Phone + Joined cards
    val gap = 12f; val cardW = (fw - 48 - gap) / 2
    fun infoCard(x: Float, label: String, value: String) {
        c.drawRoundRect(RectF(x, y, x + cardW, y + 66), 14f, 14f, glassPaint)
        c.drawText(label, x + 14, y + 24, reg(13f, dim, Paint.Align.LEFT))
        c.drawText(value, x + 14, y + 48, bold(17f, white, Paint.Align.LEFT))
    }
    infoCard(24f, "Phone Number", w.phone.ifBlank { "—" })
    infoCard(24f + cardW + gap, "Joined Date", w.joinDate.format(dateFmt))
    y += 66 + 22

    // QR label + pill (left), QR box (right)
    c.drawText("QR Code for Attendance", 24f, y + 4, reg(14f, dim, Paint.Align.LEFT))
    val qrId = "${w.workerCode}QR${w.joinDate.year}"
    c.drawRoundRect(RectF(24f, y + 16, 24f + 220, y + 54), 12f, 12f, glassPaint)
    c.drawText("QR ID: $qrId", 40f, y + 40, reg(15f, white, Paint.Align.LEFT))
    val qrBox = 108f; val qrX = fw - 24 - qrBox
    c.drawRoundRect(RectF(qrX, y, qrX + qrBox, y + qrBox), 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white })
    val qr = QrCodeGen.encode(QrCodeGen.workerPayload(w.workerCode), 400)
    c.drawBitmap(qr, null, RectF(qrX + 10, y + 10, qrX + qrBox - 10, y + qrBox - 10), null)
    y += qrBox + 20

    // Divider + footer
    c.drawLine(24f, y, fw - 24, y, Paint().apply { color = 0x33FFFFFF; strokeWidth = 1f }); y += 22
    c.drawText("Valid for attendance marking", 24f, y, reg(12f, dim, Paint.Align.LEFT))
    c.drawText("Generated: ${LocalDate.now().format(dateFmt)}", fw - 24, y, reg(12f, dim, Paint.Align.RIGHT))

    doc.finishPage(page)
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "id_card_${w.workerCode}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share ID card",
        ),
    )
}
