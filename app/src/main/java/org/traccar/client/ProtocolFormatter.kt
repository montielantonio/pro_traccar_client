/*
 * Copyright 2012 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client

import android.net.Uri
import android.util.Log

object ProtocolFormatter {

    private const val TAG = "ProtocolFormatter"

    /**
     * Formats HTTP GET request URL with query parameters
     * Format: http://server:5055/?id=DEVICE_ID&lat=LATITUDE&lon=LONGITUDE&speed=SPEED&bearing=BEARING
     * Example: http://track.gpslinkusa.com:5055/?id=PHONE001&lat=37.421998&lon=-122.084&speed=0&bearing=0
     */
    fun formatRequest(url: String, position: Position, alarm: String? = null): String {
        // Validate URL uses HTTP (not HTTPS)
        val urlLower = url.lowercase()
        if (urlLower.startsWith("https://")) {
            Log.e(TAG, "ERROR: Server URL uses HTTPS, but must use HTTP!")
            Log.e(TAG, "Please change server URL to: http://track.gpslinkusa.com:5055")
        }
        
        val serverUrl = Uri.parse(url)
        
        // Ensure port 5055 is present
        val port = serverUrl.port
        if (port != 5055 && port != -1) {
            Log.w(TAG, "WARNING: Server URL port is $port, expected 5055")
        }
        
        // CRITICAL: Ensure leading slash is present before query parameters
        // Traccar's OsmAnd HTTP decoder REQUIRES the path to be "/"
        val builder = serverUrl.buildUpon()
        val path = serverUrl.path
        if (path.isNullOrEmpty() || path == "") {
            builder.path("/")
        }
        
        builder
            .appendQueryParameter("id", position.deviceId)
            .appendQueryParameter("lat", position.latitude.toString())
            .appendQueryParameter("lon", position.longitude.toString())
            .appendQueryParameter("speed", position.speed.toString())
            .appendQueryParameter("bearing", position.course.toString())
        
        // Optional parameters
        if (position.altitude != 0.0) {
            builder.appendQueryParameter("altitude", position.altitude.toString())
        }
        if (position.accuracy != 0.0) {
            builder.appendQueryParameter("accuracy", position.accuracy.toString())
        }
        if (position.battery != 0.0) {
            builder.appendQueryParameter("batt", position.battery.toString())
        }
        if (alarm != null) {
            builder.appendQueryParameter("alarm", alarm)
        }
        
        var finalUrl = builder.build().toString()
        
        // CRITICAL: Ensure leading slash before query parameters
        // Traccar's OsmAnd HTTP decoder REQUIRES: http://server:5055/?id=...
        // NOT: http://server:5055?id=...
        if (finalUrl.contains("?") && !finalUrl.contains("/?")) {
            // Fix: ensure "/" before "?" if missing
            // Pattern: find "?" that's not preceded by "/" and add "/" before it
            finalUrl = finalUrl.replace(Regex("([^/])\\?"), "$1/?")
            Log.w(TAG, "Fixed missing leading slash before query parameters")
            Log.d(TAG, "Final URL: $finalUrl")
        }
        
        // Final validation: log the URL format for debugging
        if (finalUrl.contains("?")) {
            val hasLeadingSlash = finalUrl.contains("/?")
            if (!hasLeadingSlash) {
                Log.e(TAG, "ERROR: URL still missing leading slash! URL: $finalUrl")
            } else {
                Log.d(TAG, "URL format correct: has leading slash before query parameters")
            }
        }
        
        return finalUrl
    }
}
