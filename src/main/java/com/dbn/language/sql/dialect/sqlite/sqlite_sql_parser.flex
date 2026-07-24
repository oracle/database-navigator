package com.dbn.language.sql.dialect.sqlite;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class SqliteSQLParserFlexLexer
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
    public SqliteSQLParserFlexLexer(TokenTypeBundle tt) {
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
BLOB = ("x"|"X")"'"([0-9a-fA-F][0-9a-fA-F])*"'"

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

VARIABLE = "?"{digit}*|(":"|"@"|"$")({IDENTIFIER}|{INTEGER})

%state DIV
%%

{WHITE_SPACE}+   { return stt.whiteSpace; }

{BLOCK_COMMENT}  { return stt.blockComment; }
{LINE_COMMENT}   { return stt.lineComment; }

{VARIABLE}       { return stt.variable; }
{INTEGER}        { return stt.integer; }
{NUMBER}         { return stt.number; }
{BLOB}           { return stt.string; }
{STRING}         { return stt.string; }

"=="             { return tt.getOperatorTokenType(0); }
"||"             { return tt.getOperatorTokenType(1); }
"<="             { return tt.getOperatorTokenType(2); }
">="             { return tt.getOperatorTokenType(3); }
"<>"             { return tt.getOperatorTokenType(4); }
"!="             { return tt.getOperatorTokenType(5); }
":="             { return tt.getOperatorTokenType(6); }
"=>"             { return tt.getOperatorTokenType(7); }
".."             { return tt.getOperatorTokenType(8); }
"::"             { return tt.getOperatorTokenType(9); }
"->>"            { return tt.getOperatorTokenType(23); }
"->"             { return tt.getOperatorTokenType(22); }
"<<"             { return tt.getOperatorTokenType(17); }
">>"             { return tt.getOperatorTokenType(18); }

"("{wso}"+"{wso}")"  {return tt.getTokenType("CT_OUTER_JOIN");}

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
"&" {return tt.getCharacterTokenType(22);}
"~" {return tt.getCharacterTokenType(23);}





// MARKER_BEGIN_DATATYPES
"bigint" {return tt.dtt(0);}
"blob" {return tt.dtt(1);}
"boolean" {return tt.dtt(2);}
"character" {return tt.dtt(3);}
"clob" {return tt.dtt(4);}
"date" {return tt.dtt(5);}
"datetime" {return tt.dtt(6);}
"decimal" {return tt.dtt(7);}
"double" {return tt.dtt(8);}
"double precision" {return tt.dtt(9);}
"float" {return tt.dtt(10);}
"int" {return tt.dtt(11);}
"int2" {return tt.dtt(12);}
"int8" {return tt.dtt(13);}
"integer" {return tt.dtt(14);}
"mediumint" {return tt.dtt(15);}
"native character" {return tt.dtt(16);}
"nchar" {return tt.dtt(17);}
"numeric" {return tt.dtt(18);}
"nvarchar" {return tt.dtt(19);}
"real" {return tt.dtt(20);}
"smallint" {return tt.dtt(21);}
"text" {return tt.dtt(22);}
"time" {return tt.dtt(23);}
"tinyint" {return tt.dtt(24);}
"unsigned big int" {return tt.dtt(25);}
"varchar" {return tt.dtt(26);}
"varying character" {return tt.dtt(27);}
// MARKER_END_DATATYPES




// MARKER_BEGIN_KEYWORDS
"abort" {return tt.ktt(0);}
"always" {return tt.ktt(1);}
"action" {return tt.ktt(2);}
"add" {return tt.ktt(3);}
"after" {return tt.ktt(4);}
"all" {return tt.ktt(5);}
"alter" {return tt.ktt(6);}
"analyze" {return tt.ktt(7);}
"and" {return tt.ktt(8);}
"as" {return tt.ktt(9);}
"asc" {return tt.ktt(10);}
"attach" {return tt.ktt(11);}
"autoincrement" {return tt.ktt(12);}
"before" {return tt.ktt(13);}
"begin" {return tt.ktt(14);}
"between" {return tt.ktt(15);}
"by" {return tt.ktt(16);}
"cascade" {return tt.ktt(17);}
"case" {return tt.ktt(18);}
"cast" {return tt.ktt(19);}
"check" {return tt.ktt(20);}
"collate" {return tt.ktt(21);}
"column" {return tt.ktt(22);}
"commit" {return tt.ktt(23);}
"conflict" {return tt.ktt(24);}
"constraint" {return tt.ktt(25);}
"create" {return tt.ktt(26);}
"cross" {return tt.ktt(27);}
"current_date" {return tt.ktt(28);}
"current_time" {return tt.ktt(29);}
"current_timestamp" {return tt.ktt(30);}
"current" {return tt.ktt(31);}
"database" {return tt.ktt(32);}
"default" {return tt.ktt(33);}
"deferrable" {return tt.ktt(34);}
"deferred" {return tt.ktt(35);}
"delete" {return tt.ktt(36);}
"desc" {return tt.ktt(37);}
"detach" {return tt.ktt(38);}
"distinct" {return tt.ktt(39);}
"drop" {return tt.ktt(40);}
"exclude" {return tt.ktt(41);}
"each" {return tt.ktt(42);}
"else" {return tt.ktt(43);}
"end" {return tt.ktt(44);}
"escape" {return tt.ktt(45);}
"except" {return tt.ktt(46);}
"exclusive" {return tt.ktt(47);}
"exists" {return tt.ktt(48);}
"explain" {return tt.ktt(49);}
"fail" {return tt.ktt(50);}
"false" {return tt.ktt(51);}
"filter" {return tt.ktt(52);}
"for" {return tt.ktt(53);}
"foreign" {return tt.ktt(54);}
"following" {return tt.ktt(55);}
"from" {return tt.ktt(56);}
"full" {return tt.ktt(57);}
"generated" {return tt.ktt(58);}
"glob" {return tt.ktt(59);}
"group" {return tt.ktt(60);}
"groups" {return tt.ktt(61);}
"having" {return tt.ktt(62);}
"if" {return tt.ktt(63);}
"ignore" {return tt.ktt(64);}
"immediate" {return tt.ktt(65);}
"in" {return tt.ktt(66);}
"index" {return tt.ktt(67);}
"indexed" {return tt.ktt(68);}
"initially" {return tt.ktt(69);}
"inner" {return tt.ktt(70);}
"insert" {return tt.ktt(71);}
"instead" {return tt.ktt(72);}
"intersect" {return tt.ktt(73);}
"into" {return tt.ktt(74);}
"is" {return tt.ktt(75);}
"isnull" {return tt.ktt(76);}
"join" {return tt.ktt(77);}
"key" {return tt.ktt(78);}
"left" {return tt.ktt(79);}
"like" {return tt.ktt(80);}
"limit" {return tt.ktt(81);}
"match" {return tt.ktt(82);}
"materialized" {return tt.ktt(83);}
"natural" {return tt.ktt(84);}
"no" {return tt.ktt(85);}
"not" {return tt.ktt(86);}
"notnull" {return tt.ktt(87);}
"null" {return tt.ktt(88);}
"of" {return tt.ktt(89);}
"off" {return tt.ktt(90);}
"offset" {return tt.ktt(91);}
"on" {return tt.ktt(92);}
"or" {return tt.ktt(93);}
"order" {return tt.ktt(94);}
"others" {return tt.ktt(95);}
"outer" {return tt.ktt(96);}
"over" {return tt.ktt(97);}
"partition" {return tt.ktt(98);}
"plan" {return tt.ktt(99);}
"pragma" {return tt.ktt(100);}
"preceding" {return tt.ktt(101);}
"primary" {return tt.ktt(102);}
"query" {return tt.ktt(103);}
"raise" {return tt.ktt(104);}
"recursive" {return tt.ktt(105);}
"references" {return tt.ktt(106);}
"range" {return tt.ktt(107);}
"regexp" {return tt.ktt(108);}
"reindex" {return tt.ktt(109);}
"release" {return tt.ktt(110);}
"rename" {return tt.ktt(111);}
"replace" {return tt.ktt(112);}
"restrict" {return tt.ktt(113);}
"right" {return tt.ktt(114);}
"rollback" {return tt.ktt(115);}
"rows" {return tt.ktt(116);}
"row" {return tt.ktt(117);}
"rowid" {return tt.ktt(118);}
"savepoint" {return tt.ktt(119);}
"select" {return tt.ktt(120);}
"set" {return tt.ktt(121);}
"strict" {return tt.ktt(122);}
"stored" {return tt.ktt(123);}
"table" {return tt.ktt(124);}
"temp" {return tt.ktt(125);}
"temporary" {return tt.ktt(126);}
"then" {return tt.ktt(127);}
"ties" {return tt.ktt(128);}
"to" {return tt.ktt(129);}
"transaction" {return tt.ktt(130);}
"trigger" {return tt.ktt(131);}
"true" {return tt.ktt(132);}
"union" {return tt.ktt(133);}
"unique" {return tt.ktt(134);}
"unbounded" {return tt.ktt(135);}
"update" {return tt.ktt(136);}
"using" {return tt.ktt(137);}
"vacuum" {return tt.ktt(138);}
"values" {return tt.ktt(139);}
"window" {return tt.ktt(140);}
"view" {return tt.ktt(141);}
"virtual" {return tt.ktt(142);}
"when" {return tt.ktt(143);}
"where" {return tt.ktt(144);}
"with" {return tt.ktt(145);}
"without" {return tt.ktt(146);}
// MARKER_END_KEYWORDS



// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.ftt(0);}
"acos" {return tt.ftt(1);}
"acosh" {return tt.ftt(2);}
"asin" {return tt.ftt(3);}
"asinh" {return tt.ftt(4);}
"atan" {return tt.ftt(5);}
"atan2" {return tt.ftt(6);}
"atanh" {return tt.ftt(7);}
"avg" {return tt.ftt(8);}
"ceil" {return tt.ftt(9);}
"ceiling" {return tt.ftt(10);}
"changes" {return tt.ftt(11);}
"char" {return tt.ftt(12);}
"coalesce" {return tt.ftt(13);}
"concat" {return tt.ftt(14);}
"concat_ws" {return tt.ftt(15);}
"cos" {return tt.ftt(16);}
"cosh" {return tt.ftt(17);}
"count" {return tt.ftt(18);}
"cume_dist" {return tt.ftt(19);}
"degrees" {return tt.ftt(20);}
"dense_rank" {return tt.ftt(21);}
"exp" {return tt.ftt(22);}
"first_value" {return tt.ftt(23);}
"floor" {return tt.ftt(24);}
"format" {return tt.ftt(25);}
"group_concat" {return tt.ftt(26);}
"hex" {return tt.ftt(27);}
"ifnull" {return tt.ftt(28);}
"iif" {return tt.ftt(29);}
"instr" {return tt.ftt(30);}
"json" {return tt.ftt(31);}
"jsonb" {return tt.ftt(32);}
"jsonb_array" {return tt.ftt(33);}
"jsonb_array_insert" {return tt.ftt(34);}
"jsonb_each" {return tt.ftt(35);}
"jsonb_extract" {return tt.ftt(36);}
"jsonb_group_array" {return tt.ftt(37);}
"jsonb_group_object" {return tt.ftt(38);}
"jsonb_insert" {return tt.ftt(39);}
"jsonb_object" {return tt.ftt(40);}
"jsonb_patch" {return tt.ftt(41);}
"jsonb_remove" {return tt.ftt(42);}
"jsonb_replace" {return tt.ftt(43);}
"jsonb_set" {return tt.ftt(44);}
"jsonb_tree" {return tt.ftt(45);}
"json_array" {return tt.ftt(46);}
"json_array_insert" {return tt.ftt(47);}
"json_array_length" {return tt.ftt(48);}
"json_each" {return tt.ftt(49);}
"json_error_position" {return tt.ftt(50);}
"json_extract" {return tt.ftt(51);}
"json_group_array" {return tt.ftt(52);}
"json_group_object" {return tt.ftt(53);}
"json_insert" {return tt.ftt(54);}
"json_object" {return tt.ftt(55);}
"json_patch" {return tt.ftt(56);}
"json_pretty" {return tt.ftt(57);}
"json_remove" {return tt.ftt(58);}
"json_replace" {return tt.ftt(59);}
"json_set" {return tt.ftt(60);}
"json_tree" {return tt.ftt(61);}
"json_type" {return tt.ftt(62);}
"json_valid" {return tt.ftt(63);}
"julianday" {return tt.ftt(64);}
"lag" {return tt.ftt(65);}
"last_insert_rowid" {return tt.ftt(66);}
"last_value" {return tt.ftt(67);}
"lead" {return tt.ftt(68);}
"length" {return tt.ftt(69);}
"likelihood" {return tt.ftt(70);}
"likely" {return tt.ftt(71);}
"ln" {return tt.ftt(72);}
"load_extension" {return tt.ftt(73);}
"log" {return tt.ftt(74);}
"log10" {return tt.ftt(75);}
"log2" {return tt.ftt(76);}
"lower" {return tt.ftt(77);}
"ltrim" {return tt.ftt(78);}
"max" {return tt.ftt(79);}
"median" {return tt.ftt(80);}
"min" {return tt.ftt(81);}
"mod" {return tt.ftt(82);}
"nth_value" {return tt.ftt(83);}
"ntile" {return tt.ftt(84);}
"nullif" {return tt.ftt(85);}
"octet_length" {return tt.ftt(86);}
"percentile" {return tt.ftt(87);}
"percentile_cont" {return tt.ftt(88);}
"percentile_disc" {return tt.ftt(89);}
"percent_rank" {return tt.ftt(90);}
"pi" {return tt.ftt(91);}
"pow" {return tt.ftt(92);}
"power" {return tt.ftt(93);}
"printf" {return tt.ftt(94);}
"quote" {return tt.ftt(95);}
"radians" {return tt.ftt(96);}
"random" {return tt.ftt(97);}
"randomblob" {return tt.ftt(98);}
"rank" {return tt.ftt(99);}
"round" {return tt.ftt(100);}
"row_number" {return tt.ftt(101);}
"rtrim" {return tt.ftt(102);}
"sign" {return tt.ftt(103);}
"sin" {return tt.ftt(104);}
"sinh" {return tt.ftt(105);}
"soundex" {return tt.ftt(106);}
"sqlite_compileoption_get" {return tt.ftt(107);}
"sqlite_compileoption_used" {return tt.ftt(108);}
"sqlite_offset" {return tt.ftt(109);}
"sqlite_source_id" {return tt.ftt(110);}
"sqlite_version" {return tt.ftt(111);}
"sqrt" {return tt.ftt(112);}
"strftime" {return tt.ftt(113);}
"string_agg" {return tt.ftt(114);}
"substr" {return tt.ftt(115);}
"substring" {return tt.ftt(116);}
"sum" {return tt.ftt(117);}
"tan" {return tt.ftt(118);}
"tanh" {return tt.ftt(119);}
"timediff" {return tt.ftt(120);}
"total" {return tt.ftt(121);}
"total_changes" {return tt.ftt(122);}
"trim" {return tt.ftt(123);}
"trunc" {return tt.ftt(124);}
"typeof" {return tt.ftt(125);}
"unhex" {return tt.ftt(126);}
"unicode" {return tt.ftt(127);}
"unistr" {return tt.ftt(128);}
"unistr_quote" {return tt.ftt(129);}
"unixepoch" {return tt.ftt(130);}
"unlikely" {return tt.ftt(131);}
"upper" {return tt.ftt(132);}
"zeroblob" {return tt.ftt(133);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
"application_id" {return tt.ptt(0);}
"automatic_index" {return tt.ptt(1);}
"auto_vacuum" {return tt.ptt(2);}
"busy_timeout" {return tt.ptt(3);}
"cache_size" {return tt.ptt(4);}
"cache_spill" {return tt.ptt(5);}
"case_sensitive_like" {return tt.ptt(6);}
"cell_size_check" {return tt.ptt(7);}
"checkpoint_fullfsync" {return tt.ptt(8);}
"collation_list" {return tt.ptt(9);}
"compile_options" {return tt.ptt(10);}
"count_changes" {return tt.ptt(11);}
"database_list" {return tt.ptt(12);}
"data_store_directory" {return tt.ptt(13);}
"data_version" {return tt.ptt(14);}
"default_cache_size" {return tt.ptt(15);}
"defer_foreign_keys" {return tt.ptt(16);}
"empty_result_callbacks" {return tt.ptt(17);}
"encoding" {return tt.ptt(18);}
"foreign_keys" {return tt.ptt(19);}
"foreign_key_check" {return tt.ptt(20);}
"foreign_key_list" {return tt.ptt(21);}
"freelist_count" {return tt.ptt(22);}
"fullfsync" {return tt.ptt(23);}
"full_column_names" {return tt.ptt(24);}
"ignore_check_constraints" {return tt.ptt(25);}
"incremental_vacuum" {return tt.ptt(26);}
"index_info" {return tt.ptt(27);}
"index_list" {return tt.ptt(28);}
"index_xinfo" {return tt.ptt(29);}
"integrity_check" {return tt.ptt(30);}
"journal_mode" {return tt.ptt(31);}
"journal_size_limit" {return tt.ptt(32);}
"legacy_file_format" {return tt.ptt(33);}
"locking_mode" {return tt.ptt(34);}
"max_page_count" {return tt.ptt(35);}
"mmap_size" {return tt.ptt(36);}
"page_count" {return tt.ptt(37);}
"page_size" {return tt.ptt(38);}
"parser_trace" {return tt.ptt(39);}
"query_only" {return tt.ptt(40);}
"quick_check" {return tt.ptt(41);}
"read_uncommitted" {return tt.ptt(42);}
"recursive_triggers" {return tt.ptt(43);}
"reverse_unordered_selects" {return tt.ptt(44);}
"schema_version" {return tt.ptt(45);}
"secure_delete" {return tt.ptt(46);}
"short_column_names" {return tt.ptt(47);}
"shrink_memory" {return tt.ptt(48);}
"soft_heap_limit" {return tt.ptt(49);}
"stats" {return tt.ptt(50);}
"synchronous" {return tt.ptt(51);}
"table_info" {return tt.ptt(52);}
"temp_store" {return tt.ptt(53);}
"temp_store_directory" {return tt.ptt(54);}
"threads" {return tt.ptt(55);}
"user_version" {return tt.ptt(56);}
"vdbe_addoptrace" {return tt.ptt(57);}
"vdbe_debug" {return tt.ptt(58);}
"vdbe_listing" {return tt.ptt(59);}
"vdbe_trace" {return tt.ptt(60);}
"wal_autocheckpoint" {return tt.ptt(61);}
"wal_checkpoint" {return tt.ptt(62);}
"writable_schema" {return tt.ptt(63);}
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS


{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }
