package com.example.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CompanySetting
import com.example.data.model.Worker
import com.example.ui.CardBorder
import com.example.ui.LocalAppContainer
import com.example.ui.SwTopBar
import com.example.ui.theme.AvatarBlueBg
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkBlue
import com.example.ui.theme.Navy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextSecondary
import com.example.util.ImageStore
import com.example.util.LocalImage
import com.example.util.QrCodeGen
import java.io.File

/**
 * Digital worker ID card with scannable QR (port of Flask id_card.html).
 * The QR payload is SMARTWORKER:<code>, readable by the Quick Mark scanner.
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
                androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }
        val qr = remember(w.workerCode) { QrCodeGen.encode(QrCodeGen.workerPayload(w.workerCode), 480) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(4.dp),
                border = CardBorder,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Company banner
                    Box(
                        Modifier.fillMaxWidth().background(DarkBlue).padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            company?.name ?: "Smart Worker",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    // Photo
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(AvatarBlueBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalImage(
                            path = w.profileImage,
                            contentDescription = "Photo of ${w.fullName}",
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                        ) {
                            Icon(Icons.Filled.Person, null, tint = PrimaryBlue, modifier = Modifier.size(52.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(w.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("${w.position} • ${w.department}", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        w.workerCode,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "QR code for ${w.workerCode}",
                        modifier = Modifier.size(180.dp),
                    )
                    Text(
                        "Scan with Quick Mark to check in/out",
                        fontSize = 11.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { exportIdCardPdf(context, company, w) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            ) {
                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export ID Card PDF", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Renders a printable CR80-proportioned ID card PDF and opens the share sheet. */
private fun exportIdCardPdf(context: Context, company: CompanySetting?, w: Worker) {
    val pageW = 324 // ~ CR80 card at 96dpi, portrait
    val pageH = 512
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
    val canvas = page.canvas

    val navy = 0xFF001B4E.toInt()
    val blue = 0xFF0D5BFF.toInt()
    val gray = 0xFF64748B.toInt()

    // Banner
    canvas.drawRect(0f, 0f, pageW.toFloat(), 56f, Paint().apply { color = navy })
    val banner = Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
    }
    canvas.drawText(company?.name ?: "Smart Worker", pageW / 2f, 34f, banner)

    // Photo (circle-cropped square draw)
    val photo = ImageStore.loadBitmap(w.profileImage, 256)
    if (photo != null) {
        val dst = android.graphics.RectF(pageW / 2f - 48f, 76f, pageW / 2f + 48f, 172f)
        canvas.drawBitmap(photo, null, dst, null)
    }

    val name = Paint().apply {
        color = navy; textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
    }
    val sub = Paint().apply { color = gray; textSize = 12f; textAlign = Paint.Align.CENTER }
    val code = Paint().apply {
        color = blue; textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
    }
    var y = if (photo != null) 196f else 110f
    canvas.drawText(w.fullName, pageW / 2f, y, name); y += 18f
    canvas.drawText("${w.position} • ${w.department}", pageW / 2f, y, sub); y += 20f
    canvas.drawText(w.workerCode, pageW / 2f, y, code); y += 16f

    val qr = QrCodeGen.encode(QrCodeGen.workerPayload(w.workerCode), 400)
    val qrSize = 190f
    canvas.drawBitmap(
        qr, null,
        android.graphics.RectF(pageW / 2f - qrSize / 2, y, pageW / 2f + qrSize / 2, y + qrSize),
        null,
    )
    canvas.drawText("Scan to check in / out", pageW / 2f, y + qrSize + 18f, sub)

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
        )
    )
}
