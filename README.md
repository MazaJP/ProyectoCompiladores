Analizador Léxico: MiniLand (Fase #1)
Este proyecto consiste en el diseño e implementación de un analizador léxico  para el lenguaje MiniLand, desarrollado en la Universidad Rafael Landívar.

Especificaciones Técnicas
Conjunto de Tokens
El lenguaje reconoce las siguientes categorías de tokens:


Palabras Reservadas: int, float, string, bool, if, else, while, funcion, read, return, imprime, true, false.


Operadores: Aritméticos (+, -, *, /), de asignación (=) y relacionales (>, <, >=, <=, ==, !=).


Literales: Números enteros (NUMENTERO), decimales (NUMDECIMAL) y cadenas de texto entre comillas (CADENASTRING).


Símbolos Especiales: (, ), ,, :, ;.

Gramática (BNF)
El análisis se basa en una gramática determinística para evitar ambigüedades. Algunos componentes clave incluyen:


Estructura General: Un programa consiste en una lista de sentencias que finaliza con un token EOF.


Sentencias: Incluye declaraciones de tipo, asignaciones, estructuras de control (if, while), definiciones de función y operaciones de entrada/salida.


Expresiones: Maneja precedencia de operadores mediante definiciones de términos y factores.


Errores Detectados
El escáner identifica y reporta los siguientes problemas léxicos:

Caracteres inválidos.

Cadenas de texto sin cerrar.

Números mal formados.

Identificadores que exceden los 31 caracteres.

Indentación inconsistente o inválida.

Integrantes del Proyecto:

Ana Paula Ortiz Hernandez 

Juan Palo Mazariegos Sepúlveda
