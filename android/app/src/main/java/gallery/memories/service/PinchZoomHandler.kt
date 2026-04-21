package gallery.memories.service

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.webkit.WebView
import kotlin.math.abs

/**
 * Handles pinch-to-zoom gestures for WebView content.
 * Provides smooth zoom functionality similar to iPhone gallery app.
 */
class PinchZoomHandler(private val webView: WebView) {
    companion object {
        private const val TAG = "PinchZoomHandler"
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 4.0f
    }

    private val scaleGestureDetector = ScaleGestureDetector(webView.context, ScaleListener())
    private var currentZoomLevel = 1.0f

    inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor

            // Update zoom level
            currentZoomLevel *= scaleFactor

            // Clamp zoom level between min and max
            currentZoomLevel = currentZoomLevel.coerceIn(MIN_ZOOM, MAX_ZOOM)

            // Apply zoom to webview using JavaScript
            applyZoom(currentZoomLevel)

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // No-op: zoom persists until next gesture
        }
    }

    /**
     * Process motion events for pinch zoom detection.
     * Call this from the WebView's onTouchEvent or from a touch listener.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        return scaleGestureDetector.onTouchEvent(event)
    }

    /**
     * Apply zoom level to the WebView content via JavaScript.
     */
    private fun applyZoom(zoomLevel: Float) {
        // Use CSS transform for smooth zooming centered on the image
        val javascript = """
            javascript:
            (function() {
                var images = document.querySelectorAll('img[data-zoomable], .photo-viewer-image, [data-photo-main]');
                if (images.length > 0) {
                    images.forEach(function(img) {
                        if (img.style.transformOrigin === '') {
                            img.style.transformOrigin = 'center center';
                        }
                        img.style.transform = 'scale($zoomLevel)';
                        img.style.transition = 'none';
                    });
                } else {
                    // Fallback: try zooming the entire viewport
                    var body = document.body;
                    var html = document.documentElement;
                    if (body.style.transformOrigin === '') {
                        body.style.transformOrigin = 'center center';
                        html.style.transformOrigin = 'center center';
                    }
                    body.style.transform = 'scale($zoomLevel)';
                    html.style.transform = 'scale($zoomLevel)';
                    body.style.transition = 'none';
                    html.style.transition = 'none';
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }

    /**
     * Reset zoom to normal (1.0x).
     */
    fun resetZoom() {
        currentZoomLevel = 1.0f
        applyZoom(1.0f)
    }

    /**
     * Get the current zoom level.
     */
    fun getCurrentZoom(): Float = currentZoomLevel

    /**
     * Set zoom to a specific level.
     */
    fun setZoom(level: Float) {
        currentZoomLevel = level.coerceIn(MIN_ZOOM, MAX_ZOOM)
        applyZoom(currentZoomLevel)
    }

    /**
     * Check if zoom is at minimum level.
     */
    fun isAtMinZoom(): Boolean = currentZoomLevel <= MIN_ZOOM

    /**
     * Check if zoom is at maximum level.
     */
    fun isAtMaxZoom(): Boolean = currentZoomLevel >= MAX_ZOOM

    /**
     * Inject CSS to prepare elements for zooming.
     */
    fun injectZoomSupportCSS() {
        val css = """
            javascript:
            (function() {
                if (!document.getElementById('pinch-zoom-style')) {
                    var style = document.createElement('style');
                    style.id = 'pinch-zoom-style';
                    style.innerHTML = `
                        img[data-zoomable],
                        .photo-viewer-image,
                        [data-photo-main] {
                            will-change: transform;
                            backface-visibility: hidden;
                            -webkit-backface-visibility: hidden;
                        }
                        body, html {
                            -webkit-user-select: none;
                            user-select: none;
                            -webkit-touch-callout: none;
                        }
                    `;
                    document.head.appendChild(style);
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(css, null)
    }
}

