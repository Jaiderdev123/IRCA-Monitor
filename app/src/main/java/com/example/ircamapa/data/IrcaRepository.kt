package com.example.ircamapa.data

import android.content.Context
import com.example.ircamapa.model.PuntoIrca
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositorio encargado de gestionar la persistencia local de los puntos de monitoreo IRCA
 * (Índice de Riesgo de la Calidad del Agua) utilizando SharedPreferences en formato JSON.

 */
class IrcaRepository(context: Context) {

    /** Instancia de SharedPreferences donde se almacenan los datos de los puntos IRCA. */
    private val prefs = context.getSharedPreferences("irca_puntos_prefs", Context.MODE_PRIVATE)

    /** Clave usada para guardar y recuperar la lista JSON de puntos IRCA. */
    private val keyPuntos = "lista_puntos_irca"

    /**
     * Obtiene la lista completa de puntos de monitoreo IRCA guardados localmente.
     * Si no existe ninguna lista previa en SharedPreferences, genera una lista de ejemplo inicial,
     * la guarda de forma permanente y la retorna.
        */
    fun obtenerPuntos(): List<PuntoIrca> {
        val jsonString = prefs.getString(keyPuntos, null)
        // Si no hay datos guardados, inicializamos con los puntos de ejemplo predeterminados
        if (jsonString.isNullOrEmpty()) {
            val iniciales = puntosEjemplo()
            guardarPuntos(iniciales)
            return iniciales
        }

        // Deserialización del String JSON a una lista de objetos PuntoIrca
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

    /**
     * Serializa y guarda la lista dada de puntos IRCA en SharedPreferences.
     */
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

    /**
     * Agrega un nuevo punto de monitoreo IRCA al inicio de la lista y persiste los cambios.
        */
    fun agregarPunto(punto: PuntoIrca) {
        val actuales = obtenerPuntos().toMutableList()
        actuales.add(0, punto) // Inserta al inicio para que aparezca primero
        guardarPuntos(actuales)
    }

    /**
     * Actualiza la información de un punto de monitoreo IRCA existente.
     * Busca el punto por su identificador único [PuntoIrca.id] y reemplaza sus datos.
         */
    fun actualizarPunto(punto: PuntoIrca) {
        val actuales = obtenerPuntos().map { if (it.id == punto.id) punto else it }
        guardarPuntos(actuales)
    }

    /**
     * Elimina un punto de monitoreo IRCA de la lista persistida según su ID.
        */
    fun eliminarPunto(id: String) {
        val actuales = obtenerPuntos().filterNot { it.id == id }
        guardarPuntos(actuales)
    }

    /**
     * Proporciona la lista inicial de puntos de monitoreo de prueba ubicados en Popayán / Cauca.
     * Se usa únicamente la primera vez que se ejecuta la aplicación cuando no existen datos previos.
     */
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
