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
            protocoloCapaEnlace(datos);

            // Registrar el mensaje enviado
            mensajesEnviados.add(mensaje); // Guardar el mensaje en el set para evitar duplicados
        }

        // Cerrar el scanner
        scanner.close();
    }

    private static void protocoloCapaEnlace(byte[] datos) {
        List<Marco> marcos = dividirDatosEnMarcos(datos);
        int ventanaInicio = 0;  // Inicio de la ventana deslizante
        int marcosEnviados = 0; // Contador de marcos enviados en la ventana

        while (marcosEnviados < marcos.size()) {
            // Mostrar ventana actual
            System.out.println("\nVentana actual: Enviando marcos del " + marcosEnviados + " al " + Math.min(marcosEnviados + TAMANO_VENTANA - 1, marcos.size() - 1));

            // Enviar marcos dentro de la ventana
            for (int i = 0; i < TAMANO_VENTANA && (marcosEnviados + i) < marcos.size(); i++) {
                Marco marco = marcos.get(marcosEnviados + i);
                enviarMarco(marco);

                // Esperar ACK para el marco
                System.out.println("Esperando ACK para el marco " + numeroSecuenciaActual + "...");
                if (esperarACKConTiempoDeEspera(marco)) {
                    System.out.println("ACK recibido para el marco " + numeroSecuenciaActual + "\n");
                    numeroSecuenciaActual++; // Incrementar número de secuencia solo después de recibir el ACK
                } else {
                    System.out.println("Error en el marco " + numeroSecuenciaActual + ". Se retransmite.");
                    enviarMarco(marco); // Retransmitir marco si hubo error
                    System.out.println("Esperando ACK para el marco retransmitido " + numeroSecuenciaActual + "...");
                    // Esperar el ACK para el marco retransmitido
                    if (esperarACKConTiempoDeEspera(marco)) {
                        System.out.println("ACK recibido para el marco retransmitido " + numeroSecuenciaActual + "\n");
                        numeroSecuenciaActual++;
                    }
                }
            }

            // Avanzar la ventana si todos los ACKs fueron recibidos
            marcosEnviados += TAMANO_VENTANA;
            ventanaInicio = marcosEnviados; // Ajusta el inicio de la ventana

            // Verificar si ya no hay más marcos por enviar
            if (ventanaInicio >= marcos.size()) {
                System.out.println("Se enviaron todos los marcos. El mensaje se envió con éxito.\n");
            } else {
                System.out.println("Deslizando la ventana...\n");
            }
        }
    }

    private static boolean esperarACKConTiempoDeEspera(Marco marco) {
        // Simulación de la recepción de un ACK con un tiempo de espera (ej. 2 segundos)
        int tiempoEspera = 2000; // 2 segundos
        long tiempoInicio = System.currentTimeMillis();

        while (System.currentTimeMillis() - tiempoInicio < tiempoEspera) {
            if (recibirACK()) {
                return true; // ACK recibido antes de que se acabe el tiempo
            }

            // Pausa breve para evitar un uso intensivo de la CPU
            try {
                Thread.sleep(100); // Espera 100 ms antes de volver a verificar
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false; // Manejo de interrupciones
            }
        }

        return false; // Se acabó el tiempo y no se recibió el ACK
    }

    private static List<Marco> dividirDatosEnMarcos(byte[] datos) {
        List<Marco> marcos = new ArrayList<>();
        int cantidadDeMarcos = (int) Math.ceil((double) datos.length / TAMANO_MARCO);

        for (int i = 0; i < cantidadDeMarcos; i++) {
            int inicio = i * TAMANO_MARCO;
            int fin = Math.min(inicio + TAMANO_MARCO, datos.length);
            byte[] datosDelMarco = Arrays.copyOfRange(datos, inicio, fin);

            // Crear un nuevo marco con la secuencia actual
            Marco marco = new Marco("direccionRemitente", "direccionReceptor", numeroSecuenciaActual++, datosDelMarco);
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
        // Simulación de la recepción de un ACK con temporizador
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Future<Boolean> future = executor.submit(() -> {
            // Simula el tiempo de espera para recibir un ACK (por ejemplo, 2 segundos)
            Thread.sleep(2000);
            // Simula un ACK recibido con 50% de probabilidad
            return new Random().nextBoolean();
        });

        try {
            // Esperar hasta 2 segundos para recibir un ACK
            return future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Si el tiempo se excede, consideramos que hubo un error
            System.out.println("Tiempo excedido. Retransmitiendo el marco.");
            return false;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        } finally {
            executor.shutdown();
        }
    }
}
