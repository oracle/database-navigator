package com.dbn.language.psql.dialect.oracle;

import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageLexerBase;
import com.intellij.psi.tree.IElementType;

%%

%class OraclePLSQLHighlighterFlexLexer
%extends DBLanguageLexerBase
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}


%{
    public OraclePLSQLHighlighterFlexLexer(TokenTypeBundle tt) {
        super(tt);
    }
%}

%include ../../../common/lexer/shared_elements.flext
%include ../../../common/lexer/shared_elements_oracle.flext
%include ../../../common/lexer/shared_elements_oracle_plsql.flext

VARIABLE = ":"({IDENTIFIER}|{INTEGER})
SQLP_VARIABLE = "&""&"?{IDENTIFIER}

%state WRAPPED
%%

<WRAPPED> {
    {WHITE_SPACE}    { return stt.whiteSpace; }
    .*               { return stt.lineComment; }
    .                { return stt.lineComment; }
}


//{VARIABLE}           {return stt.variable; }
{SQLP_VARIABLE}      { return stt.variable; }

{BLOCK_COMMENT}      { return stt.blockComment; }
{LINE_COMMENT}       { return stt.lineComment; }

"wrapped"            { yybegin(WRAPPED); return tt.getKeyword();}

{INTEGER}            { return stt.integer; }
{NUMBER}             { return stt.number; }
{STRING}             { return stt.string; }

{PLSQL_FUNCTION}     { return tt.getFunction();}
{PLSQL_PARAMETER}    { return tt.getParameter();}
{PLSQL_EXCEPTION}    { return tt.getException();}
{PLSQL_DATATYPE}     { return tt.getDataType(); }
{PLSQL_KEYWORD}      { return tt.getKeyword(); }

{OPERATOR}           { return tt.getOperator(); }
{IDENTIFIER}         { return stt.identifier; }
{QUOTED_IDENTIFIER}  { return stt.identifier; }

"("                  { return stt.chrLeftParenthesis; }
")"                  { return stt.chrRightParenthesis; }
"["                  { return stt.chrLeftBracket; }
"]"                  { return stt.chrRightBracket; }

{WHITE_SPACE}        { return stt.whiteSpace; }
.                    { return stt.identifier; }

