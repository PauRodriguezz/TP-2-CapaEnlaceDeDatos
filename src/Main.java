import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final int TAMANO_MARCO = 10; // Tamaño máximo de los marcos
    private static final int TAMANO_VENTANA = 3; // Tamaño de la ventana deslizante (cuántos marcos enviar sin esperar ACK)
    private static Set<String> mensajesEnviados = new HashSet<>(); // Almacena los contenidos de los mensajes enviados
    private static int numeroSecuenciaActual = 0; // Lleva el control de la secuencia actual global

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Ingrese el mensaje a transmitir (o escriba 'salir' para terminar):");
            String mensaje = scanner.nextLine();

            if (mensaje.equalsIgnoreCase("salir")) {
                break; // Salir del bucle si el usuario escribe "salir"
            }

            // Verificar si el mensaje ya fue enviado
            if (mensajesEnviados.contains(mensaje)) {
                System.out.println("El mensaje '" + mensaje + "' ya fue enviado anteriormente. Se omite.");
                continue; // Omitir el envío si el mensaje ya fue transmitido
            }

            // Convertir el mensaje a bytes
            byte[] datos = mensaje.getBytes();

            // Ejecutar el protocolo de capa de enlace
            boolean exitoEnvio = protocoloCapaEnlace(datos);

            // Si se envió correctamente, registrar el mensaje
            if (exitoEnvio) {
                mensajesEnviados.add(mensaje); // Guardar solo si el envío fue exitoso
                System.out.println("El mensaje se envió con éxito y se ha registrado.");
            } else {
                System.out.println("No se pudo enviar el mensaje correctamente. No se ha registrado.");
            }
        }

        // Cerrar el scanner
        scanner.close();
    }

    private static boolean protocoloCapaEnlace(byte[] datos) {
        List<Marco> marcos = dividirDatosEnMarcos(datos);
        int marcosEnviados = 0;

        while (marcosEnviados < marcos.size()) {
            System.out.println("\nVentana actual: Enviando marcos del " + marcosEnviados + " al " + Math.min(marcosEnviados + TAMANO_VENTANA - 1, marcos.size() - 1));

            boolean envioExitoso = true; // Resetear para cada ventana
            for (int i = 0; i < TAMANO_VENTANA && (marcosEnviados + i) < marcos.size(); i++) {
                Marco marco = marcos.get(marcosEnviados + i);
                enviarMarco(marco);

                System.out.println("Esperando ACK para el marco " + marco.getNumeroSecuencia() + "...");

                // Esperar ACK para el marco
                if (esperarACKConTiempoDeEspera(marco)) {
                    System.out.println("ACK recibido para el marco " + marco.getNumeroSecuencia() + "\n");
                } else {
                    System.out.println("Error en el marco " + marco.getNumeroSecuencia() + ". Se retransmite.");
                    enviarMarco(marco);
                    System.out.println("Esperando ACK para el marco retransmitido " + marco.getNumeroSecuencia() + "...");

                    // Esperar ACK nuevamente
                    if (esperarACKConTiempoDeEspera(marco)) {
                        System.out.println("ACK recibido para el marco retransmitido " + marco.getNumeroSecuencia() + "\n");
                    } else {
                        System.out.println("Error continuo en el marco " + marco.getNumeroSecuencia() + ". Retransmisión fallida.");
                        envioExitoso = false; // Marcar el envío como fallido
                        break; // Salir del bucle si hay fallo
                    }
                }
            }

            if (!envioExitoso) {
                System.out.println("Error: No se pudo enviar el mensaje correctamente. Se agotaron los intentos de retransmisión.\n");
                return false; // Salir si hubo un fallo en el envío}

            }

            // Incrementar el número de secuencia después de confirmar los ACKs
            marcosEnviados += TAMANO_VENTANA;
            System.out.println("Deslizando la ventana...\n");
        }

        return true; // Si se completó el bucle sin errores, devuelve true
    }


    private static boolean esperarACKConTiempoDeEspera(Marco marco) {
        int tiempoEspera = 2000; // 2 segundos
        long tiempoInicio = System.currentTimeMillis();
        int intentos = 0; // Contador de intentos

        while (System.currentTimeMillis() - tiempoInicio < tiempoEspera) {
            if (recibirACK()) {
                return true; // ACK recibido antes de que se acabe el tiempo
            }

            intentos++;
            // Pausa breve para evitar un uso intensivo de la CPU
            try {
                Thread.sleep(100); // Espera 100 ms antes de volver a verificar
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false; // Manejo de interrupciones
            }
        }

        System.out.println("No se recibió el ACK después de " + intentos + " intentos. Se considerará un error.");
        return false; // Se acabó el tiempo y no se recibió el ACK
    }



    private static List<Marco> dividirDatosEnMarcos(byte[] datos) {
        List<Marco> marcos = new ArrayList<>();
        int cantidadDeMarcos = (int) Math.ceil((double) datos.length / TAMANO_MARCO);
        int secuenciaLocal = numeroSecuenciaActual;  // Variable local para la secuencia

        for (int i = 0; i < cantidadDeMarcos; i++) {
            int inicio = i * TAMANO_MARCO;
            int fin = Math.min(inicio + TAMANO_MARCO, datos.length);
            byte[] datosDelMarco = Arrays.copyOfRange(datos, inicio, fin);

            // Crear un nuevo marco con la secuencia local
            Marco marco = new Marco("direccionRemitente", "direccionReceptor", secuenciaLocal++, datosDelMarco);
            marcos.add(marco);
        }

        return marcos;
    }


    private static void enviarMarco(Marco marco) {
        System.out.println("Enviando marco: " +
                "Seq: " + marco.getNumeroSecuencia() +
                ", Datos: " + new String(marco.getDatos()) +
                ", Pie: " + marco.getPie());
    }

    private static boolean recibirACK() {
        // Simular un tiempo de espera para recibir un ACK
        try {
            // Simula un tiempo de espera de hasta 1 segundo (ajusta según sea necesario)
            Thread.sleep((long) (Math.random() * 1000)); // Espera aleatoria entre 0 y 1 segundo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false; // Manejo de interrupciones
        }

        // Simulación de la recepción de un ACK
        double probabilidadExito = new Random().nextDouble();
        if (probabilidadExito < 0.3) { // 70% de éxito
            return true; // ACK recibido
        } else {
            return false; // ACK no recibido
        }
    }

}