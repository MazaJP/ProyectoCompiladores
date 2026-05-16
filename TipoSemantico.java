/**
 * Representa los tipos de dato que maneja MiniLang.
 * Se usa en la tabla de símbolos y en la comprobación de tipos.
 */
public enum TipoSemantico {
    INT,        // número entero
    FLOAT,      // número decimal (en MiniLang se llama float)
    STRING,     // cadena de texto
    BOOL,       // valor booleano (true / false)
    VOID,       // sin valor de retorno (funciones void)
    ERROR;      // tipo inválido, resultado de una operación incorrecta

    /**
     * Convierte el nombre del TokenType al TipoSemantico correspondiente.
     * Por ejemplo "INT" → INT, "FLOAT" → FLOAT.
     */
    public static TipoSemantico desdeCadena(String nombre) {
        switch (nombre.toUpperCase()) {
            case "INT":    return INT;
            case "FLOAT":  return FLOAT;
            case "STRING": return STRING;
            case "BOOL":   return BOOL;
            case "VOID":   return VOID;
            default:       return ERROR;
        }
    }

    /**
     * Devuelve true si este tipo puede convertirse (coerción implícita) al tipo destino.
     * La única coerción permitida es int → float.
     * Ejemplo: asignar un int a una variable float es válido.
     */
    public boolean esCompatibleCon(TipoSemantico destino) {
        if (this == destino)        return true;   // mismo tipo, siempre compatible
        if (this == INT && destino == FLOAT) return true;  // int cabe en float
        return false;
    }

    /**
     * Dado dos tipos en una operación aritmética, devuelve el tipo resultante.
     * Si uno es float y el otro int, el resultado es float (promoción).
     * Si los tipos no son compatibles aritméticamente, devuelve ERROR.
     */
    public static TipoSemantico resultadoAritmetico(TipoSemantico a, TipoSemantico b) {
        if (a == ERROR || b == ERROR) return ERROR;
        if ((a == INT || a == FLOAT) && (b == INT || b == FLOAT)) {
            // si alguno es float, el resultado es float
            return (a == FLOAT || b == FLOAT) ? FLOAT : INT;
        }
        return ERROR; // string, bool, etc. no soportan aritmética
    }

    /**
     * Devuelve el tipo resultante de una comparación (>, <, ==, etc.).
     * El resultado siempre es BOOL si los operandos son numéricos o del mismo tipo.
     */
    public static TipoSemantico resultadoComparacion(TipoSemantico a, TipoSemantico b) {
        if (a == ERROR || b == ERROR) return ERROR;
        // se puede comparar numéricos entre sí (con coerción) o mismo tipo
        if (a.esCompatibleCon(b) || b.esCompatibleCon(a)) return BOOL;
        return ERROR;
    }
}