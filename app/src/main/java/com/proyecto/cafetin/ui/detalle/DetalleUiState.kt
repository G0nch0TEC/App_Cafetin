package com.proyecto.cafetin.ui.detalle

/**
 * Eventos de un solo disparo emitidos por DetalleViewModel hacia la UI.
 * Se consumen via Channel para garantizar que no se repitan al recomponerse.
 */
sealed class DetalleEvent {
    data class CompartirPdf(val uri: android.net.Uri) : DetalleEvent()
}

/**
 * Estado del diálogo de exportación a PDF.
 */
data class ExportState(
    val mostrando: Boolean = false,
    val generando: Boolean = false,
    val error: String?     = null
)
