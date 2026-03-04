package com.dbn.language.sql.dialect.sqlite;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
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

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

VARIABLE = ":"{wso}({IDENTIFIER}|{INTEGER})

%state DIV
%%

{WHITE_SPACE}+   { return stt.getWhiteSpace(); }

{BLOCK_COMMENT}  { return stt.getBlockComment(); }
{LINE_COMMENT}   { return stt.getLineComment(); }

{VARIABLE}       { return stt.getVariable(); }
{INTEGER}        { return stt.getInteger(); }
{NUMBER}         { return stt.getNumber(); }
{STRING}         { return stt.getString(); }

"="{wso}"="      { return tt.getOperatorTokenType(0); }
"|"{wso}"|"      { return tt.getOperatorTokenType(1); }
"<"{wso}"="      { return tt.getOperatorTokenType(2); }
">"{wso}"="      { return tt.getOperatorTokenType(3); }
"<"{wso}">"      { return tt.getOperatorTokenType(4); }
"!"{wso}"="      { return tt.getOperatorTokenType(5); }
":"{wso}"="      { return tt.getOperatorTokenType(6); }
"="{wso}">"      { return tt.getOperatorTokenType(7); }
".."             { return tt.getOperatorTokenType(8); }
"::"             { return tt.getOperatorTokenType(9); }

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





// MARKER_BEGIN_DATATYPES
"bigint" {return tt.getDataTypeTokenType(0);}
"blob" {return tt.getDataTypeTokenType(1);}
"boolean" {return tt.getDataTypeTokenType(2);}
"character" {return tt.getDataTypeTokenType(3);}
"clob" {return tt.getDataTypeTokenType(4);}
"date" {return tt.getDataTypeTokenType(5);}
"datetime" {return tt.getDataTypeTokenType(6);}
"decimal" {return tt.getDataTypeTokenType(7);}
"double" {return tt.getDataTypeTokenType(8);}
"double"{ws}"precision" {return tt.getDataTypeTokenType(9);}
"float" {return tt.getDataTypeTokenType(10);}
"int" {return tt.getDataTypeTokenType(11);}
"int2" {return tt.getDataTypeTokenType(12);}
"int8" {return tt.getDataTypeTokenType(13);}
"integer" {return tt.getDataTypeTokenType(14);}
"mediumint" {return tt.getDataTypeTokenType(15);}
"native"{ws}"character" {return tt.getDataTypeTokenType(16);}
"nchar" {return tt.getDataTypeTokenType(17);}
"numeric" {return tt.getDataTypeTokenType(18);}
"nvarchar" {return tt.getDataTypeTokenType(19);}
"real" {return tt.getDataTypeTokenType(20);}
"smallint" {return tt.getDataTypeTokenType(21);}
"text" {return tt.getDataTypeTokenType(22);}
"time" {return tt.getDataTypeTokenType(23);}
"tinyint" {return tt.getDataTypeTokenType(24);}
"unsigned"{ws}"big"{ws}"int" {return tt.getDataTypeTokenType(25);}
"varchar" {return tt.getDataTypeTokenType(26);}
"varying"{ws}"character" {return tt.getDataTypeTokenType(27);}
// MARKER_END_DATATYPES




// MARKER_BEGIN_KEYWORDS
"abort" {return tt.getKeywordTokenType(0);}
"action" {return tt.getKeywordTokenType(1);}
"add" {return tt.getKeywordTokenType(2);}
"after" {return tt.getKeywordTokenType(3);}
"all" {return tt.getKeywordTokenType(4);}
"alter" {return tt.getKeywordTokenType(5);}
"analyze" {return tt.getKeywordTokenType(6);}
"and" {return tt.getKeywordTokenType(7);}
"as" {return tt.getKeywordTokenType(8);}
"asc" {return tt.getKeywordTokenType(9);}
"attach" {return tt.getKeywordTokenType(10);}
"autoincrement" {return tt.getKeywordTokenType(11);}
"before" {return tt.getKeywordTokenType(12);}
"begin" {return tt.getKeywordTokenType(13);}
"between" {return tt.getKeywordTokenType(14);}
"by" {return tt.getKeywordTokenType(15);}
"cascade" {return tt.getKeywordTokenType(16);}
"case" {return tt.getKeywordTokenType(17);}
"cast" {return tt.getKeywordTokenType(18);}
"check" {return tt.getKeywordTokenType(19);}
"collate" {return tt.getKeywordTokenType(20);}
"column" {return tt.getKeywordTokenType(21);}
"commit" {return tt.getKeywordTokenType(22);}
"conflict" {return tt.getKeywordTokenType(23);}
"constraint" {return tt.getKeywordTokenType(24);}
"create" {return tt.getKeywordTokenType(25);}
"cross" {return tt.getKeywordTokenType(26);}
"current_date" {return tt.getKeywordTokenType(27);}
"current_time" {return tt.getKeywordTokenType(28);}
"current_timestamp" {return tt.getKeywordTokenType(29);}
"database" {return tt.getKeywordTokenType(30);}
"default" {return tt.getKeywordTokenType(31);}
"deferrable" {return tt.getKeywordTokenType(32);}
"deferred" {return tt.getKeywordTokenType(33);}
"delete" {return tt.getKeywordTokenType(34);}
"desc" {return tt.getKeywordTokenType(35);}
"detach" {return tt.getKeywordTokenType(36);}
"distinct" {return tt.getKeywordTokenType(37);}
"drop" {return tt.getKeywordTokenType(38);}
"each" {return tt.getKeywordTokenType(39);}
"else" {return tt.getKeywordTokenType(40);}
"end" {return tt.getKeywordTokenType(41);}
"escape" {return tt.getKeywordTokenType(42);}
"except" {return tt.getKeywordTokenType(43);}
"exclusive" {return tt.getKeywordTokenType(44);}
"exists" {return tt.getKeywordTokenType(45);}
"explain" {return tt.getKeywordTokenType(46);}
"fail" {return tt.getKeywordTokenType(47);}
"for" {return tt.getKeywordTokenType(48);}
"foreign" {return tt.getKeywordTokenType(49);}
"from" {return tt.getKeywordTokenType(50);}
"full" {return tt.getKeywordTokenType(51);}
"glob" {return tt.getKeywordTokenType(52);}
"group" {return tt.getKeywordTokenType(53);}
"having" {return tt.getKeywordTokenType(54);}
"if" {return tt.getKeywordTokenType(55);}
"ignore" {return tt.getKeywordTokenType(56);}
"immediate" {return tt.getKeywordTokenType(57);}
"in" {return tt.getKeywordTokenType(58);}
"index" {return tt.getKeywordTokenType(59);}
"indexed" {return tt.getKeywordTokenType(60);}
"initially" {return tt.getKeywordTokenType(61);}
"inner" {return tt.getKeywordTokenType(62);}
"insert" {return tt.getKeywordTokenType(63);}
"instead" {return tt.getKeywordTokenType(64);}
"intersect" {return tt.getKeywordTokenType(65);}
"into" {return tt.getKeywordTokenType(66);}
"is" {return tt.getKeywordTokenType(67);}
"isnull" {return tt.getKeywordTokenType(68);}
"join" {return tt.getKeywordTokenType(69);}
"key" {return tt.getKeywordTokenType(70);}
"left" {return tt.getKeywordTokenType(71);}
"like" {return tt.getKeywordTokenType(72);}
"limit" {return tt.getKeywordTokenType(73);}
"match" {return tt.getKeywordTokenType(74);}
"natural" {return tt.getKeywordTokenType(75);}
"no" {return tt.getKeywordTokenType(76);}
"not" {return tt.getKeywordTokenType(77);}
"notnull" {return tt.getKeywordTokenType(78);}
"null" {return tt.getKeywordTokenType(79);}
"of" {return tt.getKeywordTokenType(80);}
"off" {return tt.getKeywordTokenType(81);}
"offset" {return tt.getKeywordTokenType(82);}
"on" {return tt.getKeywordTokenType(83);}
"or" {return tt.getKeywordTokenType(84);}
"order" {return tt.getKeywordTokenType(85);}
"outer" {return tt.getKeywordTokenType(86);}
"plan" {return tt.getKeywordTokenType(87);}
"pragma" {return tt.getKeywordTokenType(88);}
"primary" {return tt.getKeywordTokenType(89);}
"query" {return tt.getKeywordTokenType(90);}
"raise" {return tt.getKeywordTokenType(91);}
"recursive" {return tt.getKeywordTokenType(92);}
"references" {return tt.getKeywordTokenType(93);}
"regexp" {return tt.getKeywordTokenType(94);}
"reindex" {return tt.getKeywordTokenType(95);}
"release" {return tt.getKeywordTokenType(96);}
"rename" {return tt.getKeywordTokenType(97);}
"replace" {return tt.getKeywordTokenType(98);}
"restrict" {return tt.getKeywordTokenType(99);}
"right" {return tt.getKeywordTokenType(100);}
"rollback" {return tt.getKeywordTokenType(101);}
"row" {return tt.getKeywordTokenType(102);}
"rowid" {return tt.getKeywordTokenType(103);}
"savepoint" {return tt.getKeywordTokenType(104);}
"select" {return tt.getKeywordTokenType(105);}
"set" {return tt.getKeywordTokenType(106);}
"table" {return tt.getKeywordTokenType(107);}
"temp" {return tt.getKeywordTokenType(108);}
"temporary" {return tt.getKeywordTokenType(109);}
"then" {return tt.getKeywordTokenType(110);}
"to" {return tt.getKeywordTokenType(111);}
"transaction" {return tt.getKeywordTokenType(112);}
"trigger" {return tt.getKeywordTokenType(113);}
"union" {return tt.getKeywordTokenType(114);}
"unique" {return tt.getKeywordTokenType(115);}
"update" {return tt.getKeywordTokenType(116);}
"using" {return tt.getKeywordTokenType(117);}
"vacuum" {return tt.getKeywordTokenType(118);}
"values" {return tt.getKeywordTokenType(119);}
"view" {return tt.getKeywordTokenType(120);}
"virtual" {return tt.getKeywordTokenType(121);}
"when" {return tt.getKeywordTokenType(122);}
"where" {return tt.getKeywordTokenType(123);}
"with" {return tt.getKeywordTokenType(124);}
"without" {return tt.getKeywordTokenType(125);}
// MARKER_END_KEYWORDS



// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.getFunctionTokenType(0);}
"avg" {return tt.getFunctionTokenType(1);}
"changes" {return tt.getFunctionTokenType(2);}
"char" {return tt.getFunctionTokenType(3);}
"coalesce" {return tt.getFunctionTokenType(4);}
"count" {return tt.getFunctionTokenType(5);}
"group_concat" {return tt.getFunctionTokenType(6);}
"hex" {return tt.getFunctionTokenType(7);}
"ifnull" {return tt.getFunctionTokenType(8);}
"instr" {return tt.getFunctionTokenType(9);}
"json" {return tt.getFunctionTokenType(10);}
"json_array" {return tt.getFunctionTokenType(11);}
"json_array_length" {return tt.getFunctionTokenType(12);}
"json_extract" {return tt.getFunctionTokenType(13);}
"json_insert" {return tt.getFunctionTokenType(14);}
"json_object" {return tt.getFunctionTokenType(15);}
"json_remove" {return tt.getFunctionTokenType(16);}
"json_replace" {return tt.getFunctionTokenType(17);}
"json_set" {return tt.getFunctionTokenType(18);}
"json_type" {return tt.getFunctionTokenType(19);}
"json_valid" {return tt.getFunctionTokenType(20);}
"julianday" {return tt.getFunctionTokenType(21);}
"last_insert_rowid" {return tt.getFunctionTokenType(22);}
"length" {return tt.getFunctionTokenType(23);}
"likelihood" {return tt.getFunctionTokenType(24);}
"likely" {return tt.getFunctionTokenType(25);}
"load_extension" {return tt.getFunctionTokenType(26);}
"lower" {return tt.getFunctionTokenType(27);}
"ltrim" {return tt.getFunctionTokenType(28);}
"max" {return tt.getFunctionTokenType(29);}
"min" {return tt.getFunctionTokenType(30);}
"nullif" {return tt.getFunctionTokenType(31);}
"printf" {return tt.getFunctionTokenType(32);}
"quote" {return tt.getFunctionTokenType(33);}
"random" {return tt.getFunctionTokenType(34);}
"randomblob" {return tt.getFunctionTokenType(35);}
"round" {return tt.getFunctionTokenType(36);}
"rtrim" {return tt.getFunctionTokenType(37);}
"soundex" {return tt.getFunctionTokenType(38);}
"sqlite_compileoption_get" {return tt.getFunctionTokenType(39);}
"sqlite_compileoption_used" {return tt.getFunctionTokenType(40);}
"sqlite_source_id" {return tt.getFunctionTokenType(41);}
"sqlite_version" {return tt.getFunctionTokenType(42);}
"strftime" {return tt.getFunctionTokenType(43);}
"substr" {return tt.getFunctionTokenType(44);}
"sum" {return tt.getFunctionTokenType(45);}
"total" {return tt.getFunctionTokenType(46);}
"total_changes" {return tt.getFunctionTokenType(47);}
"trim" {return tt.getFunctionTokenType(48);}
"typeof" {return tt.getFunctionTokenType(49);}
"unlikely" {return tt.getFunctionTokenType(50);}
"unicode" {return tt.getFunctionTokenType(51);}
"upper" {return tt.getFunctionTokenType(52);}
"zeroblob" {return tt.getFunctionTokenType(53);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
"application_id" {return tt.getParameterTokenType(0);}
"auto_vacuum" {return tt.getParameterTokenType(1);}
"automatic_index" {return tt.getParameterTokenType(2);}
"busy_timeout" {return tt.getParameterTokenType(3);}
"cache_size" {return tt.getParameterTokenType(4);}
"cache_spill" {return tt.getParameterTokenType(5);}
"case_sensitive_like" {return tt.getParameterTokenType(6);}
"cell_size_check" {return tt.getParameterTokenType(7);}
"checkpoint_fullfsync" {return tt.getParameterTokenType(8);}
"collation_list" {return tt.getParameterTokenType(9);}
"compile_options" {return tt.getParameterTokenType(10);}
"count_changes" {return tt.getParameterTokenType(11);}
"data_store_directory" {return tt.getParameterTokenType(12);}
"data_version" {return tt.getParameterTokenType(13);}
"database_list" {return tt.getParameterTokenType(14);}
"default_cache_size" {return tt.getParameterTokenType(15);}
"defer_foreign_keys" {return tt.getParameterTokenType(16);}
"empty_result_callbacks" {return tt.getParameterTokenType(17);}
"encoding" {return tt.getParameterTokenType(18);}
"foreign_key_check" {return tt.getParameterTokenType(19);}
"foreign_key_list" {return tt.getParameterTokenType(20);}
"foreign_keys" {return tt.getParameterTokenType(21);}
"freelist_count" {return tt.getParameterTokenType(22);}
"full_column_names" {return tt.getParameterTokenType(23);}
"fullfsync" {return tt.getParameterTokenType(24);}
"ignore_check_constraints" {return tt.getParameterTokenType(25);}
"incremental_vacuum" {return tt.getParameterTokenType(26);}
"index_info" {return tt.getParameterTokenType(27);}
"index_list" {return tt.getParameterTokenType(28);}
"index_xinfo" {return tt.getParameterTokenType(29);}
"integrity_check" {return tt.getParameterTokenType(30);}
"journal_mode" {return tt.getParameterTokenType(31);}
"journal_size_limit" {return tt.getParameterTokenType(32);}
"legacy_file_format" {return tt.getParameterTokenType(33);}
"locking_mode" {return tt.getParameterTokenType(34);}
"max_page_count" {return tt.getParameterTokenType(35);}
"mmap_size" {return tt.getParameterTokenType(36);}
"page_count" {return tt.getParameterTokenType(37);}
"page_size" {return tt.getParameterTokenType(38);}
"parser_trace" {return tt.getParameterTokenType(39);}
"query_only" {return tt.getParameterTokenType(40);}
"quick_check" {return tt.getParameterTokenType(41);}
"read_uncommitted" {return tt.getParameterTokenType(42);}
"recursive_triggers" {return tt.getParameterTokenType(43);}
"reverse_unordered_selects" {return tt.getParameterTokenType(44);}
"schema_version" {return tt.getParameterTokenType(45);}
"secure_delete" {return tt.getParameterTokenType(46);}
"short_column_names" {return tt.getParameterTokenType(47);}
"shrink_memory" {return tt.getParameterTokenType(48);}
"soft_heap_limit" {return tt.getParameterTokenType(49);}
"stats" {return tt.getParameterTokenType(50);}
"synchronous" {return tt.getParameterTokenType(51);}
"table_info" {return tt.getParameterTokenType(52);}
"temp_store" {return tt.getParameterTokenType(53);}
"temp_store_directory" {return tt.getParameterTokenType(54);}
"threads" {return tt.getParameterTokenType(55);}
"user_version" {return tt.getParameterTokenType(56);}
"vdbe_addoptrace" {return tt.getParameterTokenType(57);}
"vdbe_debug" {return tt.getParameterTokenType(58);}
"vdbe_listing" {return tt.getParameterTokenType(59);}
"vdbe_trace" {return tt.getParameterTokenType(60);}
"wal_autocheckpoint" {return tt.getParameterTokenType(61);}
"wal_checkpoint" {return tt.getParameterTokenType(62);}
"writable_schema" {return tt.getParameterTokenType(63);}
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS


{IDENTIFIER}           { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}    { return stt.getQuotedIdentifier(); }
.                      { return stt.getIdentifier(); }
