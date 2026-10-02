"""Convert a vetted Protomaps PMTiles extract into Android-compatible MBTiles.

Usage: python convert_pmtiles_to_mbtiles.py INPUT.pmtiles OUTPUT.mbtiles PMTILES_PY_DIR
The optional Python package is Protomaps' `pmtiles` from PyPI.
"""

import json
import sqlite3
import sys
from pathlib import Path


def main() -> None:
    source_path, destination_path, package_directory = map(Path, sys.argv[1:4])
    sys.path.insert(0, str(package_directory))
    from pmtiles.reader import MmapSource, Reader, all_tiles

    if destination_path.exists():
        raise SystemExit(f"Destination already exists: {destination_path}")

    with source_path.open("rb") as source:
        reader = Reader(MmapSource(source))
        header = reader.header()
        metadata = reader.metadata()
        if header["tile_type"].name != "MVT":
            raise SystemExit("Expected MVT vector tiles")

        connection = sqlite3.connect(destination_path)
        try:
            connection.executescript(
                "CREATE TABLE metadata (name TEXT, value TEXT);"
                "CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, "
                "tile_row INTEGER, tile_data BLOB);"
                "CREATE UNIQUE INDEX tile_index ON tiles "
                "(zoom_level, tile_column, tile_row);"
            )
            values = {
                "name": metadata.get("name", "Ruralitos Ecuador"),
                "type": "baselayer",
                "version": metadata.get("version", "4"),
                "description": metadata.get("description", ""),
                "format": "pbf",
                "minzoom": str(header["min_zoom"]),
                "maxzoom": str(header["max_zoom"]),
                "bounds": ",".join(str(header[key] / 10_000_000) for key in (
                    "min_lon_e7", "min_lat_e7", "max_lon_e7", "max_lat_e7"
                )),
                "center": ",".join(str(header[key] / 10_000_000) for key in (
                    "center_lon_e7", "center_lat_e7"
                )) + f",{header['center_zoom']}",
                "attribution": metadata.get("attribution", "© OpenStreetMap contributors"),
                "json": json.dumps({"vector_layers": metadata["vector_layers"]}),
            }
            with connection:
                connection.executemany(
                    "INSERT INTO metadata (name, value) VALUES (?, ?)", values.items()
                )
                batch = []
                count = 0
                for (zoom, x, y), content in all_tiles(reader.get_bytes):
                    batch.append((zoom, x, (1 << zoom) - 1 - y, content))
                    if len(batch) == 500:
                        connection.executemany(
                            "INSERT INTO tiles VALUES (?, ?, ?, ?)", batch
                        )
                        count += len(batch)
                        batch.clear()
                if batch:
                    connection.executemany(
                        "INSERT INTO tiles VALUES (?, ?, ?, ?)", batch
                    )
                    count += len(batch)
            status = connection.execute("PRAGMA integrity_check").fetchone()[0]
            if status != "ok" or count < 40_000:
                raise SystemExit(f"Invalid conversion: {count} tiles, integrity={status}")
            print(f"Converted {count} tiles to {destination_path}")
        finally:
            connection.close()


if __name__ == "__main__":
    main()
