package com.example.GenerateDeployments.service;

import com.example.GenerateDeployments.model.ConfigMapRef;
import com.example.GenerateDeployments.model.DeploymentRequest;
import com.example.GenerateDeployments.model.SecretRef;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class GenerateDeployments {

    public String generateDeploymentYaml(DeploymentRequest deployments) {
        List<EnvVar> envVars = new ArrayList<>();

        if (deployments.getEnviroments() != null) {
            for (Map.Entry<String, String> e : deployments.getEnviroments().entrySet()) {
                envVars.add(new EnvVar(e.getKey(), e.getValue(), null));
            }
        }

        // Desde ConfigMap como env
        if (deployments.getConfigMapRefs() != null) {
            for (ConfigMapRef cm : deployments.getConfigMapRefs()) {
                envVars.add(new EnvVar(cm.getNameDeploy(), null,
                        new EnvVarSourceBuilder()
                                .withConfigMapKeyRef(new ConfigMapKeySelectorBuilder()
                                        .withName(cm.getNameConfigMap())
                                        .withKey(cm.getKey())
                                        .build())
                                .build()));
            }
        }

        // Desde Secret como env
        if (deployments.getSecretRefs() != null) {
            for (SecretRef secret : deployments.getSecretRefs()) {
                envVars.add(new EnvVar(secret.getNameDeploy(), null,
                        new EnvVarSourceBuilder()
                                .withSecretKeyRef(new SecretKeySelectorBuilder()
                                        .withName("secret-" + secret.getNameSecret())
                                        .withKey(secret.getKey())
                                        .build())
                                .build()));
            }
        }

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(deployments.getName())
                    .withNamespace(deployments.getNamespace())
                    .withLabels(Collections.singletonMap("app", deployments.getName()))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(deployments.getReplicas())
                    .withNewSelector()
                        .addToMatchLabels("app", deployments.getName())
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", deployments.getName())
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(deployments.getName())
                                .withImage(deployments.getImage())
                                .withEnv(envVars)
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_HOST")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("secret-rabbit")
                                            .withKey("RABBITMQ_TRANSACTION_HOST")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_PORT")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("secret-rabbit")
                                            .withKey("RABBITMQ_TRANSACTION_PORT")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_USR")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("secret-rabbit")
                                            .withKey("RABBITMQ_TRANSACTION_USR")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_PWD")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("secret-rabbit")
                                            .withKey("RABBITMQ_TRANSACTION_PWD")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewPort()
                                    .withContainerPort(8080)
                                    .withProtocol("TCP")
                                .endPort()
                                .withNewResources()
                                    .addToRequests("cpu", new Quantity("10m"))
                                    .addToRequests("memory", new Quantity("10Mi"))
                                    .addToLimits("cpu", new Quantity("500m"))
                                    .addToLimits("memory", new Quantity("1Gi"))
                                .endResources()
                            .endContainer()
                            .withRestartPolicy("Always")
                            .addNewImagePullSecret()
                                .withName("registry-secret")
                            .endImagePullSecret()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        String yaml = Serialization.asYaml(deployment);

        String filePath = "deployments-anthos/" + deployments.getName() + "/" + deployments.getName() + ".yaml";

        try {
            Files.createDirectories(Path.of("deployments-anthos/"+ deployments.getName()));
            Files.write(Path.of(filePath), yaml.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir archivo YAML", e);
        }

        return filePath;
    }
}
