import java.util.Scanner;

/**
 * Punto de entrada del compilador MiniLang.
 *
 * Ejecuta las tres fases en orden:
 *   1. Análisis léxico   (Lexer)
 *   2. Análisis sintáctico (AnalizadorAscendente)
 *   3. Análisis semántico  (AnalizadorSemantico)
 *
 * Solo pasa a la siguiente fase si la anterior no tuvo errores,
 * para evitar cascadas de falsos positivos.
 *
 * Salida final:
 *   - "OK" si el archivo es léxica, sintáctica y semánticamente correcto.
 *   - Lista de errores si alguna fase falla.
 */
public class Main {

    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);
        System.out.print("Ingrese archivo .mlng: ");
        String ruta = entrada.nextLine().trim();

        // ── FASE 1: Análisis léxico ────────────────────────────────────────
        Lexer lexer = new Lexer(ruta);
        lexer.analizar();

        // Si hubo errores léxicos, los reporta y detiene la compilación
        if (lexer.isHayErrores()) {
            System.out.println("\nCompilación detenida: errores léxicos encontrados.");
            return;
        }

        // ── FASE 2: Análisis sintáctico ────────────────────────────────────
        AnalizadorAscendente parser = new AnalizadorAscendente(lexer.getTokens());
        boolean sintacticoOK = parser.analizar();

        if (!sintacticoOK) {
            // Imprimir todos los errores sintácticos encontrados
            System.out.println("\nErrores sintácticos:");
            for (String e : parser.getErrores()) {
                System.out.println("  " + e);
            }
            System.out.println("\nCompilación detenida: errores sintácticos encontrados.");
            return;
        }

        // ── FASE 3: Análisis semántico ─────────────────────────────────────
        // Se crea el analizador semántico pasándole la misma lista de tokens
        // y la ruta del archivo (para saber dónde guardar la tabla de símbolos)
        AnalizadorSemantico semantico = new AnalizadorSemantico(lexer.getTokens(), ruta);
        boolean semanticoOK = semantico.analizar();

        System.out.println(); // línea en blanco para separar la salida de la tabla

        if (!semanticoOK) {
            // Imprimir todos los errores semánticos encontrados
            System.out.println("Errores semánticos:");
            for (String e : semantico.getErrores()) {
                System.out.println("  " + e);
            }
        } else {
            // Sin errores en ninguna fase
            System.out.println("OK");
        }
    }
}