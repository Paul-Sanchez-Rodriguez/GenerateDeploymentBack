package com.example.GenerateDeployments.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SecretRef {
    private String nameSecret;       // nombre del Secret existente
    private String key;        // clave dentro del Secret
    private String nameDeploy;    // nombre de la variable de entorno que expondrá

    public SecretRef(String nameSecret, String key, String nameDeploy) {
        this.nameSecret = nameSecret;
        this.key = key;
        this.nameDeploy = nameDeploy;
    }
}
