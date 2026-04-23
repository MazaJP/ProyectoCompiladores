//representa cada uno de los token identificados guardando 4 cosas importanes
public class Token {

    private TokenType tipo;
    private String lexema;
    private int linea;
    private int columnaInicio;
    private int columnaFin;

    public Token(TokenType tipo, String lexema, int linea, int columnaInicio, int columnaFin) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columnaInicio = columnaInicio;
        this.columnaFin = columnaFin;
    }

    public TokenType getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinea() { return linea; }
    public int getColumnaInicio() { return columnaInicio; }
    public int getColumnaFin() { return columnaFin; }

    @Override
    public String toString() {
        return "(" + linea + ", " + columnaInicio + "-" + columnaFin + ") " + tipo +
               (lexema.isEmpty() ? "" : " " + lexema);
    }
}