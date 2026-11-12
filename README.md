# Monitor IRCA - Mapa de Calidad del Agua

Aplicación móvil Android desarrollada en **Kotlin** con **Jetpack Compose** y la **API de Google Maps** (`maps-compose`) para el registro y visualización georreferenciada del **Índice de Riesgo de la Calidad del Agua (IRCA)** según la Resolución 2115 de 2007.

---

## Problema que Resuelve
En salud pública y gestión ambiental, monitorear la potabilidad del agua por sectores requiere georreferenciar puntos de muestreo y clasificar el nivel de riesgo de forma visual e inmediata. Esta app permite registrar mediciones en cualquier coordenada de la ciudad, calculando al instante su nivel y representándolo con marcadores de color en el mapa.

---

## 🚦 Clasificación de Niveles IRCA
| Rango (%) | Nivel de Riesgo | Color Marcador |
| :--- | :--- | :--- |
| **0.0 - 5.0%** | Sin Riesgo | Verde |
| **5.1 - 14.0%** | Riesgo Bajo | Amarillo |
| **14.1 - 35.0%** | Riesgo Medio | Naranja |
| **35.1 - 80.0%** | Riesgo Alto | Rojo |
| **80.1 - 100.0%** | Inviable Sanitariamente | Púrpura |

---

## Funcionalidades
1. **Mapa Interactivo:** Visualiza todos los puntos de muestreo guardados con pines coloreados según su nivel de riesgo.
2. **Buscador de Lugares:** Barra de búsqueda superior para encontrar barrios, parques, direcciones o puntos guardados en la ciudad y situar la cámara inmediatamente.
3. **Auto-rellenado de Nombre:** Al pulsar sobre cualquier lugar del mapa (o el botón `+`), se realiza geocodificación inversa para auto-completar el nombre de la calle, barrio o lugar en el diálogo.
4. **Edición de Puntos:** Botón `Editar` en la ficha de detalle para actualizar el nombre o valor IRCA recalculando automáticamente su nivel de riesgo.
5. **Botón GPS:** Botón flotante dedicado (ícono de mira/GPS) que solicita permiso de ubicación en tiempo de ejecución y centra la cámara suavemente en tu posición actual.
6. **Previsualización Dinámica:** Al ingresar el valor IRCA, el diálogo muestra en tiempo real el nivel de riesgo calculado y su color antes de guardar.
7. **Ficha de Detalle:** Toca un marcador para ver Nombre, IRCA %, clasificación, coordenadas, fecha y opciones para editar o eliminar.
8. **Leyenda Informativa:** Botón `(i)` en la barra superior para consultar la tabla oficial de rangos.
9. **Persistencia Local:** Los puntos se guardan localmente con `SharedPreferences` en formato JSON ligero.

---

## Configuración de Google Maps API Key
Para ver los mapas de Google en tu dispositivo o emulador:

1. Ve a [Google Cloud Console](https://console.cloud.google.com/) y habilita **Maps SDK for Android**.
2. Genera una **API Key**.
3. Abre el archivo:
   ```
   app/src/main/res/values/strings.xml
   ```
4. Reemplaza `YOUR_API_KEY_HERE` con tu clave:
   ```xml
   <string name="google_maps_key" translatable="false">AIzaSy...</string>
   ```

---

## Compilación y Ejecución
- **En Android Studio:** Abre la carpeta donde guardaste el proyecto y presiona **Run** (`Shift + F10`).
- **Por terminal:**
  ```powershell
  .\gradlew.bat assembleDebug
  ```
  El APK se genera en: `app\build\outputs\apk\debug\app-debug.apk`
