package com.mx.josesaid.develop.documentos.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentoTest {

    private static final UUID ID = UUID.randomUUID();
    private static final LocalDate HOY = LocalDate.of(2026, 1, 1);
    private static final Instant AHORA = Instant.parse("2026-01-01T00:00:00Z");

    private static Documento contrato() {
        return Documento.crear(ID, TipoDocumento.CONTRATO, "Contrato marco", "Administración y legal",
                null, HOY, "hash1", "ref1", AHORA);
    }

    private static Documento permiso(LocalDate venceEn) {
        return Documento.crear(ID, TipoDocumento.PERMISO, "Permiso ambiental", "Operaciones",
                venceEn, HOY, "hash1", "ref1", AHORA);
    }

    @Test
    void creaVersionUnoYRetencionSegunTipo() {
        Documento d = contrato();
        assertEquals(1, d.getVersionActual());
        assertEquals(LocalDate.of(2036, 1, 1), d.getRetencionHasta());
    }

    @Test
    void unPermisoRequiereFechaDeVencimiento() {
        assertThrows(ReglaDeNegocioException.class, () -> permiso(null));
    }

    @Test
    void unaNuevaVersionIncrementaElNumero() {
        Documento d = contrato();
        VersionDocumento v = d.agregarVersion("hash2", "ref2", HOY, AHORA);
        assertEquals(2, v.numero());
        assertEquals(2, d.getVersionActual());
    }

    @Test
    void elMismoContenidoNoCreaVersion() {
        Documento d = contrato();
        assertThrows(ConflictoException.class, () -> d.agregarVersion("hash1", "ref2", HOY, AHORA));
    }

    @Test
    void unDocumentoVencidoNoRecibeVersiones() {
        Documento p = permiso(LocalDate.of(2026, 3, 1));
        assertThrows(ConflictoException.class,
                () -> p.agregarVersion("hash2", "ref2", LocalDate.of(2026, 4, 1), AHORA));
    }

    @Test
    void eliminarDentroDeRetencionDevuelveConflicto() {
        assertThrows(ConflictoException.class,
                () -> contrato().eliminar("Solicitud", LocalDate.of(2030, 1, 1), AHORA));
    }

    @Test
    void eliminarRequiereMotivo() {
        assertThrows(SolicitudInvalidaException.class,
                () -> contrato().eliminar("   ", LocalDate.of(2040, 1, 1), AHORA));
    }

    @Test
    void eliminarDespuesDeLaRetencionMarcaElDocumento() {
        Documento d = contrato();
        d.eliminar("Fin de retención", LocalDate.of(2037, 1, 1), AHORA);
        assertTrue(d.isEliminado());
    }

    @Test
    void unPermisoPorVencerGeneraAlertaUnaSolaVez() {
        Documento p = permiso(HOY.plusDays(10));
        assertTrue(p.necesitaAlertaVencimiento(HOY, 30));
        p.registrarAlertaVencimiento();
        assertFalse(p.necesitaAlertaVencimiento(HOY, 30));
    }

    @Test
    void unContratoNoGeneraAlertaDeVencimiento() {
        assertFalse(contrato().necesitaAlertaVencimiento(HOY, 30));
    }

    @Test
    void laRetencionVencidaSeMarcaParaRevisionUnaVez() {
        Documento d = contrato();
        LocalDate despues = LocalDate.of(2037, 1, 1);
        assertTrue(d.necesitaRevisionRetencion(despues));
        d.marcarRevisionRetencion();
        assertFalse(d.necesitaRevisionRetencion(despues));
    }
}
