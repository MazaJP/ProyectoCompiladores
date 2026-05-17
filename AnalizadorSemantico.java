import java.util.*;

/**
 * AnalizadorSemantico — segunda pasada sobre los tokens.
 * Llena la tabla de simbolos, evalua tipo Y valor de expresiones,
 * y reporta errores semanticos con recuperacion hasta EOF.
 */
public class AnalizadorSemantico {

    private final List<Token>   tokens;
    private int                 pos;
    private final TablaSimbolos tabla;
    private final List<String>  errores;
    private String              scopeActual;

    public AnalizadorSemantico(List<Token> tokens, String rutaArchivo) {
        this.tokens      = tokens;
        this.pos         = 0;
        this.tabla       = new TablaSimbolos(rutaArchivo);
        this.errores     = new ArrayList<>();
        this.scopeActual = "global";
    }

    public boolean analizar() {
        while (!es(TokenType.EOF)) analizarSentencia();
        tabla.escribirArchivo();
        return errores.isEmpty();
    }

    public List<String> getErrores() { return errores; }

    // ── Clase interna: resultado de evaluar una expresion ─────────────────
    private static class ResultadoExpr {
        TipoSemantico tipo;
        Object        valor;
        ResultadoExpr(TipoSemantico tipo, Object valor) { this.tipo = tipo; this.valor = valor; }
        static ResultadoExpr error() { return new ResultadoExpr(TipoSemantico.ERROR, null); }
    }

    // ── Despachador principal ─────────────────────────────────────────────
    private void analizarSentencia() {
        switch (actual().getTipo()) {
            case INT: case FLOAT: case STRING: case BOOL: analizarDeclaracion(); break;
            case ID:      analizarAsignacionOLlamada(); break;
            case IF:      analizarIf(); break;
            case WHILE:   analizarWhile(); break;
            case FUNCION: analizarDefinicionFuncion(); break;
            case RETURN:  analizarReturn(); break;
            case READ:    analizarRead(); break;
            case WRITE:   analizarWrite(); break;
            default:      avanzar(); break;
        }
    }

    // ── Declaracion: TIPO ID [= EXPR] ─────────────────────────────────────
    private void analizarDeclaracion() {
        TipoSemantico tipo = TipoSemantico.desdeCadena(actual().getTipo().name());
        avanzar(); // consumir tipo

        if (!es(TokenType.ID)) {
            registrarError("se esperaba identificador despues del tipo", actual());
            saltarHastaNewline(); return;
        }
        Token tokenId = actual();
        String nombre = tokenId.getLexema();
        avanzar(); // consumir ID

        // Registrar en la tabla de simbolos
        EntradaTabla nueva = new EntradaTabla(nombre, tipo,
                EntradaTabla.Categoria.VARIABLE, scopeActual, tokenId.getLinea());
        if (!tabla.declarar(nueva))
            registrarError("variable '" + nombre + "' ya fue declarada en este scope", tokenId);

        // Si hay inicializacion evaluar y guardar valor
        if (es(TokenType.IGUAL)) {
            avanzar(); // consumir '='
            ResultadoExpr res = evaluarExpresion();
            if (res.tipo != TipoSemantico.ERROR && !res.tipo.esCompatibleCon(tipo)) {
                registrarError("no se puede asignar tipo '" + res.tipo +
                        "' a variable de tipo '" + tipo + "'", tokenId);
            } else if (res.valor != null) {
                tabla.asignarValor(nombre, formatearValor(res.valor, tipo));
            }
        }
        saltarHastaNewline();
    }

