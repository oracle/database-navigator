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
%include ../../../common/lexer/shared_elements_oracle_psql.flext

VARIABLE = ":"({IDENTIFIER}|{INTEGER})
SQLP_VARIABLE = "&""&"?({IDENTIFIER}|{INTEGER})
VARIABLE_IDENTIFIER={IDENTIFIER}"&""&"?({IDENTIFIER}|{INTEGER})|"<"{IDENTIFIER}({ws}{IDENTIFIER})*">"

%state PSQL_BLOCK
%state NON_PSQL_BLOCK
%state SELECT_AI
%%

<YYINITIAL, NON_PSQL_BLOCK> {
    {BLOCK_COMMENT}       { return stt.getBlockComment(); }
    {LINE_COMMENT}        { return stt.getLineComment(); }

    {VARIABLE}            { return stt.getVariable(); }
    {VARIABLE_IDENTIFIER} { return stt.getIdentifier(); }
    {SQLP_VARIABLE}       { return stt.getVariable(); }

    {PLSQL_BLOCK_START}   { yybegin(PSQL_BLOCK); return tt.getKeyword();}
    {SELECT_AI_START}     { yybegin(SELECT_AI); yypushback(yylength());}

    {INTEGER}             { return tt.getInteger(); }
    {NUMBER}              { return tt.getNumber(); }
    {STRING}              { return tt.getString(); }

    {SQL_FUNCTION}        { return tt.getFunction();}
    {SQL_PARAMETER}       { return tt.getParameter();}
    {SQL_DATA_TYPE}       { return tt.getDataType(); }
    {SQL_KEYWORD}         { return tt.getKeyword(); }

    {OPERATOR}            { return tt.getOperator(); }
    {IDENTIFIER}          { return stt.getIdentifier(); }
    {QUOTED_IDENTIFIER}   { return stt.getQuotedIdentifier(); }

    "("                   { return stt.getChrLeftParenthesis(); }
    ")"                   { return stt.getChrRightParenthesis(); }
    "["                   { return stt.getChrLeftBracket(); }
    "]"                   { return stt.getChrRightBracket(); }

    {WHITE_SPACE}         { return stt.getWhiteSpace(); }
    .                     { return stt.getIdentifier(); }
}

<PSQL_BLOCK> {
    {BLOCK_COMMENT}       { return stt.getBlockComment(); }
    {LINE_COMMENT}        { return stt.getLineComment(); }
//  {VARIABLE}            { return stt.getVariable(); }
    {SQLP_VARIABLE}       { return stt.getVariable(); }

    {PLSQL_BLOCK_END}     { yybegin(YYINITIAL); return stt.getIdentifier(); }

    {INTEGER}             { return tt.getInteger(); }
    {NUMBER}              { return tt.getNumber(); }
    {STRING}              { return tt.getString(); }

    {PLSQL_FUNCTION}      { return tt.getFunction();}
    {PLSQL_PARAMETER}     { return tt.getParameter();}
    {PLSQL_EXCEPTION}     { return tt.getException();}
    {PLSQL_DATA_TYPE}     { return tt.getDataType(); }
    {PLSQL_KEYWORD}       { return tt.getKeyword(); }

    {OPERATOR}            { return tt.getOperator(); }
    {IDENTIFIER}          { return stt.getIdentifier(); }
    {QUOTED_IDENTIFIER}   { return stt.getQuotedIdentifier(); }

    "("                   { return stt.getChrLeftParenthesis(); }
    ")"                   { return stt.getChrRightParenthesis(); }
    "["                   { return stt.getChrLeftBracket(); }
    "]"                   { return stt.getChrRightBracket(); }

    {WHITE_SPACE}         { return stt.getWhiteSpace(); }
    .                     { return stt.getIdentifier(); }
}

<SELECT_AI> {
    "select"           { return tt.getKeyword(); }
    "ai"               { return tt.getKeyword(); }
    "showprompt"       { return tt.getKeyword(); }
    "showsql"          { return tt.getKeyword(); }
    "explainsql"       { return tt.getKeyword(); }
    "executesql"       { return tt.getKeyword(); }
    "narrate"          { return tt.getKeyword(); }
    "chat"             { return tt.getKeyword(); }
    {STRING}           { yybegin(YYINITIAL); return stt.getString(); }    // string is allowed to have eols
    {eol}              { yybegin(YYINITIAL); return stt.getWhiteSpace();} // end of line -> exit the SELECT_AI block
    ";"                { yybegin(YYINITIAL); return stt.getChrSemicolon();}
    "/"                { yybegin(YYINITIAL); return stt.getChrSlash();}
    [^\r\n\t\f ;/]+    { return stt.getString();}
    {wsc}+             { return stt.getWhiteSpace(); }
}

