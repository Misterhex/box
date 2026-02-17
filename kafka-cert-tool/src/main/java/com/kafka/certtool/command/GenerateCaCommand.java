package com.kafka.certtool.command;

import com.kafka.certtool.service.CertificateService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

@Component
@Command(name = "generate-ca", description = "Generate a self-signed CA certificate")
public class GenerateCaCommand implements Callable<Integer> {

    private final CertificateService certificateService;

    @Option(names = {"--output-dir"}, description = "Output directory", defaultValue = ".")
    private String outputDir;

    @Option(names = {"--cn"}, description = "Common Name", defaultValue = "Kafka CA")
    private String cn;

    @Option(names = {"--validity-days"}, description = "Validity in days", defaultValue = "3650")
    private int validityDays;

    public GenerateCaCommand(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public Integer call() throws Exception {
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        System.out.println("Generating CA certificate...");
        System.out.println("CN: " + cn);
        System.out.println("Validity: " + validityDays + " days");
        System.out.println("Output directory: " + outputDirectory.getAbsolutePath());

        KeyPair keyPair = certificateService.generateKeyPair();
        X509Certificate caCertificate = certificateService.generateCaCertificate(keyPair, cn, validityDays);

        String keyPath = new File(outputDirectory, "ca-key.pem").getAbsolutePath();
        String certPath = new File(outputDirectory, "ca-cert.pem").getAbsolutePath();

        certificateService.writePrivateKeyToPem(keyPair.getPrivate(), keyPath);
        certificateService.writeCertificateToPem(caCertificate, certPath);

        System.out.println("CA certificate generated successfully!");
        System.out.println("Private key: " + keyPath);
        System.out.println("Certificate: " + certPath);

        return 0;
    }
}
