
//Elementos que puede estar en la pila del analizador pueden ser un terminal (Token) o un no-terminal (String).
public class SimboloPila {

    private final Token token;     //si no es null es un terminal, pero si es null es un no terminal
    private final String nombre;   // nombre del símbolo

    // Crea un símbolo terminal a partir del lexer
    public SimboloPila(Token token) {
        this.token  = token;
        this.nombre = token.getTipo().name();
    }

    // Crea un símbolo no-terminal siendo el resultado de la reduccion .
    public SimboloPila(String noTerminal) {
        this.token  = null;
        this.nombre = noTerminal;
    }
// si el token no es null regresa true
    public boolean esTerminal()  
    { 
        return token != null;
    }
    //devuelve el token
    public Token   getToken()     
    { 
        return token; 
    }
    //devuelve el nombre del simbolo
    public String  getNombre()    
    { 
        return nombre; 
    }
    //devuelve la liena del token pero si es 0 si es un no terminal
    public int     getLinea()     
    { 
        return token != null ? token.getLinea() : 0; 
    }
    //columna del token 
    public int     getColumna()   
    { 
        return token != null ? token.getColumnaInicio() : 0; 
    }

    @Override
    public String toString() {
        return token != null ? nombre + "(" + token.getLexema() + ")" : nombre;
    }
}
