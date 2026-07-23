package com.dbn.language.sql.dialect.sqlite;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
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

VARIABLE = ":"({IDENTIFIER}|{INTEGER})

OPERATOR = "=="|"||"|"<="|">="|"<>"|"!="|":="|"=>"|".."|"::"|"->>"|"->"|"="|">"|"<"


SQL_KEYWORD = "abort"|"action"|"add"|"after"|"all"|"alter"|"analyze"|"and"|"as"|"asc"|"attach"|"autoincrement"|"before"|"begin"|"between"|"by"|"cascade"|"case"|"cast"|"check"|"collate"|"column"|"commit"|"conflict"|"constraint"|"create"|"cross"|"current_date"|"current_time"|"current_timestamp"|"current"|"database"|"default"|"deferrable"|"deferred"|"delete"|"desc"|"detach"|"distinct"|"drop"|"exclude"|"each"|"else"|"end"|"escape"|"except"|"exclusive"|"exists"|"explain"|"fail"|"filter"|"for"|"foreign"|"following"|"from"|"full"|"glob"|"group"|"groups"|"having"|"if"|"ignore"|"immediate"|"in"|"index"|"indexed"|"initially"|"inner"|"insert"|"instead"|"intersect"|"into"|"is"|"isnull"|"join"|"key"|"left"|"like"|"limit"|"match"|"natural"|"no"|"not"|"notnull"|"null"|"of"|"off"|"offset"|"on"|"or"|"order"|"others"|"outer"|"over"|"partition"|"plan"|"pragma"|"preceding"|"primary"|"query"|"raise"|"recursive"|"references"|"range"|"regexp"|"reindex"|"release"|"rename"|"replace"|"restrict"|"right"|"rollback"|"rows"|"row"|"rowid"|"savepoint"|"select"|"set"|"table"|"temp"|"temporary"|"then"|"ties"|"to"|"transaction"|"trigger"|"union"|"unique"|"unbounded"|"update"|"using"|"vacuum"|"values"|"window"|"view"|"virtual"|"when"|"where"|"with"|"without"
SQL_FUNCTION = "abs"|"acos"|"acosh"|"asin"|"asinh"|"atan"|"atan2"|"atanh"|"avg"|"ceil"|"ceiling"|"changes"|"char"|"coalesce"|"concat"|"concat_ws"|"cos"|"cosh"|"count"|"cume_dist"|"degrees"|"dense_rank"|"exp"|"first_value"|"floor"|"format"|"group_concat"|"hex"|"ifnull"|"iif"|"instr"|"json"|"jsonb"|"jsonb_array"|"jsonb_array_insert"|"jsonb_each"|"jsonb_extract"|"jsonb_group_array"|"jsonb_group_object"|"jsonb_insert"|"jsonb_object"|"jsonb_patch"|"jsonb_remove"|"jsonb_replace"|"jsonb_set"|"jsonb_tree"|"json_array"|"json_array_insert"|"json_array_length"|"json_each"|"json_error_position"|"json_extract"|"json_group_array"|"json_group_object"|"json_insert"|"json_object"|"json_patch"|"json_pretty"|"json_remove"|"json_replace"|"json_set"|"json_tree"|"json_type"|"json_valid"|"julianday"|"lag"|"last_insert_rowid"|"last_value"|"lead"|"length"|"likelihood"|"likely"|"ln"|"load_extension"|"log"|"log10"|"log2"|"lower"|"ltrim"|"max"|"median"|"min"|"mod"|"nth_value"|"ntile"|"nullif"|"octet_length"|"percentile"|"percentile_cont"|"percentile_disc"|"percent_rank"|"pi"|"pow"|"power"|"printf"|"quote"|"radians"|"random"|"randomblob"|"rank"|"round"|"row_number"|"rtrim"|"sign"|"sin"|"sinh"|"soundex"|"sqlite_compileoption_get"|"sqlite_compileoption_used"|"sqlite_offset"|"sqlite_source_id"|"sqlite_version"|"sqrt"|"strftime"|"string_agg"|"substr"|"substring"|"sum"|"tan"|"tanh"|"timediff"|"total"|"total_changes"|"trim"|"trunc"|"typeof"|"unhex"|"unicode"|"unistr"|"unistr_quote"|"unixepoch"|"unlikely"|"upper"|"zeroblob"
SQL_DATATYPE = "bigint"|"blob"|"boolean"|"character"|"clob"|"date"|"datetime"|"decimal"|"double"|"double precision"|"float"|"int"|"int2"|"int8"|"integer"|"mediumint"|"native character"|"nchar"|"numeric"|"nvarchar"|"real"|"smallint"|"text"|"time"|"tinyint"|"unsigned big int"|"varchar"|"varying character"
SQL_PARAMETER = "application_id"|"automatic_index"|"auto_vacuum"|"busy_timeout"|"cache_size"|"cache_spill"|"case_sensitive_like"|"cell_size_check"|"checkpoint_fullfsync"|"collation_list"|"compile_options"|"count_changes"|"database_list"|"data_store_directory"|"data_version"|"default_cache_size"|"defer_foreign_keys"|"empty_result_callbacks"|"encoding"|"foreign_keys"|"foreign_key_check"|"foreign_key_list"|"freelist_count"|"fullfsync"|"full_column_names"|"ignore_check_constraints"|"incremental_vacuum"|"index_info"|"index_list"|"index_xinfo"|"integrity_check"|"journal_mode"|"journal_size_limit"|"legacy_file_format"|"locking_mode"|"max_page_count"|"mmap_size"|"page_count"|"page_size"|"parser_trace"|"query_only"|"quick_check"|"read_uncommitted"|"recursive_triggers"|"reverse_unordered_selects"|"schema_version"|"secure_delete"|"short_column_names"|"shrink_memory"|"soft_heap_limit"|"stats"|"synchronous"|"table_info"|"temp_store"|"temp_store_directory"|"threads"|"user_version"|"vdbe_addoptrace"|"vdbe_debug"|"vdbe_listing"|"vdbe_trace"|"wal_autocheckpoint"|"wal_checkpoint"|"writable_schema"

%state DIV
%%

{VARIABLE}           { return tt.getTokenType("VARIABLE"); }

{WHITE_SPACE}+       { return stt.whiteSpace; }

{BLOCK_COMMENT}      { return stt.blockComment; }
{LINE_COMMENT}       { return stt.lineComment; }

{INTEGER}            { return stt.integer; }
{NUMBER}             { return stt.number; }
{STRING}             { return stt.string; }

{SQL_FUNCTION}       { return tt.getTokenType("FUNCTION");}
{SQL_PARAMETER}      { return tt.getTokenType("PARAMETER"); }
{SQL_DATATYPE}       { return tt.getTokenType("DATA_TYPE"); }
{SQL_KEYWORD}        { return tt.getTokenType("KEYWORD"); }

{OPERATOR}           { return tt.getTokenType("OPERATOR"); }

{IDENTIFIER}         { return stt.identifier; }
{QUOTED_IDENTIFIER}  { return stt.identifier; }


"("                  { return stt.chrLeftParenthesis; }
")"                  { return stt.chrRightParenthesis; }

.                    { return stt.identifier; }
