package com.example.GenerateDeployments.service.gke;

import com.example.GenerateDeployments.model.ServiceRequest;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Service
public class GenerateServiceGke {

    public String generateServiceGkeYaml(ServiceRequest req){
        io.fabric8.kubernetes.api.model.Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(req.getName())
                    .withNamespace(req.getNamespace())
                    .withLabels(Collections.singletonMap("name", req.getName()))
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                    .withName("http")
                    .withProtocol("TCP")
                    .withPort(req.getPort())
                    .withTargetPort(new IntOrString(req.getTargetPort()))
                .endPort()
                .addToSelector("name", req.getName())
                .withType("NodePort")
                .endSpec()
                .build();

        String yaml = Serialization.asYaml(service);

        try {
            Files.createDirectories(Path.of("deployments-GKE/" + req.getName()));
            Files.writeString(Path.of("deployments-GKE/"+ req.getName() + "/" + req.getName() + "-service.yaml"), yaml);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return yaml;
    }
}
