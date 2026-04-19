import java.io.*;
import java.util.*;

public class Lexer {

    private String rutaEntrada;
    private boolean hayErrores = false;
    private Stack<Integer> pilaIndentacion = new Stack<>();
    private List<Token> tokens = new ArrayList<>();

    public Lexer(String rutaEntrada) {
        this.rutaEntrada = rutaEntrada;
        pilaIndentacion.push(0);
    }

    public List<Token> getTokens() { return tokens; }
    public boolean isHayErrores() { return hayErrores; }

    private void emitir(BufferedWriter writer, Token token) throws IOException {
        tokens.add(token);
        writer.write(token.toString());
        writer.newLine();
    }

    public void analizar() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(rutaEntrada));

            String rutaSalida = rutaEntrada.endsWith(".mlng")
                    ? rutaEntrada.substring(0, rutaEntrada.length() - 5) + ".out"
                    : rutaEntrada + ".out";

            BufferedWriter writer = new BufferedWriter(new FileWriter(rutaSalida));

            String linea;
            int numeroLinea = 0;

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                int i = 0;
                int espacios = 0;

                while (i < linea.length() && linea.charAt(i) == ' ') { espacios++; i++; }

                // Línea vacía o comentario: no altera indentación
                if (i >= linea.length() || linea.charAt(i) == '#') {
                    emitir(writer, new Token(TokenType.NEWLINE, "\\n", numeroLinea, 1, 1));
                    continue;
                }

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

                while (i < linea.length()) {
                    char c = linea.charAt(i);

                    if (Character.isWhitespace(c)) { i++; columna++; continue; }
                    if (c == '#') break;

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
                            System.out.println("line " + numeroLinea + ", col " + inicio + ": ERROR cadena sin cerrar");
                            hayErrores = true;
                        } else {
                            emitir(writer, new Token(TokenType.CADENASTRING, sb.toString(), numeroLinea, inicio, columna - 1));
                        }
                        continue;
                    }

                    if (Character.isDigit(c)) {
                        int inicio = columna;
                        StringBuilder sb = new StringBuilder();
                        boolean esFloat = false;
                        while (i < linea.length() && (Character.isDigit(linea.charAt(i)) || linea.charAt(i) == '.')) {
                            if (linea.charAt(i) == '.') { if (esFloat) break; esFloat = true; }
                            sb.append(linea.charAt(i++));
                            columna++;
                        }
                        emitir(writer, new Token(
                                esFloat ? TokenType.NUMDECIMAL : TokenType.NUMENTERO,
                                sb.toString(), numeroLinea, inicio, columna - 1));
                        continue;
                    }

                    if (Character.isLetter(c) || c == '_') {
                        int inicio = columna;
                        StringBuilder sb = new StringBuilder();
                        while (i < linea.length() && (Character.isLetterOrDigit(linea.charAt(i)) || linea.charAt(i) == '_')) {
                            sb.append(linea.charAt(i++));
                            columna++;
                        }
                        String lex = sb.toString();
                        if (lex.length() > 31) {
                            System.out.println("line " + numeroLinea + ", col " + inicio + ": ERROR identificador mayor a 31 caracteres");
                            hayErrores = true;
                            lex = lex.substring(0, 31);
                        }
                        emitir(writer, new Token(clasificar(lex), lex, numeroLinea, inicio, inicio + lex.length() - 1));
                        continue;
                    }

                    // Operadores de dos caracteres
                    if (i + 1 < linea.length()) {
                        String doble = "" + c + linea.charAt(i + 1);
                        TokenType td = null;
                        switch (doble) {
                            case "==": td = TokenType.EQUIVA; break;
                            case "!=": td = TokenType.NEGA; break;
                            case ">=": td = TokenType.MAYORIGU; break;
                            case "<=": td = TokenType.MENORIGU; break;
                        }
                        if (td != null) {
                            emitir(writer, new Token(td, doble, numeroLinea, columna, columna + 1));
                            i += 2; columna += 2;
                            continue;
                        }
                    }

                    // Operadores de un carácter
                    TokenType ts = null;
                    switch (c) {
                        case '+': ts = TokenType.SUMA; break;
                        case '-': ts = TokenType.RESTA; break;
                        case '*': ts = TokenType.MULTI; break;
                        case '/': ts = TokenType.DIV; break;
                        case '<': ts = TokenType.MENORQ; break;
                        case '>': ts = TokenType.MAYORQ; break;
                        case '=': ts = TokenType.IGUAL; break;
                        case '(': ts = TokenType.PARENI; break;
                        case ')': ts = TokenType.PAREND; break;
                        case ',': ts = TokenType.COMA; break;
                        case ':': ts = TokenType.DOSP; break;
                        case ';': ts = TokenType.PUNCOM; break;
                    }
                    if (ts != null) {
                        emitir(writer, new Token(ts, String.valueOf(c), numeroLinea, columna, columna));
                        i++; columna++;
                        continue;
                    }

                    System.out.println("line " + numeroLinea + ", col " + columna + ": ERROR caracter invalido '" + c + "'");
                    hayErrores = true;
                    i++; columna++;
                }

                emitir(writer, new Token(TokenType.NEWLINE, "\\n", numeroLinea, 1, 1));
            }

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

    private TokenType clasificar(String lex) {
        switch (lex) {
            case "int":     return TokenType.INT;
            case "float":   return TokenType.FLOAT;
            case "string":  return TokenType.STRING;
            case "bool":    return TokenType.BOOL;
            case "if":      return TokenType.IF;
            case "else":    return TokenType.ELSE;
            case "while":   return TokenType.WHILE;
            case "funcion": return TokenType.FUNCION;
            case "return":  return TokenType.RETURN;
            case "read":    return TokenType.READ;
            case "write":   return TokenType.WRITE;
            case "true":    return TokenType.TRUE;
            case "false":   return TokenType.FALSE;
            case "and":     return TokenType.AND;
            case "or":      return TokenType.OR;
            case "not":     return TokenType.NOT;
            default:        return TokenType.ID;
        }
    }
}