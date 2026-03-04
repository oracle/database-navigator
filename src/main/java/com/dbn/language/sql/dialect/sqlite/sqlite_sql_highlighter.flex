package com.dbn.language.sql.dialect.sqlite;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.psi.tree.IElementType;

%%

%class SqliteSQLHighlighterFlexLexer
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
    public SqliteSQLHighlighterFlexLexer(TokenTypeBundle tt) {
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
LINE_COMMENT = "--"{input_character}*

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = `[^\`]*`?|\"[^\"]*\"?|\[[^\]]*\]?

CHARSET ="armscii8"|"ascii"|"big5"|"binary"|"cp1250"|"cp1251"|"cp1256"|"cp1257"|"cp850"|"cp852"|"cp866"|"cp932"|"dec8"|"eucjpms"|"euckr"|"gb2312"|"gbk"|"geostd8"|"greek"|"hebrew"|"hp8"|"keybcs2"|"koi8r"|"koi8u"|"latin1"|"latin2"|"latin5"|"latin7"|"macce"|"macroman"|"sjis"|"swe7"|"tis620"|"ucs2"|"ujis"|"utf8"

string_simple_quoted      = "'"([^\']|"''"|{WHITE_SPACE})*"'"?
STRING = ("n"|"_"{CHARSET})?{wso}{string_simple_quoted}

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

VARIABLE = ":"{wso}({IDENTIFIER}|{INTEGER})

operator_equals             = "="
operator_not_equals         = (("!"|"^"){wso}"=")|("<"{wso}">")
operator_greater_than       = ">"
operator_greater_equal_than = ">"{wso}"="
operator_less_than          = "<"
operator_less_equal_than    = ">"{wso}"="
OPERATOR                    = {operator_equals}|{operator_not_equals}|{operator_greater_than}|{operator_greater_equal_than}|{operator_less_than}|{operator_less_equal_than}


SQL_KEYWORD   = "abort"|"action"|"add"|"after"|"all"|"alter"|"analyze"|"and"|"as"|"asc"|"attach"|"autoincrement"|"before"|"begin"|"between"|"by"|"cascade"|"case"|"cast"|"check"|"collate"|"column"|"commit"|"conflict"|"constraint"|"create"|"cross"|"current_date"|"current_time"|"current_timestamp"|"database"|"default"|"deferrable"|"deferred"|"delete"|"desc"|"detach"|"distinct"|"drop"|"each"|"else"|"end"|"escape"|"except"|"exclusive"|"exists"|"explain"|"fail"|"for"|"foreign"|"from"|"full"|"glob"|"group"|"having"|"if"|"ignore"|"immediate"|"in"|"index"|"indexed"|"initially"|"inner"|"insert"|"instead"|"intersect"|"into"|"is"|"isnull"|"join"|"key"|"left"|"like"|"limit"|"match"|"natural"|"no"|"not"|"notnull"|"null"|"of"|"offset"|"on"|"or"|"order"|"outer"|"plan"|"pragma"|"primary"|"query"|"raise"|"recursive"|"references"|"regexp"|"reindex"|"release"|"rename"|"replace"|"restrict"|"right"|"rollback"|"row"|"savepoint"|"select"|"set"|"table"|"temp"|"temporary"|"then"|"to"|"transaction"|"trigger"|"union"|"unique"|"update"|"using"|"vacuum"|"values"|"view"|"virtual"|"when"|"where"|"with"|"without"
SQL_FUNCTION  = "abs"|"avg"|"changes"|"char"|"coalesce"|"count"|"date"|"datetime"|"glob"|"group_concat"|"hex"|"ifnull"|"instr"|"json"|"json_array"|"json_array_length"|"json_extract"|"json_insert"|"json_object"|"json_remove"|"json_replace"|"json_set"|"json_type"|"json_valid"|"julianday"|"last_insert_rowid"|"length"|"like"|"likelihood"|"likely"|"load_extension"|"lower"|"ltrim"|"max"|"min"|"nullif"|"printf"|"quote"|"random"|"randomblob"|"replace"|"round"|"rtrim"|"soundex"|"sqlite_compileoption_get"|"sqlite_compileoption_used"|"sqlite_source_id"|"sqlite_version"|"strftime"|"substr"|"sum"|"time"|"total"|"total_changes"|"trim"|"typeof"|"unlikely"|"unicode"|"upper"|"zeroblob"
SQL_DATATYPE = "bigint"|"blob"|"boolean"|"character"|"clob"|"date"|"datetime"|"decimal"|"double"|"double"{ws}"precision"|"float"|"int"|"int2"|"int8"|"integer"|"mediumint"|"native"{ws}"character"|"nchar"|"null"|"numeric"|"nvarchar"|"real"|"smallint"|"text"|"tinyint"|"unsigned"{ws}"big"{ws}"int"|"varchar"|"varying"{ws}"character"
SQL_PARAMETER = "application_id"|"auto_vacuum"|"automatic_index"|"busy_timeout"|"cache_size"|"cache_spill"|"case_sensitive_like"|"cell_size_check"|"checkpoint_fullfsync"|"collation_list"|"compile_options"|"count_changes"|"data_store_directory"|"data_version"|"database_list"|"default_cache_size"|"defer_foreign_keys"|"empty_result_callbacks"|"encoding"|"foreign_key_check"|"foreign_key_list"|"foreign_keys"|"freelist_count"|"full_column_names"|"fullfsync"|"ignore_check_constraints"|"incremental_vacuum"|"index_info"|"index_list"|"index_xinfo"|"integrity_check"|"journal_mode"|"journal_size_limit"|"legacy_file_format"|"locking_mode"|"max_page_count"|"mmap_size"|"page_count"|"page_size"|"parser_trace"|"query_only"|"quick_check"|"read_uncommitted"|"recursive_triggers"|"reverse_unordered_selects"|"schema_version"|"secure_delete"|"short_column_names"|"shrink_memory"|"soft_heap_limit"|"stats"|"synchronous"|"table_info"|"temp_store"|"temp_store_directory"|"threads"|"user_version"|"vdbe_addoptrace"|"vdbe_debug"|"vdbe_listing"|"vdbe_trace"|"wal_autocheckpoint"|"wal_checkpoint"|"writable_schema"

%state DIV
%%

{VARIABLE}           { return tt.getTokenType("VARIABLE"); }

{WHITE_SPACE}+       { return stt.getWhiteSpace(); }

{BLOCK_COMMENT}      { return stt.getBlockComment(); }
{LINE_COMMENT}       { return stt.getLineComment(); }

{INTEGER}            { return stt.getInteger(); }
{NUMBER}             { return stt.getNumber(); }
{STRING}             { return stt.getString(); }

{SQL_FUNCTION}       { return tt.getTokenType("FUNCTION");}
{SQL_PARAMETER}      { return tt.getTokenType("PARAMETER"); }
{SQL_DATATYPE}       { return tt.getTokenType("DATA_TYPE"); }
{SQL_KEYWORD}        { return tt.getTokenType("KEYWORD"); }

{OPERATOR}           { return tt.getTokenType("OPERATOR"); }

{IDENTIFIER}         { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}  { return stt.getQuotedIdentifier(); }


"("                  { return stt.getChrLeftParenthesis(); }
")"                  { return stt.getChrRightParenthesis(); }

.                    { return stt.getIdentifier(); }
