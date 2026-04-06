package com.urlshortener.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.net.InetAddress;

@Service
@Slf4j
public class GeoIpService {

    @Value("${app.geoip.db-path}")
    private String databasePath;

    private DatabaseReader dbReader;

    @PostConstruct
    public void init() {
        try {
            File database = new File(databasePath);
            if (database.exists()) {
                dbReader = new DatabaseReader.Builder(database).build();
                log.info("GeoIP database loaded successfully from {}", databasePath);
            } else {
                String classpathParam = databasePath.replace("src/main/resources/", "");
                ClassPathResource resource = new ClassPathResource(classpathParam);
                if (resource.exists()) {
                    dbReader = new DatabaseReader.Builder(resource.getInputStream()).build();
                    log.info("GeoIP database loaded successfully from classpath: {}", classpathParam);
                } else {
                    log.warn("GeoIP database not found at {}. GeoIP lookups will be disabled.", databasePath);
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize GeoIP database", e);
        }
    }

    public CityResponse getCityResponse(String ip) {
        if (dbReader == null || ip == null || ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return null;
        }

        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            return dbReader.city(ipAddress);
        } catch (Exception e) {
            log.debug("GeoIP lookup failed for IP {}: {}", ip, e.getMessage());
            return null;
        }
    }
}
