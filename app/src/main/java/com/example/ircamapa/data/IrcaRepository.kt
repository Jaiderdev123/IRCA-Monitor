package com.example.ircamapa.data

import android.content.Context
import com.example.ircamapa.model.PuntoIrca
import org.json.JSONArray
import org.json.JSONObject

class IrcaRepository(context: Context) {
    private val prefs = context.getSharedPreferences("irca_puntos_prefs", Context.MODE_PRIVATE)
    private val keyPuntos = "lista_puntos_irca"

    fun obtenerPuntos(): List<PuntoIrca> {
        val jsonString = prefs.getString(keyPuntos, null)
        if (jsonString.isNullOrEmpty()) {
            val iniciales = puntosEjemplo()
            guardarPuntos(iniciales)
            return iniciales
        }

        val lista = mutableListOf<PuntoIrca>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            lista.add(
                PuntoIrca(
                    id = obj.getString("id"),
                    nombre = obj.getString("nombre"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    valorIrca = obj.getDouble("valorIrca").toFloat(),
                    fecha = obj.optLong("fecha", System.currentTimeMillis())
                )
            )
        }
        return lista
    }

    fun guardarPuntos(puntos: List<PuntoIrca>) {
        val jsonArray = JSONArray()
        puntos.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("nombre", p.nombre)
                put("lat", p.lat)
                put("lng", p.lng)
                put("valorIrca", p.valorIrca)
                put("fecha", p.fecha)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(keyPuntos, jsonArray.toString()).apply()
    }

    fun agregarPunto(punto: PuntoIrca) {
        val actuales = obtenerPuntos().toMutableList()
        actuales.add(0, punto)
        guardarPuntos(actuales)
    }

    fun actualizarPunto(punto: PuntoIrca) {
        val actuales = obtenerPuntos().map { if (it.id == punto.id) punto else it }
        guardarPuntos(actuales)
    }

    fun eliminarPunto(id: String) {
        val actuales = obtenerPuntos().filterNot { it.id == id }
        guardarPuntos(actuales)
    }

    private fun puntosEjemplo(): List<PuntoIrca> {
        // Puntos de muestra iniciales en Popayán / Cauca
        return listOf(
            PuntoIrca(
                nombre = "Planta de Tratamiento Central",
                lat = 2.4442,
                lng = -76.6058,
                valorIrca = 2.5f
            ),
            PuntoIrca(
                nombre = "Red Comuna 2 - Grifo Norte",
                lat = 2.4530,
                lng = -76.5980,
                valorIrca = 11.2f
            ),
            PuntoIrca(
                nombre = "Sector Rural - Quebrada El Molino",
                lat = 2.4350,
                lng = -76.6180,
                valorIrca = 48.0f
            ),
            PuntoIrca(
                nombre = "Toma Industrial Río Cauca",
                lat = 2.4270,
                lng = -76.6250,
                valorIrca = 88.5f
            )
        )
    }
}
