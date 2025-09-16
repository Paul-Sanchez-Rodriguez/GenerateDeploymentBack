package com.example.GenerateDeployments.service.gke;

import com.example.GenerateDeployments.model.*;
import io.fabric8.kubernetes.api.model.*;
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
public class GenerateDeploymetsGKE {

    public String generateDeploymentYamlCore(DeploymentRequestCore deployments) {

        List<EnvVar> envVars = new ArrayList<>();
        List<VolumeMount> volumeMounts = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();

        for (VolumeSpec vol : deployments.getVolumeSpecs()) {
            volumeMounts.add(new VolumeMountBuilder()
                    .withName(vol.getName())
                    .withMountPath(vol.getMountPath())
                    .withSubPath(vol.getSubPath())
                    .build());
            VolumeBuilder volBuilder = new VolumeBuilder().withName(vol.getName());

            switch (vol.getType()) {
                case "configMap":
                    volBuilder.withNewConfigMap().withName(vol.getName()).withDefaultMode(420).endConfigMap();
                    break;
                case "secret":
                    volBuilder.withNewSecret().withSecretName(vol.getName()).endSecret();
                    break;
                case "emptyDir":
                    volBuilder.withNewEmptyDir().endEmptyDir();
                    break;
                // otros tipos según lo que necesites
                default:
                    throw new IllegalArgumentException("Tipo de volumen no soportado: " + vol.getType());
            }

            volumes.add(volBuilder.build());
        }

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
                    .withLabels(Collections.singletonMap("name", deployments.getName()))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(deployments.getReplicas())
                    .withNewSelector()
                        .addToMatchLabels("name", deployments.getName())
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("name", deployments.getName())
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(deployments.getName())
                                .withImage("us-central1-docker.pkg.dev/foh-gke-des/foh-docker-dev/" + deployments.getImage() + ":latest")
                                .withEnv(envVars)
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_HOST")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
                                            .withKey("RABBITMQ_TRANSACTION_HOST")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_PORT")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
                                            .withKey("RABBITMQ_TRANSACTION_PORT")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_USR")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
                                            .withKey("RABBITMQ_TRANSACTION_USR")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_PWD")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
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
                                    .addToLimits("memory", new Quantity("900Mi"))
                                .endResources()
                                .withVolumeMounts(volumeMounts)
                                .addNewVolumeMount()
                                    .withName("secret-volume")
                                    .withMountPath("/etc/ssl/certs/gkefoh.pfx")
                                    .withSubPath("gkefoh.pfx")
                                    .withReadOnly(true)
                                .endVolumeMount()
                            .endContainer()
                            .withVolumes(volumes)
                            .addNewContainer()
                                .withName("alloydb-proxy")
                                .withImage("us-central1-docker.pkg.dev/foh-gke-des/foh-docker-dev/fohalloydbproxy:latest")
                                .withCommand("/alloydb-auth-proxy")
                                .withArgs(
                                    "--credentials-file=/secrets/service_account.json",
                                    "--psc",
                                    "projects/foh-gke-des/locations/us-central1/clusters/"+ deployments.getCluster() +"/instances/"+ deployments.getInstance(),
                                    "--port=5432"
                                )
                                .withNewResources()
                                    .addToRequests("cpu", new Quantity("10m"))
                                    .addToRequests("memory", new Quantity("10Mi"))
                                .endResources()
                                .addNewVolumeMount()
                                    .withName("secret-volume1")
                                    .withMountPath("/secrets")
                                    .withReadOnly(true)
                                .endVolumeMount()
                            .endContainer()
                            .addNewVolume()
                                .withName("secret-volume1")
                                .withNewSecret()
                                    .withSecretName("gke-client-sql-fohct23")
                                    .withDefaultMode(420)
                                .endSecret()
                            .endVolume()
                            .addNewVolume()
                                .withName("secret-volume")
                                .withNewSecret()
                                    .withSecretName("gkefinancieraoh-local-secret")
                                    .withDefaultMode(420)
                                .endSecret()
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        String yaml = Serialization.asYaml(deployment);

        String filePath = "deployments-GKE/"+ deployments.getName() + "/" + deployments.getName() + ".yaml";

        try {
            Files.createDirectories(Path.of("deployments-GKE/" + deployments.getName()));
            Files.write(Path.of(filePath), yaml.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir archivo YAML", e);
        }

        return filePath;
    }



    public String generateDeploymentYamlGeneric(DeploymentRequest deployments) {

        List<EnvVar> envVars = new ArrayList<>();
        List<VolumeMount> volumeMounts = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();

        for (VolumeSpec vol : deployments.getVolumeSpecs()) {
            volumeMounts.add(new VolumeMountBuilder()
                    .withName(vol.getName())
                    .withMountPath(vol.getMountPath())
                    .withSubPath(vol.getSubPath())
                    .build());
            VolumeBuilder volBuilder = new VolumeBuilder().withName(vol.getName());

            switch (vol.getType()) {
                case "configMap":
                    volBuilder.withNewConfigMap().withName(vol.getName()).withDefaultMode(420).endConfigMap();
                    break;
                case "secret":
                    volBuilder.withNewSecret().withSecretName(vol.getName()).endSecret();
                    break;
                case "emptyDir":
                    volBuilder.withNewEmptyDir().endEmptyDir();
                    break;
                // otros tipos según lo que necesites
                default:
                    throw new IllegalArgumentException("Tipo de volumen no soportado: " + vol.getType());
            }

            volumes.add(volBuilder.build());
        }

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
                    .withLabels(Collections.singletonMap("name", deployments.getName()))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(deployments.getReplicas())
                    .withNewSelector()
                        .addToMatchLabels("name", deployments.getName())
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("name", deployments.getName())
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(deployments.getName())
                                .withImage("us-central1-docker.pkg.dev/foh-gke-des/foh-docker-dev/" + deployments.getImage() + ":latest")
                                .withEnv(envVars)
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_HOST")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
                                            .withKey("RABBITMQ_TRANSACTION_HOST")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_PORT")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
                                            .withKey("RABBITMQ_TRANSACTION_PORT")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_USR")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
                                            .withKey("RABBITMQ_TRANSACTION_USR")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                                .addNewEnv()
                                    .withName("RABBITMQ_TRANSACTION_PWD")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("rabbitmq-cred")
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
                                    .addToLimits("memory", new Quantity("900Mi"))
                                .endResources()
                                .withVolumeMounts(volumeMounts)
                                .addNewVolumeMount()
                                    .withName("secret-volume")
                                    .withMountPath("/etc/ssl/certs/gkefoh.pfx")
                                    .withSubPath("gkefoh.pfx")
                                .endVolumeMount()
                            .endContainer()
                            .withVolumes(volumes)
                            .addNewVolume()
                                .withName("secret-volume")
                                .withNewSecret()
                                    .withSecretName("gkefinancieraoh-local-secret")
                                    .withDefaultMode(420)
                                .endSecret()
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        String yaml = Serialization.asYaml(deployment);

        String filePath = "deployments-GKE/"+ deployments.getName() + "/" + deployments.getName() + ".yaml";

        try {
            Files.createDirectories(Path.of("deployments-GKE/" + deployments.getName()));
            Files.write(Path.of(filePath), yaml.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir archivo YAML", e);
        }

        return filePath;
    }
}
