package com.example.tracking;

import lombok.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class TrackingController {
    private final VisitorRepository repo;
    private final GeoIPService geo;

    @PostMapping("/track/device")
    public ResponseEntity<?> device(HttpServletRequest r, @RequestBody(required = false) Map<String, Object> payload) {
        VisitorInfo v = new VisitorInfo();
        // Determine real IP with proxy headers if present
        String ip = ""; String ipSource = "RemoteAddr";
        String xff = r.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            ip = (comma != -1) ? xff.substring(0, comma).trim() : xff.trim();
            ipSource = "X-Forwarded-For";
        } else {
            String realIp = r.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) { ip = realIp; ipSource = "X-Real-IP"; }
            else ip = r.getRemoteAddr();
        }
        System.out.println("Received tracking from IP: " + ip + " (source: " + ipSource + ")");
        v.setIp(ip);
        v.setIpSource(ipSource);
        v.setCountry(geo.getCountry(ip));
        v.setCity(geo.getCity(ip));
        v.setUserAgent(r.getHeader("User-Agent"));
        v.setDeviceType(r.getHeader("User-Agent").toLowerCase().contains("mobile")?"Mobile":"Desktop");
        v.setShortCode(UUID.randomUUID().toString().substring(0,6));
 
        // Prefer client-provided coords if available
        if (payload != null) {
            Object latObj = payload.get("latitude");
            Object lonObj = payload.get("longitude");
            if (latObj instanceof Number) v.setLatitude(((Number) latObj).doubleValue());
            if (lonObj instanceof Number) v.setLongitude(((Number) lonObj).doubleValue());
        }
 
        // Fallback to IP-based geolocation if client data isn't provided
        if (v.getLatitude() == null || v.getLongitude() == null) {
            v.setLatitude(geo.getLatitude(ip));
            v.setLongitude(geo.getLongitude(ip));
        }
 
        repo.save(v);
        Map<String, Object> resp = new HashMap<>();
        resp.put("shortCode", v.getShortCode());
        resp.put("ip", ip);
        resp.put("ipSource", ipSource);
        if (v.getLatitude() != null) resp.put("lat", v.getLatitude());
        if (v.getLongitude() != null) resp.put("lon", v.getLongitude());
        return ResponseEntity.ok(resp);
    }


    @PostMapping("/track/mapdata")
    public ResponseEntity<?> mapData(HttpServletRequest r) {
        // Determine IP and source
        String ip = ""; String ipSource = "RemoteAddr";
        String xff = r.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            ip = (comma != -1) ? xff.substring(0, comma).trim() : xff.trim();
            ipSource = "X-Forwarded-For";
        } else {
            String realIp = r.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) { ip = realIp; ipSource = "X-Real-IP"; }
            else ip = r.getRemoteAddr();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("ip", ip);
        data.put("ipSource", ipSource);
        data.put("lat", geo.getLatitude(ip));
        data.put("lon", geo.getLongitude(ip));
        data.put("country", geo.getCountry(ip));
        data.put("city", geo.getCity(ip));
        return ResponseEntity.ok(data);
    }

    @GetMapping("/admin/data")
    public List<VisitorInfo> all() {
        return repo.findAll();
    }

    @PostMapping("/admin/reset")
    public ResponseEntity<?> reset() {
        repo.clearAll();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code:[^\\.]+}")
    public ResponseEntity<?> redirect(@PathVariable String code) {
        VisitorInfo i = repo.findByShortCode(code);
        if (i == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.status(302).header("Location", "/track.html").build();
    }

    @PostMapping("/admin/reset2")
    public ResponseEntity<?> reset2() {
        repo.clearAll();
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest r) {
        String ip = r.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int comma = ip.indexOf(',');
            if (comma != -1) ip = ip.substring(0, comma).trim();
            return ip;
        }
        ip = r.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        return r.getRemoteAddr();
    }
}
