/**
 * Representa una producción de la gramática de MiniLang.
 * Forma: cabeza → cuerpo[0] cuerpo[1] ... cuerpo[n]
 */
public class Produccion {

    private final int numero;
    private final String cabeza;
    private final String[] cuerpo;

    public Produccion(int numero, String cabeza, String... cuerpo) {
        this.numero = numero;
        this.cabeza = cabeza;
        this.cuerpo = cuerpo;
    }

    public int getNumero()   { return numero; }
    public String getCabeza(){ return cabeza; }
    public String[] getCuerpo(){ return cuerpo; }

    @Override
    public String toString() {
        return "P" + numero + ": " + cabeza + " → " + String.join(" ", cuerpo);
    }
}