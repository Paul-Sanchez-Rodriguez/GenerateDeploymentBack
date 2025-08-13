package com.example.GenerateDeployments.model;

import lombok.Data;

import java.util.Map;

@Data
public class SecretRequest {
    private String name;
    private String namespace;
    private Map<String, String> data;

    public SecretRequest(String name, String namespace, Map<String, String> data) {
        this.name = name;
        this.namespace = namespace;
        this.data = data;
    }
}
