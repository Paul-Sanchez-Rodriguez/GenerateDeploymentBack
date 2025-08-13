package com.example.GenerateDeployments.controller;

import com.example.GenerateDeployments.model.ConfigMapRequest;
import com.example.GenerateDeployments.model.DeploymentRequest;
import com.example.GenerateDeployments.model.SecretRequest;
import com.example.GenerateDeployments.model.ServiceRequest;
import com.example.GenerateDeployments.service.GenerateConfig;
import com.example.GenerateDeployments.service.GenerateDeployments;
import com.example.GenerateDeployments.service.GenerateSecrets;
import com.example.GenerateDeployments.service.GenerateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deployments/anthos")
public class DeploymentController {

    @Autowired
    private GenerateDeployments deploymentService;

    @Autowired
    private GenerateConfig generateConfig;

    @Autowired
    private GenerateSecrets generateSecrets;

    @Autowired GenerateService generateService;

    @PostMapping(value = "/deployments")
    public ResponseEntity<String> generateDeployment(@RequestBody DeploymentRequest request) {
        String path = deploymentService.generateDeploymentYaml(request);
        return ResponseEntity.ok("Archivo generado en: " + path);
    }

    @PostMapping(value = "/configMap")
    public String generateConfigMap(@RequestBody ConfigMapRequest request) {
        return generateConfig.generateConfigMapYaml(request);
    }

    @PostMapping(value = "/secret")
    public String generateSecret(@RequestBody SecretRequest request) {
        return generateSecrets.generateSecretYaml(request);
    }

    @PostMapping(value = "/service")
    public String generateService(@RequestBody ServiceRequest request) {       
        return generateService.generateServiceYaml(request);
    }
}
