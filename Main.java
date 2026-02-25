import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.print("Ingrese archivo .mlng: ");
        String rutaEntrada = input.nextLine();

        Lexer lexer = new Lexer(rutaEntrada);
        lexer.analizar();
    }
}