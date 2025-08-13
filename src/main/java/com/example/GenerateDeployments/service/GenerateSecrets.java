package com.example.GenerateDeployments.service;

import com.example.GenerateDeployments.model.SecretRequest;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GenerateSecrets {

    public String generateSecretYaml(SecretRequest req) {
        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName("secret-" + req.getName())
                .withNamespace(req.getNamespace())
                .endMetadata()
                .withType("Opaque")
                .withData(encodeToBase64Map(req.getData()))
                .build();

        String yaml = Serialization.asYaml(secret);

        try {
            Files.createDirectories(Path.of("Secret/" + req.getName()));
            Files.writeString(Path.of("Secret/"+ req.getName()+ "/" +"secret-"+ req.getName() +".yaml"), yaml);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return yaml;
    }



    private Map<String, String> encodeToBase64Map(Map<String, String> input) {
    return input.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> Base64.getEncoder().encodeToString(e.getValue().getBytes(StandardCharsets.UTF_8))
        ));
}
    
}
