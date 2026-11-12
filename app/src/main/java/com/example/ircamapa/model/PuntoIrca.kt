package com.example.ircamapa.model

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class NivelRiesgo(
    val etiqueta: String,
    val rango: String,
    val hue: Float,
    val colorHex: Long
) {
    SIN_RIESGO("Sin Riesgo", "0 - 5%", BitmapDescriptorFactory.HUE_GREEN, 0xFF2E7D32),
    BAJO("Riesgo Bajo", "5.1 - 14%", BitmapDescriptorFactory.HUE_YELLOW, 0xFFF9A825),
    MEDIO("Riesgo Medio", "14.1 - 35%", BitmapDescriptorFactory.HUE_ORANGE, 0xFFEF6C00),
    ALTO("Riesgo Alto", "35.1 - 80%", BitmapDescriptorFactory.HUE_RED, 0xFFC62828),
    INVIABLE("Inviable Sanitariamente", "80.1 - 100%", BitmapDescriptorFactory.HUE_VIOLET, 0xFF6A1B9A)
}

data class PuntoIrca(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val valorIrca: Float,
    val fecha: Long = System.currentTimeMillis()
) {
    val nivelRiesgo: NivelRiesgo
        get() = when {
            valorIrca <= 5f -> NivelRiesgo.SIN_RIESGO
            valorIrca <= 14f -> NivelRiesgo.BAJO
            valorIrca <= 35f -> NivelRiesgo.MEDIO
            valorIrca <= 80f -> NivelRiesgo.ALTO
            else -> NivelRiesgo.INVIABLE
        }

    val fechaFormateada: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(fecha))
        }
}
