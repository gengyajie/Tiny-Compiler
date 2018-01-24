grammar Micro;

// Program
program
    : 'PROGRAM' id 'BEGIN' pgm_body 'END'
	;

id
    : IDENTIFIER
	;

pgm_body
    :  decl func_declarations
    | //empty
	;

decl
    : string_decl decl
	| var_decl decl
	| //empty
	;

// Global String Declaration
string_decl
	: 'STRING' id ':=' str ';'
	;

str
	: STRINGLITERAL
	;

// Variable Declaration
var_decl
	: var_type id_list ';'
	;

var_type
	: 'FLOAT'
	| 'INT'
	;

any_type
	: var_type
	| 'VOID'
	;

id_list
	: id id_tail
	;

id_tail
	: ',' id id_tail
	| //empty
	;

// Function Parameter List
param_decl_list
	: param_decl param_decl_tail
	| //empty
	;

param_decl
	: var_type id
	;

param_decl_tail
	: ',' param_decl param_decl_tail
	| //empty
	;

// Function Declarations
func_declarations
	: func_decl func_declarations
	| //empty
	;

func_decl
	: 'FUNCTION' any_type id '('param_decl_list')' 'BEGIN' func_body 'END'
	;

func_body
	: decl stmt_list
	;

// Statement List
stmt_list
	: stmt stmt_list
	| //empty
	;

stmt
	: base_stmt
	| if_stmt
	| for_stmt
	;

base_stmt
	: assign_stmt //{System.out.println($assign_stmt.text);}
	| read_stmt
	| write_stmt //{System.out.println($write_stmt.text);}
	| return_stmt
	;

// Basic Statement
assign_stmt
	: assign_expr ';'
	;

assign_expr
	: id ':=' expr
	;

read_stmt
	: 'READ' '(' id_list ')' ';'
	;

write_stmt
	: 'WRITE' '(' id_list ')' ';'
	;

return_stmt
	: 'RETURN' expr ';'
	;

// Expressions
expr
	: expr_prefix factor
	;

expr_prefix
	: expr_prefix factor addop
	| //empty
	;

factor
	: factor_prefix postfix_expr
	;

factor_prefix
	: factor_prefix postfix_expr mulop
	| //empty
	;

postfix_expr
	: primary
	| call_expr
	;

call_expr
	: id '(' expr_list ')'
	;

expr_list
	: expr expr_list_tail
	| //empty
	;

expr_list_tail
	: ',' expr expr_list_tail
	| //empty
	;

primary
	: '(' expr ')'
	| id
	| INTLITERAL
	| FLOATLITERAL
	;

addop
	: '+'
	| '-'
	;

mulop
	: '*'
	| '/'
	;

// Complex Statements and Conditions
if_stmt
	: 'IF' '(' cond ')' decl stmt_list elif_part 'ENDIF'
	;

elif_part
	: 'ELIF' '(' cond ')' decl stmt_list elif_part
	|  else_part
	;

else_part
	: 'ELSE' decl stmt_list
	| //empty
	;

cond
	: lit cond_suffix
	;

cond_suffix
	: 'AND' lit cond_suffix
	| 'OR' lit cond_suffix
	| //empty
	;

lit
	: 'NOT' basic_cond
	| basic_cond
	;

basic_cond
	: expr compop expr
	| 'TRUE'
	| 'FALSE'
	;

compop
	: '<'
	| '>'
	| '='
	| '!='
	| '<='
	| '>='
	;

// For Statements
init_stmt
	: assign_expr
	| //empty
	;

incr_stmt
	: assign_expr
	| //empty
	;

for_stmt
	: 'FOR' '(' init_stmt ';' cond ';' incr_stmt ')'decl aug_stmt_list 'ENDFOR'
	;

aug_stmt_list
	: aug_stmt aug_stmt_list
	| //empty
	;

aug_stmt
	: base_stmt
	| aug_if_stmt
	| for_stmt
	| 'CONTINUE' ';'
	| 'BREAK' ';'
	;

aug_if_stmt
	: 'IF' '(' cond ')' decl aug_stmt_list aug_elif_part 'ENDIF'
	;

aug_elif_part
	: 'ELIF' '(' cond ')' decl aug_stmt_list aug_elif_part
	| aug_else_part
	;

aug_else_part
	: 'ELSE' decl aug_stmt_list
	| //empty
	;

INTLITERAL		: ['-']? [0-9]+ ;

FLOATLITERAL	: ['-']? ([0-9]+)? ('.' [0-9]*)+ ;

STRINGLITERAL	:  '"' (~["])* '"';

OPERATOR		: ':=' | '+' | '-' | '*' | '/' | '=' | '!=' | '<' | '>' | '(' | ')' | ';' | ',' | '<=' | '>=' | 'OR' | 'AND' | 'NOT' | 'TRUE' | 'FALSE' ;

COMMENT			: '--' (~[\r\n])* '\r'? '\n' -> skip ;

KEYWORD		    : 'PROGRAM' | 'BEGIN' | 'END' | 'FUNCTION' | 'READ' | 'WRITE' | 'IF' | 'ELIF' | 'ELSE' | 'ENDIF' | 'FOR' | 'ENDFOR' | 'CONTINUE' | 'BREAK' | 'RETURN' | 'INT' | 'VOID' | 'STRING' | 'FLOAT' ;

WS				: [ \t\r\n] -> skip ;

IDENTIFIER		: [A-Za-z] [A-Za-z0-9]* ;
