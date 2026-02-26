import java.io.*; 
import java.util.Stack;

public class Lexer {

    private String rutaEntrada;

    private boolean hayErrores = false;

    private Stack<Integer> pilaIndentacion = new Stack<>();

    public Lexer(String rutaEntrada) {
        this.rutaEntrada = rutaEntrada;

        pilaIndentacion.push(0);
    }

    // Método principal que realiza el análisis léxico
    public void analizar() {

        try {

            // Abrir archivo de entrada
            BufferedReader reader = new BufferedReader(new FileReader(rutaEntrada));

            // Crear nombre del archivo de salida (.out)
            String rutaSalida;
            if (rutaEntrada.endsWith(".mlng"))
                rutaSalida = rutaEntrada.substring(0, rutaEntrada.length() - 5) + ".out";
            else
                rutaSalida = rutaEntrada + ".out";

            BufferedWriter writer = new BufferedWriter(new FileWriter(rutaSalida));

            String linea;
            int numeroLinea = 0;

            // Leer el archivo línea por línea
            while ((linea = reader.readLine()) != null) {

                numeroLinea++;

                int espacios = 0;
                int i = 0;

                // Contar espacios al inicio en blanco
                while (i < linea.length() && linea.charAt(i) == ' ') {
                    espacios++;
                    i++;
                }

                int nivelActual = pilaIndentacion.peek();

                // Si aumentó indentación genera una indentacion
                if (espacios > nivelActual) {
                    pilaIndentacion.push(espacios);
                    writer.write("(" + numeroLinea + ", 1-1) INDENT");
                    writer.newLine();
                }

                // Si disminuyó indentación genera DEDENT
                else if (espacios < nivelActual) {
                    while (pilaIndentacion.peek() > espacios) {
                        pilaIndentacion.pop();
                        writer.write("(" + numeroLinea + ", 1-1) DEDENT");
                        writer.newLine();
                    }
                }

                int columna = espacios + 1;

                // Analizar carácter por carácter
                while (i < linea.length()) {

                    char c = linea.charAt(i);

                    // Ignorar espacios internos
                    if (Character.isWhitespace(c)) {
                        i++;
                        columna++;
                        continue;
                    }

                    // proceso que detecta las cadenas de texto
                    if (c == '"') {

                        int inicioCol = columna;
                        i++;
                        columna++;

                        StringBuilder sb = new StringBuilder();
                        boolean cerrado = false;

                        // Leer hasta encontrar comilla de cierre
                        while (i < linea.length()) {
                            if (linea.charAt(i) == '"') {
                                cerrado = true;
                                i++;
                                columna++;
                                break;
                            }
                            sb.append(linea.charAt(i));
                            i++;
                            columna++;
                        }

                        // Si no se cerró correctamente
                        if (!cerrado) {
                            System.out.println("line " + numeroLinea +", col " + inicioCol + ": ERROR cadena sin cerrar");
                            hayErrores = true;
                        } else {
                            writer.write("(" + numeroLinea + ", " + inicioCol + "-" + (columna - 1) +
                                    ") CADENASTRING " + sb.toString());
                            writer.newLine();
                        }
                        continue;
                    }

                    // lee los numeros
                    if (Character.isDigit(c)) {

                        int inicioCol = columna;
                        StringBuilder sb = new StringBuilder();
                        boolean esFloat = false;

                        // Leer dígitos y posible punto decimal
                        while (i < linea.length() &&
                                (Character.isDigit(linea.charAt(i)) ||
                                 linea.charAt(i) == '.')) {

                            if (linea.charAt(i) == '.') {
                                if (esFloat) break; 
                                esFloat = true;
                            }
                            sb.append(linea.charAt(i));
                            i++;
                            columna++;
                        }

                        // Clasificar como entero o decimal
                        if (esFloat)
                            writer.write("(" + numeroLinea + ", " +  inicioCol + "-" + (columna - 1) +") NUMDECIMAL " + sb.toString());
                        else
                            writer.write("(" + numeroLinea + ", " + inicioCol + "-" + (columna - 1) + ") NUMENTERO " + sb.toString());

                        writer.newLine();
                        continue;
                    }

                    // detectar palabras reservadas e identificadores
                    if (Character.isLetter(c)) {

                        int inicioCol = columna;
                        StringBuilder sb = new StringBuilder();

                        // Leer letras, números o _
                        while (i < linea.length() && (Character.isLetterOrDigit(linea.charAt(i)) ||linea.charAt(i) == '_')) {

                            sb.append(linea.charAt(i));
                            i++;
                            columna++;
                        }

                        String lexema = sb.toString();

                        // Validar longitud máxima 31 de un id
                        if (lexema.length() > 31) {
                            System.out.println("line " + numeroLinea + ", col " + inicioCol + ": ERROR identificador mayor a 31 caracteres");
                            hayErrores = true;
                            lexema = lexema.substring(0, 31);
                        }

                        // empareja cada lexema dentro de la cadena de texto con uno de los token creados
                        String tipo = "ID";

                        if (lexema.equals("int")) tipo = "INT";
                        else if (lexema.equals("float")) tipo = "FLOAT";
                        else if (lexema.equals("string")) tipo = "STRING";
                        else if (lexema.equals("bool")) tipo = "BOOL";
                        else if (lexema.equals("if")) tipo = "IF";
                        else if (lexema.equals("else")) tipo = "ELSE";
                        else if (lexema.equals("while")) tipo = "WHILE";
                        else if (lexema.equals("read")) tipo = "READ";
                        else if (lexema.equals("write")) tipo = "WRITE";
                        else if (lexema.equals("true")) tipo = "TRUE";
                        else if (lexema.equals("false")) tipo = "FALSE";

                        writer.write("(" + numeroLinea + ", " + inicioCol + "-" + (inicioCol + lexema.length() - 1) + ") " + tipo + " " + lexema);
                        writer.newLine();

                        continue;
                    }

                    // proceso para detectar si hay operadores dobles
                    if (i + 1 < linea.length()) {

                        String doble = "" + c + linea.charAt(i + 1);

                        if (doble.equals("==") || doble.equals("!=") ||
                                doble.equals(">=") || doble.equals("<=")) {

                            writer.write("(" + numeroLinea + ", " + columna + "-" + (columna + 1) + ") OPDOBLE " + doble);
                            writer.newLine();
                            i += 2;
                            columna += 2;
                            continue;
                        }
                    }

                    // identificar los operadores aritmeticos
                    if (c == '+' || c == '-' || c == '*' || c == '/') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna + ") OP_ARIT " + c);
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }
                    // identifica los mayor que y los menor que 
                    if (c == '<' || c == '>') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna +  ") OP_REL " + c);
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }
                    // identifica el simbolo igual
                    if (c == '=') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna +  ") IGUAL =");
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }

                    // identificar los cierres y abrir parentesis, comas
                    if (c == '(') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna + ") PARENI (");
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }

                    if (c == ')') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna + ") PAREND )");
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }

                    if (c == ',') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna + ") COMA ,");
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }

                    if (c == ':') {
                        writer.write("(" + numeroLinea + ", " + columna + "-" + columna +") DOSP :");
                        writer.newLine();
                        i++; columna++;
                        continue;
                    }

                    // Detecta los errores en las cadenas
                    System.out.println("line " + numeroLinea + ", col " + columna +": ERROR caracter invalido '" + c + "'");
                    hayErrores = true;

                    i++;
                    columna++;
                }

                // Agregar NEWLINE al final de cada línea
                writer.write("(" + numeroLinea + ", 1-1) NEWLINE \\n");
                writer.newLine();
            }

            // Vaciar indentaciones pendientes
            while (pilaIndentacion.size() > 1) {
                pilaIndentacion.pop();
                writer.write("(" + (numeroLinea + 1) + ", 1-1) DEDENT");
                writer.newLine();
            }

            // Agregar símbolo final $
            writer.write("(" + (numeroLinea + 1) + ", 1-1) $");
            writer.newLine();

            reader.close();
            writer.close();

            if (!hayErrores)
                System.out.println("Análisis léxico finalizado con éxito.");
            else
                System.out.println("Se encontraron errores léxicos.");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}