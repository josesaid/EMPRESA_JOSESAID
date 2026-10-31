package com.mx.josesaid.develop.nexus.application;

/**
 * Puerto del asistente. La aplicación depende de esta interfaz, no del proveedor de modelo.
 * El adaptador concreto vive en infrastructure.
 */
public interface AsistenteIA {

    RespuestaAsistente consultar(PreguntaAsistente pregunta);
}
