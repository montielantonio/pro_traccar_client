package org.traccar.client

import android.location.Location
import android.os.Build
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.traccar.client.ProtocolFormatter.formatRequest

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class ProtocolFormatterTest {

    @Test
    fun testFormatRequest() {
        val location = Location("gps").apply {
            latitude = 37.421998
            longitude = -122.084
            time = System.currentTimeMillis()
        }
        val position = Position("PHONE001", location, BatteryStatus())
        val url = formatRequest("http://track.gpslinkusa.com:5055", position)
        
        // Verify URL format matches expected format
        Assert.assertTrue(url.contains("id=PHONE001"))
        Assert.assertTrue(url.contains("lat=37.421998"))
        Assert.assertTrue(url.contains("lon=-122.084"))
        Assert.assertTrue(url.contains("speed="))
        Assert.assertTrue(url.contains("bearing="))
        // CRITICAL: Must have leading slash before query parameters
        Assert.assertTrue(url.startsWith("http://track.gpslinkusa.com:5055/?"))
        Assert.assertTrue(url.contains("/?"))
    }
    
    @Test
    fun testFormatRequestHasLeadingSlash() {
        val location = Location("gps").apply {
            latitude = 37.421998
            longitude = -122.084
            time = System.currentTimeMillis()
        }
        val position = Position("PHONE001", location, BatteryStatus())
        val url = formatRequest("http://track.gpslinkusa.com:5055", position)
        
        // CRITICAL: Verify leading slash is present before query parameters
        // Required: http://track.gpslinkusa.com:5055/?id=...
        // NOT: http://track.gpslinkusa.com:5055?id=...
        Assert.assertTrue("URL must contain '/?' pattern", url.contains("/?"))
        Assert.assertFalse("URL must NOT have '?' without leading slash", 
            url.matches(Regex("^https?://[^/]+\\?")))
    }

    @Test
    fun testFormatRequestWithCoordinates() {
        val location = Location("gps").apply {
            latitude = 40.7128
            longitude = -74.0060
            time = System.currentTimeMillis()
        }
        val position = Position("test-device", location, BatteryStatus())
        val url = formatRequest("http://localhost:5055", position)
        
        // Verify URL contains expected query parameters
        Assert.assertTrue(url.contains("id=test-device"))
        Assert.assertTrue(url.contains("lat=40.7128"))
        Assert.assertTrue(url.contains("lon=-74.0060"))
        Assert.assertTrue(url.contains("speed="))
        Assert.assertTrue(url.contains("bearing="))
        // CRITICAL: Must have leading slash before query parameters
        Assert.assertTrue("URL must contain '/?' pattern", url.contains("/?"))
    }

    @Test
    fun testFormatAlarmRequest() {
        val location = Location("gps").apply {
            latitude = 0.0
            longitude = 0.0
            time = System.currentTimeMillis()
        }
        val position = Position("123456789012345", location, BatteryStatus())
        val url = formatRequest("http://localhost:5055", position, "sos")
        
        // Verify URL format with alarm
        Assert.assertTrue(url.contains("id=123456789012345"))
        Assert.assertTrue(url.contains("alarm=sos"))
        Assert.assertTrue(url.contains("lat=0.0"))
        Assert.assertTrue(url.contains("lon=0.0"))
        // CRITICAL: Must have leading slash before query parameters
        Assert.assertTrue("URL must contain '/?' pattern", url.contains("/?"))
    }

}
