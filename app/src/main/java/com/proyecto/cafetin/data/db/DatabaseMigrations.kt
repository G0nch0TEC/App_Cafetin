package com.proyecto.cafetin.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.proyecto.cafetin.data.catalog.ProductosCatalogo

internal object DatabaseMigrations {

    /** Migración 1→2: agrega enviadoHasta a personas */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE personas ADD COLUMN enviadoHasta INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * Migración 2→3: crea tablas del catálogo y las pre-puebla
     * con los datos que antes estaban hardcodeados en ProductosCatalogo.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Crear tabla de categorías
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS catalogo_categorias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nombre TEXT NOT NULL,
                    emoji TEXT NOT NULL,
                    orden INTEGER NOT NULL DEFAULT 0
                )"""
            )
            // Crear tabla de productos
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS catalogo_productos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    categoriaId INTEGER NOT NULL,
                    nombre TEXT NOT NULL,
                    montoCentavos INTEGER NOT NULL,
                    orden INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(categoriaId) REFERENCES catalogo_categorias(id) ON DELETE CASCADE
                )"""
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_catalogo_productos_categoriaId ON catalogo_productos(categoriaId)")

            // Pre-poblar con datos del catálogo estático
            ProductosCatalogo.categorias.forEachIndexed { catIdx, cat ->
                db.execSQL(
                    "INSERT INTO catalogo_categorias (nombre, emoji, orden) VALUES (?, ?, ?)",
                    arrayOf(cat.nombre, cat.emoji, catIdx)
                )
                // Obtener el ID de la categoría recién insertada
                val cursor = db.query("SELECT last_insert_rowid()")
                cursor.moveToFirst()
                val catId = cursor.getLong(0)
                cursor.close()

                cat.productos.forEachIndexed { prodIdx, prod ->
                    db.execSQL(
                        "INSERT INTO catalogo_productos (categoriaId, nombre, montoCentavos, orden) VALUES (?, ?, ?, ?)",
                        arrayOf(catId, prod.nombre, prod.montoCentavos, prodIdx)
                    )
                }
            }
        }
    }
}
