package com.kafka.certtool.command;

import com.kafka.certtool.model.CertificateInfo;
import com.kafka.certtool.service.CertificateService;
import com.kafka.certtool.service.KeyStoreService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.Callable;

@Component
@Command(name = "generate-broker-cert", description = "Generate wildcard broker certificate signed by CA")
public class GenerateBrokerCertCommand implements Callable<Integer> {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final CertificateService certificateService;
    private final KeyStoreService keyStoreService;

    @Option(names = {"--ca-cert"}, description = "CA certificate file", required = true)
    private String caCertPath;

    @Option(names = {"--ca-key"}, description = "CA private key file", required = true)
    private String caKeyPath;

    @Option(names = {"--output-dir"}, description = "Output directory", defaultValue = ".")
    private String outputDir;

    @Option(names = {"--domain"}, description = "Domain name (supports wildcards)", defaultValue = "*.kafka.local")
    private String domain;

    @Option(names = {"--validity-days"}, description = "Validity in days", defaultValue = "365")
    private int validityDays;

    @Option(names = {"--keystore-password"}, description = "Keystore password", defaultValue = "changeit")
    private String keystorePassword;

    public GenerateBrokerCertCommand(CertificateService certificateService, KeyStoreService keyStoreService) {
        this.certificateService = certificateService;
        this.keyStoreService = keyStoreService;
    }

    @Override
    public Integer call() throws Exception {
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        System.out.println("Generating broker certificate...");
        System.out.println("Domain: " + domain);
        System.out.println("Validity: " + validityDays + " days");
        System.out.println("Output directory: " + outputDirectory.getAbsolutePath());

        // Load CA certificate and private key
        X509Certificate caCert = loadCertificate(caCertPath);
        PrivateKey caPrivateKey = loadPrivateKey(caKeyPath);

        // Generate broker key pair
        KeyPair brokerKeyPair = certificateService.generateKeyPair();

        // Create certificate info with SAN
        CertificateInfo certInfo = new CertificateInfo(
                domain,
                new String[]{domain},
                validityDays
        );

        // Generate signed certificate
        X509Certificate brokerCert = certificateService.generateSignedCertificate(
                brokerKeyPair,
                caCert,
                caPrivateKey,
                certInfo
        );

        // Write PEM files
        String keyPath = new File(outputDirectory, "broker-key.pem").getAbsolutePath();
        String certPath = new File(outputDirectory, "broker-cert.pem").getAbsolutePath();
        certificateService.writePrivateKeyToPem(brokerKeyPair.getPrivate(), keyPath);
        certificateService.writeCertificateToPem(brokerCert, certPath);

        // Create keystores
        String keystorePath = new File(outputDirectory, "broker.keystore.jks").getAbsolutePath();
        String truststorePath = new File(outputDirectory, "broker.truststore.jks").getAbsolutePath();

        keyStoreService.createKeyStore(
                brokerKeyPair.getPrivate(),
                brokerCert,
                caCert,
                "broker",
                keystorePassword,
                keystorePath
        );

        keyStoreService.createTrustStore(
                caCert,
                "ca",
                keystorePassword,
                truststorePath
        );

        System.out.println("Broker certificate generated successfully!");
        System.out.println("Private key: " + keyPath);
        System.out.println("Certificate: " + certPath);
        System.out.println("Keystore: " + keystorePath);
        System.out.println("Truststore: " + truststorePath);

        return 0;
    }

    private X509Certificate loadCertificate(String path) throws Exception {
        try (FileReader reader = new FileReader(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new java.io.FileInputStream(path));
        }
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        try (PemReader pemReader = new PemReader(new FileReader(path))) {
            PemObject pemObject = pemReader.readPemObject();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }
}
