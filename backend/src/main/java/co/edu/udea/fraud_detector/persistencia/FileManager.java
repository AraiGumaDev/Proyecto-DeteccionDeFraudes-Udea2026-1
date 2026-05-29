package co.edu.udea.fraud_detector.persistencia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

// Maneja todas las operaciones de lectura/escritura sobre transacciones.dat
// usa RandomAccessFile con try-with-resources para no dejar el archivo abierto
@Slf4j
@Component
public class FileManager {

    private final String filePath;

    public FileManager(@Value("${app.datos.ruta:transacciones.dat}") String filePath) {
        this.filePath = filePath;
    }

    // borra el archivo y crea uno vacío, usado por el endpoint seed con forzar=true
    public void resetear() throws IOException {
        File f = new File(filePath);
        if (f.exists() && !f.delete()) {
            throw new IOException("No se pudo eliminar el archivo: " + filePath);
        }
        inicializar();
    }

    // crea el archivo si no existe; debe llamarse al arrancar antes de cualquier otra operación
    public void inicializar() throws IOException {
        File f = new File(filePath);
        if (!f.exists()) {
            boolean created = f.createNewFile();
            if (created) {
                log.info("Archivo creado: {}", f.getAbsolutePath());
            }
        } else {
            log.info("Archivo encontrado: {} ({} registros)", f.getAbsolutePath(), contarRegistros());
        }
    }

    // escribe 128 bytes al final del archivo y devuelve el número de registro asignado
    public int append(RegistroTransaccion registro) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            long fileLen = raf.length();
            int numRegistro = (int) (fileLen / RegistroTransaccion.RECORD_SIZE);
            raf.seek(fileLen);
            raf.write(registro.serializar());
            return numRegistro;
        }
    }

    // lee los 128 bytes del registro en la posición dada y asigna numRegistro
    public RegistroTransaccion read(int numRegistro) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek((long) numRegistro * RegistroTransaccion.RECORD_SIZE);
            byte[] buffer = new byte[RegistroTransaccion.RECORD_SIZE];
            raf.readFully(buffer);
            RegistroTransaccion r = RegistroTransaccion.deserializar(buffer);
            r.numRegistro = numRegistro;
            return r;
        }
    }

    // actualiza solo el byte de estado_alerta (offset 96) sin reescribir el registro completo
    public void updateEstadoAlerta(int numRegistro, byte estado) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek((long) numRegistro * RegistroTransaccion.RECORD_SIZE
                    + RegistroTransaccion.OFFSET_ESTADO_ALERTA);
            raf.writeByte(estado);
        }
    }

    // eliminación lógica: pone deleted=1 en offset 97, el registro queda en disco
    public void logicalDelete(int numRegistro) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek((long) numRegistro * RegistroTransaccion.RECORD_SIZE
                    + RegistroTransaccion.OFFSET_DELETED);
            raf.writeByte(1);
        }
    }

    // reescribe los 128 bytes completos del registro en su posición original
    public void updateRegistro(int numRegistro, RegistroTransaccion registro) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek((long) numRegistro * RegistroTransaccion.RECORD_SIZE);
            raf.write(registro.serializar());
        }
    }

    // lee todos los registros del archivo (activos y eliminados), asignando numRegistro por posición
    public List<RegistroTransaccion> loadAll() throws IOException {
        List<RegistroTransaccion> registros = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists() || f.length() == 0) return registros;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            byte[] buffer = new byte[RegistroTransaccion.RECORD_SIZE];
            int numRegistro = 0;
            while (raf.getFilePointer() < raf.length()) {
                raf.readFully(buffer);
                RegistroTransaccion r = RegistroTransaccion.deserializar(buffer);
                r.numRegistro = numRegistro++;
                registros.add(r);
            }
        }
        return registros;
    }

    public int contarRegistros() throws IOException {
        File f = new File(filePath);
        if (!f.exists()) return 0;
        return (int) (f.length() / RegistroTransaccion.RECORD_SIZE);
    }

    public boolean isEmpty() throws IOException {
        File f = new File(filePath);
        return !f.exists() || f.length() == 0;
    }

    public String getFilePath() {
        return filePath;
    }
}
