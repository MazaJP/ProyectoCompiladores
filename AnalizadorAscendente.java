import java.util.*;

/**
 *  P0 : PROGRAMA      → LISTA_SENT
 *  P1 : LISTA_SENT    → LISTA_SENT SENTENCIA
 *  P2 : LISTA_SENT    → SENTENCIA
 *  P3 : SENTENCIA     → NEWLINE
 *  P4 : SENTENCIA     → DECL NEWLINE
 *  P5 : SENTENCIA     → ASIGN NEWLINE
 *  P6 : SENTENCIA     → LLAMADA NEWLINE
 *  P7 : SENTENCIA     → SENT_IF
 *  P8 : SENTENCIA     → SENT_WHILE
 *  P9 : SENTENCIA     → DEF_FUNC
 *  P10: SENTENCIA     → SENT_RETURN NEWLINE
 *  P11: SENTENCIA     → SENT_READ NEWLINE
 *  P12: SENTENCIA     → SENT_WRITE NEWLINE
 *  P13: TIPO          → INT
 *  P14: TIPO          → FLOAT
 *  P15: TIPO          → STRING
 *  P16: TIPO          → BOOL
 *  P17: DECL          → TIPO ID IGUAL EXPR
 *  P18: DECL          → TIPO ID
 *  P19: ASIGN         → ID IGUAL EXPR
 *  P20: LLAMADA       → ID PARENI LISTA_ARGS PAREND
 *  P21: LLAMADA       → ID PARENI PAREND
 *  P22: SENT_IF       → IF EXPR DOSP NEWLINE BLOQUE ELSE DOSP NEWLINE BLOQUE
 *  P23: SENT_IF       → IF EXPR DOSP NEWLINE BLOQUE
 *  P24: SENT_WHILE    → WHILE EXPR DOSP NEWLINE BLOQUE
 *  P25: DEF_FUNC      → FUNCION ID PARENI LISTA_PARAMS PAREND DOSP NEWLINE BLOQUE
 *  P26: DEF_FUNC      → FUNCION ID PARENI PAREND DOSP NEWLINE BLOQUE
 *  P27: SENT_RETURN   → RETURN EXPR
 *  P28: SENT_RETURN   → RETURN
 *  P29: SENT_READ     → READ PARENI ID PAREND
 *  P30: SENT_WRITE    → WRITE PARENI LISTA_ARGS PAREND
 *  P31: BLOQUE        → INDENT LISTA_SENT DEDENT
 *  P32: LISTA_PARAMS  → LISTA_PARAMS COMA PARAM
 *  P33: LISTA_PARAMS  → PARAM
 *  P34: PARAM         → TIPO ID
 *  P35: LISTA_ARGS    → LISTA_ARGS COMA EXPR
 *  P36: LISTA_ARGS    → EXPR
 *  P37: EXPR          → EXPR OR EXPR
 *  P38: EXPR          → EXPR AND EXPR
 *  P39: EXPR          → NOT EXPR
 *  P40: EXPR          → EXPR MAYORQ   EXPR
 *  P41: EXPR          → EXPR MENORQ   EXPR
 *  P42: EXPR          → EXPR MAYORIGU EXPR
 *  P43: EXPR          → EXPR MENORIGU EXPR
 *  P44: EXPR          → EXPR EQUIVA   EXPR
 *  P45: EXPR          → EXPR NEGA     EXPR
 *  P46: EXPR          → EXPR SUMA     EXPR
 *  P47: EXPR          → EXPR RESTA    EXPR
 *  P48: EXPR          → EXPR MULTI    EXPR
 *  P49: EXPR          → EXPR DIV      EXPR
 *  P50: EXPR          → RESTA EXPR  (menos unario)
 *  P51: EXPR          → PARENI EXPR PAREND
 *  P52: EXPR          → ID PARENI LISTA_ARGS PAREND
 *  P53: EXPR          → ID PARENI PAREND
 *  P54: EXPR          → ID
 *  P55: EXPR          → NUMENTERO
 *  P56: EXPR          → NUMDECIMAL
 *  P57: EXPR          → CADENASTRING
 *  P58: EXPR          → TRUE
 *  P59: EXPR          → FALSE
 */
