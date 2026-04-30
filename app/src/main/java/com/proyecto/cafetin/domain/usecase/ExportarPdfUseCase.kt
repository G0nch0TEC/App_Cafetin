package com.proyecto.cafetin.domain.usecase

import android.content.Context
import android.net.Uri
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.util.DateUtils.finDeDia
import com.proyecto.cafetin.util.PdfExporter

/**
 * Encapsula la lógica de exportación a PDF para una persona en un rango de fechas:
 *  1. Obtiene los movimientos del repositorio.
 *  2. Genera el archivo PDF via [PdfExporter].
 *  3. Marca la persona como enviada hasta el fin del día actual.
 *  4. Devuelve el [Uri] listo para compartir, o un [Result] con el error concreto.
 */
class ExportarPdfUseCase(
    private val repo: ICafetinRepository,
    private val appContext: Context
) {
    sealed class Resultado {
        data class Exito(val uri: Uri) : Resultado()
        data object SinMovimientos : Resultado()
        data object ErrorAlGenerar : Resultado()
    }

    suspend operator fun invoke(
        persona: Persona,
        desdeMs: Long,
        hastaMs: Long
    ): Resultado {
        val movs = repo.movimientosPorPersonaEnRango(persona.id, desdeMs, hastaMs)

        if (movs.isEmpty()) return Resultado.SinMovimientos

        val file = PdfExporter.generar(
            context            = appContext,
            nombrePersona      = persona.nombre,
            descripcionPersona = persona.descripcion,
            movimientos        = movs,
            desde              = desdeMs,
            hasta              = hastaMs
        ) ?: return Resultado.ErrorAlGenerar

        repo.marcarEnviado(persona.id, finDeDia(System.currentTimeMillis()))

        return Resultado.Exito(PdfExporter.uriParaCompartir(appContext, file))
    }
}
