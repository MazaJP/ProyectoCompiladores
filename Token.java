public class Token {

    private TokenType tipo;
    private String lexema;
    private int linea;
    private int columnaInicio;
    private int columnaFin;

    public Token(TokenType tipo, String lexema, int linea,
                 int columnaInicio, int columnaFin) {

        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columnaInicio = columnaInicio;
        this.columnaFin = columnaFin;
    }

    @Override
    public String toString() {
        return "(" + linea + ", " +
                columnaInicio + "-" + columnaFin + ") " +
                tipo + " " + lexema;
    }
}