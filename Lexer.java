import java.io.*;
import java.util.*;

/**
 * Lexer de MiniLang.
 * Lee el archivo .mlng línea por línea y produce una lista de tokens.
 * Maneja indentación con pila (INDENT/DEDENT), comentarios con #,
 * cadenas con comillas dobles, números enteros y decimales,
 * identificadores (máx 31 caracteres) y todos los operadores del lenguaje.
 *
 * Novedades respecto a la versión anterior:
 *   - Reconoce la palabra reservada "const"
 *   - Reconoce la palabra reservada "double" como tipo de dato
 *   - Reconoce comentarios de bloque con /* ... * /
 */
public class Lexer {

    private final String rutaEntrada;
    private boolean hayErrores = false;
    private final Stack<Integer> pilaIndentacion = new Stack<>();
    private final List<Token> tokens = new ArrayList<>();

    public Lexer(String rutaEntrada) {
        this.rutaEntrada = rutaEntrada;
        pilaIndentacion.push(0); // nivel de indentación inicial = 0
    }

    public List<Token> getTokens()  { return tokens; }
    public boolean isHayErrores()   { return hayErrores; }

    // Agrega el token a la lista y lo escribe en el archivo de salida
    private void emitir(BufferedWriter writer, Token token) throws IOException {
        tokens.add(token);
        writer.write(token.toString());
        writer.newLine();
    }

