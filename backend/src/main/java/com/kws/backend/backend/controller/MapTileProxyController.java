package com.kws.backend.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Proxies CARTO/OSM raster tiles so the MAUI WebView never receives the
 * upstream API key. The key is read from {@code CARTO_API_KEY} / application settings.
 */
@RestController
@RequestMapping("/api/map")
@CrossOrigin
public class MapTileProxyController {

    private static final Logger log = LoggerFactory.getLogger(MapTileProxyController.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${carto.api-key:}")
    private String cartoApiKey;

    @GetMapping("/tiles/{z}/{x}/{y}.png")
    public ResponseEntity<byte[]> getTile(
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @RequestParam(value = "r", required = false, defaultValue = "") String retinaSuffix) {
        try {
            String upstream = buildUpstreamUrl(z, x, y, retinaSuffix == null ? "" : retinaSuffix);
            HttpRequest request = HttpRequest.newBuilder(URI.create(upstream))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "KWS-WildGuard-TileProxy/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) {
                log.debug("Tile upstream returned {} for z/x/y {}/{}/{}", response.statusCode(), z, x, y);
                return ResponseEntity.status(response.statusCode()).build();
            }

            String contentType = response.headers().firstValue("Content-Type")
                    .orElse(MediaType.IMAGE_PNG_VALUE);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().build();
        } catch (Exception ex) {
            log.warn("Failed to proxy map tile z/x/y {}/{}/{}: {}", z, x, y, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildUpstreamUrl(int z, int x, int y, String retinaSuffix) {
        String r = retinaSuffix.startsWith("@") || retinaSuffix.isBlank() ? retinaSuffix : "";
        String subdomain = switch (Math.floorMod(x + y, 4)) {
            case 0 -> "a";
            case 1 -> "b";
            case 2 -> "c";
            default -> "d";
        };

        if (cartoApiKey != null && !cartoApiKey.isBlank()) {
            return "https://" + subdomain + ".basemaps.cartocdn.com/rastertiles/voyager/"
                    + z + "/" + x + "/" + y + r + ".png?key=" + cartoApiKey.trim();
        }

        // Esri World Imagery — no API key, no watermark.
        return "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
                + z + "/" + y + "/" + x;
    }
}
