package com.example.GenerateDeployments.controller;

import com.example.GenerateDeployments.model.DeploymentRequest;
import com.example.GenerateDeployments.model.DeploymentRequestCore;
import com.example.GenerateDeployments.model.ServiceRequest;
import com.example.GenerateDeployments.service.gke.GenerateDeploymetsGKE;
import com.example.GenerateDeployments.service.gke.GenerateServiceGke;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deployments/gke")
public class DeploymentControllerGKE {

    @Autowired
    private GenerateDeploymetsGKE generateDeploymetsGKE;

    @Autowired
    private GenerateServiceGke generateServiceGke;

    private final Path yamlFolder = Paths.get("deployments-GKE/");

    @PostMapping("/deployments/core")
    public ResponseEntity<String> crearDeploymentCore(@RequestBody DeploymentRequestCore request) {
        generateDeploymetsGKE.generateDeploymentYamlCore(request);
        return ResponseEntity.ok("Deployment Core creado exitosamente.");
    }

    @PostMapping("/deployments/generico")
    public ResponseEntity<String> crearDeploymentGeneric(@RequestBody DeploymentRequest request) {
        System.out.println();
        generateDeploymetsGKE.generateDeploymentYamlGeneric(request);
        return ResponseEntity.ok("Deployment Generico creado exitosamente.");
    }

    @PostMapping("/service")
    public ResponseEntity<String> generateService(@RequestBody ServiceRequest request) {  
        generateServiceGke.generateServiceGkeYaml(request);     
        return ResponseEntity.ok("Service creado exitosamente.");
    }

    @GetMapping("/{filename}")
    public Object readYamlAsJson(@PathVariable String filename) throws IOException {
        Path filePath = yamlFolder.resolve(filename + "/" + filename + ".yaml");

        System.out.println("ruta en la que estamos buscando: " + filePath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Archivo no encontrado: " + filename);
        }
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap;
        try (var inputStream = Files.newInputStream(filePath)) {
            //Object data = yaml.load(inputStream);
            yamlMap = yaml.load(inputStream);
            //return data;
        }

        List<Map<String, Object>> envList = JsonPath.read(yamlMap, "$.spec.template.spec.containers[0].env");

        List<Map<String, String>> simples = new ArrayList<>();
        List<Map<String, Object>> secrets = new ArrayList<>();
        List<Map<String, Object>> configs = new ArrayList<>();

        for (Map<String, Object> env : envList) {
            if (env.containsKey("value")) {
                simples.add(Map.of(env.get("name").toString(), env.get("value").toString()));
            } else if (env.containsKey("valueFrom")) {
                Map<String, Object> valueFrom = (Map<String, Object>) env.get("valueFrom");
                if (valueFrom.containsKey("secretKeyRef")) {
                    secrets.add(Map.of(
                            "name", env.get("name").toString(),
                            "secretKeyRef", valueFrom.get("secretKeyRef")
                    ));
                } else if (valueFrom.containsKey("configMapKeyRef")) {
                    configs.add(Map.of(
                            "name", env.get("name").toString(),
                            "configMapKeyRef", valueFrom.get("configMapKeyRef")
                    ));
                }
            }
    }
        return Map.of(
                "simples", simples,
                "configs", configs,
                "secrets", secrets
        );
    }
}
