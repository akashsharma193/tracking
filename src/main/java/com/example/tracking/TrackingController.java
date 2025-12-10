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
        String ip = r.getRemoteAddr();
        v.setIp(ip);
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
        return ResponseEntity.ok(v.getShortCode());
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

}
