package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapWebView(
    storeLat: Double,
    storeLon: Double,
    storeName: String,
    currentLat: Double,
    currentLon: Double,
    onLocationChanged: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate the raw Leaflet HTML with dynamic stores and user coordinate markers
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #101524; }
                /* Custom styles to match our premium slate look */
                .leaflet-bar { border: none !important; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1) !important; }
                .leaflet-bar a { background-color: #191D30 !important; color: #00FFCC !important; border-bottom: 1px solid #101524 !important; }
                
                @keyframes pulse {
                    0% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 255, 204, 0.7); }
                    70% { transform: scale(1.1); box-shadow: 0 0 0 10px rgba(0, 255, 204, 0); }
                    100% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 255, 204, 0); }
                }
                .pulse-marker {
                    animation: pulse 2s infinite;
                    border-radius: 50%;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: true,
                    attributionControl: false
                });

                // Utilize a beautiful premium dark map tile layer to match database and sales theme
                L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    maxZoom: 20
                }).addTo(map);

                // Shop marker with custom design popup
                var storeIcon = L.divIcon({
                    className: 'custom-icon',
                    html: '<div style="background-color: #FFB800; width: 14px; height: 14px; border-radius: 50%; border: 2px solid white; box-shadow: 0 0 8px #FFB800;"></div>',
                    iconSize: [20, 20],
                    iconAnchor: [10, 10]
                });

                var userIcon = L.divIcon({
                    className: 'custom-icon-user',
                    html: '<div class="pulse-marker" style="background-color: #00FFCC; width: 16px; height: 16px; border-radius: 50%; border: 2px solid white;"></div>',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });

                var storeMarker = L.marker([$storeLat, $storeLon], { icon: storeIcon }).addTo(map)
                    .bindPopup("<div style='font-family: sans-serif; color: #333;'><b>$storeName</b><br/>Lokasi Terdaftar</div>")
                    .openPopup();

                // User draggable pin
                var userMarker = L.marker([$currentLat, $currentLon], {
                    draggable: true,
                    icon: userIcon
                }).addTo(map);

                userMarker.bindPopup("<div style='font-family: sans-serif; color: #333;'><b>Lokasi Salesman</b><br/>Geser dot hijau ini untuk simulasi koordinat lapangan!</div>");

                // Initialize bounds to instantly fit both the store and the current salesman location properly
                var bounds = L.latLngBounds([[$storeLat, $storeLon], [$currentLat, $currentLon]]);
                map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });

                function notifyAndroid(lat, lng) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onLocationChanged(lat, lng);
                    }
                }

                userMarker.on('dragend', function(e) {
                    var latlng = userMarker.getLatLng();
                    notifyAndroid(latlng.lat, latlng.lng);
                });

                map.on('click', function(e) {
                    userMarker.setLatLng(e.latlng);
                    notifyAndroid(e.latlng.lat, e.latlng.lng);
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    val callback = onLocationChanged

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()

                // Ensure the webview intercepts touch gestures correctly and stops parent container from scrolling
                setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN,
                        android.view.MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false // Allow normal WebView touch handling
                }

                // Register javascript interface to bridge coordinates back
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onLocationChanged(lat: Double, lng: Double) {
                        post {
                            callback(lat, lng)
                        }
                    }
                }, "AndroidBridge")

                loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Let it refresh if coordinates or store details change externally
            webView.evaluateJavascript("""
                if (typeof map !== 'undefined') {
                    // 1. Update store marker if it exists and changed
                    if (typeof storeMarker !== 'undefined') {
                        var currentStoreLatLng = storeMarker.getLatLng();
                        var diffStoreLat = Math.abs(currentStoreLatLng.lat - ($storeLat));
                        var diffStoreLng = Math.abs(currentStoreLatLng.lng - ($storeLon));
                        if (diffStoreLat > 0.0001 || diffStoreLng > 0.0001) {
                            storeMarker.setLatLng([$storeLat, $storeLon]);
                        }
                    }

                    // 2. Update user marker if it exists and changed
                    if (typeof userMarker !== 'undefined') {
                        var currentLatLng = userMarker.getLatLng();
                        var diffLat = Math.abs(currentLatLng.lat - ($currentLat));
                        var diffLng = Math.abs(currentLatLng.lng - ($currentLon));
                        if (diffLat > 0.0001 || diffLng > 0.0001) {
                            userMarker.setLatLng([$currentLat, $currentLon]);
                            
                            // If user jumped far, fit bounds dynamically. If small change, pan smoothly.
                            if (diffLat > 0.005 || diffLng > 0.005) {
                                var bounds = L.latLngBounds([[$storeLat, $storeLon], [$currentLat, $currentLon]]);
                                map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
                            } else {
                                if (!map.getBounds().contains([$currentLat, $currentLon])) {
                                    map.panTo([$currentLat, $currentLon]);
                                }
                            }
                        }
                    }
                }
            """.trimIndent(), null)
        }
    )
}
