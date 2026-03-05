package com.dbn.language.psql.dialect.mysql;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class MysqlPSQLParserFlexLexer
%implements FlexLexer
%public
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    private TokenTypeBundle tt;
    private SharedTokenTypeBundle stt;
    public MysqlPSQLParserFlexLexer(TokenTypeBundle tt) {
        this.tt = tt;
        this.stt = tt.getSharedTokenTypes();
    }
%}

WHITE_SPACE= {white_space_char}|{line_terminator}
line_terminator = \r|\n|\r\n
input_character = [^\r\n]
white_space = [ \t\f]
white_space_char= [ \n\r\t\f]
ws  = {WHITE_SPACE}+
wso = {WHITE_SPACE}*

comment_tail =([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
BLOCK_COMMENT=("/*"[^]{comment_tail})|"/*"
LINE_COMMENT = "--" {input_character}*

IDENTIFIER = [:jletter:] ([:jletterdigit:]|"#")*
QUOTED_IDENTIFIER = "\""[^\"]*"\""?

string_simple_quoted      = "'"([^\']|"''"|{WHITE_SPACE})*"'"?
STRING = "n"?{string_simple_quoted}

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

%state DIV
%%

{WHITE_SPACE}+   { return stt.whiteSpace; }

{BLOCK_COMMENT}  { return stt.blockComment; }
{LINE_COMMENT}   { return stt.lineComment; }

{INTEGER}        { return stt.integer; }
{NUMBER}         { return stt.number; }
{STRING}         { return stt.string; }

"="{wso}"="      { return tt.getOperatorTokenType(0);}
"|"{wso}"|"      { return tt.getOperatorTokenType(1);}
"<"{wso}"="      { return tt.getOperatorTokenType(2);}
">"{wso}"="      { return tt.getOperatorTokenType(3);}
"<"{wso}">"      { return tt.getOperatorTokenType(4);}
"!"{wso}"="      { return tt.getOperatorTokenType(5);}
":"{wso}"="      { return tt.getOperatorTokenType(6);}
"="{wso}">"      { return tt.getOperatorTokenType(7);}
".."             { return tt.getOperatorTokenType(8);}
"::"             { return tt.getOperatorTokenType(9);}


"@" {return tt.getCharacterTokenType(0);}
":" {return tt.getCharacterTokenType(1);}
"," {return tt.getCharacterTokenType(2);}
"." {return tt.getCharacterTokenType(3);}
"=" {return tt.getCharacterTokenType(4);}
"!" {return tt.getCharacterTokenType(5);}
">" {return tt.getCharacterTokenType(6);}
"#" {return tt.getCharacterTokenType(7);}
"[" {return tt.getCharacterTokenType(8);}
"{" {return tt.getCharacterTokenType(9);}
"(" {return tt.getCharacterTokenType(10);}
"<" {return tt.getCharacterTokenType(11);}
"-" {return tt.getCharacterTokenType(12);}
"%" {return tt.getCharacterTokenType(13);}
"+" {return tt.getCharacterTokenType(14);}
"]" {return tt.getCharacterTokenType(15);}
"}" {return tt.getCharacterTokenType(16);}
")" {return tt.getCharacterTokenType(17);}
";" {return tt.getCharacterTokenType(18);}
"/" {return tt.getCharacterTokenType(19);}
"*" {return tt.getCharacterTokenType(20);}
"|" {return tt.getCharacterTokenType(21);}


{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.quotedIdentifier; }
.                      { return stt.identifier; }
