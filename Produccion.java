//guarda una regla de la gramática para que el parser la consulte al momento de reducir.
public class Produccion {

    private final int numero; //numero de la producion 
    private final String cabeza; //lado izquierdo de la produccion
    private final String[] cuerpo; //lado derecho de la produccion 

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