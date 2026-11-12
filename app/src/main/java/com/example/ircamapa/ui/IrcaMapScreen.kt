package com.example.ircamapa.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ircamapa.model.NivelRiesgo
import com.example.ircamapa.model.PuntoIrca
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class LugarSeleccionado(
    val nombre: String,
    val latLng: LatLng,
    val direccion: String = ""
)

data class SugerenciaLugar(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val latLng: LatLng,
    val esPuntoRegistrado: Boolean = false,
    val puntoRegistrado: PuntoIrca? = null,
    val distanciaMetros: Float? = null
)

private fun formatearDistancia(distMetros: Float?): String {
    if (distMetros == null) return ""
    return if (distMetros < 1000f) {
        "${distMetros.toInt()} m"
    } else {
        String.format(Locale.getDefault(), "%.1f km", distMetros / 1000f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrcaMapScreen(viewModel: IrcaViewModel) {
    val puntos by viewModel.puntos.collectAsState()
    val puntoSeleccionado by viewModel.puntoSeleccionado.collectAsState()
    val coordenadaRegistro by viewModel.coordenadaRegistro.collectAsState()
    val nombreSugeridoRegistro by viewModel.nombreSugeridoRegistro.collectAsState()
    val puntoParaEditar by viewModel.puntoParaEditar.collectAsState()
    val mostrarLeyenda by viewModel.mostrarLeyenda.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var textoBusqueda by remember { mutableStateOf("") }
    var sugerencias by remember { mutableStateOf<List<SugerenciaLugar>>(emptyList()) }
    var lugarSeleccionado by remember { mutableStateOf<LugarSeleccionado?>(null) }
    var ubicacionUsuario by remember { mutableStateOf<LatLng?>(null) }

    var tienePermisoUbicacion by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(tienePermisoUbicacion) {
        if (tienePermisoUbicacion) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        ubicacionUsuario = LatLng(loc.latitude, loc.longitude)
                    }
                }
            } catch (_: SecurityException) {}
        }
    }

    // Posición inicial centrada en el primer punto o en coordenadas predeterminadas
    val puntoInicial = puntos.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        ?: LatLng(2.4442, -76.6058)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(puntoInicial, 13.5f)
    }

    val centrarEnGps: () -> Unit = {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        ubicacionUsuario = LatLng(loc.latitude, loc.longitude)
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(loc.latitude, loc.longitude),
                                    15.5f
                                )
                            )
                        }
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            lastLoc?.let {
                                ubicacionUsuario = LatLng(it.latitude, it.longitude)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(it.latitude, it.longitude),
                                            15.5f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        } catch (_: SecurityException) {
            // Permiso no concedido
        }
    }

    val launcherPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val fine = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            tienePermisoUbicacion = true
            centrarEnGps()
        }
    }

    val seleccionarSugerencia: (SugerenciaLugar) -> Unit = { sugerencia ->
        textoBusqueda = sugerencia.titulo
        sugerencias = emptyList()
        focusManager.clearFocus()
        coroutineScope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(sugerencia.latLng, 16.5f)
            )
        }
        if (sugerencia.esPuntoRegistrado && sugerencia.puntoRegistrado != null) {
            lugarSeleccionado = null
            viewModel.seleccionarPunto(sugerencia.puntoRegistrado)
        } else {
            viewModel.seleccionarPunto(null)
            lugarSeleccionado = LugarSeleccionado(
                nombre = sugerencia.titulo,
                latLng = sugerencia.latLng,
                direccion = sugerencia.subtitulo
            )
        }
    }

    // Búsqueda de lugares en la ciudad
    val ejecutarBusqueda: (String) -> Unit = { query ->
        val q = query.trim()
        if (q.isNotEmpty()) {
            val primerMatch = sugerencias.firstOrNull()
            if (primerMatch != null) {
                seleccionarSugerencia(primerMatch)
            } else {
                val puntoExistente = puntos.find { it.nombre.contains(q, ignoreCase = true) }
                if (puntoExistente != null) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(puntoExistente.lat, puntoExistente.lng),
                                16f
                            )
                        )
                        lugarSeleccionado = null
                        viewModel.seleccionarPunto(puntoExistente)
                    }
                } else {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            @Suppress("DEPRECATION")
                            val resultados = geocoder.getFromLocationName(q, 1)
                            val lugar = resultados?.firstOrNull()
                            if (lugar != null) {
                                val coord = LatLng(lugar.latitude, lugar.longitude)
                                val titulo = lugar.featureName ?: q
                                val direccion = lugar.getAddressLine(0) ?: ""
                                withContext(Dispatchers.Main) {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(coord, 16f)
                                    )
                                    lugarSeleccionado = LugarSeleccionado(titulo, coord, direccion)
                                    viewModel.seleccionarPunto(null)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "No se encontró el lugar '$q'", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    // Geocodificación inversa para auto-rellenar el nombre del lugar
    val obtenerNombreLugar: (LatLng, (nombre: String, direccion: String) -> Unit) -> Unit = { coord, onResult ->
        coroutineScope.launch(Dispatchers.IO) {
            var nombre = ""
            var direccion = ""
            try {
                @Suppress("DEPRECATION")
                val addrs = geocoder.getFromLocation(coord.latitude, coord.longitude, 1)
                addrs?.firstOrNull()?.let { addr ->
                    val feature = addr.featureName?.takeIf { it.isNotBlank() && !it.all { c -> c.isDigit() } }
                    val premise = addr.premises
                    val calle = addr.thoroughfare?.let { t ->
                        val num = addr.subThoroughfare
                        if (num != null) "$t #$num" else t
                    }
                    val barrio = addr.subLocality
                    val localidad = addr.locality
                    nombre = listOfNotNull(feature, premise, calle, barrio, localidad).firstOrNull()
                        ?: addr.getAddressLine(0)?.split(",")?.firstOrNull()?.trim()
                        ?: ""
                    direccion = addr.getAddressLine(0) ?: listOfNotNull(barrio, localidad).joinToString(", ")
                }
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                onResult(nombre, direccion)
            }
        }
    }

    // Autocompletar sugerencias cercanas mientras el usuario escribe
    LaunchedEffect(textoBusqueda) {
        val q = textoBusqueda.trim()
        if (q.length < 2) {
            sugerencias = emptyList()
            return@LaunchedEffect
        }
        delay(280)

        withContext(Dispatchers.IO) {
            val centro = ubicacionUsuario ?: cameraPositionState.position.target
            val delta = 0.12 // ~13 km alrededor de la ubicación

            val puntosLocales = puntos
                .filter { it.nombre.contains(q, ignoreCase = true) }
                .map { punto ->
                    val distArray = FloatArray(1)
                    Location.distanceBetween(centro.latitude, centro.longitude, punto.lat, punto.lng, distArray)
                    SugerenciaLugar(
                        id = "punto_${punto.id}",
                        titulo = punto.nombre,
                        subtitulo = "Punto registrado • IRCA: ${punto.valorIrca}% (${punto.nivelRiesgo.etiqueta})",
                        latLng = LatLng(punto.lat, punto.lng),
                        esPuntoRegistrado = true,
                        puntoRegistrado = punto,
                        distanciaMetros = distArray[0]
                    )
                }

            val lowerLeftLat = centro.latitude - delta
            val lowerLeftLng = centro.longitude - delta
            val upperRightLat = centro.latitude + delta
            val upperRightLng = centro.longitude + delta

            @Suppress("DEPRECATION")
            val resultadosCercanos = try {
                geocoder.getFromLocationName(q, 6, lowerLeftLat, lowerLeftLng, upperRightLat, upperRightLng)
            } catch (_: Exception) {
                null
            }

            @Suppress("DEPRECATION")
            val resultadosGenerales = if ((resultadosCercanos?.size ?: 0) < 3) {
                try {
                    geocoder.getFromLocationName(q, 5)
                } catch (_: Exception) {
                    null
                }
            } else null

            val listaDirecciones = (resultadosCercanos ?: emptyList()) +
                (resultadosGenerales ?: emptyList()).filter { gen ->
                    resultadosCercanos?.none { cerc ->
                        Math.abs(cerc.latitude - gen.latitude) < 0.0001 &&
                        Math.abs(cerc.longitude - gen.longitude) < 0.0001
                    } ?: true
                }

            val sugerenciasGeo = listaDirecciones.mapNotNull { addr ->
                val titulo = addr.featureName?.takeIf { it.isNotBlank() && !it.all { c -> c.isDigit() } }
                    ?: addr.thoroughfare
                    ?: addr.getAddressLine(0)?.split(",")?.firstOrNull()?.trim()
                    ?: return@mapNotNull null

                val partesSub = listOfNotNull(
                    addr.subLocality,
                    addr.locality,
                    addr.adminArea
                ).filter { it.isNotBlank() && !it.equals(titulo, ignoreCase = true) }

                val subtitulo = if (partesSub.isNotEmpty()) partesSub.joinToString(", ")
                                else addr.getAddressLine(0) ?: ""

                val distArray = FloatArray(1)
                Location.distanceBetween(centro.latitude, centro.longitude, addr.latitude, addr.longitude, distArray)

                SugerenciaLugar(
                    id = "geo_${addr.latitude}_${addr.longitude}",
                    titulo = titulo,
                    subtitulo = subtitulo,
                    latLng = LatLng(addr.latitude, addr.longitude),
                    esPuntoRegistrado = false,
                    puntoRegistrado = null,
                    distanciaMetros = distArray[0]
                )
            }

            val listaFinal = (puntosLocales + sugerenciasGeo)
                .distinctBy { "${String.format(Locale.US, "%.4f", it.latLng.latitude)}_${String.format(Locale.US, "%.4f", it.latLng.longitude)}" }
                .sortedBy { it.distanciaMetros ?: Float.MAX_VALUE }
                .take(6)

            withContext(Dispatchers.Main) {
                sugerencias = listaFinal
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monitor IRCA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${puntos.size} puntos registrados",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLeyenda() }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Ver rangos IRCA"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Botón GPS: centrar en ubicación actual
                FloatingActionButton(
                    onClick = {
                        if (tienePermisoUbicacion) {
                            centrarEnGps()
                        } else {
                            launcherPermisos.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Mi ubicación GPS"
                    )
                }

                // Botón registrar en el lugar seleccionado o en el centro actual del mapa
                FloatingActionButton(
                    onClick = {
                        val lugar = lugarSeleccionado
                        if (lugar != null) {
                            viewModel.iniciarRegistro(lugar.latLng, lugar.nombre)
                        } else {
                            val centro = cameraPositionState.position.target
                            obtenerNombreLugar(centro) { nombreDetectado, _ ->
                                viewModel.iniciarRegistro(centro, nombreDetectado)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Registrar punto"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = tienePermisoUbicacion),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapClick = { latLng ->
                    viewModel.seleccionarPunto(null)
                    obtenerNombreLugar(latLng) { nombreDetectado, direccionDetectada ->
                        val nombre = nombreDetectado.ifBlank { "Punto en el mapa" }
                        lugarSeleccionado = LugarSeleccionado(nombre, latLng, direccionDetectada)
                    }
                },
                onPOIClick = { poi ->
                    viewModel.seleccionarPunto(null)
                    lugarSeleccionado = LugarSeleccionado(
                        nombre = poi.name,
                        latLng = poi.latLng,
                        direccion = ""
                    )
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLng(poi.latLng))
                    }
                    obtenerNombreLugar(poi.latLng) { _, direccion ->
                        if (lugarSeleccionado?.latLng == poi.latLng && direccion.isNotBlank()) {
                            lugarSeleccionado = lugarSeleccionado?.copy(direccion = direccion)
                        }
                    }
                }
            ) {
                // Marcador del lugar seleccionado temporalmente
                lugarSeleccionado?.let { lugar ->
                    Marker(
                        state = MarkerState(position = lugar.latLng),
                        title = lugar.nombre,
                        snippet = lugar.direccion.ifBlank { "Toca para registrar muestra IRCA" },
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = {
                            viewModel.iniciarRegistro(lugar.latLng, lugar.nombre)
                            false
                        }
                    )
                }

                puntos.forEach { punto ->
                    Marker(
                        state = MarkerState(position = LatLng(punto.lat, punto.lng)),
                        title = punto.nombre,
                        snippet = "IRCA: ${punto.valorIrca}% - ${punto.nivelRiesgo.etiqueta}",
                        icon = BitmapDescriptorFactory.defaultMarker(punto.nivelRiesgo.hue),
                        onClick = {
                            lugarSeleccionado = null
                            viewModel.seleccionarPunto(punto)
                            false
                        }
                    )
                }
            }

            // Buscador de lugares en la ciudad con autocompletado
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = textoBusqueda,
                        onValueChange = { textoBusqueda = it },
                        placeholder = { Text("Buscar lugar cercano o punto IRCA...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (textoBusqueda.isNotBlank()) {
                                IconButton(onClick = {
                                    textoBusqueda = ""
                                    sugerencias = emptyList()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            ejecutarBusqueda(textoBusqueda)
                        }),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (sugerencias.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(sugerencias, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { seleccionarSugerencia(item) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.esPuntoRegistrado) Icons.Default.WaterDrop else Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (item.esPuntoRegistrado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.titulo,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        if (item.subtitulo.isNotBlank()) {
                                            Text(
                                                text = item.subtitulo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    val dist = formatearDistancia(item.distanciaMetros)
                                    if (dist.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dist,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ficha de lugar seleccionado en el mapa
            if (puntoSeleccionado == null) {
                lugarSeleccionado?.let { lugar ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = lugar.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (lugar.direccion.isNotBlank()) {
                                            Text(
                                                text = lugar.direccion,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { lugarSeleccionado = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.iniciarRegistro(lugar.latLng, lugar.nombre)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddLocation, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Registrar muestra IRCA en este lugar")
                            }
                        }
                    }
                }
            }

            // Detalle del punto seleccionado (Card inferior)
            puntoSeleccionado?.let { punto ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = punto.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.seleccionarPunto(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(punto.nivelRiesgo.colorHex), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IRCA: ${punto.valorIrca}% (${punto.nivelRiesgo.etiqueta})",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(punto.nivelRiesgo.colorHex)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Coordenadas: ${String.format("%.4f", punto.lat)}, ${String.format("%.4f", punto.lng)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Fecha: ${punto.fechaFormateada}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.iniciarEdicion(punto) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = { viewModel.eliminarPunto(punto.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eliminar punto", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para nuevo registro
    coordenadaRegistro?.let { coord ->
        RegistrarPuntoDialog(
            coordenada = coord,
            nombreInicial = nombreSugeridoRegistro,
            onDismiss = { viewModel.cancelarRegistro() },
            onConfirm = { nombre, valorIrca ->
                viewModel.registrarPunto(nombre, valorIrca)
                lugarSeleccionado = null
            }
        )
    }

    // Diálogo para editar punto existente
    puntoParaEditar?.let { punto ->
        EditarPuntoDialog(
            punto = punto,
            onDismiss = { viewModel.cancelarEdicion() },
            onConfirm = { nuevoNombre, nuevoIrca ->
                viewModel.guardarEdicion(nuevoNombre, nuevoIrca)
            }
        )
    }

    // Diálogo de leyenda informativa
    if (mostrarLeyenda) {
        LeyendaIrcaDialog(onDismiss = { viewModel.toggleLeyenda() })
    }
}

@Composable
fun RegistrarPuntoDialog(
    coordenada: LatLng,
    nombreInicial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, valorIrca: Float) -> Unit
) {
    var nombre by remember(nombreInicial) { mutableStateOf(nombreInicial) }
    var ircaTexto by remember { mutableStateOf("") }

    val ircaFloat = ircaTexto.toFloatOrNull() ?: 0f
    val nivelCalculado = when {
        ircaFloat <= 5f -> NivelRiesgo.SIN_RIESGO
        ircaFloat <= 14f -> NivelRiesgo.BAJO
        ircaFloat <= 35f -> NivelRiesgo.MEDIO
        ircaFloat <= 80f -> NivelRiesgo.ALTO
        else -> NivelRiesgo.INVIABLE
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nuevo Punto de Muestreo")
            }
        },
        text = {
            Column {
                Text(
                    text = "Coordenadas: ${String.format("%.4f", coordenada.latitude)}, ${String.format("%.4f", coordenada.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre o Sector del punto") },
                    placeholder = { Text("Ej. Grifo Comuna 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ircaTexto,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d{0,3}(\.\d{0,2})?$"""))) {
                            ircaTexto = it
                        }
                    },
                    label = { Text("Valor IRCA (0 - 100 %)") },
                    placeholder = { Text("Ej. 12.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (ircaTexto.isNotBlank() && ircaTexto.toFloatOrNull() != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                Color(nivelCalculado.colorHex).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(nivelCalculado.colorHex), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nivel: ${nivelCalculado.etiqueta}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(nivelCalculado.colorHex)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valor = ircaTexto.toFloatOrNull() ?: 0f
                    onConfirm(nombre, valor)
                },
                enabled = nombre.isNotBlank() && ircaTexto.isNotBlank() && ircaTexto.toFloatOrNull() != null
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditarPuntoDialog(
    punto: PuntoIrca,
    onDismiss: () -> Unit,
    onConfirm: (nuevoNombre: String, nuevoIrca: Float) -> Unit
) {
    var nombre by remember(punto) { mutableStateOf(punto.nombre) }
    var ircaTexto by remember(punto) { mutableStateOf(punto.valorIrca.toString()) }

    val ircaFloat = ircaTexto.toFloatOrNull() ?: 0f
    val nivelCalculado = when {
        ircaFloat <= 5f -> NivelRiesgo.SIN_RIESGO
        ircaFloat <= 14f -> NivelRiesgo.BAJO
        ircaFloat <= 35f -> NivelRiesgo.MEDIO
        ircaFloat <= 80f -> NivelRiesgo.ALTO
        else -> NivelRiesgo.INVIABLE
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Punto de Muestreo")
            }
        },
        text = {
            Column {
                Text(
                    text = "Coordenadas: ${String.format("%.4f", punto.lat)}, ${String.format("%.4f", punto.lng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre o Sector del punto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ircaTexto,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("""^\d{0,3}(\.\d{0,2})?$"""))) {
                            ircaTexto = it
                        }
                    },
                    label = { Text("Valor IRCA (0 - 100 %)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (ircaTexto.isNotBlank() && ircaTexto.toFloatOrNull() != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                Color(nivelCalculado.colorHex).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(nivelCalculado.colorHex), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nivel: ${nivelCalculado.etiqueta}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(nivelCalculado.colorHex)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valor = ircaTexto.toFloatOrNull() ?: punto.valorIrca
                    onConfirm(nombre, valor)
                },
                enabled = nombre.isNotBlank() && ircaTexto.isNotBlank() && ircaTexto.toFloatOrNull() != null
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun LeyendaIrcaDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Clasificación IRCA (Res. 2115/2007)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NivelRiesgo.values().forEach { nivel ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(nivel.colorHex), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = nivel.etiqueta,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Rango: ${nivel.rango}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}
