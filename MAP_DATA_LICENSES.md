# Datos cartográficos incluidos en Ruralitos

## Mapa vectorial local de Ecuador

- Archivo: `app/src/main/assets/ecuador_base.mbtiles`.
- SHA-256 del archivo empaquetado: `CD22FA987CD2070D6BCEF393B54C2779E053C86A86BD72E6CF3DDEDBC44FA817`.
- Origen: recorte de Ecuador del basemap Protomaps v4, compilación del 25 de septiembre de 2026 (`https://build.protomaps.com/20260925.pmtiles`), niveles de zoom 0–14.
- Datos: OpenStreetMap y Natural Earth. Es una obra producida bajo ODbL; atribución requerida: **© OpenStreetMap contributors · Protomaps**.
- La cartografía base se incluye en la APK y no solicita teselas a terceros para funcionar.
- El archivo se copia una sola vez al almacenamiento privado para que MapLibre pueda abrirlo como base SQLite. Es un recorte del archivo PMTiles original; la conversión está documentada en `tools/convert_pmtiles_to_mbtiles.py`.
- El estilo local muestra vías, caminos, edificios, parques y agua, además de etiquetas de provincias, ciudades, localidades, barrios, calles, ríos y establecimientos de salud cuando esos nombres existen en OpenStreetMap. La ausencia de un nombre o edificio en la fuente no puede corregirse aumentando el zoom de una tesela de nivel 14.
- La tipografía cartográfica `Noto Sans Regular` se guarda en `app/src/main/assets/fonts/` y se utiliza sin red. Es un recurso distinto de las teselas; su licencia SIL Open Font License 1.1 está incluida en `app/src/main/assets/fonts/OFL.txt`.
- El 29 de septiembre de 2026 se comprobó en el emulador Android que las etiquetas de calles, barrios y la ciudad de Quito seguían apareciendo con Wi‑Fi y datos móviles apagados.

## Detalle incluido de Ecuador, zoom 15

- Archivo: `app/src/main/assets/ecuador_zoom15.pmtiles` (140.147.517 bytes; SHA-256 `547BFCCA1D425CB2B8D928F5DF411CF414B30506AD0DFD99DA2708DBE85779CC`).
- Origen: el mismo recorte de Ecuador de Protomaps v4, compilación del 25 de septiembre de 2026 (`https://build.protomaps.com/20260925.pmtiles`), extraído únicamente para el nivel de zoom 15. Contiene 936.052 teselas direccionables.
- Datos y atribución: OpenStreetMap y Natural Earth, ODbL; **© OpenStreetMap contributors · Protomaps**. Consultar también los términos de distribución de Protomaps antes de publicar nuevas versiones comerciales.
- Se distribuye con la APK y el módulo base de la AAB, sin descarga posterior. La aplicación hace una copia privada una sola vez porque el lector PMTiles de MapLibre requiere lecturas por rangos; el mapa base 0–14 continúa disponible si la copia falla.
- El zoom 15 permite distinguir edificios y caminos presentes en OSM, pero no agrega información que OSM no tenga. Al hacer más zoom se amplían los datos existentes, no se generan imágenes ni edificios nuevos.

La licencia CC BY 4.0 de EOxCloudless 2016 corresponde a imágenes satelitales, no a estas teselas vectoriales de Protomaps/OSM.

## Rutas automáticas sin conexión

- Archivo: `app/src/main/assets/ecuador_valhalla_tiles.tar` (224.225.280 bytes; SHA-256 `7EAF63F95336F74AAB97BC262D84CFA166134F07F007D99D414B03E7E05E43DA`). Se incluye en el módulo base de la aplicación.
- Origen de las vías: extracto Ecuador de Geofabrik/OpenStreetMap (`https://download.geofabrik.de/south-america/ecuador.html`), transformado en grafo de Valhalla. Los datos de OpenStreetMap están sujetos a ODbL; se mantiene el crédito visible **© OpenStreetMap contributors · Protomaps** en las pantallas de mapa y se conserva aquí la procedencia de la transformación. La distribución pública de este grafo requiere mantener disponible la base derivada o el procedimiento de reproducción bajo las condiciones ODbL; no se debe presentar como datos exclusivos de Ruralitos.
- Motor: Valhalla y Valhalla Mobile, distribuidos bajo licencia MIT. Sus avisos de copyright y licencia están incluidos en la APK en `app/src/main/assets/third_party_licenses/VALHALLA_MIT.txt`; fuentes oficiales: `https://github.com/valhalla/valhalla/blob/master/COPYING` y `https://github.com/Rallista/valhalla-mobile/blob/main/LICENSE.md`. El aviso y los enlaces ODbL también están incluidos en `app/src/main/assets/third_party_licenses/OSM_ODBL.txt`.
- Función integrada: cálculo local de recorridos y maniobras en auto, a pie o bicicleta. No incluye tráfico en tiempo real, navegación con voz, rutas optimizadas de múltiples paradas ni garantía de transitabilidad de senderos.
- El grafo y el zoom 15 aumentan significativamente la descarga. Antes de publicar en Play Store hay que verificar el tamaño del AAB final y realizar revisión jurídica de las atribuciones y de cualquier obligación ODbL si se distribuyen datos derivados de otra forma.

Fuentes: https://docs.protomaps.com/basemaps/downloads · https://docs.protomaps.com/basemaps/layers · https://github.com/protomaps/basemaps-assets
