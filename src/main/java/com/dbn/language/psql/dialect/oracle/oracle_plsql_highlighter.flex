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
    {WHITE_SPACE}    { return stt.getWhiteSpace(); }
    .*               { return stt.getLineComment(); }
    .                { return stt.getLineComment(); }
}


//{VARIABLE}           {return stt.getVariable(); }
{SQLP_VARIABLE}      { return stt.getVariable(); }

{BLOCK_COMMENT}      { return stt.getBlockComment(); }
{LINE_COMMENT}       { return stt.getLineComment(); }

"wrapped"            { yybegin(WRAPPED); return tt.getKeyword();}

{INTEGER}            { return stt.getInteger(); }
{NUMBER}             { return stt.getNumber(); }
{STRING}             { return stt.getString(); }

{PLSQL_FUNCTION}     { return tt.getFunction();}
{PLSQL_PARAMETER}    { return tt.getParameter();}
{PLSQL_EXCEPTION}    { return tt.getException();}
{PLSQL_DATATYPE}     { return tt.getDataType(); }
{PLSQL_KEYWORD}      { return tt.getKeyword(); }

{OPERATOR}           { return tt.getOperator(); }
{IDENTIFIER}         { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}  { return stt.getQuotedIdentifier(); }

"("                  { return stt.getChrLeftParenthesis(); }
")"                  { return stt.getChrRightParenthesis(); }
"["                  { return stt.getChrLeftBracket(); }
"]"                  { return stt.getChrRightBracket(); }

{WHITE_SPACE}        { return stt.getWhiteSpace(); }
.                    { return stt.getIdentifier(); }

