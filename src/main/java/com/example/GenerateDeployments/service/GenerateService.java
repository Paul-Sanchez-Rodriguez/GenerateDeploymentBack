package com.example.GenerateDeployments.service;

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
public class GenerateService {


    public String generateServiceYaml(ServiceRequest req){
        io.fabric8.kubernetes.api.model.Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(req.getName())
                    .withNamespace(req.getNamespace())
                    .withLabels(Collections.singletonMap("app", req.getName()))
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                    .withName("8080-tcp")
                    .withProtocol("TCP")
                    .withPort(req.getPort())
                    .withTargetPort(new IntOrString(req.getTargetPort()))
                .endPort()
                .addToSelector("app", req.getName())
                .endSpec()
                .build();

        String yaml = Serialization.asYaml(service);

        try {
            Files.createDirectories(Path.of("deployments-anthos/" + req.getName()));
            Files.writeString(Path.of("deployments-anthos/"+ req.getName() +"/" + req.getName() + "-service.yaml"), yaml);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return yaml;
    }
}
