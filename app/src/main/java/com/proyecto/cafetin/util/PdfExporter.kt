package com.proyecto.cafetin.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private const val PAGE_WIDTH  = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN      = 44f
    private const val CONTENT_W   = PAGE_WIDTH - MARGIN * 2

    // ── Paleta Cafetín ────────────────────────────────────────────────────────
    private val C_PRIMARY     = Color.rgb(103, 80, 164)   // violeta
    private val C_PRIMARY_DK  = Color.rgb(72,  56, 115)   // violeta oscuro (header bg)
    private val C_PRIMARY_LT  = Color.rgb(234, 228, 255)  // violeta muy claro
    private val C_RED         = Color.rgb(163, 45,  45)
    private val C_RED_LT      = Color.rgb(255, 235, 235)
    private val C_GREEN       = Color.rgb(39,  110, 10)
    private val C_GREEN_LT    = Color.rgb(230, 248, 225)
    private val C_GRAY        = Color.rgb(110, 110, 120)
    private val C_GRAY_LT     = Color.rgb(245, 244, 248)
    private val C_LINE        = Color.rgb(218, 213, 230)
    private val C_WHITE       = Color.WHITE
    private val C_BLACK       = Color.rgb(25,  22,  35)

    private val sdfFull   = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es"))
    private val sdfShort  = SimpleDateFormat("dd/MM/yyyy",             Locale("es"))
    private val sdfDia    = SimpleDateFormat("EEEE",                   Locale("es"))
    private val sdfHora   = SimpleDateFormat("h:mm a",                 Locale("es"))
    private val sdfNombre = SimpleDateFormat("yyyyMMdd",               Locale("es"))

    // ── Helpers de Paint ──────────────────────────────────────────────────────
    private fun paint(
        color: Int     = C_BLACK,
        size: Float    = 12f,
        bold: Boolean  = false,
        align: Paint.Align = Paint.Align.LEFT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color         = color
        this.textSize      = size
        this.isFakeBoldText = bold
        this.textAlign     = align
    }

    private fun linePaint(color: Int = C_LINE, width: Float = 1f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color       = color
            this.strokeWidth = width
            this.style       = Paint.Style.STROKE
        }

    private fun fillPaint(color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.FILL
        }

    // ── API pública ───────────────────────────────────────────────────────────
    fun generar(
        context: Context,
        nombrePersona: String,
        descripcionPersona: String,
        movimientos: List<Movimiento>,
        desde: Long,
        hasta: Long,
        saldoRealCentavos: Long  // Saldo acumulado TOTAL de la persona (no solo del rango)
    ): File? = runCatching {

        val movOrdenados  = movimientos.sortedBy { it.fecha }
        // El saldo real es el que viene de TODA la historia del cliente
        val saldoPendienteReal = saldoRealCentavos
        val totalPaginas  = estimarPaginas(movOrdenados.size)

        val doc = PdfDocument()
        var pageNum = 1

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page     = doc.startPage(pageInfo)
        var canvas   = page.canvas
        var y        = 0f

        // ── Nueva página ──────────────────────────────────────────────────────
        fun nuevaPagina() {
            // footer antes de cerrar
            dibujarFooter(canvas, pageNum, totalPaginas, nombrePersona, desde, hasta)
            doc.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page   = doc.startPage(pageInfo)
            canvas = page.canvas
            y      = MARGIN + 8f
            // mini-header en páginas siguientes
            dibujarMiniHeader(canvas, nombrePersona)
            y = 52f
        }

        fun checkPagina(altura: Float) {
            // reservar 48px para footer
            if (y + altura > PAGE_HEIGHT - 48f) nuevaPagina()
        }

        // ═════════════════════════════════════════════════════════════════════
        // PÁGINA 1 — HEADER PRINCIPAL
        // ═════════════════════════════════════════════════════════════════════
        y = dibujarHeaderPrincipal(
            canvas, nombrePersona, descripcionPersona, desde, hasta
        )

        // ── Cabecera de tabla ─────────────────────────────────────────────────
        y = dibujarCabeceraTabla(canvas, y)

        // ═════════════════════════════════════════════════════════════════════
        // FILAS DE MOVIMIENTOS agrupadas por día
        // ═════════════════════════════════════════════════════════════════════
        val porDia = movOrdenados.groupBy { sdfShort.format(Date(it.fecha)) }

        if (movOrdenados.isEmpty()) {
            checkPagina(30f)
            canvas.drawText(
                "No hay movimientos en este período.",
                MARGIN, y + 16f,
                paint(C_GRAY, 12f)
            )
            y += 30f
        } else {
            var filaAlterna = false

            for ((dia, movsDelDia) in porDia) {
                // ── Separador de día ──────────────────────────────────────────
                checkPagina(28f)
                val bgDia = fillPaint(C_PRIMARY_LT)
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 22f, bgDia)

                // Columna HORA: abreviatura del día + fecha corta (ej. "LUN 28/04/25")
                val fechaDia  = movsDelDia.first().fecha
                val nombreDia = sdfDia.format(Date(fechaDia)).take(3).uppercase(Locale("es"))
                val diaCorto  = dia.let { it.substring(0,5) + "/" + it.substring(8) } // "28/04/25"
                canvas.drawText(
                    "$nombreDia $diaCorto",
                    MARGIN + 6f, y + 15f,
                    paint(C_PRIMARY, 9.5f, bold = true)
                )

                // Columna DESCRIPCIÓN: total fiado/cobrado del día (sin "S/" doble, centavosAtexto ya lo incluye)
                val fiadoDelDia   = movsDelDia.filter { it.tipo == TipoMovimiento.FIADO }.sumOf { it.monto }
                val cobradoDelDia = movsDelDia.filter { it.tipo == TipoMovimiento.PAGO  }.sumOf { it.monto }
                val resumenDia = buildString {
                    if (fiadoDelDia > 0)   append("Fiado: ${fiadoDelDia.centavosAtexto()}")
                    if (fiadoDelDia > 0 && cobradoDelDia > 0) append("   ")
                    if (cobradoDelDia > 0) append("Cobrado: ${cobradoDelDia.centavosAtexto()}")
                }
                canvas.drawText(
                    resumenDia,
                    MARGIN + 72f, y + 15f,
                    paint(C_PRIMARY, 9f)
                )

                y += 22f

                for (mov in movsDelDia) {
                    checkPagina(24f)

                    val esPago     = mov.tipo == TipoMovimiento.PAGO
                    val hora       = sdfHora.format(Date(mov.fecha))
                    val nota       = mov.nota.ifBlank { if (esPago) "Pago" else "Fiado" }
                    val montoStr   = mov.monto.centavosAtexto()
                    val prefijo    = if (esPago) "+S/ " else "−S/ "
                    val colorMonto = if (esPago) C_GREEN else C_RED
                    val colorTipo  = if (esPago) C_GREEN else C_RED
                    val bgTipo     = if (esPago) C_GREEN_LT else C_RED_LT
                    val tipoStr    = if (esPago) "PAGO" else "FIADO"

                    // fondo alterno
                    if (filaAlterna) {
                        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 24f, fillPaint(C_GRAY_LT))
                    }
                    filaAlterna = !filaAlterna

                    val yCentro = y + 16f

                    // hora
                    canvas.drawText(hora, MARGIN + 6f, yCentro, paint(C_GRAY, 10f))

                    // nota (descripción) — truncar si es muy larga
                    val notaTrunc = if (nota.length > 32) nota.take(29) + "…" else nota
                    canvas.drawText(notaTrunc, MARGIN + 72f, yCentro, paint(C_BLACK, 11f))

                    // chip tipo
                    val chipX = MARGIN + CONTENT_W - 180f
                    val chipRect = RectF(chipX, y + 4f, chipX + 48f, y + 20f)
                    canvas.drawRoundRect(chipRect, 6f, 6f, fillPaint(bgTipo))
                    canvas.drawText(
                        tipoStr,
                        chipX + 24f, y + 15f,
                        paint(colorTipo, 8f, bold = true, align = Paint.Align.CENTER)
                    )

                    // monto alineado derecha
                    canvas.drawText(
                        prefijo + montoStr,
                        MARGIN + CONTENT_W - 4f, yCentro,
                        paint(colorMonto, 11f, bold = true, align = Paint.Align.RIGHT)
                    )

                    y += 24f
                }
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        // TOTALES FINALES
        // ═════════════════════════════════════════════════════════════════════
        checkPagina(110f)
        y += 10f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, linePaint(C_LINE, 1f))
        y += 16f

        // ── Caja de saldo pendiente (protagonista) ────────────────────────────
        y += 6f
        val saldoPositivo = saldoPendienteReal > 0L
        val bgSaldo   = if (saldoPositivo) C_RED_LT   else C_GREEN_LT
        val fgSaldo   = if (saldoPositivo) C_RED      else C_GREEN
        val labelSaldo= if (saldoPositivo) "SALDO PENDIENTE TOTAL" else "CUENTA AL DÍA ✓"

        val cajaH = 54f
        val cajaRect = RectF(MARGIN, y, MARGIN + CONTENT_W, y + cajaH)
        canvas.drawRoundRect(cajaRect, 10f, 10f, fillPaint(bgSaldo))
        canvas.drawRoundRect(cajaRect, 10f, 10f, linePaint(fgSaldo, 1.5f))

        // barra lateral de acento
        val accentRect = RectF(MARGIN, y, MARGIN + 5f, y + cajaH)
        canvas.drawRoundRect(accentRect, 4f, 4f, fillPaint(fgSaldo))

        canvas.drawText(
            labelSaldo,
            MARGIN + 16f, y + 22f,
            paint(fgSaldo, 11f, bold = true)
        )
        canvas.drawText(
            "(incluye deuda de períodos anteriores)",
            MARGIN + 16f, y + 38f,
            paint(fgSaldo, 8f)
        )
        canvas.drawText(
            saldoPendienteReal.centavosAtexto(),
            MARGIN + CONTENT_W - 10f, y + 35f,
            paint(fgSaldo, 22f, bold = true, align = Paint.Align.RIGHT)
        )
        y += cajaH + 16f

        // ── Footer última página ──────────────────────────────────────────────
        dibujarFooter(canvas, pageNum, totalPaginas, nombrePersona, desde, hasta)
        doc.finishPage(page)

        // ── Guardar ───────────────────────────────────────────────────────────
        val nombreLimpio = nombrePersona.replace("\\s+".toRegex(), "_")
        val dir  = File(context.filesDir, "reportes").also { it.mkdirs() }
        val file = File(dir, "Reporte_${nombreLimpio}_${sdfNombre.format(Date())}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        file
    }.getOrNull()

    // ── Header principal (solo página 1) ──────────────────────────────────────
    private fun dibujarHeaderPrincipal(
        canvas: android.graphics.Canvas,
        nombre: String,
        descripcion: String,
        desde: Long,
        hasta: Long
    ): Float {
        val headerH = if (descripcion.isNotBlank()) 108f else 92f

        // Fondo violeta
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerH, fillPaint(C_PRIMARY_DK))

        // Línea de acento en la base del header
        canvas.drawRect(0f, headerH, PAGE_WIDTH.toFloat(), headerH + 3f, fillPaint(C_PRIMARY))

        // "Cafetín" — marca
        canvas.drawText(
            "Cafetín",
            MARGIN, 32f,
            paint(C_WHITE, 13f, bold = true)
        )

        // Tipo de documento alineado a la derecha
        canvas.drawText(
            "REPORTE DE CUENTA",
            PAGE_WIDTH - MARGIN, 32f,
            paint(Color.argb(180, 255, 255, 255), 9f, align = Paint.Align.RIGHT)
        )

        // Nombre del cliente
        canvas.drawText(
            nombre,
            MARGIN, 60f,
            paint(C_WHITE, 20f, bold = true)
        )

        var yH = 78f
        if (descripcion.isNotBlank()) {
            canvas.drawText(
                descripcion,
                MARGIN, yH,
                paint(Color.argb(200, 255, 255, 255), 11f)
            )
            yH += 16f
        }

        // Período y fecha generación
        val periodo = "${sdfShort.format(Date(desde))}  →  ${sdfShort.format(Date(hasta))}"
        canvas.drawText(
            "Período: $periodo",
            MARGIN, yH,
            paint(Color.argb(200, 255, 255, 255), 10f)
        )
        canvas.drawText(
            "Generado: ${sdfFull.format(Date())}",
            PAGE_WIDTH - MARGIN, yH,
            paint(Color.argb(160, 255, 255, 255), 9f, align = Paint.Align.RIGHT)
        )

        return headerH + 20f
    }

    // ── Mini-header para páginas 2+ ───────────────────────────────────────────
    private fun dibujarMiniHeader(canvas: android.graphics.Canvas, nombre: String) {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 32f, fillPaint(C_PRIMARY_DK))
        canvas.drawRect(0f, 32f, PAGE_WIDTH.toFloat(), 34f, fillPaint(C_PRIMARY))
        canvas.drawText("Cafetín", MARGIN, 22f, paint(C_WHITE, 10f, bold = true))
        canvas.drawText(
            "Reporte — $nombre",
            PAGE_WIDTH - MARGIN, 22f,
            paint(Color.argb(180, 255, 255, 255), 9f, align = Paint.Align.RIGHT)
        )
    }

    // ── Cabecera de tabla ─────────────────────────────────────────────────────
    private fun dibujarCabeceraTabla(canvas: android.graphics.Canvas, y: Float): Float {
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 22f, fillPaint(C_PRIMARY))
        canvas.drawText("Hora",        MARGIN + 6f,                    y + 15f, paint(C_WHITE, 9f, bold = true))
        canvas.drawText("Descripción", MARGIN + 72f,                   y + 15f, paint(C_WHITE, 9f, bold = true))
        canvas.drawText("Tipo",        MARGIN + CONTENT_W - 168f,      y + 15f, paint(C_WHITE, 9f, bold = true))
        canvas.drawText("Monto",       MARGIN + CONTENT_W - 4f,        y + 15f, paint(C_WHITE, 9f, bold = true, align = Paint.Align.RIGHT))
        return y + 22f
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private fun dibujarFooter(
        canvas: android.graphics.Canvas,
        pagina: Int,
        total: Int,
        nombre: String,
        desde: Long,
        hasta: Long
    ) {
        val yF = PAGE_HEIGHT - 28f
        canvas.drawLine(MARGIN, yF - 6f, MARGIN + CONTENT_W, yF - 6f, linePaint(C_LINE))
        canvas.drawText(
            "Cafetín  •  $nombre  •  ${sdfShort.format(Date(desde))} – ${sdfShort.format(Date(hasta))}",
            MARGIN, yF + 8f,
            paint(C_GRAY, 8f)
        )
        canvas.drawText(
            "Página $pagina de $total",
            MARGIN + CONTENT_W, yF + 8f,
            paint(C_GRAY, 8f, align = Paint.Align.RIGHT)
        )
    }

    // Estimación simple de páginas para el footer
    private fun estimarPaginas(totalMovimientos: Int): Int {
        val filasPorPagina = 22
        return maxOf(1, kotlin.math.ceil(totalMovimientos.toDouble() / filasPorPagina).toInt())
    }

    fun uriParaCompartir(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}