    // ── Asignacion o llamada ──────────────────────────────────────────────
    private void analizarAsignacionOLlamada() {
        Token tokenId = actual();
        String nombre = tokenId.getLexema();
        avanzar(); // consumir ID

        if (es(TokenType.IGUAL)) {
            avanzar(); // consumir '='
            EntradaTabla entrada = tabla.buscar(nombre);
            if (entrada == null) {
                registrarError("variable '" + nombre + "' no fue declarada", tokenId);
                saltarHastaNewline(); return;
            }
            ResultadoExpr res = evaluarExpresion();
            if (res.tipo != TipoSemantico.ERROR && !res.tipo.esCompatibleCon(entrada.getTipo())) {
                registrarError("no se puede asignar tipo '" + res.tipo +
                        "' a variable '" + nombre + "' de tipo '" + entrada.getTipo() + "'", tokenId);
            } else if (res.valor != null) {
                tabla.asignarValor(nombre, formatearValor(res.valor, entrada.getTipo()));
            }
        } else if (es(TokenType.PARENI)) {
            avanzar(); // consumir '('
            analizarArgumentos(nombre, tokenId);
            if (es(TokenType.PAREND)) avanzar(); // consumir ')'
        }
        saltarHastaNewline();
    }

    // ── If ────────────────────────────────────────────────────────────────
    private void analizarIf() {
        avanzar(); // consumir 'if'
        ResultadoExpr cond = evaluarExpresion();
        if (cond.tipo != TipoSemantico.BOOL && cond.tipo != TipoSemantico.ERROR)
            registrarError("la condicion del if debe ser de tipo bool", actual());
        saltarHastaNewline();
        analizarBloque();
        if (es(TokenType.ELSE)) { avanzar(); saltarHastaNewline(); analizarBloque(); }
    }

    // ── While ─────────────────────────────────────────────────────────────
    private void analizarWhile() {
        avanzar(); // consumir 'while'
        ResultadoExpr cond = evaluarExpresion();
        if (cond.tipo != TipoSemantico.BOOL && cond.tipo != TipoSemantico.ERROR)
            registrarError("la condicion del while debe ser de tipo bool", actual());
        saltarHastaNewline();
        analizarBloque();
    }

    // ── Definicion de funcion ─────────────────────────────────────────────
    private void analizarDefinicionFuncion() {
        avanzar(); // consumir 'funcion'
        if (!es(TokenType.ID)) {
            registrarError("se esperaba nombre de funcion", actual());
            saltarHastaNewline(); return;
        }
        Token tokenNombre = actual();
        String nombreFun  = tokenNombre.getLexema();
        avanzar(); // consumir nombre

        List<TipoSemantico> tiposParams = new ArrayList<>();
        if (es(TokenType.PARENI)) {
            avanzar(); // consumir '('
            if (!es(TokenType.PAREND)) tiposParams = analizarParametrosFuncion(nombreFun);
            if (es(TokenType.PAREND)) avanzar(); // consumir ')'
        }

        EntradaTabla entradaFun = new EntradaTabla(nombreFun, TipoSemantico.VOID,
                EntradaTabla.Categoria.FUNCION, scopeActual, tokenNombre.getLinea(), tiposParams);
        if (!tabla.declarar(entradaFun))
            registrarError("funcion '" + nombreFun + "' ya fue declarada", tokenNombre);

        String scopeAnterior = scopeActual;
        scopeActual = nombreFun;
        tabla.abrirScope(nombreFun);
        saltarHastaNewline();
        analizarBloque();
        tabla.cerrarScope();
        scopeActual = scopeAnterior;
    }

    private List<TipoSemantico> analizarParametrosFuncion(String nombreFun) {
        List<TipoSemantico> tipos = new ArrayList<>();
        while (!es(TokenType.PAREND) && !es(TokenType.EOF)) {
            if (!esTipo(actual().getTipo())) { registrarError("se esperaba tipo en parametro", actual()); avanzar(); continue; }
            TipoSemantico tipoParam = TipoSemantico.desdeCadena(actual().getTipo().name());
            avanzar();
            if (!es(TokenType.ID)) { registrarError("se esperaba nombre de parametro", actual()); continue; }
            Token tokenParam = actual(); avanzar();
            tipos.add(tipoParam);
            tabla.declarar(new EntradaTabla(tokenParam.getLexema(), tipoParam,
                    EntradaTabla.Categoria.PARAMETRO, nombreFun, tokenParam.getLinea()));
            if (es(TokenType.COMA)) avanzar();
        }
        return tipos;
    }

