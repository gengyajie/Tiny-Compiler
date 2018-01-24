# Tiny-Compiler
A compiler for Micro LANGUAGE implemented by antlr v4 and Java. The compiler includes lexer, parser, semantic routines, IR code generater, code optimization, and assembly code generator. The compiler supports function calls, loop optimization, and register allocation(4 data registers).
## Compile and Build
$ make all<br />
build and classes directory will be created.
## Remove build and classes directory
$ make clean
## Generate Tiny Assembly Code
$ java -cp classes/:lib/antlr.jar Micro < testcase.micro > assembly.out
## Run Assembly with Tiny Simulator
$ g++ -o tiny4Rregs tiny4Rregs.cpp <br />
$ ./tiny4Rregs < testcase.input > testcase.tinyout <br />
The tinyout will include the output of the assembly code and the memory and register usage.
