/**
 * Elemento que puede vivir en la pila del analizador.
 * Puede ser un terminal (Token) o un no-terminal (String).
 */
public class SimboloPila {

    private final Token token;     // no-null si es terminal
    private final String nombre;   // nombre del símbolo (tipo token o no-terminal)

    /** Crea un símbolo terminal a partir de un token. */
    public SimboloPila(Token token) {
        this.token  = token;
        this.nombre = token.getTipo().name();
    }

    /** Crea un símbolo no-terminal. */
    public SimboloPila(String noTerminal) {
        this.token  = null;
        this.nombre = noTerminal;
    }

    public boolean esTerminal()   { return token != null; }
    public Token   getToken()     { return token; }
    public String  getNombre()    { return nombre; }
    public int     getLinea()     { return token != null ? token.getLinea() : 0; }
    public int     getColumna()   { return token != null ? token.getColumnaInicio() : 0; }

    @Override
    public String toString() {
        return token != null ? nombre + "(" + token.getLexema() + ")" : nombre;
    }
}
