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
package org.traccar.client

import android.content.Context
import org.traccar.client.ProtocolFormatter.formatRequest
import org.traccar.client.RequestManager.sendRequestAsync
import org.traccar.client.PositionProvider.PositionListener
import org.traccar.client.NetworkManager.NetworkHandler
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import android.util.Log
import org.traccar.client.DatabaseHelper.DatabaseHandler
import org.traccar.client.RequestManager.RequestHandler

class TrackingController(private val context: Context) : PositionListener, NetworkHandler {

    private val handler = Handler(Looper.getMainLooper())
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val positionProvider = PositionProviderFactory.create(context, this)
    private val databaseHelper = DatabaseHelper(context)
    private val networkManager = NetworkManager(context, this)

    private val url: String = preferences.getString(MainFragment.KEY_URL, context.getString(R.string.settings_url_default_value))!!
    private val buffer: Boolean = preferences.getBoolean(MainFragment.KEY_BUFFER, true)
    private val deviceId: String = preferences.getString(MainFragment.KEY_DEVICE, "undefined")!!
    private val interval: Long = preferences.getString(MainFragment.KEY_INTERVAL, "10")!!.toLong() * 1000

    private var isOnline = networkManager.isOnline
    private var isWaiting = false
    private var periodicLocationCheck: Runnable? = null
    private var lastSentPosition: Position? = null
    private var lastSendTime: Long = 0

