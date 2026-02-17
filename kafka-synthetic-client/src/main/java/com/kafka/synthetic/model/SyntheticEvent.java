package com.kafka.synthetic.model;

public record SyntheticEvent(
    String id,
    long timestamp,
    String source,
    String payload
) {}
