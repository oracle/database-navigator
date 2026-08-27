package com.dbn.language.sql.dialect.oracle;

import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageLexerBase;
import com.intellij.psi.tree.IElementType;

%%

%class OracleSQLHighlighterFlexLexer
%extends DBLanguageLexerBase
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    public OracleSQLHighlighterFlexLexer(TokenTypeBundle tt) {
        super(tt);
    }
%}


PLSQL_BLOCK_START = "create"({ws}"or"{ws}"replace")? {ws} ("function"|"procedure"|"type"|"trigger"|"package") | "declare" | "begin"
PLSQL_BLOCK_END = ";"{wso}"/"[^*]
SELECT_AI_START = "select"{ws}"ai"

%include ../../../common/lexer/shared_elements.flext
%include ../../../common/lexer/shared_elements_oracle.flext
%include ../../../common/lexer/shared_elements_oracle_sql.flext
%include ../../../common/lexer/shared_elements_oracle_plsql.flext

VARIABLE = ":"({IDENTIFIER}|{INTEGER})
SQLP_VARIABLE = "&""&"?({IDENTIFIER}|{INTEGER})
VARIABLE_IDENTIFIER={IDENTIFIER}"&""&"?({IDENTIFIER}|{INTEGER})|"<"{IDENTIFIER}({ws}{IDENTIFIER})*">"

%state PSQL_BLOCK
%state NON_PSQL_BLOCK
%state SELECT_AI
%%

<YYINITIAL, NON_PSQL_BLOCK> {
    {BLOCK_COMMENT}       { return stt.blockComment; }
    {LINE_COMMENT}        { return stt.lineComment; }

    {VARIABLE}            { return stt.variable; }
    {VARIABLE_IDENTIFIER} { return stt.identifier; }
    {SQLP_VARIABLE}       { return stt.variable; }

    {PLSQL_BLOCK_START}   { yybegin(PSQL_BLOCK); return tt.keyword;}
    {SELECT_AI_START}     { yybegin(SELECT_AI); yypushback(yylength());}

    {INTEGER}             { return tt.integer; }
    {NUMBER}              { return tt.number; }
    {STRING}              { return tt.string; }

    {SQL_FUNCTION}        { return tt.function;}
    {SQL_PARAMETER}       { return tt.parameter;}
    {SQL_DATATYPE}        { return tt.dataType; }
    {SQL_KEYWORD}         { return tt.keyword; }

    {OPERATOR}            { return tt.operator; }
    {IDENTIFIER}          { return stt.identifier; }
    {QUOTED_IDENTIFIER}   { return stt.identifier; }

    "("                   { return stt.chrLeftParenthesis; }
    ")"                   { return stt.chrRightParenthesis; }
    "["                   { return stt.chrLeftBracket; }
    "]"                   { return stt.chrRightBracket; }
    "{"                   { return stt.chrLeftBrace; }
    "}"                   { return stt.chrRightBrace; }


    {WHITE_SPACE}         { return stt.whiteSpace; }
    .                     { return stt.identifier; }
}

<PSQL_BLOCK> {
    {BLOCK_COMMENT}       { return stt.blockComment; }
    {LINE_COMMENT}        { return stt.lineComment; }
//  {VARIABLE}            { return stt.variable; }
    {SQLP_VARIABLE}       { return stt.variable; }

    {PLSQL_BLOCK_END}     { yybegin(YYINITIAL); return stt.identifier; }

    {INTEGER}             { return tt.integer; }
    {NUMBER}              { return tt.number; }
    {STRING}              { return tt.string; }

    {PLSQL_FUNCTION}      { return tt.function;}
    {PLSQL_PARAMETER}     { return tt.parameter;}
    {PLSQL_EXCEPTION}     { return tt.exception;}
    {PLSQL_DATATYPE}      { return tt.dataType; }
    {PLSQL_KEYWORD}       { return tt.keyword; }

    {OPERATOR}            { return tt.operator; }
    {IDENTIFIER}          { return stt.identifier; }
    {QUOTED_IDENTIFIER}   { return stt.identifier; }

    "("                   { return stt.chrLeftParenthesis; }
    ")"                   { return stt.chrRightParenthesis; }
    "["                   { return stt.chrLeftBracket; }
    "]"                   { return stt.chrRightBracket; }
    "{"                   { return stt.chrLeftBrace; }
    "}"                   { return stt.chrRightBrace; }

    {WHITE_SPACE}         { return stt.whiteSpace; }
    .                     { return stt.identifier; }
}

<SELECT_AI> {
    "select"           { return tt.keyword; }
    "ai"               { return tt.keyword; }
    "showprompt"       { return tt.keyword; }
    "showsql"          { return tt.keyword; }
    "explainsql"       { return tt.keyword; }
    "executesql"       { return tt.keyword; }
    "narrate"          { return tt.keyword; }
    "chat"             { return tt.keyword; }
    {STRING}           { yybegin(YYINITIAL); return stt.string; }    // string is allowed to have eols
    {eol}              { yybegin(YYINITIAL); return stt.whiteSpace;} // end of line -> exit the SELECT_AI block
    ";"                { yybegin(YYINITIAL); return stt.chrSemicolon;}
    "/"                { yybegin(YYINITIAL); return stt.chrSlash;}
    [^\r\n\t\f ;/]+    { return stt.string;}
    {wsc}+             { return stt.whiteSpace; }
}
