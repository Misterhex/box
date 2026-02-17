package com.kafka.certtool.command;

import com.kafka.certtool.model.CertificateInfo;
import com.kafka.certtool.service.CertificateService;
import com.kafka.certtool.service.KeyStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GenerateBrokerCertCommandTest {

    private CertificateService certificateService;
    private KeyStoreService keyStoreService;
    private GenerateBrokerCertCommand command;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        certificateService = mock(CertificateService.class);
        keyStoreService = mock(KeyStoreService.class);
        command = new GenerateBrokerCertCommand(certificateService, keyStoreService);

        // Create mock CA files
        File caCertFile = tempDir.resolve("ca-cert.pem").toFile();
        File caKeyFile = tempDir.resolve("ca-key.pem").toFile();
        caCertFile.createNewFile();
        caKeyFile.createNewFile();

        // Write minimal PEM content to avoid parsing errors
        java.nio.file.Files.writeString(caCertFile.toPath(),
            "-----BEGIN CERTIFICATE-----\nMIIDXTCCAkWgAwIBAgIJAKJ\n-----END CERTIFICATE-----\n");
        java.nio.file.Files.writeString(caKeyFile.toPath(),
            "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0B\n-----END PRIVATE KEY-----\n");

        setField(command, "caCertPath", caCertFile.getAbsolutePath());
        setField(command, "caKeyPath", caKeyFile.getAbsolutePath());
    }

    @Test
    void testCallWithDefaultValues() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockCaCert = mock(X509Certificate.class);
        X509Certificate mockBrokerCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateSignedCertificate(any(), any(), any(), any())).thenReturn(mockBrokerCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());
        doNothing().when(keyStoreService).createKeyStore(any(), any(), any(), anyString(), anyString(), anyString());
        doNothing().when(keyStoreService).createTrustStore(any(), anyString(), anyString(), anyString());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "domain", "*.kafka.local");
        setField(command, "validityDays", 365);
        setField(command, "keystorePassword", "changeit");

        Integer result = command.call();

        assertEquals(0, result);
        verify(certificateService).generateKeyPair();
        verify(certificateService).writePrivateKeyToPem(any(), anyString());
        verify(certificateService).writeCertificateToPem(any(), anyString());
        verify(keyStoreService).createKeyStore(any(), any(), any(), eq("broker"), eq("changeit"), anyString());
        verify(keyStoreService).createTrustStore(any(), eq("ca"), eq("changeit"), anyString());
    }

    @Test
    void testCallWithCustomDomain() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockBrokerCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateSignedCertificate(any(), any(), any(), any())).thenReturn(mockBrokerCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());
        doNothing().when(keyStoreService).createKeyStore(any(), any(), any(), anyString(), anyString(), anyString());
        doNothing().when(keyStoreService).createTrustStore(any(), anyString(), anyString(), anyString());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "domain", "*.example.com");
        setField(command, "validityDays", 365);
        setField(command, "keystorePassword", "changeit");

        ArgumentCaptor<CertificateInfo> certInfoCaptor = ArgumentCaptor.forClass(CertificateInfo.class);
        when(certificateService.generateSignedCertificate(any(), any(), any(), certInfoCaptor.capture()))
            .thenReturn(mockBrokerCert);

        Integer result = command.call();

        assertEquals(0, result);
        CertificateInfo capturedInfo = certInfoCaptor.getValue();
        assertEquals("*.example.com", capturedInfo.cn());
        assertArrayEquals(new String[]{"*.example.com"}, capturedInfo.sans());
    }

    @Test
    void testOutputDirectoryCreation() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockBrokerCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateSignedCertificate(any(), any(), any(), any())).thenReturn(mockBrokerCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());
        doNothing().when(keyStoreService).createKeyStore(any(), any(), any(), anyString(), anyString(), anyString());
        doNothing().when(keyStoreService).createTrustStore(any(), anyString(), anyString(), anyString());

        File newDir = tempDir.resolve("broker-certs").toFile();
        assertFalse(newDir.exists());

        setField(command, "outputDir", newDir.getAbsolutePath());
        setField(command, "domain", "*.kafka.local");
        setField(command, "validityDays", 365);
        setField(command, "keystorePassword", "changeit");

        Integer result = command.call();

        assertEquals(0, result);
        assertTrue(newDir.exists());
        assertTrue(newDir.isDirectory());
    }

    @Test
    void testCorrectFilePathsGenerated() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockBrokerCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateSignedCertificate(any(), any(), any(), any())).thenReturn(mockBrokerCert);

        ArgumentCaptor<String> keyPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> certPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keystorePathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> truststorePathCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(certificateService).writePrivateKeyToPem(any(), keyPathCaptor.capture());
        doNothing().when(certificateService).writeCertificateToPem(any(), certPathCaptor.capture());
        doNothing().when(keyStoreService).createKeyStore(any(), any(), any(), anyString(), anyString(), keystorePathCaptor.capture());
        doNothing().when(keyStoreService).createTrustStore(any(), anyString(), anyString(), truststorePathCaptor.capture());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "domain", "*.kafka.local");
        setField(command, "validityDays", 365);
        setField(command, "keystorePassword", "changeit");

        command.call();

        assertTrue(keyPathCaptor.getValue().endsWith("broker-key.pem"));
        assertTrue(certPathCaptor.getValue().endsWith("broker-cert.pem"));
        assertTrue(keystorePathCaptor.getValue().endsWith("broker.keystore.jks"));
        assertTrue(truststorePathCaptor.getValue().endsWith("broker.truststore.jks"));
    }

    @Test
    void testCustomKeystorePassword() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockBrokerCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateSignedCertificate(any(), any(), any(), any())).thenReturn(mockBrokerCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());
        doNothing().when(keyStoreService).createKeyStore(any(), any(), any(), anyString(), anyString(), anyString());
        doNothing().when(keyStoreService).createTrustStore(any(), anyString(), anyString(), anyString());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "domain", "*.kafka.local");
        setField(command, "validityDays", 365);
        setField(command, "keystorePassword", "customPassword123");

        command.call();

        verify(keyStoreService).createKeyStore(any(), any(), any(), anyString(), eq("customPassword123"), anyString());
        verify(keyStoreService).createTrustStore(any(), anyString(), eq("customPassword123"), anyString());
    }

    @Test
    void testServiceInteractions() throws Exception {
        KeyPair mockKeyPair = generateMockKeyPair();
        X509Certificate mockBrokerCert = mock(X509Certificate.class);

        when(certificateService.generateKeyPair()).thenReturn(mockKeyPair);
        when(certificateService.generateSignedCertificate(any(), any(), any(), any())).thenReturn(mockBrokerCert);
        doNothing().when(certificateService).writePrivateKeyToPem(any(), anyString());
        doNothing().when(certificateService).writeCertificateToPem(any(), anyString());
        doNothing().when(keyStoreService).createKeyStore(any(), any(), any(), anyString(), anyString(), anyString());
        doNothing().when(keyStoreService).createTrustStore(any(), anyString(), anyString(), anyString());

        setField(command, "outputDir", tempDir.toString());
        setField(command, "domain", "*.kafka.local");
        setField(command, "validityDays", 365);
        setField(command, "keystorePassword", "changeit");

        command.call();

        verify(certificateService, times(1)).generateKeyPair();
        verify(certificateService, times(1)).generateSignedCertificate(any(), any(), any(), any());
        verify(certificateService, times(1)).writePrivateKeyToPem(any(), anyString());
        verify(certificateService, times(1)).writeCertificateToPem(any(), anyString());
        verify(keyStoreService, times(1)).createKeyStore(any(), any(), any(), anyString(), anyString(), anyString());
        verify(keyStoreService, times(1)).createTrustStore(any(), anyString(), anyString(), anyString());
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
