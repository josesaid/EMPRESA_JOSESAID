package com.mx.josesaid.develop.nexus.application;

import java.util.List;

import com.mx.josesaid.develop.nexus.application.ContextoContable.CuentaCatalogo;
import com.mx.josesaid.develop.nexus.application.ContextoContable.PolizaEjemplo;

/** Propone una póliza a partir de una descripción, con pólizas históricas como referencia. */
public interface SugeridorPolizas {

    PolizaSugerida sugerir(String usuario, String descripcion, List<CuentaCatalogo> catalogo,
                           List<PolizaEjemplo> ejemplos);
}