    fun start() {
        Log.d(TAG, "Starting TrackingController: url=$url, deviceId=$deviceId, buffer=$buffer, isOnline=$isOnline, interval=${interval}ms")
        if (isOnline) {
            read()
        }
        try {
            positionProvider.startUpdates()
            Log.d(TAG, "Position provider started")
            // Start periodic location requests to ensure we send every interval
            periodicLocationCheck = object : Runnable {
                override fun run() {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastSend = currentTime - lastSendTime
                    
                    Log.i(TAG, "=== Periodic check triggered ===")
                    Log.i(TAG, "Current time: $currentTime")
                    Log.i(TAG, "Last send time: $lastSendTime")
                    Log.i(TAG, "Time since last send: ${timeSinceLastSend}ms")
                    Log.i(TAG, "Interval: ${interval}ms")
                    Log.i(TAG, "Has last position: ${lastSentPosition != null}")
                    
                    // Request a single location update to ensure we send periodically
                    try {
                        Log.d(TAG, "Requesting single location update...")
                        positionProvider.requestSingleLocation()
                        Log.d(TAG, "Location request sent")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException requesting location", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception requesting location", e)
                    }
                    
                    // Always send last known position if we have one and enough time has passed
                    // This ensures we send every 10 seconds even if no new location comes in
                    if (timeSinceLastSend >= interval) {
                        if (lastSentPosition != null) {
                            Log.i(TAG, "=== Sending position (${timeSinceLastSend}ms since last send) ===")
                            // Update timestamp to current time for periodic sends
                            val updatedPosition = lastSentPosition!!.copy(time = java.util.Date(currentTime))
                            lastSendTime = currentTime
                            if (buffer) {
                                write(updatedPosition)
                            } else {
                                send(updatedPosition)
                            }
                        } else {
                            Log.w(TAG, "No position available yet, waiting for first location...")
                        }
                    } else {
                        Log.d(TAG, "Not enough time passed (${timeSinceLastSend}ms < ${interval}ms)")
                    }
                    
                    // Schedule next check
                    Log.d(TAG, "Scheduling next check in ${interval}ms")
                    periodicLocationCheck?.let { handler.postDelayed(it, interval) }
                }
            }
            Log.i(TAG, "Starting periodic timer with interval: ${interval}ms (${interval/1000}s)")
            periodicLocationCheck?.let { handler.postDelayed(it, interval) }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting position provider - location permission may be missing", e)
        }
        networkManager.start()
    }

    fun stop() {
        networkManager.stop()
        try {
            positionProvider.stopUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        periodicLocationCheck?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPositionUpdate(position: Position) {
        Log.i(TAG, "=== Position update received ===")
        Log.i(TAG, "Device ID: ${position.deviceId}")
        Log.i(TAG, "Latitude: ${position.latitude}")
        Log.i(TAG, "Longitude: ${position.longitude}")
        Log.i(TAG, "Time: ${position.time}")
        StatusActivity.addMessage(context.getString(R.string.status_location_update))
        // Store the last sent position for periodic resending
        lastSentPosition = position
        lastSendTime = System.currentTimeMillis()
        Log.i(TAG, "Stored position for periodic sending")
        if (buffer) {
            Log.d(TAG, "Writing position to buffer...")
            write(position)
        } else {
            Log.d(TAG, "Sending position directly...")
            send(position)
        }
    }

    override fun onPositionError(error: Throwable) {
        Log.e(TAG, "Position error occurred", error)
        StatusActivity.addMessage("Location error: ${error.message}")
    }
    override fun onNetworkUpdate(isOnline: Boolean) {
        val message = if (isOnline) R.string.status_network_online else R.string.status_network_offline
        StatusActivity.addMessage(context.getString(message))
        if (!this.isOnline && isOnline) {
            read()
        }
        this.isOnline = isOnline
    }

    //
    // State transition examples:
    //
    // write -> read -> send -> delete -> read
    //
    // read -> send -> retry -> read -> send
    //

    private fun log(action: String, position: Position?) {
        var formattedAction: String = action
        if (position != null) {
            formattedAction +=
                    " (id:" + position.id +
                    " time:" + position.time.time / 1000 +
                    " lat:" + position.latitude +
                    " lon:" + position.longitude + ")"
        }
        Log.d(TAG, formattedAction)
    }

    private fun write(position: Position) {
        log("write", position)
        databaseHelper.insertPositionAsync(position, object : DatabaseHandler<Unit?> {
            override fun onComplete(success: Boolean, result: Unit?) {
                if (success) {
                    if (isOnline && isWaiting) {
                        read()
                        isWaiting = false
                    }
                }
            }
        })
    }

    private fun read() {
        log("read", null)
        databaseHelper.selectPositionAsync(object : DatabaseHandler<Position?> {
            override fun onComplete(success: Boolean, result: Position?) {
                if (success) {
                    if (result != null) {
                        if (result.deviceId == preferences.getString(MainFragment.KEY_DEVICE, null)) {
                            send(result)
                        } else {
                            delete(result)
                        }
                    } else {
                        isWaiting = true
                    }
                } else {
                    retry()
                }
            }
        })
    }

    private fun delete(position: Position) {
        log("delete", position)
        databaseHelper.deletePositionAsync(position.id, object : DatabaseHandler<Unit?> {
            override fun onComplete(success: Boolean, result: Unit?) {
                if (success) {
                    read()
                } else {
                    retry()
                }
            }
        })
    }

    private fun send(position: Position) {
        log("send", position)
        val request = formatRequest(url, position)
        Log.i(TAG, "=== Preparing to send position via HTTP GET ===")
        Log.i(TAG, "Formatted request URL: $request")
        Log.i(TAG, "Device ID: ${position.deviceId}")
        Log.i(TAG, "Lat: ${position.latitude}, Lon: ${position.longitude}")
        sendRequestAsync(request, object : RequestHandler {
            override fun onComplete(success: Boolean) {
                if (success) {
                    Log.i(TAG, "=== Position sent SUCCESSFULLY ===")
                    if (buffer) {
                        delete(position)
                    }
                } else {
                    Log.e(TAG, "=== Position send FAILED ===")
                    StatusActivity.addMessage(context.getString(R.string.status_send_fail))
                    if (buffer) {
                        retry()
                    }
                }
            }
        })
    }

    private fun retry() {
        log("retry", null)
        handler.postDelayed({
            if (isOnline) {
                read()
            }
        }, RETRY_DELAY.toLong())
    }

    companion object {
        private val TAG = TrackingController::class.java.simpleName
        private const val RETRY_DELAY = 30 * 1000
    }

}
