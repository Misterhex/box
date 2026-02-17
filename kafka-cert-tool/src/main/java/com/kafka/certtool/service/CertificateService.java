package com.kafka.certtool.service;

import com.kafka.certtool.model.CertificateInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

@Service
public class CertificateService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        return keyPairGenerator.generateKeyPair();
    }

    public X509Certificate generateCaCertificate(KeyPair keyPair, String cn, int validityDays) throws Exception {
        X500Name issuer = new X500Name("CN=" + cn);
        X500Name subject = issuer;
        BigInteger serialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (validityDays * 24L * 60L * 60L * 1000L));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );

        // Add CA basic constraint
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(true)
        );

        // Add key usage
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    public X509Certificate generateSignedCertificate(
            KeyPair keyPair,
            X509Certificate caCert,
            PrivateKey caPrivateKey,
            CertificateInfo certInfo
    ) throws Exception {
        X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName());
        X500Name subject = new X500Name("CN=" + certInfo.cn());
        BigInteger serialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (certInfo.validityDays() * 24L * 60L * 60L * 1000L));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );

        // Add Subject Alternative Names if provided
        if (certInfo.sans() != null && certInfo.sans().length > 0) {
            GeneralName[] generalNames = new GeneralName[certInfo.sans().length];
            for (int i = 0; i < certInfo.sans().length; i++) {
                generalNames[i] = new GeneralName(GeneralName.dNSName, certInfo.sans()[i]);
            }
            certBuilder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    new GeneralNames(generalNames)
            );
        }

        // Add key usage for server/client authentication
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );

        // Add extended key usage
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(new KeyPurposeId[]{
                        KeyPurposeId.id_kp_serverAuth,
                        KeyPurposeId.id_kp_clientAuth
                })
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(caPrivateKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    public void writePrivateKeyToPem(PrivateKey privateKey, String filePath) throws IOException {
        try (PemWriter pemWriter = new PemWriter(new FileWriter(filePath))) {
            PemObject pemObject = new PemObject("PRIVATE KEY", privateKey.getEncoded());
            pemWriter.writeObject(pemObject);
        }
    }

    public void writeCertificateToPem(X509Certificate certificate, String filePath) throws IOException {
        try (PemWriter pemWriter = new PemWriter(new FileWriter(filePath))) {
            PemObject pemObject = new PemObject("CERTIFICATE", certificate.getEncoded());
            pemWriter.writeObject(pemObject);
        } catch (Exception e) {
            throw new IOException("Failed to write certificate to PEM", e);
        }
    }
}
