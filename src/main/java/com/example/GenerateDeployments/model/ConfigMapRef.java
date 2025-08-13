package com.example.GenerateDeployments.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfigMapRef {
    private String nameConfigMap;       // nombre del ConfigMap existente
    private String key;        // clave dentro del ConfigMap
    private String nameDeploy;    // nombre de la variable de entorno que expondrá

    public ConfigMapRef(String nameConfigMap, String key, String nameDeploy) {
        this.nameConfigMap = nameConfigMap;
        this.key = key;
        this.nameDeploy = nameDeploy;
    }
}
