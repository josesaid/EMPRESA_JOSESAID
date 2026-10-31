package com.mx.josesaid.develop.nexus.application;

/** Puerto de auditoría: registra cada consulta. La implementación vive en infrastructure. */
public interface AuditoriaConsultas {

    void registrar(RegistroConsulta registro);
}