    // ── Return ────────────────────────────────────────────────────────────
    private void analizarReturn() {
        avanzar(); // consumir 'return'
        if (esInicioExpresion()) evaluarExpresion();
        saltarHastaNewline();
    }

    // ── Read ──────────────────────────────────────────────────────────────
    private void analizarRead() {
        avanzar(); // consumir 'read'
        if (es(TokenType.PARENI)) avanzar();
        if (es(TokenType.ID)) {
            Token t = actual();
            if (tabla.buscar(t.getLexema()) == null)
                registrarError("variable '" + t.getLexema() + "' no fue declarada", t);
            avanzar();
        } else registrarError("se esperaba identificador en read()", actual());
        if (es(TokenType.PAREND)) avanzar();
        saltarHastaNewline();
    }

    // ── Write ─────────────────────────────────────────────────────────────
    private void analizarWrite() {
        avanzar(); // consumir 'write'
        if (es(TokenType.PARENI)) avanzar();
        while (!es(TokenType.PAREND) && !es(TokenType.EOF) && !es(TokenType.NEWLINE)) {
            evaluarExpresion();
            if (es(TokenType.COMA)) avanzar();
        }
        if (es(TokenType.PAREND)) avanzar();
        saltarHastaNewline();
    }

    // ── Bloque ────────────────────────────────────────────────────────────
    private void analizarBloque() {
        if (!es(TokenType.INDENT)) return;
        avanzar(); // consumir INDENT
        tabla.abrirScope(scopeActual + "_bloque");
        while (!es(TokenType.DEDENT) && !es(TokenType.EOF)) analizarSentencia();
        tabla.cerrarScope();
        if (es(TokenType.DEDENT)) avanzar();
    }

    // ── Argumentos de llamada ─────────────────────────────────────────────
    private void analizarArgumentos(String nombreFun, Token tokenLlamada) {
        List<TipoSemantico> tiposArgs = new ArrayList<>();
        while (!es(TokenType.PAREND) && !es(TokenType.EOF) && !es(TokenType.NEWLINE)) {
            tiposArgs.add(evaluarExpresion().tipo);
            if (es(TokenType.COMA)) avanzar();
        }
        EntradaTabla funcion = tabla.buscar(nombreFun);
        if (funcion == null) { registrarError("funcion '" + nombreFun + "' no fue declarada", tokenLlamada); return; }
        List<TipoSemantico> tiposParams = funcion.getTiposParams();
        if (tiposParams == null) return;
        if (tiposArgs.size() != tiposParams.size()) {
            registrarError("funcion '" + nombreFun + "' espera " + tiposParams.size() +
                    " argumento(s), se dieron " + tiposArgs.size(), tokenLlamada); return;
        }
        for (int i = 0; i < tiposArgs.size(); i++) {
            TipoSemantico arg = tiposArgs.get(i), param = tiposParams.get(i);
            if (arg != TipoSemantico.ERROR && !arg.esCompatibleCon(param))
                registrarError("argumento " + (i+1) + " de funcion '" + nombreFun +
                        "' es invalido: se esperaba '" + param + "', se recibio '" + arg + "'", tokenLlamada);
        }
    }

