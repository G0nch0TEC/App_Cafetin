package com.proyecto.cafetin.domain.usecase

import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.NotaUtils

/**
 * Acumula un producto en el historial de hoy para una persona:
 *  - Si ya existe un FIADO con la misma [notaBase], hace UPDATE incrementando cantidad y monto.
 *  - Si no existe, hace INSERT con "Nombre x1".
 *
 * Retorna el mensaje listo para mostrar en el Snackbar, evitando que el ViewModel
 * repita la lógica de cálculo de texto.
 */
class AcumularProductoUseCase(private val repo: ICafetinRepository) {

    suspend operator fun invoke(
        personaId: Int,
        movimientosHoy: List<Movimiento>,
        notaBase: String,
        precioCentavos: Long
    ): String {
        val existente = movimientosHoy.firstOrNull { mov ->
            mov.tipo == TipoMovimiento.FIADO &&
                    NotaUtils.notaBase(mov.nota) == notaBase
        }

        return if (existente != null) {
            val nuevaCantidad = NotaUtils.cantidadDeNota(existente.nota) + 1
            val nuevoMonto    = precioCentavos * nuevaCantidad
            val nuevaNota     = "$notaBase x$nuevaCantidad"

            repo.editarMovimiento(
                existente.copy(monto = nuevoMonto, nota = nuevaNota)
            )
            "$nuevaNota — ${nuevoMonto.centavosAtexto()}"
        } else {
            repo.registrarFiado(personaId, precioCentavos, "$notaBase x1")
            "$notaBase x1 — ${precioCentavos.centavosAtexto()}"
        }
    }
}
