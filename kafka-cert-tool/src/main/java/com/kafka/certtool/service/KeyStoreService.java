package com.kafka.certtool.service;

import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Service
public class KeyStoreService {

    public void createKeyStore(
            PrivateKey privateKey,
            X509Certificate certificate,
            X509Certificate caCertificate,
            String alias,
            String password,
            String filePath
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        Certificate[] chain = caCertificate != null
                ? new Certificate[]{certificate, caCertificate}
                : new Certificate[]{certificate};

        keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), chain);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            keyStore.store(fos, password.toCharArray());
        }
    }

    public void createTrustStore(
            X509Certificate caCertificate,
            String alias,
            String password,
            String filePath
    ) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);

        trustStore.setCertificateEntry(alias, caCertificate);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            trustStore.store(fos, password.toCharArray());
        }
    }
}
