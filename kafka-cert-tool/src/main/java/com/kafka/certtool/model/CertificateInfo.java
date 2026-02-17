package com.kafka.certtool.model;

public record CertificateInfo(String cn, String[] sans, int validityDays) {
}
