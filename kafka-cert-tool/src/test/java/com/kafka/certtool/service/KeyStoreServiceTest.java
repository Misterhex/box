package com.kafka.certtool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreServiceTest {

    private KeyStoreService keyStoreService;
    private CertificateService certificateService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        keyStoreService = new KeyStoreService();
        certificateService = new CertificateService();
    }

    @Test
    void testCreateKeyStore() throws Exception {
        // Generate test certificates
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        KeyPair brokerKeyPair = certificateService.generateKeyPair();
        String password = "test123";
        File keystoreFile = tempDir.resolve("test.keystore.jks").toFile();

        keyStoreService.createKeyStore(
                brokerKeyPair.getPrivate(),
                caCert,
                caCert,
                "broker",
                password,
                keystoreFile.getAbsolutePath()
        );

        assertTrue(keystoreFile.exists());

        // Verify keystore can be loaded
        KeyStore loadedKeyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            loadedKeyStore.load(fis, password.toCharArray());
        }

        assertTrue(loadedKeyStore.containsAlias("broker"));
        assertNotNull(loadedKeyStore.getKey("broker", password.toCharArray()));
    }

    @Test
    void testCreateKeyStoreWithCertificateChain() throws Exception {
        // Generate CA
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        // Generate broker cert signed by CA
        KeyPair brokerKeyPair = certificateService.generateKeyPair();
        String password = "test123";
        File keystoreFile = tempDir.resolve("test.keystore.jks").toFile();

        keyStoreService.createKeyStore(
                brokerKeyPair.getPrivate(),
                caCert,
                caCert,
                "broker",
                password,
                keystoreFile.getAbsolutePath()
        );

        // Verify certificate chain
        KeyStore loadedKeyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            loadedKeyStore.load(fis, password.toCharArray());
        }

        Certificate[] chain = loadedKeyStore.getCertificateChain("broker");
        assertNotNull(chain);
        assertTrue(chain.length >= 1);
    }

    @Test
    void testCreateTrustStore() throws Exception {
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        String password = "test123";
        File truststoreFile = tempDir.resolve("test.truststore.jks").toFile();

        keyStoreService.createTrustStore(
                caCert,
                "ca",
                password,
                truststoreFile.getAbsolutePath()
        );

        assertTrue(truststoreFile.exists());

        // Verify truststore can be loaded
        KeyStore loadedTrustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststoreFile)) {
            loadedTrustStore.load(fis, password.toCharArray());
        }

        assertTrue(loadedTrustStore.containsAlias("ca"));
        assertNotNull(loadedTrustStore.getCertificate("ca"));
    }

    @Test
    void testKeyStorePasswordProtection() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        X509Certificate cert = certificateService.generateCaCertificate(keyPair, "Test", 365);

        String password = "securePassword123";
        File keystoreFile = tempDir.resolve("protected.keystore.jks").toFile();

        keyStoreService.createKeyStore(
                keyPair.getPrivate(),
                cert,
                null,
                "test",
                password,
                keystoreFile.getAbsolutePath()
        );

        // Verify correct password works
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            assertDoesNotThrow(() -> keyStore.load(fis, password.toCharArray()));
        }

        // Verify wrong password fails
        KeyStore keyStore2 = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            assertThrows(IOException.class, () -> keyStore2.load(fis, "wrongPassword".toCharArray()));
        }
    }

    @Test
    void testTrustStorePasswordProtection() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        X509Certificate cert = certificateService.generateCaCertificate(keyPair, "Test", 365);

        String password = "securePassword123";
        File truststoreFile = tempDir.resolve("protected.truststore.jks").toFile();

        keyStoreService.createTrustStore(
                cert,
                "ca",
                password,
                truststoreFile.getAbsolutePath()
        );

        // Verify correct password works
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststoreFile)) {
            assertDoesNotThrow(() -> trustStore.load(fis, password.toCharArray()));
        }

        // Verify wrong password fails
        KeyStore trustStore2 = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststoreFile)) {
            assertThrows(IOException.class, () -> trustStore2.load(fis, "wrongPassword".toCharArray()));
        }
    }

    @Test
    void testKeyStoreWithoutCaCertificate() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        X509Certificate cert = certificateService.generateCaCertificate(keyPair, "Test", 365);

        String password = "test123";
        File keystoreFile = tempDir.resolve("single-cert.keystore.jks").toFile();

        // Create keystore without CA certificate (null)
        keyStoreService.createKeyStore(
                keyPair.getPrivate(),
                cert,
                null,
                "test",
                password,
                keystoreFile.getAbsolutePath()
        );

        assertTrue(keystoreFile.exists());

        // Verify certificate chain contains only one certificate
        KeyStore loadedKeyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            loadedKeyStore.load(fis, password.toCharArray());
        }

        Certificate[] chain = loadedKeyStore.getCertificateChain("test");
        assertNotNull(chain);
        assertEquals(1, chain.length);
    }

    @Test
    void testMultipleCertificatesInTrustStore() throws Exception {
        KeyPair keyPair1 = certificateService.generateKeyPair();
        X509Certificate cert1 = certificateService.generateCaCertificate(keyPair1, "CA 1", 365);

        String password = "test123";
        File truststoreFile = tempDir.resolve("multi.truststore.jks").toFile();

        keyStoreService.createTrustStore(cert1, "ca1", password, truststoreFile.getAbsolutePath());

        // Verify first certificate
        KeyStore loadedTrustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststoreFile)) {
            loadedTrustStore.load(fis, password.toCharArray());
        }

        assertTrue(loadedTrustStore.containsAlias("ca1"));
        assertEquals(1, loadedTrustStore.size());
    }
}
