-- V3: la retención confirmada es CONTRATO 10 años, PERMISO 5 años, REGISTRO_LEGAL 10 años.
-- Los documentos cargados antes calcularon 5 años para CONTRATO y REGISTRO_LEGAL; se les suman 5 años más
-- (la fecha original era la carga + 5 años, así que queda carga + 10 años). Los PERMISO no cambian.
-- Los eliminados no se tocan: su retención ya venció y están fuera de alcance.

UPDATE documentos
   SET retencion_hasta = retencion_hasta + INTERVAL '5 years'
 WHERE tipo IN ('CONTRATO', 'REGISTRO_LEGAL')
   AND eliminado = FALSE;
