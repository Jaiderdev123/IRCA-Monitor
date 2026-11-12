package com.example.ircamapa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ircamapa.data.IrcaRepository
import com.example.ircamapa.model.PuntoIrca
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IrcaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IrcaRepository(application)

    private val _puntos = MutableStateFlow<List<PuntoIrca>>(emptyList())
    val puntos: StateFlow<List<PuntoIrca>> = _puntos.asStateFlow()

    private val _puntoSeleccionado = MutableStateFlow<PuntoIrca?>(null)
    val puntoSeleccionado: StateFlow<PuntoIrca?> = _puntoSeleccionado.asStateFlow()

    private val _coordenadaRegistro = MutableStateFlow<LatLng?>(null)
    val coordenadaRegistro: StateFlow<LatLng?> = _coordenadaRegistro.asStateFlow()

    private val _nombreSugeridoRegistro = MutableStateFlow("")
    val nombreSugeridoRegistro: StateFlow<String> = _nombreSugeridoRegistro.asStateFlow()

    private val _puntoParaEditar = MutableStateFlow<PuntoIrca?>(null)
    val puntoParaEditar: StateFlow<PuntoIrca?> = _puntoParaEditar.asStateFlow()

    private val _mostrarLeyenda = MutableStateFlow(false)
    val mostrarLeyenda: StateFlow<Boolean> = _mostrarLeyenda.asStateFlow()

    init {
        cargarPuntos()
    }

    private fun cargarPuntos() {
        _puntos.value = repository.obtenerPuntos()
    }

    fun seleccionarPunto(punto: PuntoIrca?) {
        _puntoSeleccionado.value = punto
    }

    fun iniciarRegistro(coordenada: LatLng, nombreSugerido: String = "") {
        _coordenadaRegistro.value = coordenada
        _nombreSugeridoRegistro.value = nombreSugerido
        _puntoSeleccionado.value = null
    }

    fun cancelarRegistro() {
        _coordenadaRegistro.value = null
        _nombreSugeridoRegistro.value = ""
    }

    fun iniciarEdicion(punto: PuntoIrca) {
        _puntoParaEditar.value = punto
    }

    fun cancelarEdicion() {
        _puntoParaEditar.value = null
    }

    fun guardarEdicion(nombre: String, valorIrca: Float) {
        val actual = _puntoParaEditar.value ?: return
        val actualizado = actual.copy(
            nombre = nombre.ifBlank { actual.nombre },
            valorIrca = valorIrca.coerceIn(0f, 100f)
        )
        repository.actualizarPunto(actualizado)
        _puntoParaEditar.value = null
        cargarPuntos()
        _puntoSeleccionado.value = actualizado
    }

    fun toggleLeyenda() {
        _mostrarLeyenda.value = !_mostrarLeyenda.value
    }

    fun registrarPunto(nombre: String, valorIrca: Float) {
        val coord = _coordenadaRegistro.value ?: return
        val nuevo = PuntoIrca(
            nombre = nombre.ifBlank { "Punto de muestreo" },
            lat = coord.latitude,
            lng = coord.longitude,
            valorIrca = valorIrca.coerceIn(0f, 100f)
        )
        repository.agregarPunto(nuevo)
        _coordenadaRegistro.value = null
        _nombreSugeridoRegistro.value = ""
        cargarPuntos()
        _puntoSeleccionado.value = nuevo
    }

    fun eliminarPunto(id: String) {
        repository.eliminarPunto(id)
        if (_puntoSeleccionado.value?.id == id) {
            _puntoSeleccionado.value = null
        }
        cargarPuntos()
    }
}
