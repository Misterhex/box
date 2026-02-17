package com.kafka.certtool.service;

import com.kafka.certtool.model.CertificateInfo;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CertificateServiceTest {

    private CertificateService certificateService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        certificateService = new CertificateService();
    }

    @Test
    void testGenerateKeyPair() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    void testGenerateCaCertificate() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        String cn = "Test CA";
        int validityDays = 365;

        X509Certificate caCert = certificateService.generateCaCertificate(keyPair, cn, validityDays);

        assertNotNull(caCert);
        assertTrue(caCert.getSubjectX500Principal().getName().contains(cn));
        assertEquals(caCert.getIssuerX500Principal(), caCert.getSubjectX500Principal());
        assertNotNull(caCert.getExtensionValue(Extension.basicConstraints.getId()));

        // Verify it's a CA certificate
        assertTrue(caCert.getBasicConstraints() >= 0);
    }

    @Test
    void testGenerateSignedCertificate() throws Exception {
        // Generate CA
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        // Generate broker cert
        KeyPair brokerKeyPair = certificateService.generateKeyPair();
        CertificateInfo certInfo = new CertificateInfo(
                "broker.kafka.local",
                new String[]{"*.kafka.local", "broker.kafka.local"},
                180
        );

        X509Certificate brokerCert = certificateService.generateSignedCertificate(
                brokerKeyPair,
                caCert,
                caKeyPair.getPrivate(),
                certInfo
        );

        assertNotNull(brokerCert);
        assertTrue(brokerCert.getSubjectX500Principal().getName().contains("broker.kafka.local"));
        assertEquals(caCert.getSubjectX500Principal(), brokerCert.getIssuerX500Principal());

        // Verify certificate chain
        brokerCert.verify(caCert.getPublicKey());
    }

    @Test
    void testGenerateSignedCertificateWithSAN() throws Exception {
        // Generate CA
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        // Generate cert with SAN
        KeyPair keyPair = certificateService.generateKeyPair();
        String[] sans = new String[]{"*.kafka.local", "broker1.kafka.local", "broker2.kafka.local"};
        CertificateInfo certInfo = new CertificateInfo("*.kafka.local", sans, 180);

        X509Certificate cert = certificateService.generateSignedCertificate(
                keyPair,
                caCert,
                caKeyPair.getPrivate(),
                certInfo
        );

        assertNotNull(cert);

        // Verify SAN entries
        Collection<List<?>> sanCollection = cert.getSubjectAlternativeNames();
        assertNotNull(sanCollection);
        assertEquals(3, sanCollection.size());
    }

    @Test
    void testGenerateSignedCertificateWithoutSAN() throws Exception {
        // Generate CA
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        // Generate cert without SAN
        KeyPair keyPair = certificateService.generateKeyPair();
        CertificateInfo certInfo = new CertificateInfo("client1", null, 180);

        X509Certificate cert = certificateService.generateSignedCertificate(
                keyPair,
                caCert,
                caKeyPair.getPrivate(),
                certInfo
        );

        assertNotNull(cert);
        assertTrue(cert.getSubjectX500Principal().getName().contains("client1"));
    }

    @Test
    void testWritePrivateKeyToPem() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        File pemFile = tempDir.resolve("test-key.pem").toFile();

        certificateService.writePrivateKeyToPem(keyPair.getPrivate(), pemFile.getAbsolutePath());

        assertTrue(pemFile.exists());
        String content = Files.readString(pemFile.toPath());
        assertTrue(content.contains("BEGIN PRIVATE KEY"));
        assertTrue(content.contains("END PRIVATE KEY"));
    }

    @Test
    void testWriteCertificateToPem() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        X509Certificate cert = certificateService.generateCaCertificate(keyPair, "Test CA", 365);
        File pemFile = tempDir.resolve("test-cert.pem").toFile();

        certificateService.writeCertificateToPem(cert, pemFile.getAbsolutePath());

        assertTrue(pemFile.exists());
        String content = Files.readString(pemFile.toPath());
        assertTrue(content.contains("BEGIN CERTIFICATE"));
        assertTrue(content.contains("END CERTIFICATE"));
    }

    @Test
    void testVerifyCertificateChain() throws Exception {
        // Generate CA
        KeyPair caKeyPair = certificateService.generateKeyPair();
        X509Certificate caCert = certificateService.generateCaCertificate(caKeyPair, "Test CA", 365);

        // Generate broker cert
        KeyPair brokerKeyPair = certificateService.generateKeyPair();
        CertificateInfo certInfo = new CertificateInfo("broker", new String[]{"broker.local"}, 180);
        X509Certificate brokerCert = certificateService.generateSignedCertificate(
                brokerKeyPair,
                caCert,
                caKeyPair.getPrivate(),
                certInfo
        );

        // Verify chain - should not throw exception
        assertDoesNotThrow(() -> brokerCert.verify(caCert.getPublicKey()));
    }

    @Test
    void testCaCertificateValidity() throws Exception {
        KeyPair keyPair = certificateService.generateKeyPair();
        int validityDays = 100;

        X509Certificate caCert = certificateService.generateCaCertificate(keyPair, "Test CA", validityDays);

        // Check if certificate is currently valid
        assertDoesNotThrow(() -> caCert.checkValidity());

        // Verify validity period is approximately correct
        long validityMillis = caCert.getNotAfter().getTime() - caCert.getNotBefore().getTime();
        long expectedMillis = validityDays * 24L * 60L * 60L * 1000L;
        assertTrue(Math.abs(validityMillis - expectedMillis) < 1000); // Within 1 second tolerance
    }
}
