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
@Command(name = "generate-client-cert", description = "Generate client certificate signed by CA")
public class GenerateClientCertCommand implements Callable<Integer> {

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

    @Option(names = {"--cn"}, description = "Common Name (Kafka principal)", required = true)
    private String cn;

    @Option(names = {"--validity-days"}, description = "Validity in days", defaultValue = "365")
    private int validityDays;

    @Option(names = {"--keystore-password"}, description = "Keystore password", defaultValue = "changeit")
    private String keystorePassword;

    public GenerateClientCertCommand(CertificateService certificateService, KeyStoreService keyStoreService) {
        this.certificateService = certificateService;
        this.keyStoreService = keyStoreService;
    }

    @Override
    public Integer call() throws Exception {
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        System.out.println("Generating client certificate...");
        System.out.println("CN (Kafka principal): " + cn);
        System.out.println("Validity: " + validityDays + " days");
        System.out.println("Output directory: " + outputDirectory.getAbsolutePath());

        // Load CA certificate and private key
        X509Certificate caCert = loadCertificate(caCertPath);
        PrivateKey caPrivateKey = loadPrivateKey(caKeyPath);

        // Generate client key pair
        KeyPair clientKeyPair = certificateService.generateKeyPair();

        // Create certificate info
        CertificateInfo certInfo = new CertificateInfo(cn, null, validityDays);

        // Generate signed certificate
        X509Certificate clientCert = certificateService.generateSignedCertificate(
                clientKeyPair,
                caCert,
                caPrivateKey,
                certInfo
        );

        // Write PEM files
        String keyPath = new File(outputDirectory, "client-key.pem").getAbsolutePath();
        String certPath = new File(outputDirectory, "client-cert.pem").getAbsolutePath();
        certificateService.writePrivateKeyToPem(clientKeyPair.getPrivate(), keyPath);
        certificateService.writeCertificateToPem(clientCert, certPath);

        // Create keystores
        String keystorePath = new File(outputDirectory, "client.keystore.jks").getAbsolutePath();
        String truststorePath = new File(outputDirectory, "client.truststore.jks").getAbsolutePath();

        keyStoreService.createKeyStore(
                clientKeyPair.getPrivate(),
                clientCert,
                caCert,
                "client",
                keystorePassword,
                keystorePath
        );

        keyStoreService.createTrustStore(
                caCert,
                "ca",
                keystorePassword,
                truststorePath
        );

        System.out.println("Client certificate generated successfully!");
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