    // ── Evaluacion de expresiones (shunting-yard con calculo de valores) ──
    private ResultadoExpr evaluarExpresion() {
        Deque<ResultadoExpr> pilaOperandos = new ArrayDeque<>();
        Deque<String>        pilaOps       = new ArrayDeque<>();
        boolean esperaOperando = true;

        bucle:
        while (true) {
            Token t = actual();
            if (esperaOperando) {
                if (t.getTipo() == TokenType.NOT)    { avanzar(); pilaOps.push("NOT");    continue; }
                if (t.getTipo() == TokenType.RESTA)  { avanzar(); pilaOps.push("UNARIO"); continue; }
                if (t.getTipo() == TokenType.PARENI) { avanzar(); pilaOps.push("PARENI"); continue; }

                // Llamada a funcion dentro de expresion
                if (t.getTipo() == TokenType.ID && siguiente().getTipo() == TokenType.PARENI) {
                    String nom = t.getLexema(); avanzar(); avanzar();
                    analizarArgumentos(nom, t);
                    if (es(TokenType.PAREND)) avanzar();
                    EntradaTabla fun = tabla.buscar(nom);
                    TipoSemantico tr = (fun != null) ? fun.getTipo() : TipoSemantico.ERROR;
                    pilaOperandos.push(new ResultadoExpr(tr, null));
                    esperaOperando = false;
                    aplicarUnariosUnarios(pilaOps, pilaOperandos, t);
                    continue;
                }

                // Operando simple (literal o ID)
                if (esOperandoSimple(t.getTipo())) {
                    pilaOperandos.push(resolverOperando(t));
                    avanzar();
                    esperaOperando = false;
                    aplicarUnariosUnarios(pilaOps, pilaOperandos, t);
                    continue;
                }
                break;

            } else {
                // Cierre de parentesis
                if (t.getTipo() == TokenType.PAREND && pilaContienePareni(pilaOps)) {
                    while (!pilaOps.isEmpty() && !pilaOps.peek().equals("PARENI"))
                        aplicarBinario(pilaOps.pop(), pilaOperandos, t);
                    if (!pilaOps.isEmpty()) pilaOps.pop();
                    avanzar(); continue;
                }
                // Operador binario
                String tipoOp = t.getTipo().name();
                if (esBinario(tipoOp)) {
                    while (!pilaOps.isEmpty() && !pilaOps.peek().equals("PARENI")
                            && !pilaOps.peek().equals("NOT") && !pilaOps.peek().equals("UNARIO")
                            && precedencia(pilaOps.peek()) >= precedencia(tipoOp))
                        aplicarBinario(pilaOps.pop(), pilaOperandos, t);
                    pilaOps.push(tipoOp); avanzar(); esperaOperando = true; continue;
                }
                break bucle;
            }
        }

        // Vaciar operadores pendientes
        while (!pilaOps.isEmpty()) {
            String op = pilaOps.pop();
            if (!op.equals("PARENI")) {
                if (op.equals("NOT") || op.equals("UNARIO")) aplicarUnario(op, pilaOperandos, actual());
                else aplicarBinario(op, pilaOperandos, actual());
            }
        }
        return pilaOperandos.isEmpty() ? ResultadoExpr.error() : pilaOperandos.peek();
    }

    // ── Resolver operando simple ──────────────────────────────────────────
    private ResultadoExpr resolverOperando(Token t) {
        switch (t.getTipo()) {
            case NUMENTERO:    return new ResultadoExpr(TipoSemantico.INT,    Integer.parseInt(t.getLexema()));
            case NUMDECIMAL:   return new ResultadoExpr(TipoSemantico.FLOAT,  Double.parseDouble(t.getLexema()));
            case CADENASTRING: return new ResultadoExpr(TipoSemantico.STRING, t.getLexema());
            case TRUE:         return new ResultadoExpr(TipoSemantico.BOOL,   true);
            case FALSE:        return new ResultadoExpr(TipoSemantico.BOOL,   false);
            case ID: {
                EntradaTabla e = tabla.buscar(t.getLexema());
                if (e == null) { registrarError("variable '" + t.getLexema() + "' no fue declarada", t); return ResultadoExpr.error(); }
                return new ResultadoExpr(e.getTipo(), e.getValor()); // valor puede ser null si no fue asignado aun
            }
            default: return ResultadoExpr.error();
        }
    }

    // ── Aplicar operadores ────────────────────────────────────────────────

    /** Aplica unarios pendientes al tope de la pila despues de leer un operando. */
    private void aplicarUnariosUnarios(Deque<String> pilaOps, Deque<ResultadoExpr> pila, Token ref) {
        while (!pilaOps.isEmpty() && (pilaOps.peek().equals("NOT") || pilaOps.peek().equals("UNARIO")))
            aplicarUnario(pilaOps.pop(), pila, ref);
    }

