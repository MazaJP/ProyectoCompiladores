import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);
        System.out.print("Ingrese archivo .mlng: ");
        String ruta = entrada.nextLine();

        // ── Análisis léxico ──────────────────────────────────────
        Lexer lexer = new Lexer(ruta);
        lexer.analizar();

        // ── Análisis sintáctico ascendente ───────────────────────
        AnalizadorAscendente parser = new AnalizadorAscendente(lexer.getTokens());
        boolean correcto = parser.analizar();

        System.out.println();
        if (!lexer.isHayErrores() && correcto) {
            System.out.println("OK");
        } else {
            if (!parser.getErrores().isEmpty()) {
                System.out.println("--- Errores sintácticos ---");
                for (String e : parser.getErrores()) {
                    System.out.println(e);
                }
            }
        }
    }
}