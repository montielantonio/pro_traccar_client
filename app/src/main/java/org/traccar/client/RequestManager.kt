/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
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
@file:Suppress("DEPRECATION")
package org.traccar.client

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object RequestManager {

    private const val TIMEOUT = 15 * 1000
    private const val TAG = "RequestManager"

    fun sendRequest(request: String?): Boolean {
        if (request == null) {
            Log.e(TAG, "Request URL is null")
            return false
        }
        
        // Validate URL uses HTTP (not HTTPS) and port 5055
        val urlLower = request.lowercase()
        if (urlLower.startsWith("https://")) {
            Log.e(TAG, "ERROR: URL uses HTTPS, but must use HTTP! URL: $request")
            Log.e(TAG, "Please change URL to use http:// (not https://) and include :5055 port")
            return false
        }
        if (!urlLower.contains(":5055")) {
            Log.w(TAG, "WARNING: URL does not include port 5055. URL: $request")
            Log.w(TAG, "Expected format: http://track.gpslinkusa.com:5055/...")
        }
        
        var inputStream: InputStream? = null
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(request)
            Log.i(TAG, "=== Sending HTTP GET request ===")
            Log.i(TAG, "Protocol: ${url.protocol}")
            Log.i(TAG, "Host: ${url.host}")
            Log.i(TAG, "Port: ${url.port}")
            Log.i(TAG, "Full URL: $request")
            connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = TIMEOUT
            connection.connectTimeout = TIMEOUT
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Traccar-Client-Android")
            Log.d(TAG, "Connecting to server...")
            connection.connect()
            val responseCode = connection.responseCode
            Log.i(TAG, "Response code: $responseCode")
            
            if (responseCode in 200..299) {
                inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }
                Log.i(TAG, "Request successful! Response: $response")
                true
            } else {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                Log.w(TAG, "Request failed with response code: $responseCode, Error: $errorResponse")
                false
            }
        } catch (error: IOException) {
            Log.e(TAG, "=== Request FAILED ===")
            Log.e(TAG, "Error type: ${error.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${error.message}")
            Log.e(TAG, "Full error:", error)
            false
        } catch (error: Exception) {
            Log.e(TAG, "=== Unexpected error ===")
            Log.e(TAG, "Error type: ${error.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${error.message}")
            Log.e(TAG, "Full error:", error)
            false
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.w(TAG, "Error closing input stream", e)
            }
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting", e)
            }
        }
    }

    fun sendRequestAsync(request: String, handler: RequestHandler) {
        RequestAsyncTask(handler).execute(request)
    }

    interface RequestHandler {
        fun onComplete(success: Boolean)
    }

    private class RequestAsyncTask(private val handler: RequestHandler) : AsyncTask<String, Unit, Boolean>() {

        override fun doInBackground(vararg request: String): Boolean {
            return sendRequest(request[0])
        }

        override fun onPostExecute(result: Boolean) {
            handler.onComplete(result)
        }
    }
}