    /** Aplica NOT o negacion numerica al tope de la pila. */
    private void aplicarUnario(String op, Deque<ResultadoExpr> pila, Token ref) {
        if (pila.isEmpty()) return;
        ResultadoExpr o = pila.pop();
        if (op.equals("NOT")) {
            if (o.tipo != TipoSemantico.BOOL && o.tipo != TipoSemantico.ERROR)
                registrarError("'not' solo aplica a expresiones de tipo bool", ref);
            Boolean val = (o.valor instanceof Boolean) ? !(Boolean) o.valor : null;
            pila.push(new ResultadoExpr(TipoSemantico.BOOL, val));
        } else { // UNARIO (menos unario)
            if (o.tipo == TipoSemantico.INT) {
                pila.push(new ResultadoExpr(TipoSemantico.INT,   o.valor instanceof Integer ? -(Integer) o.valor : null));
            } else if (o.tipo == TipoSemantico.FLOAT) {
                pila.push(new ResultadoExpr(TipoSemantico.FLOAT, o.valor instanceof Double  ? -(Double)  o.valor : null));
            } else if (o.tipo != TipoSemantico.ERROR) {
                registrarError("negacion unaria solo aplica a tipos numericos", ref);
                pila.push(ResultadoExpr.error());
            } else pila.push(ResultadoExpr.error());
        }
    }

    /** Aplica un operador binario a los dos topes de la pila y empuja el resultado. */
    private void aplicarBinario(String op, Deque<ResultadoExpr> pila, Token ref) {
        if (pila.size() < 2) return;
        ResultadoExpr der = pila.pop(), izq = pila.pop();
        switch (op) {
            case "SUMA": case "RESTA": case "MULTI": case "DIV": {
                TipoSemantico tr = TipoSemantico.resultadoAritmetico(izq.tipo, der.tipo);
                if (tr == TipoSemantico.ERROR && izq.tipo != TipoSemantico.ERROR && der.tipo != TipoSemantico.ERROR)
                    registrarError("no se puede operar tipo '" + izq.tipo + "' y '" + der.tipo + "'", ref);
                pila.push(new ResultadoExpr(tr, calcularAritmetico(op, izq, der, tr)));
                break;
            }
            case "MAYORQ": case "MENORQ": case "MAYORIGU": case "MENORIGU": case "EQUIVA": case "NEGA": {
                TipoSemantico tr = TipoSemantico.resultadoComparacion(izq.tipo, der.tipo);
                if (tr == TipoSemantico.ERROR && izq.tipo != TipoSemantico.ERROR && der.tipo != TipoSemantico.ERROR)
                    registrarError("no se puede comparar tipo '" + izq.tipo + "' con '" + der.tipo + "'", ref);
                pila.push(new ResultadoExpr(TipoSemantico.BOOL, calcularRelacional(op, izq, der)));
                break;
            }
            case "AND": case "OR": {
                if ((izq.tipo != TipoSemantico.BOOL && izq.tipo != TipoSemantico.ERROR) ||
                    (der.tipo != TipoSemantico.BOOL && der.tipo != TipoSemantico.ERROR))
                    registrarError("operador '" + op.toLowerCase() + "' requiere operandos bool", ref);
                pila.push(new ResultadoExpr(TipoSemantico.BOOL, calcularLogico(op, izq, der)));
                break;
            }
            default: pila.push(ResultadoExpr.error());
        }
    }

    // ── Calculos de valores ───────────────────────────────────────────────

    /** Calcula el valor de una operacion aritmetica. Devuelve null si algun operando es desconocido. */
    private Object calcularAritmetico(String op, ResultadoExpr izq, ResultadoExpr der, TipoSemantico tipoRes) {
        if (izq.valor == null || der.valor == null) return null;
        try {
            double a = toDouble(izq.valor), b = toDouble(der.valor), r;
            switch (op) {
                case "SUMA":  r = a + b; break;
                case "RESTA": r = a - b; break;
                case "MULTI": r = a * b; break;
                case "DIV":   if (b == 0) return null; r = a / b; break;
                default: return null;
            }
            // Devolver entero si el tipo resultado es INT, double si es FLOAT
            return (tipoRes == TipoSemantico.INT) ? (Integer)(int) r : (Double) r;
        } catch (Exception e) { return null; }
    }

