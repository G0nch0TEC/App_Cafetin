package com.proyecto.cafetin.repository

/**
 * Interfaz unificada del repositorio.
 *
 * Hereda de las tres interfaces especializadas para mantener compatibilidad
 * con los ViewModels actuales que reciben ICafetinRepository directamente.
 *
 * En el futuro, cada ViewModel puede recibir solo la interfaz que necesita:
 *   - PersonasViewModel  → IPersonaRepository + IMovimientoRepository
 *   - HistorialViewModel → IPersonaRepository + IMovimientoRepository
 *   - CatalogoViewModel  → ICatalogoRepository
 *   - DetalleViewModel   → las tres
 */
interface ICafetinRepository : IPersonaRepository, IMovimientoRepository, ICatalogoRepository
