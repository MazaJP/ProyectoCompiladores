MiniLand Lexer
Manual de Usuario

1, Descripción de su uso:

Programa que realiza el análisis léxico del lenguaje MiniLand.
Lee un archivo .mlng o .txt, identifica tokens y genera un archivo .out con los resultados.

2. Ejecutar

Ejecutar Main.java.

3. Ingresar la ruta del archivo cuando se solicite.

Ejemplo:

C:\Users\USUARIO\Desktop\prueba.mlng
---> Lee Entrada

4, Archivo con código MiniLand.

Ejemplo de lo que contiene:

int x
x = 10
write x
📤 Salida

Se genera automáticamente:

nombreArchivo.out

Formato de tokens:

(linea, colInicio-colFin) TIPO lexema

Ejemplo:

(1, 1-3) INT int
(2, 5-6) INT_LITERAL 10
(4, 1-1) EOF $

Reconocimiento de tokens:

Palabras clave: INT, FLOAT, STRING, BOOL, IF, ELSE, WHILE, READ, WRITE

Identificadores: letras + números (máx. 31 caracteres)

Literales: enteros, decimales y cadenas

Operadores: + - * / = > < >= <= == !=

Símbolos: ( ) , :

Verificador de  Errores

Si hay caracteres inválidos se muestra:

line X, col Y: ERROR caracter invalido