    public void analizar() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(rutaEntrada));

            // El archivo de tokens de salida tiene la misma ruta pero extensión .out
            String rutaSalida = rutaEntrada.endsWith(".mlng")
                    ? rutaEntrada.substring(0, rutaEntrada.length() - 5) + ".out"
                    : rutaEntrada + ".out";

            BufferedWriter writer = new BufferedWriter(new FileWriter(rutaSalida));

            String linea;
            int numeroLinea = 0;
            boolean enComentarioBloque = false; // true si estamos dentro de /* ... */

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                int i = 0;

                // ── Manejo de comentarios de bloque /* ... */ ─────────────
                if (enComentarioBloque) {
                    int cierre = linea.indexOf("*/");
                    if (cierre >= 0) {
                        enComentarioBloque = false;
                        i = cierre + 2; // continuar después del cierre
                    } else {
                        // toda la línea sigue dentro del bloque de comentario
                        emitir(writer, new Token(TokenType.NEWLINE, "\\n", numeroLinea, 1, 1));
                        continue;
                    }
                }

                // ── Contar espacios de indentación ────────────────────────
                int espacios = 0;
                while (i < linea.length() && linea.charAt(i) == ' ') { espacios++; i++; }

                // Línea vacía o comentario de línea: no altera la indentación
                if (i >= linea.length() || linea.charAt(i) == '#') {
                    emitir(writer, new Token(TokenType.NEWLINE, "\\n", numeroLinea, 1, 1));
                    continue;
                }

                // ── Emitir INDENT / DEDENT según el nivel de indentación ──
                int nivelActual = pilaIndentacion.peek();
                if (espacios > nivelActual) {
                    pilaIndentacion.push(espacios);
                    emitir(writer, new Token(TokenType.INDENT, "", numeroLinea, 1, 1));
                } else if (espacios < nivelActual) {
                    while (pilaIndentacion.peek() > espacios) {
                        pilaIndentacion.pop();
                        emitir(writer, new Token(TokenType.DEDENT, "", numeroLinea, 1, 1));
                    }
                }

                int columna = espacios + 1;

                // ── Recorrer el resto de la línea ─────────────────────────
                while (i < linea.length()) {
                    char c = linea.charAt(i);

                    // Saltar espacios en blanco
                    if (Character.isWhitespace(c)) { i++; columna++; continue; }

                    // Comentario de línea con #
                    if (c == '#') break;

                    // Comentario de bloque /* ... */
                    if (c == '/' && i + 1 < linea.length() && linea.charAt(i + 1) == '*') {
                        int cierre = linea.indexOf("*/", i + 2);
                        if (cierre >= 0) {
                            // el comentario cierra en la misma línea
                            i = cierre + 2;
                            columna = i + 1;
                        } else {
                            // el comentario sigue en las siguientes líneas
                            enComentarioBloque = true;
                            break;
                        }
                        continue;
                    }

                    // ── Cadena de texto entre comillas dobles ─────────────
                    if (c == '"') {
                        int inicio = columna;
                        i++; columna++;
                        StringBuilder sb = new StringBuilder();
                        boolean cerrada = false;
                        while (i < linea.length()) {
                            if (linea.charAt(i) == '"') { cerrada = true; i++; columna++; break; }
                            sb.append(linea.charAt(i++));
                            columna++;
                        }
                        if (!cerrada) {
                            System.out.println("line " + numeroLinea + ", col " + inicio +
                                    ": ERROR cadena sin cerrar");
                            hayErrores = true;
                        } else {
                            emitir(writer, new Token(TokenType.CADENASTRING, sb.toString(),
                                    numeroLinea, inicio, columna - 1));
                        }
                        continue;
                    }

                    // ── Número entero o decimal ───────────────────────────
                    if (Character.isDigit(c)) {
                        int inicio = columna;
                        StringBuilder sb = new StringBuilder();
                        boolean esFloat = false;
                        while (i < linea.length() &&
                               (Character.isDigit(linea.charAt(i)) || linea.charAt(i) == '.')) {
                            if (linea.charAt(i) == '.') {
                                if (esFloat) break; // segundo punto → detener
                                esFloat = true;
                            }
                            sb.append(linea.charAt(i++));
                            columna++;
                        }
                        emitir(writer, new Token(
                                esFloat ? TokenType.NUMDECIMAL : TokenType.NUMENTERO,
                                sb.toString(), numeroLinea, inicio, columna - 1));
                        continue;
                    }

                    // ── Identificador o palabra reservada ─────────────────
                    if (Character.isLetter(c) || c == '_') {
                        int inicio = columna;
                        StringBuilder sb = new StringBuilder();
                        while (i < linea.length() &&
                               (Character.isLetterOrDigit(linea.charAt(i)) || linea.charAt(i) == '_')) {
                            sb.append(linea.charAt(i++));
                            columna++;
                        }
                        String lex = sb.toString();
                        // Los identificadores tienen un máximo de 31 caracteres
                        if (lex.length() > 31) {
                            System.out.println("line " + numeroLinea + ", col " + inicio +
                                    ": ERROR identificador mayor a 31 caracteres");
                            hayErrores = true;
                            lex = lex.substring(0, 31);
                        }
                        emitir(writer, new Token(clasificar(lex), lex,
                                numeroLinea, inicio, inicio + lex.length() - 1));
                        continue;
                    }

                    // ── Operadores de dos caracteres (==, !=, >=, <=) ────
                    if (i + 1 < linea.length()) {
                        String doble = "" + c + linea.charAt(i + 1);
                        TokenType td = null;
                        switch (doble) {
                            case "==": td = TokenType.EQUIVA;   break;
                            case "!=": td = TokenType.NEGA;     break;
                            case ">=": td = TokenType.MAYORIGU; break;
                            case "<=": td = TokenType.MENORIGU; break;
                        }
                        if (td != null) {
                            emitir(writer, new Token(td, doble, numeroLinea, columna, columna + 1));
                            i += 2; columna += 2;
                            continue;
                        }
                    }

                    // ── Punto y coma: tratado como terminador de sentencia ─
                    if (c == ';') {
                        emitir(writer, new Token(TokenType.NEWLINE, "\\n",
                                numeroLinea, columna, columna));
                        i++; columna++;
                        continue;
                    }

                    // ── Operadores y delimitadores de un carácter ─────────
                    TokenType ts = null;
                    switch (c) {
                        case '+': ts = TokenType.SUMA;   break;
                        case '-': ts = TokenType.RESTA;  break;
                        case '*': ts = TokenType.MULTI;  break;
                        case '/': ts = TokenType.DIV;    break;
                        case '<': ts = TokenType.MENORQ; break;
                        case '>': ts = TokenType.MAYORQ; break;
                        case '=': ts = TokenType.IGUAL;  break;
                        case '(': ts = TokenType.PARENI; break;
                        case ')': ts = TokenType.PAREND; break;
                        case ',': ts = TokenType.COMA;   break;
                        case ':': ts = TokenType.DOSP;   break;
                    }
                    if (ts != null) {
                        emitir(writer, new Token(ts, String.valueOf(c),
                                numeroLinea, columna, columna));
                        i++; columna++;
                        continue;
                    }

                    // ── Carácter no reconocido → error léxico ─────────────
                    System.out.println("line " + numeroLinea + ", col " + columna +
                            ": ERROR caracter invalido '" + c + "'");
                    hayErrores = true;
                    i++; columna++;
                }

                emitir(writer, new Token(TokenType.NEWLINE, "\\n", numeroLinea, 1, 1));
            }

            // Al final del archivo cerrar los bloques que quedaron abiertos
            while (pilaIndentacion.size() > 1) {
                pilaIndentacion.pop();
                emitir(writer, new Token(TokenType.DEDENT, "", numeroLinea + 1, 1, 1));
            }

            emitir(writer, new Token(TokenType.EOF, "$", numeroLinea + 1, 1, 1));

            reader.close();
            writer.close();

            System.out.println(hayErrores
                    ? "Se encontraron errores léxicos."
                    : "Análisis léxico finalizado con éxito.");

        } catch (Exception e) {
            System.out.println("Error al leer archivo: " + e.getMessage());
        }
    }

    /**
     * Clasifica un lexema como palabra reservada o identificador.
     * Incluye "const" y "double" como nuevas palabras reservadas.
     */
    private TokenType clasificar(String lex) {
        switch (lex) {
            // Tipos de dato
            case "int":    return TokenType.INT;
            case "float":  return TokenType.FLOAT;
            case "double": return TokenType.DOUBLE;   // nuevo
            case "string": return TokenType.STRING;
            case "bool":   return TokenType.BOOL;
            // Modificador de constante
            case "const":  return TokenType.CONST;    // nuevo
            // Control de flujo
            case "if":     return TokenType.IF;
            case "else":   return TokenType.ELSE;
            case "while":  return TokenType.WHILE;
            // Funciones
            case "funcion": return TokenType.FUNCION;
            case "return":  return TokenType.RETURN;
            // E/S
            case "read":   return TokenType.READ;
            case "write":  return TokenType.WRITE;
            // Literales booleanos
            case "true":   return TokenType.TRUE;
            case "false":  return TokenType.FALSE;
            // Operadores lógicos
            case "and":    return TokenType.AND;
            case "or":     return TokenType.OR;
            case "not":    return TokenType.NOT;
            // Todo lo demás es identificador
            default:       return TokenType.ID;
        }
    }
}