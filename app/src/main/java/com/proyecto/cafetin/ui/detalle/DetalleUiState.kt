package com.proyecto.cafetin.ui.detalle

/**
 * Eventos de un solo disparo emitidos por DetalleViewModel hacia la UI.
 * Se consumen via Channel para garantizar que no se repitan al recomponerse.
 */
sealed class DetalleEvent {
    /**
     * El PDF ya está generado. La UI debe lanzar el share sheet y, si el usuario
     * completa el envío, llamar a [DetalleViewModel.confirmarEnviado].
     */
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