    /** Calcula el valor booleano de una comparacion numerica. */
    private Object calcularRelacional(String op, ResultadoExpr izq, ResultadoExpr der) {
        if (izq.valor == null || der.valor == null) return null;
        try {
            double a = toDouble(izq.valor), b = toDouble(der.valor);
            switch (op) {
                case "MAYORQ":   return a > b;  case "MENORQ":   return a < b;
                case "MAYORIGU": return a >= b; case "MENORIGU": return a <= b;
                case "EQUIVA":   return a == b; case "NEGA":     return a != b;
                default: return null;
            }
        } catch (Exception e) { return null; }
    }

    /** Calcula el valor de una operacion logica. */
    private Object calcularLogico(String op, ResultadoExpr izq, ResultadoExpr der) {
        if (!(izq.valor instanceof Boolean) || !(der.valor instanceof Boolean)) return null;
        boolean a = (Boolean) izq.valor, b = (Boolean) der.valor;
        return op.equals("AND") ? a && b : a || b;
    }

    /** Convierte Integer o Double a double para operar. */
    private double toDouble(Object val) {
        if (val instanceof Integer) return ((Integer) val).doubleValue();
        if (val instanceof Double)  return (Double) val;
        throw new IllegalArgumentException("No es numerico: " + val);
    }

    /** Si la variable es FLOAT y el valor es Integer, lo convierte a Double. */
    private Object formatearValor(Object valor, TipoSemantico tipo) {
        if (valor == null) return null;
        if (tipo == TipoSemantico.FLOAT && valor instanceof Integer)
            return ((Integer) valor).doubleValue();
        return valor;
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private void registrarError(String msg, Token t) {
        errores.add("Linea " + t.getLinea() + ", col " + t.getColumnaInicio() + ": **ERROR** " + msg);
    }

    private void saltarHastaNewline() {
        while (!es(TokenType.NEWLINE) && !es(TokenType.DEDENT) && !es(TokenType.EOF)) avanzar();
        if (es(TokenType.NEWLINE)) avanzar();
    }

    private void avanzar()          { if (pos < tokens.size() - 1) pos++; }
    private Token actual()          { return tokens.get(pos); }
    private Token siguiente()       { return pos + 1 < tokens.size() ? tokens.get(pos + 1) : tokens.get(tokens.size() - 1); }
    private boolean es(TokenType t) { return actual().getTipo() == t; }

    private boolean esTipo(TokenType t) {
        return t == TokenType.INT || t == TokenType.FLOAT || t == TokenType.STRING || t == TokenType.BOOL;
    }

    private boolean esInicioExpresion() {
        switch (actual().getTipo()) {
            case ID: case NUMENTERO: case NUMDECIMAL: case CADENASTRING:
            case TRUE: case FALSE: case PARENI: case NOT: case RESTA: return true;
            default: return false;
        }
    }

    private boolean esOperandoSimple(TokenType t) {
        return t == TokenType.ID || t == TokenType.NUMENTERO || t == TokenType.NUMDECIMAL
            || t == TokenType.CADENASTRING || t == TokenType.TRUE || t == TokenType.FALSE;
    }

    private boolean esBinario(String n) {
        switch (n) {
            case "SUMA": case "RESTA": case "MULTI": case "DIV":
            case "MAYORQ": case "MENORQ": case "MAYORIGU": case "MENORIGU":
            case "EQUIVA": case "NEGA": case "AND": case "OR": return true;
            default: return false;
        }
    }

    private int precedencia(String op) {
        switch (op) {
            case "OR": return 1; case "AND": return 2;
            case "EQUIVA": case "NEGA": case "MAYORQ": case "MENORQ":
            case "MAYORIGU": case "MENORIGU": return 4;
            case "SUMA": case "RESTA": return 5;
            case "MULTI": case "DIV": return 6;
            default: return 0;
        }
    }

    private boolean pilaContienePareni(Deque<String> p) {
        for (String s : p) if (s.equals("PARENI")) return true;
        return false;
    }
}