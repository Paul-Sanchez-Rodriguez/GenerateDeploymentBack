package com.example.GenerateDeployments.model;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class DeploymentRequest {
    private String name;
    private String image;
    private String namespace;
    private int replicas;
    private Map<String, String> enviroments;
    private List<ConfigMapRef> configMapRefs;
    private List<SecretRef> secretRefs;
    private List<VolumeSpec> volumeSpecs;

    public DeploymentRequest(String name, String image, String namespace, int replicas, Map<String, String> enviroments, List<ConfigMapRef> configMapRefs, List<SecretRef> secretRefs, List<VolumeSpec> volumeSpecs) {
        this.name = name;
        this.image = image;
        this.namespace = namespace;
        this.replicas = replicas;
        this.enviroments = enviroments;
        this.configMapRefs = configMapRefs;
        this.secretRefs = secretRefs;
        this.volumeSpecs = volumeSpecs;
    }
}
