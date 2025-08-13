package com.example.GenerateDeployments.model;

import lombok.Data;

@Data
public class ServiceRequest {

    private String name;
    private String namespace;
    private String type; // ClusterIP, NodePort, LoadBalancer
    private int port;
    private int targetPort;

    public ServiceRequest(String name, String namespace, String type, int port, int targetPort) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
        this.port = port;
        this.targetPort = targetPort;
    }
}
