/**
 * Representa los tipos de dato que maneja MiniLang.
 * Se usa en la tabla de símbolos y en la comprobación de tipos.
 *
 * DOUBLE es un alias de FLOAT: internamente se tratan igual,
 * pero se distinguen en la tabla de símbolos para mostrar el tipo
 * exacto que el programador escribió.
 */
public enum TipoSemantico {
    INT,        // número entero
    FLOAT,      // número decimal declarado con "float"
    DOUBLE,     // número decimal declarado con "double" (mismo comportamiento que FLOAT)
    STRING,     // cadena de texto
    BOOL,       // valor booleano (true / false)
    VOID,       // sin valor de retorno (funciones)
    ERROR;      // tipo inválido, resultado de una operación incorrecta

    /**
     * Convierte el nombre de un TokenType al TipoSemantico correspondiente.
     * "DOUBLE" y "FLOAT" mapean a sus respectivos valores del enum.
     */
    public static TipoSemantico desdeCadena(String nombre) {
        switch (nombre.toUpperCase()) {
            case "INT":    return INT;
            case "FLOAT":  return FLOAT;
            case "DOUBLE": return DOUBLE;
            case "STRING": return STRING;
            case "BOOL":   return BOOL;
            case "VOID":   return VOID;
            default:       return ERROR;
        }
    }

    /**
     * Devuelve true si este tipo es numérico decimal (float o double).
     * Se usa para unificar ambos en operaciones aritméticas.
     */
    public boolean esDecimal() {
        return this == FLOAT || this == DOUBLE;
    }

    /**
     * Devuelve true si este tipo puede convertirse implícitamente al tipo destino.
     * Coerciones permitidas:
     *   int   → float   (válido)
     *   int   → double  (válido)
     *   float → double  (válido)
     *   double→ float   (válido, mismo nivel)
     */
    public boolean esCompatibleCon(TipoSemantico destino) {
        if (this == destino) return true;
        // int puede ir a cualquier tipo decimal
        if (this == INT && destino.esDecimal()) return true;
        // float y double son intercambiables entre sí
        if (this.esDecimal() && destino.esDecimal()) return true;
        return false;
    }

    /**
     * Dado dos tipos en una operación aritmética, devuelve el tipo resultante.
     * Reglas:
     *   int   op int    → int
     *   int   op float  → float
     *   int   op double → double
     *   float op double → double
     *   cualquier otro  → ERROR
     */
    public static TipoSemantico resultadoAritmetico(TipoSemantico a, TipoSemantico b) {
        if (a == ERROR || b == ERROR) return ERROR;
        boolean aNum = (a == INT || a.esDecimal());
        boolean bNum = (b == INT || b.esDecimal());
        if (!aNum || !bNum) return ERROR; // string, bool, etc. no soportan aritmética

        // Si alguno es DOUBLE, el resultado es DOUBLE
        if (a == DOUBLE || b == DOUBLE) return DOUBLE;
        // Si alguno es FLOAT, el resultado es FLOAT
        if (a == FLOAT  || b == FLOAT)  return FLOAT;
        // Ambos INT
        return INT;
    }

    /**
     * Devuelve el tipo resultante de una comparación (>, <, ==, etc.).
     * El resultado siempre es BOOL si los operandos son compatibles entre sí.
     */
    public static TipoSemantico resultadoComparacion(TipoSemantico a, TipoSemantico b) {
        if (a == ERROR || b == ERROR) return ERROR;
        if (a.esCompatibleCon(b) || b.esCompatibleCon(a)) return BOOL;
        return ERROR;
    }
}