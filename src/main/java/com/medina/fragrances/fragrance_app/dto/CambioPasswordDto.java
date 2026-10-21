package com.medina.fragrances.fragrance_app.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class CambioPasswordDto {

    @NotEmpty(message = "La contrasena actual es obligatoria")
    private String passwordActual;

    @NotEmpty(message = "La nueva contrasena es obligatoria")
    @Size(min = 6, message = "La nueva contrasena debe tener al menos 6 caracteres")
    private String passwordNueva;

    @NotEmpty(message = "Debes confirmar la nueva contrasena")
    private String confirmarPassword;

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public String getPasswordNueva() {
        return passwordNueva;
    }

    public void setPasswordNueva(String passwordNueva) {
        this.passwordNueva = passwordNueva;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }
}