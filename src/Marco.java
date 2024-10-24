public class Marco {
    private String direccionRemitente;
    private String direccionReceptor;
    private int numeroSecuencia;
    private byte[] datos;
    private String pie;

    // Constructor
    public Marco(String direccionRemitente, String direccionReceptor, int numeroSecuencia, byte[] datos) {
        this.direccionRemitente = direccionRemitente;
        this.direccionReceptor = direccionReceptor;
        this.numeroSecuencia = numeroSecuencia;
        this.datos = datos;
        this.pie = calcularCRC(datos); // Calcular CRC para el pie
    }

    // Métodos para obtener la información del marco
    public String getDireccionRemitente() {
        return direccionRemitente;
    }

    public String getDireccionReceptor() {
        return direccionReceptor;
    }

    public int getNumeroSecuencia() {
        return numeroSecuencia;
    }

    public byte[] getDatos() {
        return datos;
    }

    public String getPie() {
        return pie;
    }

    // Método para calcular un CRC simple (simulación)
    private String calcularCRC(byte[] datos) {
        return "CRC_" + datos.length; // Simulación de un CRC basado en la longitud
    }
}
