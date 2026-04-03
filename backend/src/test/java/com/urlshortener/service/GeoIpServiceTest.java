package com.urlshortener.service;

import com.maxmind.geoip2.model.CityResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class GeoIpServiceTest {

    @InjectMocks
    private GeoIpService geoIpService;

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "0:0:0:0:0:0:0:1", "", "null"})
    void getCityResponse_InactiveIp_ReturnsNull(String ip) {
        String testIp = "null".equals(ip) ? null : ip;
        CityResponse response = geoIpService.getCityResponse(testIp);
        assertNull(response);
    }
}
