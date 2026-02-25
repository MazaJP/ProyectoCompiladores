public enum TokenType {

    // Palabras clave
    INT,
    FLOAT,
    Tipo_String,
    BOOL,
    IF,
    ELSE,
    WHILE,
    FUNCION,
    READ,
    RETURN,
    IMPRIME,
    TRUE,
    FALSE,

    // Identificadores y literales
    ID,
    NUMENTERO,
    FNUMDECIMAL,
    CADENASTRING,

    // Operadores
    SUMA,
    RESTA,
    MULTI,
    DIV,
    IGUAL,
    MAYORQ,
    MENORQ,
    MAYORIGU,
    MENORIGU,
    EQUIVA,
    NEGA,

    // Símbolos
    PARENI,
    PAREND,
    COMA,
    DOSP,
    PUNCOM,

    // Estructurales
    NEWLINE,
    INDENT,
    DEDENT,
    EOF
}