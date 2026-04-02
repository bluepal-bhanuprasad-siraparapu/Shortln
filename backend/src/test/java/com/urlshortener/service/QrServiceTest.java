package com.urlshortener.service;

import com.google.zxing.WriterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QrServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private QrService qrService;

    @Test
    void generateQrCode_Success() throws IOException, WriterException {
        byte[] qrCode = qrService.generateQrCode("https://example.com/abc", 200, 200);

        assertNotNull(qrCode);
        verify(auditLogService).log(eq("QR_GENERATE"), anyString());
    }
}
