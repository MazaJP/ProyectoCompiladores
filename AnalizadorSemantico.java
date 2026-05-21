import java.util.*;

/**
  AnalizadorSemantico — segunda pasada sobre los tokens.
  Llena la tabla de simbolos, evalua tipo Y valor de expresiones,
  y reporta errores semanticos con recuperacion hasta EOF.
 
    - Soporta "const TIPO ID"  registra como CONSTANTE y bloquea reasignacion
  - Detecta reasignacion de constantes
 */
public class AnalizadorSemantico {

    private final List<Token>   tokens; // lista de token que esta en lexer
    private int                 pos; // en que toque empezamos
    private final TablaSimbolos tabla;
    //Guardamos los errores
    private final List<String>  errores;
    private String              scopeActual; // Ambito en el que estamos por default es Global pero este cambia al salir de un bloque

    public AnalizadorSemantico(List<Token> tokens, String rutaArchivo) {
        this.tokens      = tokens;
        this.pos         = 0;
        this.tabla       = new TablaSimbolos(rutaArchivo);
        this.errores     = new ArrayList<>();
        this.scopeActual = "global";
    }
// llamamos a analiizar sentencia hasta llegar al final del archivo
    public boolean analizar() {
        while (!es(TokenType.EOF)) analizarSentencia();
        tabla.escribirArchivo();
        return errores.isEmpty();
    }

    public List<String> getErrores() { return errores; }

    // guardamos el tipo y valor de algun calculo
    private static class ResultadoExpr {
        TipoSemantico tipo;
        Object        valor; // Integer, Double, String, Boolean o null
        ResultadoExpr(TipoSemantico tipo, Object valor) { this.tipo = tipo; this.valor = valor; }
        static ResultadoExpr error() { return new ResultadoExpr(TipoSemantico.ERROR, null); }
    }