public class AnalizadorAscendente {


    private static final List<Produccion> GRAMATICA = new ArrayList<>();

    static {
        //contador de la produccion 
        int n = 0;
        GRAMATICA.add(new Produccion(n++, "PROGRAMA",     "LISTA_SENT"));
        GRAMATICA.add(new Produccion(n++, "LISTA_SENT",   "LISTA_SENT", "SENTENCIA"));
        GRAMATICA.add(new Produccion(n++, "LISTA_SENT",   "SENTENCIA"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "DECL",        "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "ASIGN",       "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "LLAMADA",     "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_IF"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_WHILE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "DEF_FUNC"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_RETURN", "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_READ",   "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_WRITE",  "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "INT"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "FLOAT"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "STRING"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "BOOL"));
        GRAMATICA.add(new Produccion(n++, "DECL",         "TIPO", "ID", "IGUAL", "EXPR"));
        GRAMATICA.add(new Produccion(n++, "DECL",         "TIPO", "ID"));
        GRAMATICA.add(new Produccion(n++, "ASIGN",        "ID", "IGUAL", "EXPR"));
        GRAMATICA.add(new Produccion(n++, "LLAMADA",      "ID", "PARENI", "LISTA_ARGS", "PAREND"));
        GRAMATICA.add(new Produccion(n++, "LLAMADA",      "ID", "PARENI", "PAREND"));
        GRAMATICA.add(new Produccion(n++, "SENT_IF",      "IF","EXPR","DOSP","NEWLINE","BLOQUE","ELSE","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "SENT_IF",      "IF","EXPR","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "SENT_WHILE",   "WHILE","EXPR","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "DEF_FUNC",     "FUNCION","ID","PARENI","LISTA_PARAMS","PAREND","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "DEF_FUNC",     "FUNCION","ID","PARENI","PAREND","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "SENT_RETURN",  "RETURN", "EXPR"));
        GRAMATICA.add(new Produccion(n++, "SENT_RETURN",  "RETURN"));
        GRAMATICA.add(new Produccion(n++, "SENT_READ",    "READ","PARENI","ID","PAREND"));
        GRAMATICA.add(new Produccion(n++, "SENT_WRITE",   "WRITE","PARENI","LISTA_ARGS","PAREND"));
        GRAMATICA.add(new Produccion(n++, "BLOQUE",       "INDENT","LISTA_SENT","DEDENT"));
        GRAMATICA.add(new Produccion(n++, "LISTA_PARAMS", "LISTA_PARAMS","COMA","PARAM"));
        GRAMATICA.add(new Produccion(n++, "LISTA_PARAMS", "PARAM"));
        GRAMATICA.add(new Produccion(n++, "PARAM",        "TIPO","ID"));
        GRAMATICA.add(new Produccion(n++, "LISTA_ARGS",   "LISTA_ARGS","COMA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "LISTA_ARGS",   "EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","OR","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","AND","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "NOT","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MAYORQ","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MENORQ","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MAYORIGU","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MENORIGU","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","EQUIVA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","NEGA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","SUMA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","RESTA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MULTI","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","DIV","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "RESTA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "PARENI","EXPR","PAREND"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "ID","PARENI","LISTA_ARGS","PAREND"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "ID","PARENI","PAREND"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "ID"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "NUMENTERO"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "NUMDECIMAL"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "CADENASTRING"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "TRUE"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "FALSE"));
    }
    //se resuelve la presedencia por medio de que el numero mas grande se resuelve primero 
    private static final Map<String, Integer> PRECEDENCIA = new HashMap<>();

    private static final Set<String> ASOC_DERECHA = new HashSet<>();

    static {
        //se define la presendencia de cada uno de los operadores
        PRECEDENCIA.put("OR",       1);
        PRECEDENCIA.put("AND",      2);
        PRECEDENCIA.put("NOT",      3);
        PRECEDENCIA.put("EQUIVA",   4);
        PRECEDENCIA.put("NEGA",     4);
        PRECEDENCIA.put("MAYORQ",   4);
        PRECEDENCIA.put("MENORQ",   4);
        PRECEDENCIA.put("MAYORIGU", 4);
        PRECEDENCIA.put("MENORIGU", 4);
        PRECEDENCIA.put("SUMA",     5);
        PRECEDENCIA.put("RESTA",    5);
        PRECEDENCIA.put("MULTI",    6);
        PRECEDENCIA.put("DIV",      6);
        PRECEDENCIA.put("UNARIO",   7);
        ASOC_DERECHA.add("NOT");
        ASOC_DERECHA.add("UNARIO");
    }
//lista de tokens 
    private final List<Token> tokens;
    //token donde empezamos 
    private int pos;
    //push pop 
    private final Deque<SimboloPila> pila = new ArrayDeque<>();
    private final List<String> errores    = new ArrayList<>();

    //se acumulan los errores 
    private boolean yaRecupero = false;

    public AnalizadorAscendente(List<Token> tokens) {
        this.tokens = tokens; //agara la lista de los tokens
        this.pos    = 0; 
        pila.push(new SimboloPila("$"));//se indica el fondo de la pila
    }

    public boolean analizar() {
        parsearListaSentencias();
        //aplical la primera produccion que es la que cierra todo
        if (!pila.isEmpty() && pila.peek().getNombre().equals("LISTA_SENT")) {
            reducir(GRAMATICA.get(0));
        }
//si algo queda algun token por analizar se tira error
        if (!es(TokenType.EOF)) {
            registrarError("token inesperado al final del archivo", actual());
        }
//todo en orden 
        return errores.isEmpty();
    }

    public List<String> getErrores() { return errores; }

    //DESPLAZAR: mueve el token actual al tope de la pila y avanza.
    private Token desplazar() {
        Token t = actual();
        pila.push(new SimboloPila(t));//guarda el token como terminal 
        if (pos < tokens.size() - 1) pos++; //siguiente token 
        return t;
    }

     //REDUCIR: saca los elementos del cuerpo de la producción y empuja el no-terminal de la cabeza
    private void reducir(Produccion p) {
        for (int i = 0; i < p.getCuerpo().length; i++) {
            pila.pop(); // saca los elementos de la pila depediedo cuantos token haya en la produ 
        }
        pila.push(new SimboloPila(p.getCabeza())); // remplaza lo sacado por un no terminal
    }

//devuelve el token actual
    private Token actual() { return tokens.get(pos); }
//Mira el token que viene después del actual Si no hay siguiente devuelve el último 
    private Token siguiente() {
        return pos + 1 < tokens.size() ? tokens.get(pos + 1) : tokens.get(tokens.size() - 1);
    }
//verifica el tipo del token
    private boolean es(TokenType tipo) { return actual().getTipo() == tipo; }
//revisa si el token es una palabra de tipo dato  
    private boolean esTipo(TokenType t) {
        return t == TokenType.INT || t == TokenType.FLOAT
            || t == TokenType.STRING || t == TokenType.BOOL;
    }
//revisa si el token actual puede ser el inicio de una expresión.
    private boolean esInicioExpresion() {
        switch (actual().getTipo()) {
            case ID: case NUMENTERO: case NUMDECIMAL: case CADENASTRING:
            case TRUE: case FALSE: case PARENI: case NOT: case RESTA:
                return true;
            default: return false;
        }
    }

    
     //Formatea el mensaje de error con la ubicación exacta y lo agrega a la lista sin detener
    private void registrarError(String mensaje, Token t) {
        String simbolo;
        if (t.getTipo() == TokenType.NEWLINE || t.getTipo() == TokenType.EOF) {
            simbolo = "fin de línea";
        } else {
            simbolo = "'" + t.getLexema() + "'";
        }
        errores.add("Línea " + t.getLinea() + ", col " + t.getColumnaInicio() + ": " + simbolo + " Error: " + mensaje);
    }

    
    
    //al detectar un error salta hasta el siguiente salto de linea  
    private void recuperar() {
        yaRecupero = true;
        while (!es(TokenType.NEWLINE) && !es(TokenType.DEDENT) && !es(TokenType.EOF)) {
            if (pos < tokens.size() - 1) pos++;
            else break;
        }
        if (es(TokenType.NEWLINE) && pos < tokens.size() - 1) pos++;
    }

    
     //Intenta desplazar el tipo esperado. Si no coincide, reporta error y NO avanza
    private void esperarYDesplazar(TokenType tipo, String mensaje) {
    if (es(tipo)) {
        desplazar();
    } else {
         // si no es el esperado, reporta error usando el token anterior como referencia
        Token referencia = pos > 0 ? tokens.get(pos - 1) : actual();
        String simbolo;
        if (actual().getTipo() == TokenType.NEWLINE || actual().getTipo() == TokenType.EOF) {
            simbolo = "fin de línea";
        } else {
            simbolo = "'" + actual().getLexema() + "'";
        }
        errores.add("Línea " + referencia.getLinea() + ", col " + referencia.getColumnaFin()
                + ": " + simbolo + " Error: " + mensaje);
        yaRecupero = true;
    }
}
    //Consume el NEWLINE al final de una sentencia simple.
    //Si ya hubo error en esta sentencia, avanza sin reportar más.
    private void consumirNewline() {
    if (yaRecupero) {
        // ya hubo error en esta sentencia: saltar sin reportar más
        while (!es(TokenType.NEWLINE) && !es(TokenType.DEDENT) && !es(TokenType.EOF)) {
            if (pos < tokens.size() - 1) pos++;
            else break;
        }
        if (es(TokenType.NEWLINE) && pos < tokens.size() - 1) pos++;
        return; //sale
    }
    if (es(TokenType.NEWLINE)) {
        desplazar(); // consume el token newline 
    } else if (!es(TokenType.EOF) && !es(TokenType.DEDENT)) {
        // hay algo donde debería haber NEWLINE
        registrarError("se esperaba fin de línea", actual());
        yaRecupero = true;
        // saltar el resto de la línea
        while (!es(TokenType.NEWLINE) && !es(TokenType.DEDENT) && !es(TokenType.EOF)) {
            if (pos < tokens.size() - 1) pos++;
            else break;
        }
        if (es(TokenType.NEWLINE) && pos < tokens.size() - 1) pos++;
    }
}


    private void parsearListaSentencias() {
        boolean primero = true; // controla que producion de Lista sentencia va a usar
    //Procesa sentencias mientras no sea fin de archivo ni fin de bloque.
        while (!es(TokenType.EOF) && !es(TokenType.DEDENT)) {

            if (es(TokenType.NEWLINE)) {
                desplazar(); //desplazarse a newline
                reducir(GRAMATICA.get(3)); //va a la produccion 1
            } else {
                parsearSentencia(); // cualquier otro token inicia sentencia real
            }

            if (primero) {
                reducir(GRAMATICA.get(2)); // va a la produccion 2 
                primero = false;
            } else {
                reducir(GRAMATICA.get(1)); // va a la produccion 1
            }
        }

        if (primero) {
            pila.push(new SimboloPila("LISTA_SENT"));
        }
    }


    private void parsearSentencia() {
        yaRecupero = false;
        Token t = actual();
        try {
            switch (t.getTipo()) {
                case INT: case FLOAT: case STRING: case BOOL:
                    parsearDeclaracion(); break;
                case ID:
                    parsearAsignacionOLlamada(); break;
                case IF:
                    parsearIf(); break;
                case WHILE:
                    parsearWhile(); break;
                case FUNCION:
                    parsearDefinicionFuncion(); break;
                case RETURN:
                    parsearReturn(); break;
                case READ:
                    parsearRead(); break;
                case WRITE:
                    parsearWrite(); break;
                default:
                    registrarError("inicio de sentencia inválido", t);
                    recuperar();// salta al próximo NEWLINE 
                    // pone SENTENCIA vacía para continuar
                    pila.push(new SimboloPila("SENTENCIA")); 
            }
        } catch (Exception e) {
            if (!yaRecupero) {
                // por si algo lanza excepción no esperada
                registrarError("error inesperado en la sentencia", t);
                recuperar();
            }
            pila.push(new SimboloPila("SENTENCIA"));
        }
    }


    private void parsearDeclaracion() {
        desplazar();  // desplazar a int/float/string/bool
        reducirTipo(); // REDUCIR int/float/string/bool - TIPO

        if (!es(TokenType.ID)) {
            // después del tipo debe venir un nombre de variable
            registrarError("se esperaba identificador", actual());
            yaRecupero = true;
            reducir(GRAMATICA.get(18)); //hace redusccion a la produccion 18
            consumirNewline();
            reducir(GRAMATICA.get(4));//hace reduccion a la produccion 4
            return;
        }
        desplazar(); //se desplaza hacia Id

        if (es(TokenType.IGUAL)) {
            desplazar(); //desplazar a =
            parsearExpresion(); //analiza el valor 
            reducir(GRAMATICA.get(17)); //hace redusccion a la produccion 17
            consumirNewline();
        } else {
            reducir(GRAMATICA.get(18));//hace redusccion a la produccion 18
            consumirNewline();
        }

        consumirNewline();
        reducir(GRAMATICA.get(4)); //hace reduccion a la produccion 4
    }

    private void reducirTipo() {
        switch (pila.peek().getToken().getTipo()) {
            case INT:    reducir(GRAMATICA.get(13)); break; //hace reduccion a la produccion 13
            case FLOAT:  reducir(GRAMATICA.get(14)); break; //hace reduccion a la produccion 14
            case STRING: reducir(GRAMATICA.get(15)); break;//hace reduccion a la produccion 15
            default:     reducir(GRAMATICA.get(16)); break; //hace reduccion a la produccion 16
        }
    }


    private void parsearAsignacionOLlamada() {
        desplazar(); //desplazar id 

        if (es(TokenType.IGUAL)) {
            //asignacion nombreVar = expresion 
            desplazar();
            parsearExpresion();
            reducir(GRAMATICA.get(19)); //hace reduccion a la produccion 19
            consumirNewline();
            reducir(GRAMATICA.get(5)); //hace reduccion a la produccion 5
        } else if (es(TokenType.PARENI)) {
            desplazar(); //desplazar (
            if (es(TokenType.PAREND)) {
                desplazar();
                reducir(GRAMATICA.get(21)); //hace reduccion a la produccion 21
            } else {
                parsearListaArgumentos(); //analiza lo que tenemos 
                //si no se cerro parentesis marca ese error 
                esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
                reducir(GRAMATICA.get(20));
            }
            consumirNewline();
            reducir(GRAMATICA.get(6)); //hace reduccion a la produccion 6

        } else {
            // después de un ID no hay = o ( tira error
            registrarError("se esperaba '=' o '(' después del identificador", actual());
            recuperar();
            pila.push(new SimboloPila("SENTENCIA"));
        }
    }


    private void parsearIf() {
        desplazar(); //desplazar a if   
        parsearExpresion();
        esperarYDesplazar(TokenType.DOSP,    "se esperaba ':'"); 
        esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea tras ':'");
        parsearBloque();

        if (es(TokenType.ELSE)) {
            //desplazar a else
            desplazar();
            //errores 
            esperarYDesplazar(TokenType.DOSP,    "se esperaba ':' después de else");
            esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea tras else:");
            parsearBloque();
            reducir(GRAMATICA.get(22)); //hace reduccion a la produccion 22
        } else {
            reducir(GRAMATICA.get(23)); //hace reduccion a la produccion 23
        }
        reducir(GRAMATICA.get(7)); //hace reduccion a la produccion 7
    }
 
    private void parsearWhile() {
        desplazar(); //desplazar a while 
        parsearExpresion(); //condicion del while 
        //errores 
        esperarYDesplazar(TokenType.DOSP,    "se esperaba ':'");
        esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea tras ':'");
        parsearBloque(); //cuerpo del while 
        reducir(GRAMATICA.get(24)); //hace reduccion a la produccion 24
        reducir(GRAMATICA.get(8)); //hace reduccion a la produccion 8
    }

    private void parsearDefinicionFuncion() {
        desplazar(); //desplazar a funcion 
        //errores
        esperarYDesplazar(TokenType.ID,     "se esperaba nombre de función");
        esperarYDesplazar(TokenType.PARENI, "se esperaba '('");

        if (es(TokenType.PAREND)) {
            desplazar(); // desplazar ) 
            esperarYDesplazar(TokenType.DOSP,    "se esperaba ':'");
            esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea");
            parsearBloque();
            reducir(GRAMATICA.get(26)); //hace reduccion a la produccion 26
        } else {
            //revisa si la funcion tiene sus parametros
            parsearListaParametros();
            esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
            esperarYDesplazar(TokenType.DOSP,   "se esperaba ':'");
            esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea");
            parsearBloque();
            reducir(GRAMATICA.get(25)); //hace reduccion a la produccion 25
        }
        reducir(GRAMATICA.get(9)); //hace reduccion a la produccion 9
    }

    private void parsearReturn() {
        desplazar(); //desplazar a return 
        if (esInicioExpresion()) {
            parsearExpresion();
            reducir(GRAMATICA.get(27)); //hace reduccion a la produccion 27
        } else {
            reducir(GRAMATICA.get(28)); //hace reduccion a la produccion 28
        }
        consumirNewline();
        reducir(GRAMATICA.get(10)); //hace reduccion a la produccion 10
    }

    private void parsearRead() {
        desplazar(); //desplazar read
        esperarYDesplazar(TokenType.PARENI, "se esperaba '('");
        esperarYDesplazar(TokenType.ID,     "se esperaba identificador");
        esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
        reducir(GRAMATICA.get(29)); //hace reduccion a la produccion 29
        consumirNewline();
        reducir(GRAMATICA.get(11)); //hace reduccion a la produccion 11
    }


    private void parsearWrite() {
        desplazar(); //desplazar write
        esperarYDesplazar(TokenType.PARENI, "se esperaba '('");
        parsearListaArgumentos(); //acepta cualquier expresion 
        esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
        reducir(GRAMATICA.get(30)); //hace reduccion a la produccion 30
        consumirNewline();
        reducir(GRAMATICA.get(12)); //hace reduccion a la produccion 12
    }

    private void parsearBloque() {

        if (!es(TokenType.INDENT)) {
            //tira error donde deberia haber una identacion
            registrarError("se esperaba bloque indentado", actual());
            pila.push(new SimboloPila("BLOQUE")); //se pone como bloque vacio para no romper
            return;
        }
        desplazar(); // desplazar a indent
        parsearListaSentencias(); //analiza el contenido del bloque 
        if (!es(TokenType.DEDENT)) {
            registrarError("se esperaba fin del bloque (DEDENT)", actual());
        } else {
            desplazar(); //desplazar a dedent
        }
        reducir(GRAMATICA.get(31)); //hace reduccion a la produccion 31
    }

    private void parsearListaParametros() {
        parsearParametro(); //primer parametro 
        reducir(GRAMATICA.get(33)); //hace reduccion a la produccion 33

        while (es(TokenType.COMA)) { //si hay una coma puede haber mas  parametros
        //
            desplazar(); //desplazar a , 
            parsearParametro();
            reducir(GRAMATICA.get(32));//hace reduccion a la produccion 32
        }
    }

    private void parsearParametro() {
        if (!esTipo(actual().getTipo())) {
            //si no detecta el tipo de parametro tira error 
            registrarError("se esperaba tipo en el parámetro", actual());
            pila.push(new SimboloPila("PARAM"));
            return;
        }
        desplazar(); // desplazar tipo
        reducirTipo(); // REDUCIR int/float/string/bool - TIPO
        esperarYDesplazar(TokenType.ID, "se esperaba nombre de parámetro");
        reducir(GRAMATICA.get(34)); //hace reduccion a la produccion 34
    }


    private void parsearListaArgumentos() {
        if (!esInicioExpresion()) {
            //si no hay ninguna expresion la lista esta vacia 
            pila.push(new SimboloPila("LISTA_ARGS"));
            return;
        }
        parsearExpresion(); //primer argumento 
        reducir(GRAMATICA.get(36)); //hace reduccion a la produccion 36

        while (es(TokenType.COMA)) {
            desplazar(); //desplazar ,
            if (!esInicioExpresion()) {
                // Coma final sin argumento reporta error
                registrarError("se esperaba expresión después de ','", actual());
                yaRecupero = true;
                return;
            }
            parsearExpresion();
            reducir(GRAMATICA.get(35)); //hace reduccion a la produccion 35
        }
    }

    private void parsearExpresion() {
        Deque<String> pilaOps  = new ArrayDeque<>();  // pila auxiliar de operadores pendientes
        int numOperandos       = 0; // cuántos operandos hay acumulados
        boolean esperaOperando = true;

        bucle:
        while (true) {
            Token t    = actual();
            String tipo = t.getTipo().name(); // nombre del tipo string

            if (esperaOperando) {

                if (t.getTipo() == TokenType.NOT) {
                    desplazar(); // desplazar not 
                    pilaOps.push("NOT");  // va a la pila de las operaciones 

                } else if (t.getTipo() == TokenType.RESTA) {
                    desplazar(); //desplazar -
                    pilaOps.push("UNARIO");

                } else if (t.getTipo() == TokenType.PARENI) {
                    desplazar(); //desplazar (
                    pilaOps.push("PARENI");

                } else if (esOperandoSimple(t.getTipo())) {
                    //valor
                    if (t.getTipo() == TokenType.ID
                            && siguiente().getTipo() == TokenType.PARENI) {
                        parsearLlamadaEnExpresion(); // si es ID( es una llamada a función
                    } else {
                        desplazar();   // si es valor simple, solo se desplaza
                        aplicarReduccionOperando(t.getTipo());
                    }
                    numOperandos++;
                    esperaOperando = false; // ya tenemos operando, ahora esperamos operador

                    while (!pilaOps.isEmpty()
                            && (pilaOps.peek().equals("NOT")
                                || pilaOps.peek().equals("UNARIO"))) {
                        aplicarOperadorUnario(pilaOps.pop()); // REDUCIR: NOT EXPR o RESTA EXPR
                    }

                } else {
                    if (!yaRecupero) {
                        registrarError("expresión inválida", t); //se detecta token inseperado
                        yaRecupero = true;
                    }
                    break;
                }

            } else {
                if (t.getTipo() == TokenType.PAREND && pilaContienePareni(pilaOps)) {
                    // es ) y hay un ( pendiente en pila se cierra paréntesis
                    while (!pilaOps.isEmpty() && !pilaOps.peek().equals("PARENI")) {
                        reducirOperadorBinario(pilaOps.pop()); // reduce ops dentro del paréntesis
                        numOperandos--;
                    }
                    if (!pilaOps.isEmpty()) pilaOps.pop(); // saca el PARENI
                    desplazar(); //desplazar ) 
                    reducirProduccion(51); //hace la reduccion a la produccion 51

                } else if (PRECEDENCIA.containsKey(tipo)) {
                     // es un operador binario como + - * / == etc.
                // antes de meter este operador,  se reduce los del tope que tengan más precedencia
                    while (!pilaOps.isEmpty()
                            && !pilaOps.peek().equals("PARENI")
                            && !pilaOps.peek().equals("NOT")
                            && !pilaOps.peek().equals("UNARIO")) {
                        String tope      = pilaOps.peek();
                        int precTope     = PRECEDENCIA.getOrDefault(tope, 0);
                        int precActual   = PRECEDENCIA.get(tipo);
                        boolean reducir  = (precTope > precActual) // tope tiene más precedencia
                                || (precTope == precActual && !ASOC_DERECHA.contains(tipo)); // igual y asocia izquierda
                        if (reducir) {
                            reducirOperadorBinario(pilaOps.pop()); // REDUCIR operador del tope 
                            numOperandos--;
                        } else {
                            break;
                        }
                    }
                    pilaOps.push(tipo); // meter el operador actual a la pila
                    desplazar(); //desplazar el operador 
                    esperaOperando = true; // se espera el operando derecho

                } else {
                    break bucle;
                }
            }
        }

        while (!pilaOps.isEmpty()) {
            String op = pilaOps.pop();
            if (op.equals("PARENI")) {
                registrarError("paréntesis sin cerrar", actual()); // no se cerro el parentesis 
            } else if (op.equals("NOT") || op.equals("UNARIO")) {
                aplicarOperadorUnario(op);
            } else {
                reducirOperadorBinario(op); // operador binario que quedó pendiente
                numOperandos--;
            }
        }

        pila.push(new SimboloPila("EXPR"));  // resultado final en la pila principal
    }

    private void parsearLlamadaEnExpresion() {
        desplazar(); //desplazar id
        desplazar(); //desplazar (

        if (es(TokenType.PAREND)) {
            desplazar();
            reducirProduccion(53); // hacer la reduccion por la produccion 53 
        } else {
            parsearListaArgumentos();
            esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
            reducirProduccion(52); // hacer la reduccion por la produccion 52
        }
    }

    private void aplicarReduccionOperando(TokenType tipo) {
        switch (tipo) {
            //reducciones finales de los operadores 
            case ID:           reducirProduccion(54); break;
            case NUMENTERO:    reducirProduccion(55); break;
            case NUMDECIMAL:   reducirProduccion(56); break;
            case CADENASTRING: reducirProduccion(57); break;
            case TRUE:         reducirProduccion(58); break;
            default:           reducirProduccion(59); break;
        }
    }

    private void aplicarOperadorUnario(String op) {
        if (op.equals("NOT")) {
            reducirProduccion(39); // hacer la reduccion por la produccion 39
        } else {
            reducirProduccion(50); // hacer la reduccion por la produccion 50
        }
    }

    private void reducirOperadorBinario(String op) {
        switch (op) {
            //reduccion final de los demas operadores 
            case "OR":       reducirProduccion(37); break;
            case "AND":      reducirProduccion(38); break;
            case "MAYORQ":   reducirProduccion(40); break;
            case "MENORQ":   reducirProduccion(41); break;
            case "MAYORIGU": reducirProduccion(42); break;
            case "MENORIGU": reducirProduccion(43); break;
            case "EQUIVA":   reducirProduccion(44); break;
            case "NEGA":     reducirProduccion(45); break;
            case "SUMA":     reducirProduccion(46); break;
            case "RESTA":    reducirProduccion(47); break;
            case "MULTI":    reducirProduccion(48); break;
            case "DIV":      reducirProduccion(49); break;
        }
    }

    private void reducirProduccion(int numero) {
        reducir(GRAMATICA.get(numero));
   }

    private boolean esOperandoSimple(TokenType t) {
          // estos son todos los tipos que pueden ser un operando directo
        return t == TokenType.ID       || t == TokenType.NUMENTERO
            || t == TokenType.NUMDECIMAL || t == TokenType.CADENASTRING
            || t == TokenType.TRUE     || t == TokenType.FALSE;
    }

    private boolean pilaContienePareni(Deque<String> pilaOps) {
        for (String s : pilaOps) if (s.equals("PARENI")) return true;
        return false;
    }
}
 