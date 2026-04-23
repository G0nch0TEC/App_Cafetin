package com.proyecto.cafetin.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH  = 595   // A4 a 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN      = 48f

    private val sdfFecha  = SimpleDateFormat("d 'de' MMMM 'de' yyyy",  Locale("es"))
    private val sdfHora   = SimpleDateFormat("h:mm a",                  Locale("es"))
    private val sdfRango = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
    private val sdfNombre = SimpleDateFormat("yyyyMMdd",                Locale("es"))

    /**
     * Genera el PDF y devuelve el File creado, o null si hubo error.
     * El archivo se guarda en la carpeta privada de la app (no necesita permiso WRITE_EXTERNAL_STORAGE).
     */
    fun generar(
        context: Context,
        nombrePersona: String,
        descripcionPersona: String,
        movimientos: List<Movimiento>,
        desde: Long,
        hasta: Long
    ): File? = runCatching {

        val movOrdenados = movimientos.sortedBy { it.fecha }

        val doc  = PdfDocument()
        var pageNum    = 1
        var pageInfo   = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page       = doc.startPage(pageInfo)
        var canvas     = page.canvas
        var y          = MARGIN

        // ── Pinturas ────────────────────────────────────────────────────────
        val pTitulo = Paint().apply {
            color     = Color.rgb(103, 80, 164)   // CafetinPrimary
            textSize  = 22f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        val pSubtitulo = Paint().apply {
            color    = Color.rgb(117, 117, 117)
            textSize = 12f
            isAntiAlias = true
        }
        val pSeccion = Paint().apply {
            color    = Color.rgb(103, 80, 164)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        val pNormal = Paint().apply {
            color    = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val pNota = Paint().apply {
            color    = Color.rgb(60, 60, 60)
            textSize = 12f
            isAntiAlias = true
        }
        val pMontoPago = Paint().apply {
            color    = Color.rgb(39, 80, 10)    // OkGreen
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        val pMontoFiado = Paint().apply {
            color    = Color.rgb(163, 45, 45)   // DebtRed
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        val pLinea = Paint().apply {
            color       = Color.rgb(202, 196, 208)
            strokeWidth = 1f
            isAntiAlias = true
        }
        val pTotal = Paint().apply {
            color    = Color.BLACK
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias    = true
        }
        val pTotalMonto = Paint().apply {
            color    = Color.rgb(163, 45, 45)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias    = true
        }

        // ── Helper: nueva página si no cabe ─────────────────────────────────
        fun checkPagina(alturaNeeded: Float) {
            if (y + alturaNeeded > PAGE_HEIGHT - MARGIN) {
                doc.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page     = doc.startPage(pageInfo)
                canvas   = page.canvas
                y        = MARGIN
            }
        }

        // ── Encabezado ───────────────────────────────────────────────────────
        canvas.drawText("Cafetín", MARGIN, y, pTitulo)
        y += 28f

        canvas.drawText(
            "Reporte generado el ${sdfFecha.format(Date())}",
            MARGIN, y, pSubtitulo
        )
        y += 20f

        // Línea separadora
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, pLinea)
        y += 20f

        // ── Datos de la persona ──────────────────────────────────────────────
        canvas.drawText("CLIENTE", MARGIN, y, pSeccion)
        y += 18f
        canvas.drawText(nombrePersona, MARGIN, y, pNormal.apply { isFakeBoldText = true; textSize = 15f })
        pNormal.apply { isFakeBoldText = false; textSize = 12f }
        y += 18f
        if (descripcionPersona.isNotBlank()) {
            canvas.drawText(descripcionPersona, MARGIN, y, pSubtitulo)
            y += 16f
        }

        // Período
        canvas.drawText(
            "Período: ${sdfRango.format(Date(desde))} — ${sdfRango.format(Date(hasta))}",
            MARGIN, y, pSubtitulo
        )
        y += 24f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, pLinea)
        y += 20f

        // ── Movimientos agrupados por día ────────────────────────────────────
        if (movOrdenados.isEmpty()) {
            canvas.drawText("No hay movimientos en este período.", MARGIN, y, pSubtitulo)
            y += 20f
        } else {
            // Agrupar por día
            val porDia = movOrdenados.groupBy { mov ->
                sdfFecha.format(Date(mov.fecha))
            }

            for ((dia, movsDelDia) in porDia) {
                checkPagina(40f)

                // Cabecera del día
                canvas.drawText(dia.uppercase(), MARGIN, y, pSeccion)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, pLinea)
                y += 14f

                for (mov in movsDelDia) {
                    checkPagina(22f)

                    val hora       = sdfHora.format(Date(mov.fecha))
                    val nota       = mov.nota.ifBlank { if (mov.tipo == TipoMovimiento.PAGO) "Pago" else "Fiado" }
                    val esPago     = mov.tipo == TipoMovimiento.PAGO
                    val montoTexto = if (esPago) "+${mov.monto.fmt()}" else "−${mov.monto.fmt()}"
                    val pMonto     = if (esPago) pMontoPago else pMontoFiado
                    val prefijo    = if (esPago) "↑" else "↓"

                    // Hora + prefijo
                    canvas.drawText("$hora  $prefijo", MARGIN, y, pSubtitulo)

                    // Nota (concepto)
                    canvas.drawText(nota, MARGIN + 100f, y, pNota)

                    // Monto alineado a la derecha
                    val montoW = pMonto.measureText(montoTexto)
                    canvas.drawText(montoTexto, PAGE_WIDTH - MARGIN - montoW, y, pMonto)

                    y += 20f
                }

                // Subtotal del día
                val fiadoDia   = movsDelDia.filter { it.tipo == TipoMovimiento.FIADO  }.sumOf { it.monto }
                val cobradoDia = movsDelDia.filter { it.tipo == TipoMovimiento.PAGO   }.sumOf { it.monto }
                checkPagina(20f)
                val subtotalTxt = "Subtotal día: fiado ${fiadoDia.fmt()}  •  cobrado ${cobradoDia.fmt()}"
                canvas.drawText(subtotalTxt, MARGIN, y, pSubtitulo)
                y += 24f
            }
        }

        // ── Totales finales ──────────────────────────────────────────────────
        checkPagina(60f)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, pLinea)
        y += 18f

        val totalFiado   = movimientos.filter { it.tipo == TipoMovimiento.FIADO }.sumOf { it.monto }
        val totalCobrado = movimientos.filter { it.tipo == TipoMovimiento.PAGO  }.sumOf { it.monto }
        val pendiente    = totalFiado - totalCobrado

        canvas.drawText("Total fiado:",   MARGIN, y, pTotal)
        val fw = pTotalMonto.measureText(totalFiado.fmt())
        canvas.drawText(totalFiado.fmt(), PAGE_WIDTH - MARGIN - fw, y, pMontoFiado)
        y += 20f

        canvas.drawText("Total cobrado:", MARGIN, y, pTotal)
        val cw = pMontoPago.measureText(totalCobrado.fmt())
        canvas.drawText(totalCobrado.fmt(), PAGE_WIDTH - MARGIN - cw, y, pMontoPago)
        y += 20f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, pLinea)
        y += 18f

        val pendientePaint = if (pendiente > 0) pTotalMonto else pMontoPago
        val pendienteTxt   = if (pendiente > 0) "Saldo pendiente" else "Saldo saldado ✓"
        canvas.drawText(pendienteTxt, MARGIN, y, pTotal)
        val pw = pendientePaint.measureText(pendiente.fmt())
        canvas.drawText(pendiente.fmt(), PAGE_WIDTH - MARGIN - pw, y, pendientePaint)

        doc.finishPage(page)

        // ── Guardar archivo ──────────────────────────────────────────────────
        val nombreLimpio = nombrePersona.replace(" ", "_")
        val fechaArchivo = sdfNombre.format(Date())
        val dir = File(context.filesDir, "reportes").also { it.mkdirs() }
        val file = File(dir, "Reporte_${nombreLimpio}_$fechaArchivo.pdf")

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        file
    }.getOrNull()

    /** Devuelve un Uri compartible via FileProvider */
    fun uriParaCompartir(context: Context, file: File) =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

    private fun Long.fmt(): String {
        val abs = kotlin.math.abs(this)
        return "S/${abs / 100}.${ "%02d".format(abs % 100) }"
    }
}
