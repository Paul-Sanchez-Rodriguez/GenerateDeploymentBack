package com.example.GenerateDeployments.model;

import lombok.Data;

@Data
public class VolumeSpec {
    private String name;
    private String mountPath;
    private String type; // "configMap", "secret", etc.
    private String subPath;

    public VolumeSpec(String name, String mountPath, String type, String subPath) {
        this.name = name;
        this.mountPath = mountPath;
        this.type = type;
        this.subPath = subPath;
    }
}
