package com.example.GenerateDeployments.service;

import com.example.GenerateDeployments.model.ConfigMapRequest;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GenerateConfig {

    public String generateConfigMapYaml(ConfigMapRequest req) {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                    .withName("config-"+ req.getName())
                    .withNamespace(req.getNamespace())
                .endMetadata()
                .withData(req.getData())
                .build();

        String yaml = Serialization.asYaml(configMap);

        try {
            Files.createDirectories(Path.of("ConfigMap/" + req.getName()));
            Files.writeString(Path.of("ConfigMap/"+ req.getName()+ "/" + "config-"+ req.getName() +".yaml"), yaml);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return yaml;
    }
}
