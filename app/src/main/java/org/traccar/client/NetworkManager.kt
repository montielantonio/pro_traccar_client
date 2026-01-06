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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log

class NetworkManager(private val context: Context, private val handler: NetworkHandler?) : BroadcastReceiver() {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    interface NetworkHandler {
        fun onNetworkUpdate(isOnline: Boolean)
    }

    val isOnline: Boolean
        get() {
            return try {
                val activeNetwork = connectivityManager.activeNetworkInfo
                val online = activeNetwork != null && activeNetwork.isConnectedOrConnecting
                Log.d(TAG, "Network status check: online=$online, networkInfo=$activeNetwork")
                online
            } catch (e: Exception) {
                Log.e(TAG, "Error checking network status", e)
                false
            }
        }

    fun start() {
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(this, filter)
    }

    fun stop() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION && handler != null) {
            val online = isOnline
            Log.i(TAG, "=== Network status changed: ${if (online) "ONLINE" else "OFFLINE"} ===")
            handler.onNetworkUpdate(online)
        }
    }

    companion object {
        private val TAG = NetworkManager::class.java.simpleName
    }

}
