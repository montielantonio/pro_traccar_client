package org.traccar.client

import android.os.Build
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.traccar.client.RequestManager.sendRequest

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class RequestManagerTest {

    @Ignore("Not a real unit test - requires network connection")
    @Test
    fun testSendRequest() {
        // This test requires a real server connection
        // Format: sendRequest(requestUrl)
        val requestUrl = "http://track.gpslinkusa.com:5055/?id=PHONE001&lat=37.421998&lon=-122.084&speed=0&bearing=0"
        Assert.assertFalse(sendRequest("http://invalid-server-test:5055/?id=test&lat=0&lon=0&speed=0&bearing=0"))
    }

}
