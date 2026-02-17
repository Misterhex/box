package com.kafka.certtool.command;

import com.kafka.certtool.service.CertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GenerateCaCommandTest {

    private CertificateService certificateService;
    private GenerateCaCommand command;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        certificateService = mock(CertificateService.class);
        command = new GenerateCaCommand(certificateService);
    }

    @Test
    void testCallWithDefaultValues() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateCaCertificate(any(), anyString(), anyInt())).thenReturn(mockCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());

        // Use reflection to set fields since Picocli normally does this
        setField(command, "outputDir", tempDir.toString());
        setField(command, "cn", "Kafka CA");
        setField(command, "validityDays", 3650);

        Integer result = command.call();

        assertEquals(0, result);
        verify(certificateService).generateKeyPair();
        verify(certificateService).generateCaCertificate(mockKeyPair, "Kafka CA", 3650);
        verify(certificateService).writePrivateKeyToPem(eq(mockKeyPair.getPrivate()), anyString());
        verify(certificateService).writeCertificateToPem(eq(mockCert), anyString());
    }

    @Test
    void testCallWithCustomValues() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateCaCertificate(any(), anyString(), anyInt())).thenReturn(mockCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "cn", "Custom CA");
        setField(command, "validityDays", 7300);

        Integer result = command.call();

        assertEquals(0, result);
        verify(certificateService).generateCaCertificate(mockKeyPair, "Custom CA", 7300);
    }

    @Test
    void testOutputDirectoryCreation() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateCaCertificate(any(), anyString(), anyInt())).thenReturn(mockCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());

        File newDir = tempDir.resolve("new-output-dir").toFile();
        assertFalse(newDir.exists());

        setField(command, "outputDir", newDir.getAbsolutePath());
        setField(command, "cn", "Test CA");
        setField(command, "validityDays", 365);

        Integer result = command.call();

        assertEquals(0, result);
        assertTrue(newDir.exists());
        assertTrue(newDir.isDirectory());
    }

    @Test
    void testCorrectFilePathsGenerated() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateCaCertificate(any(), anyString(), anyInt())).thenReturn(mockCert);

        ArgumentCaptor<String> keyPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> certPathCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(certificateService).writePrivateKeyToPem(any(), keyPathCaptor.capture());
        doNothing().when(certificateService).writeCertificateToPem(any(), certPathCaptor.capture());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "cn", "Test CA");
        setField(command, "validityDays", 365);

        command.call();

        assertTrue(keyPathCaptor.getValue().endsWith("ca-key.pem"));
        assertTrue(certPathCaptor.getValue().endsWith("ca-cert.pem"));
    }

    @Test
    void testExceptionHandling() throws Exception {
        when(certificateService.generateKeyPair()).thenThrow(new RuntimeException("Key generation failed"));

        setField(command, "outputDir", tempDir.toString());
        setField(command, "cn", "Test CA");
        setField(command, "validityDays", 365);

        assertThrows(RuntimeException.class, () -> command.call());
    }

    @Test
    void testServiceInteractions() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateCaCertificate(any(), anyString(), anyInt())).thenReturn(mockCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "cn", "Test CA");
        setField(command, "validityDays", 365);

        command.call();

        // Verify all services were called in order
        verify(certificateService, times(1)).generateKeyPair();
        verify(certificateService, times(1)).generateCaCertificate(any(), anyString(), anyInt());
        verify(certificateService, times(1)).writePrivateKeyToPem(any(), anyString());
        verify(certificateService, times(1)).writeCertificateToPem(any(), anyString());
    }

    private KeyPair generateMockKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
