/**
 * Se usa en la tabla de símbolos y en la comprobación de tipos
 *  DOUBLE es "igual" de FLOAT: internamente se tratan igual,
 * pero se distinguen en la tabla de símbolos para mostrar el tipo
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
     * Convierte el nombre de un TokenType al TipoSemantico correspondiente siendo un puente
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
     * Devuelve true si este tipo es numérico decimal (float o double)
     * Se usa para unificar ambos en operaciones aritméticas.
     */
    public boolean esDecimal() {
        return this == FLOAT || this == DOUBLE;
    }

    /**
     * Devuelve true si este tipo puede convertirse implícitamente al tipo destino.
     * Coerciones permitidas:
     *   int a float 
     *   int a double 
     *   float a double
     *   double a float  
     */
    //validaciones para ver si se puede operar
    public boolean esCompatibleCon(TipoSemantico destino) {
        // Los tipos son igual
        if (this == destino) return true;
        // int puede ser un tipo decimal
        if (this == INT && destino.esDecimal()) return true;
        // float y double son intercambiables entre sí
        if (this.esDecimal() && destino.esDecimal()) return true;
        return false;
    }

    /**
     * Dado dos tipos en una operación aritmética, devuelve el tipo resultante.
     * Reglas:
     *   int con int  da int
     *   int con float da float
     *   int con double da double
     *   float con double da double
     *   cualquier otro  → ERROR
     */
    public static TipoSemantico resultadoAritmetico(TipoSemantico a, TipoSemantico b) {
        //si uno de los dos ya es un error da error 
        if (a == ERROR || b == ERROR) return ERROR;
        //revisa que los dos numeros sean un numero 
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
     * Devuelve el tipo resultante de una comparación >, <, ==, etc
     * El resultado siempre es BOOL si los operandos son compatibles entre sí.
     */
    public static TipoSemantico resultadoComparacion(TipoSemantico a, TipoSemantico b) {
        if (a == ERROR || b == ERROR) return ERROR;
        if (a.esCompatibleCon(b) || b.esCompatibleCon(a)) return BOOL;
        return ERROR;
    }
}