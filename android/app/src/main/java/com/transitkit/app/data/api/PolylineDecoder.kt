package com.transitkit.app.data.api

import com.mapbox.geojson.Point
import kotlin.math.pow

/**
 * Decoder for Google's Encoded Polyline Algorithm Format.
 * MOTIS exposes leg geometries in this format with `precision = 7`.
 */
object PolylineDecoder {

    fun decode(encoded: String, precision: Int = 7): List<Point> {
        if (encoded.isEmpty()) return emptyList()
        val factor = 10.0.pow(precision)
        val coords = ArrayList<Point>(encoded.length / 4)
        val bytes = encoded.toByteArray(Charsets.UTF_8)
        var i = 0
        var lat = 0
        var lng = 0
        while (i < bytes.size) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                if (i >= bytes.size) return coords
                b = (bytes[i].toInt() and 0xFF) - 63
                i += 1
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

            shift = 0
            result = 0
            do {
                if (i >= bytes.size) return coords
                b = (bytes[i].toInt() and 0xFF) - 63
                i += 1
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

            val pt = Point.fromLngLat(
                lng.toDouble() / factor,
                lat.toDouble() / factor,
            )
            // Skip duplicate consecutive points — MOTIS emits zero-diff pairs as
            // sub-segment separators which would render as zero-length segments
            // and visually break the polyline.
            val last = coords.lastOrNull()
            if (last == null || last.latitude() != pt.latitude() || last.longitude() != pt.longitude()) {
                coords.add(pt)
            }
        }
        return coords
    }
}