   // metodo que revisa el toquen en el que estamos 
    private void analizarSentencia() {
        switch (actual().getTipo()) {
            // si detecta alguno tipos sabe que viene una declaracion de variable
            case INT: case FLOAT: case DOUBLE: case STRING: case BOOL:
                analizarDeclaracion(false); break;  // false = no es constante
            case CONST:
                analizarDeclaracionConst(); break;   // maneja "const TIPO ID"
            // si ve que biene id viene una asignacion o el llamado de una funcion 
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
    //Analiza una declaracion de variable o constante.
     // Si esConst=true la registra como CONSTANTE en la tabla impiediendo reasignarla
    
    private void analizarDeclaracionConst() {
        avanzar(); // consumir "const"
        if (!esTipo(actual().getTipo())) {
            registrarError("se esperaba tipo despues de 'const'", actual());
            saltarHastaNewline(); return;
        }
        analizarDeclaracion(true); // true = es constante
    }
        // convierte el int, float y el double en tiposemantico
     private void analizarDeclaracion(boolean esConst) {
        Token tokenTipo = actual();
        TipoSemantico tipo = TipoSemantico.desdeCadena(tokenTipo.getTipo().name());
        avanzar(); // consumir tipo

        if (!es(TokenType.ID)) {
            registrarError("se esperaba identificador despues del tipo", actual());
            saltarHastaNewline(); return;
        }
        //lee el nombre de la variable
        Token tokenId = actual();
        String nombre = tokenId.getLexema();
        avanzar(); // consumir ID

        // Elegir categoria segun si es constante o variable normal
        // si es true agrega la categoria como constante sino lo pone como variable
        EntradaTabla.Categoria categoria = esConst? EntradaTabla.Categoria.CONSTANTE : EntradaTabla.Categoria.VARIABLE;
// si estamos dentro de una funcion el valor de scopeActual Cambia
// aqui se llena la tabla de simbolos 
        EntradaTabla nueva = new EntradaTabla(nombre, tipo, categoria, scopeActual, tokenId.getLinea());
        if (!tabla.declarar(nueva))
            registrarError("'" + nombre + "' ya fue declarado en este scope", tokenId);

        // si despues del nombre hay un = evalua la expresion y comprueba que el tipo sea compatible
        if (es(TokenType.IGUAL)) {
            avanzar(); // consumir '='
            ResultadoExpr res = evaluarExpresion();
            if (res.tipo != TipoSemantico.ERROR && !res.tipo.esCompatibleCon(tipo)) {
                registrarError("no se puede asignar tipo '" + res.tipo +
                        "' a variable de tipo '" + tipo + "'", tokenId);
            } else if (res.valor != null) {
                // setValor devuelve false si es const y ya tenia valor
                nueva.setValor(formatearValor(res.valor, tipo));
            }
        }
        saltarHastaNewline();
    }

    // 
    private void analizarAsignacionOLlamada() {
        Token tokenId = actual();
        String nombre = tokenId.getLexema();
        avanzar(); // consumir ID

        if (es(TokenType.IGUAL)) {
            avanzar(); // consumir '='
            //buscamos la variable ne la tabla 
            EntradaTabla entrada = tabla.buscar(nombre);
            //error si esta no existe
            if (entrada == null) {
                registrarError("variable '" + nombre + "' no fue declarada", tokenId);
                saltarHastaNewline(); return;
            }

            // Verificar que no sea una constante ya asignada
            if (entrada.isEsConstante() && entrada.isYaAsignada()) {
                registrarError("no se puede reasignar la constante '" + nombre + "'", tokenId);
                saltarHastaNewline(); return;
            }

            ResultadoExpr res = evaluarExpresion();
            if (res.tipo != TipoSemantico.ERROR && !res.tipo.esCompatibleCon(entrada.getTipo())) {
                registrarError("no se puede asignar tipo '" + res.tipo +
                        "' a '" + nombre + "' de tipo '" + entrada.getTipo() + "'", tokenId);
            } else if (res.valor != null) {
                boolean ok = entrada.setValor(formatearValor(res.valor, entrada.getTipo()));
                if (!ok) // setValor devolvio false → era constante ya asignada
                    registrarError("no se puede reasignar la constante '" + nombre + "'", tokenId);
            }

        } else if (es(TokenType.PARENI)) {
            avanzar(); // consumir '('
            //verificamos que los tipos de parametros coincidan
            analizarArgumentos(nombre, tokenId);
            if (es(TokenType.PAREND)) avanzar();
        }
        saltarHastaNewline();
    }

    // evaluamos la condicion que sea bool, luego analiza el bloque then y por ultimo si hay else lo analiza
    private void analizarIf() {
        avanzar();
        ResultadoExpr cond = evaluarExpresion();
        if (cond.tipo != TipoSemantico.BOOL && cond.tipo != TipoSemantico.ERROR)
            registrarError("la condicion del if debe ser de tipo bool", actual());
        saltarHastaNewline();
        analizarBloque();
        if (es(TokenType.ELSE)) { avanzar(); saltarHastaNewline(); analizarBloque(); }
    }

    // funciona igual que el if pero no hace los pasos de else
    private void analizarWhile() {
        avanzar();
        ResultadoExpr cond = evaluarExpresion();
        if (cond.tipo != TipoSemantico.BOOL && cond.tipo != TipoSemantico.ERROR)
            registrarError("la condicion del while debe ser de tipo bool", actual());
        saltarHastaNewline();
        analizarBloque();
    }

    // aqui es donde se ve lo de los ambitos
    private void analizarDefinicionFuncion() {
        avanzar(); // consumir 'funcion'
        if (!es(TokenType.ID)) {
            registrarError("se esperaba nombre de funcion", actual());
            saltarHastaNewline(); return;
        }
        Token tokenNombre = actual();
        String nombreFun  = tokenNombre.getLexema(); //guarda el nombre de la funcion 
        avanzar();
//revisa que inice con (, luego que este los parametro, por ultimo que cierre con )
        List<TipoSemantico> tiposParams = new ArrayList<>();
        if (es(TokenType.PARENI)) {
            avanzar();
            if (!es(TokenType.PAREND)) tiposParams = analizarParametrosFuncion(nombreFun);
            if (es(TokenType.PAREND)) avanzar();
        }

        EntradaTabla entradaFun = new EntradaTabla(nombreFun, TipoSemantico.VOID,
                EntradaTabla.Categoria.FUNCION, scopeActual, tokenNombre.getLinea(), tiposParams);
        if (!tabla.declarar(entradaFun))
            registrarError("funcion '" + nombreFun + "' ya fue declarada", tokenNombre);

        String scopeAnterior = scopeActual; //Guarda Globla
        scopeActual = nombreFun; //cambia al nombre de la funcion que habiamos guardado
        tabla.abrirScope(nombreFun); // crea el nuevo ambito en la pila
        saltarHastaNewline();
        analizarBloque(); //analiza lo que esta dentro
        tabla.cerrarScope(); // cierra el ambito en el qeu estamo
        scopeActual = scopeAnterior; //Regresa aGlobal 
    }

    private List<TipoSemantico> analizarParametrosFuncion(String nombreFun) {
        List<TipoSemantico> tipos = new ArrayList<>();
        while (!es(TokenType.PAREND) && !es(TokenType.EOF)) {
            if (!esTipo(actual().getTipo())) {
                registrarError("se esperaba tipo en parametro", actual()); avanzar(); continue;
            }
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
        avanzar();
        if (esInicioExpresion()) evaluarExpresion();
        saltarHastaNewline();
    }

    // ── Read ──────────────────────────────────────────────────────────────
    private void analizarRead() {
        avanzar();
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
        avanzar();
        if (es(TokenType.PARENI)) avanzar();
        while (!es(TokenType.PAREND) && !es(TokenType.EOF) && !es(TokenType.NEWLINE)) {
            evaluarExpresion();
            if (es(TokenType.COMA)) avanzar();
        }
        if (es(TokenType.PAREND)) avanzar();
        saltarHastaNewline();
    }

    // cuando se crea en indent se crea un bloque
    private void analizarBloque() {
        if (!es(TokenType.INDENT)) return;
        avanzar();
        //Guarda el nombre del ambito en el que estamos
        tabla.abrirScope(scopeActual + "_bloque");
        //analiza todas las sentencias hsata un dedent
        while (!es(TokenType.DEDENT) && !es(TokenType.EOF)) analizarSentencia();
        tabla.cerrarScope();
        if (es(TokenType.DEDENT)) avanzar();
    }

    // 
    private void analizarArgumentos(String nombreFun, Token tokenLlamada) {
        List<TipoSemantico> tiposArgs = new ArrayList<>();
        while (!es(TokenType.PAREND) && !es(TokenType.EOF) && !es(TokenType.NEWLINE)) {
            tiposArgs.add(evaluarExpresion().tipo);
            if (es(TokenType.COMA)) avanzar();
        }
        EntradaTabla funcion = tabla.buscar(nombreFun);
        if (funcion == null) {
            registrarError("funcion '" + nombreFun + "' no fue declarada", tokenLlamada); return;
        }
        List<TipoSemantico> tiposParams = funcion.getTiposParams();
        if (tiposParams == null) return;
        if (tiposArgs.size() != tiposParams.size()) {
            registrarError("funcion '" + nombreFun + "' espera " + tiposParams.size() +
                    " argumento(s), se dieron " + tiposArgs.size(), tokenLlamada); return;
        }
        //evalua cada argumento para tener su tipo
        for (int i = 0; i < tiposArgs.size(); i++) {
            //Busca la funcion para saber que tipo de parametros debe tener para luego verificar que estos coincidan
            TipoSemantico arg = tiposArgs.get(i), param = tiposParams.get(i);
            if (arg != TipoSemantico.ERROR && !arg.esCompatibleCon(param))
                registrarError("argumento " + (i+1) + " de funcion '" + nombreFun +
                        "' es invalido: se esperaba '" + param + "', se recibio '" + arg + "'", tokenLlamada);
        }
    }

    // 
    private ResultadoExpr evaluarExpresion() {
        Deque<ResultadoExpr> pilaOperandos = new ArrayDeque<>(); //Guarda tipos y valores
        Deque<String>        pilaOps       = new ArrayDeque<>(); //operadores pendientes
        boolean esperaOperando = true; //interruptor que espera o un valor o un operando

        bucle:
        while (true) {
            Token t = actual();
            if (esperaOperando) {
                //si detecta un not, - o ( los mete a pilaOps como operadores pendientes

                if (t.getTipo() == TokenType.NOT)    { avanzar(); pilaOps.push("NOT");    continue; }
                if (t.getTipo() == TokenType.RESTA)  { avanzar(); pilaOps.push("UNARIO"); continue; }
                if (t.getTipo() == TokenType.PARENI) { avanzar(); pilaOps.push("PARENI"); continue; }
                //si ve ID( sabe que es una llamada de funcion
                //verifica los argumento, busca la funcion en la TS  para saber su tipo de retorno
                // empuja el tipo como opernado
                if (t.getTipo() == TokenType.ID && siguiente().getTipo() == TokenType.PARENI) {
                    String nom = t.getLexema(); avanzar(); avanzar(); //consume ID y (
                    analizarArgumentos(nom, t); // verifica los parametros
                    if (es(TokenType.PAREND)) avanzar(); // consume )
                    EntradaTabla fun = tabla.buscar(nom);
                    TipoSemantico tr = (fun != null) ? fun.getTipo() : TipoSemantico.ERROR;
                    pilaOperandos.push(new ResultadoExpr(tr, null)); // si regresa null no sabemos que devuelve la funcio 
                    esperaOperando = false;
                    aplicarUnariosUnarios(pilaOps, pilaOperandos, t);
                    continue;
                }
                //
                if (esOperandoSimple(t.getTipo())) {
                    pilaOperandos.push(resolverOperando(t)); //obtenemos el tipo  valor del token 
                    avanzar();
                    //
                    esperaOperando = false;
                    aplicarUnariosUnarios(pilaOps, pilaOperandos, t); // aplica el -
                    continue;
                }
                break;

            } else {
                //Si ve ) y hay un ( pendiente, reduce todos los operadores que están entre el ( y el ), luego saca el PARENI de la pila y consume el ).
                if (t.getTipo() == TokenType.PAREND && pilaContienePareni(pilaOps)) {
                    while (!pilaOps.isEmpty() && !pilaOps.peek().equals("PARENI"))
                        aplicarBinario(pilaOps.pop(), pilaOperandos, t);
                    if (!pilaOps.isEmpty()) pilaOps.pop();
                    avanzar(); continue;
                }
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

        while (!pilaOps.isEmpty()) {
            //Cuando se sale del bucle porque el token actual ya no es parte de la expresión, se vacía todo lo que quedó pendiente en pilaOps
            String op = pilaOps.pop();
            if (!op.equals("PARENI")) {
                if (op.equals("NOT") || op.equals("UNARIO")) aplicarUnario(op, pilaOperandos, actual());
                else aplicarBinario(op, pilaOperandos, actual());
            }
        }
        return pilaOperandos.isEmpty() ? ResultadoExpr.error() : pilaOperandos.peek();
    }

    //
    private ResultadoExpr resolverOperando(Token t) {
        switch (t.getTipo())
        //conviertimos el texto en el tipo correspondiente
         {
            case NUMENTERO:    return new ResultadoExpr(TipoSemantico.INT,    Integer.parseInt(t.getLexema()));
            case NUMDECIMAL:   return new ResultadoExpr(TipoSemantico.FLOAT,  Double.parseDouble(t.getLexema()));
            case CADENASTRING: return new ResultadoExpr(TipoSemantico.STRING, t.getLexema());
            case TRUE:         return new ResultadoExpr(TipoSemantico.BOOL,   true);
            case FALSE:        return new ResultadoExpr(TipoSemantico.BOOL,   false);
            case ID: {
                EntradaTabla e = tabla.buscar(t.getLexema());
                //si la variable no existe da error
                if (e == null) { registrarError("variable '" + t.getLexema() + "' no fue declarada", t); return ResultadoExpr.error(); }
                return new ResultadoExpr(e.getTipo(), e.getValor());
            }
            default: return ResultadoExpr.error();
        }
    }

    //lo llamamos despues de empujar un operando aplicando el -
    private void aplicarUnariosUnarios(Deque<String> pilaOps, Deque<ResultadoExpr> pila, Token ref) {
        while (!pilaOps.isEmpty() && (pilaOps.peek().equals("NOT") || pilaOps.peek().equals("UNARIO")))
            aplicarUnario(pilaOps.pop(), pila, ref);
    }
    //
    private void aplicarUnario(String op, Deque<ResultadoExpr> pila, Token ref) {
        if (pila.isEmpty()) return;
        ResultadoExpr o = pila.pop();
        //samcamos el operando del tope de la pila para aplicar el op Unario
       
       //revisa que el operando se bool para luego aplicar la ngacion
        if (op.equals("NOT")) {
            if (o.tipo != TipoSemantico.BOOL && o.tipo != TipoSemantico.ERROR)
                registrarError("'not' solo aplica a expresiones de tipo bool", ref);
            Boolean val = (o.valor instanceof Boolean) ? !(Boolean) o.valor : null;
            pila.push(new ResultadoExpr(TipoSemantico.BOOL, val));
        } else {
            //Si el operado es int o double lo niega, si detecta string tira error
            if (o.tipo == TipoSemantico.INT) {
                pila.push(new ResultadoExpr(TipoSemantico.INT,    o.valor instanceof Integer ? -(Integer) o.valor : null));
            } else if (o.tipo.esDecimal()) {
                pila.push(new ResultadoExpr(o.tipo, o.valor instanceof Double ? -(Double) o.valor : null));
            } else if (o.tipo != TipoSemantico.ERROR) {
                registrarError("negacion unaria solo aplica a tipos numericos", ref);
                pila.push(ResultadoExpr.error());
            } else pila.push(ResultadoExpr.error());
        }
    }
//sacamos los operadores del tope de la pila y dependiendo cuales sea  
    private void aplicarBinario(String op, Deque<ResultadoExpr> pila, Token ref) {
        if (pila.size() < 2) return;
        ResultadoExpr der = pila.pop(), izq = pila.pop();
        switch (op) {
            case "SUMA": case "RESTA": case "MULTI": case "DIV": {
                //tipo de de operador aritemtico
                TipoSemantico tr = TipoSemantico.resultadoAritmetico(izq.tipo, der.tipo);
                if (tr == TipoSemantico.ERROR && izq.tipo != TipoSemantico.ERROR && der.tipo != TipoSemantico.ERROR)
                    registrarError("no se puede operar tipo '" + izq.tipo + "' y '" + der.tipo + "'", ref);
               // tipo de valor que debe tener resultado
                pila.push(new ResultadoExpr(tr, calcularAritmetico(op, izq, der, tr)));
                break;
            }
            // si es uno de estos operadores el resultado siempre debe ser un booleao
            case "MAYORQ": case "MENORQ": case "MAYORIGU": case "MENORIGU": case "EQUIVA": case "NEGA": {
                TipoSemantico tr = TipoSemantico.resultadoComparacion(izq.tipo, der.tipo);
                if (tr == TipoSemantico.ERROR && izq.tipo != TipoSemantico.ERROR && der.tipo != TipoSemantico.ERROR)
                    registrarError("no se puede comparar tipo '" + izq.tipo + "' con '" + der.tipo + "'", ref);
                pila.push(new ResultadoExpr(TipoSemantico.BOOL, calcularRelacional(op, izq, der)));
                break;
            }
            //ambos deben ser booleanos para calcular el resltoado
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

    // 

    private Object calcularAritmetico(String op, ResultadoExpr izq, ResultadoExpr der, TipoSemantico tipoRes) {
        //si uno de los dos valors no tiene un valor (viene de read()) devuelve null
        if (izq.valor == null || der.valor == null) return null;
        try {
            // si ambol tiene valor los opera con double para luego devolverlo a su tipo original
            double a = toDouble(izq.valor), b = toDouble(der.valor), r;
            switch (op) {
                case "SUMA":  r = a + b; break;
                case "RESTA": r = a - b; break;
                case "MULTI": r = a * b; break;
                case "DIV":   if (b == 0) return null; r = a / b; break;
                default: return null;
            }
            //double para float/double
            if (tipoRes == TipoSemantico.INT)    return (Integer)(int) r;
            return (Double) r;
        } catch (Exception e) { return null; }
    }
    //Si alguno de los dos no tiene valor conocido no puede calcular el resultado, devuelve null
    private Object calcularRelacional(String op, ResultadoExpr izq, ResultadoExpr der) {
        if (izq.valor == null || der.valor == null) return null;
        try {
            double a = toDouble(izq.valor), b = toDouble(der.valor);
            switch (op) {
                //convierte ambos operadores a double para aplicar la comparacior
                case "MAYORQ":   return a > b;  case "MENORQ":   return a < b;
                case "MAYORIGU": return a >= b; case "MENORIGU": return a <= b;
                case "EQUIVA":   return a == b; case "NEGA":     return a != b;
                default: return null;
            }
        } catch (Exception e) { return null; }
    }
// solo opera si ambos son boleanos para luego aplicar operadores logicos
    private Object calcularLogico(String op, ResultadoExpr izq, ResultadoExpr der) {
        if (!(izq.valor instanceof Boolean) || !(der.valor instanceof Boolean)) return null;
        boolean a = (Boolean) izq.valor, b = (Boolean) der.valor;
        return op.equals("AND") ? a && b : a || b;
    }

    private double toDouble(Object val) {
        if (val instanceof Integer) return ((Integer) val).doubleValue();
        if (val instanceof Double)  return (Double) val;
        throw new IllegalArgumentException("No es numerico: " + val);
    }

    /**
     * Formatea el valor segun el tipo de la variable:
     *   - Si la variable es INT, guarda Integer
     *   - Si la variable es FLOAT o DOUBLE, guarda Double
     */
    private Object formatearValor(Object valor, TipoSemantico tipo) {
        if (valor == null) return null;
        if (tipo == TipoSemantico.INT && valor instanceof Double)
            return ((Double) valor).intValue();
        if (tipo.esDecimal() && valor instanceof Integer)
            return ((Integer) valor).doubleValue();
        return valor;
    }
    //errores fase 3
    //recibe un mensjae y en que token sucedio el error 
    //agregando ña informacion del token donde ocurrio el error junto al mensaje de que fallo
    private void registrarError(String msg, Token t) {
        errores.add("Linea " + t.getLinea() + ", col " + t.getColumnaInicio() + ": **ERROR** " + msg);
    }
// recuperamos errores
//cuand encontramos algo invalido salta hasta el final de la linea ignorando los tokens anteriores
//posicionandose en el inicio de otra linea 
    private void saltarHastaNewline() {
        while (!es(TokenType.NEWLINE) && !es(TokenType.DEDENT) && !es(TokenType.EOF)) avanzar();
        if (es(TokenType.NEWLINE)) avanzar();
    }
//pasamos al siguiente token
    private void avanzar()
                {
                 if (pos < tokens.size() - 1) pos++;  
                }
 //revisa en que token estamos en ese momento
    private Token actual() 
                { 
                    return tokens.get(pos);
                }
    // revisa el oken que sigue sin avanzar 
    //se usa para la llamada de funciones principalmente 
    private Token siguiente()
            { 
            return pos + 1 < tokens.size() ? tokens.get(pos + 1) : tokens.get(tokens.size() - 1); 
            }
    //Revisa si el token actual es el token esperado
    private boolean es(TokenType t) 
    { 
        return actual().getTipo() == t; 
    }
        //analizamos los parametros de funciones para cer que los tipos sean validos
    private boolean esTipo(TokenType t) {
        return t == TokenType.INT  || t == TokenType.FLOAT  || t == TokenType.DOUBLE
            || t == TokenType.STRING || t == TokenType.BOOL;
    }
    // verifica si el token actual puede ser el inicio de una expresion 
    private boolean esInicioExpresion() {
        switch (actual().getTipo()) {
            case ID: case NUMENTERO: case NUMDECIMAL: case CADENASTRING:
            case TRUE: case FALSE: case PARENI: case NOT: case RESTA: return true;
            default: return false;
        }
    }
    // Sirve para distinguir un operador unario o un parentesis
    private boolean esOperandoSimple(TokenType t) {
        return t == TokenType.ID || t == TokenType.NUMENTERO || t == TokenType.NUMDECIMAL
            || t == TokenType.CADENASTRING || t == TokenType.TRUE || t == TokenType.FALSE;
    }
    //recibe el nombre de los token como string y devuelve true
    private boolean esBinario(String n) {
        switch (n) {
            case "SUMA": case "RESTA": case "MULTI": case "DIV":
            case "MAYORQ": case "MENORQ": case "MAYORIGU": case "MENORIGU":
            case "EQUIVA": case "NEGA": case "AND": case "OR": return true;
            default: return false;
        }
    }
    //Regresa un numero que significa la precedencia que tiene cada uno de los operadores
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
        //Busca si hay un paréntesis abierto pendiente en la pila de operadores
        //lo usamos para cuando se encuentra un ) para saber si es cerre de paretensis
        //o pertenece a una expresion 
    private boolean pilaContienePareni(Deque<String> p) {
        for (String s : p) if (s.equals("PARENI")) return true;
        return false;
    }
}