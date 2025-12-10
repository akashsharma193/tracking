package com.example.tracking;

import com.maxmind.geoip2.*;
import org.springframework.stereotype.*;
import java.io.*;
import java.net.*;

@Service
public class GeoIPService {
	private DatabaseReader dbReader;

	public GeoIPService() {
		try {
			dbReader = new DatabaseReader.Builder(new File("GeoLite2-City.mmdb")).build();
		} catch (Exception e) {
			System.out.println("GeoIP database not found, geolocation disabled.");
			dbReader = null; // Graceful degradation if DB is missing
		}
	}

	public String getCountry(String ip) {
		if (dbReader == null || ip == null || ip.isEmpty()) return "Unknown";
		try {
			InetAddress addr = InetAddress.getByName(ip);
			if (addr.isLoopbackAddress()) return "Unknown";
			return dbReader.city(addr).getCountry().getName();
		} catch (Exception e) {
			return "Unknown";
		}
	}

	public String getCity(String ip) {
		if (dbReader == null || ip == null || ip.isEmpty()) return "Unknown";
		try {
			InetAddress addr = InetAddress.getByName(ip);
			if (addr.isLoopbackAddress()) return "Unknown";
			return dbReader.city(addr).getCity().getName();
		} catch (Exception e) {
			return "Unknown";
		}
	}

	public Double getLatitude(String ip) {
		if (dbReader == null || ip == null || ip.isEmpty()) return null;
		try {
			InetAddress addr = InetAddress.getByName(ip);
			if (addr.isLoopbackAddress()) return null;
			return dbReader.city(addr).getLocation().getLatitude();
		} catch (Exception e) {
			return null;
		}
	}

	public Double getLongitude(String ip) {
		if (dbReader == null || ip == null || ip.isEmpty()) return null;
		try {
			InetAddress addr = InetAddress.getByName(ip);
			if (addr.isLoopbackAddress()) return null;
			return dbReader.city(addr).getLocation().getLongitude();
		} catch (Exception e) {
			return null;
		}
	}
}
