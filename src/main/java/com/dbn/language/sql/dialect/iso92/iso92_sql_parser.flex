package com.dbn.language.sql.dialect.iso92;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.psi.tree.IElementType;

%%

%class Iso92SQLParserFlexLexer
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
    public Iso92SQLParserFlexLexer(TokenTypeBundle tt) {
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

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = "`"[^\`]*"`"?

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

{WHITE_SPACE}+     { return stt.getWhiteSpace(); }

{BLOCK_COMMENT}    { return stt.getBlockComment(); }
{LINE_COMMENT}     { return stt.getLineComment(); }

{VARIABLE}         { return stt.getVariable(); }
{INTEGER}          { return stt.getInteger(); }
{NUMBER}           { return stt.getNumber(); }
{STRING}           { return stt.getString(); }

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
"bit" {return tt.getDataTypeTokenType(0);}
"tinyint" {return tt.getDataTypeTokenType(1);}
"bool" {return tt.getDataTypeTokenType(2);}
"boolean" {return tt.getDataTypeTokenType(3);}
"smallint" {return tt.getDataTypeTokenType(4);}
"mediumint" {return tt.getDataTypeTokenType(5);}
"int" {return tt.getDataTypeTokenType(6);}
"integer" {return tt.getDataTypeTokenType(7);}
"bigint" {return tt.getDataTypeTokenType(8);}
"double"{ws}"precision" {return tt.getDataTypeTokenType(9);}
"double" {return tt.getDataTypeTokenType(10);}
"real" {return tt.getDataTypeTokenType(11);}
"float" {return tt.getDataTypeTokenType(12);}
"decimal" {return tt.getDataTypeTokenType(13);}
"dec" {return tt.getDataTypeTokenType(14);}
"numeric" {return tt.getDataTypeTokenType(15);}
"fixed" {return tt.getDataTypeTokenType(16);}
"date" {return tt.getDataTypeTokenType(17);}
"datetime" {return tt.getDataTypeTokenType(18);}
"timestamp" {return tt.getDataTypeTokenType(19);}
"time" {return tt.getDataTypeTokenType(20);}
"year" {return tt.getDataTypeTokenType(21);}
"varchar" {return tt.getDataTypeTokenType(22);}
"national"{ws}"varchar" {return tt.getDataTypeTokenType(23);}
"binary" {return tt.getDataTypeTokenType(24);}
"varbinary" {return tt.getDataTypeTokenType(25);}
"tinyblob" {return tt.getDataTypeTokenType(26);}
"tinytext" {return tt.getDataTypeTokenType(27);}
"blob" {return tt.getDataTypeTokenType(28);}
"text" {return tt.getDataTypeTokenType(29);}
"mediumblob" {return tt.getDataTypeTokenType(30);}
"mediumtext" {return tt.getDataTypeTokenType(31);}
"longblob" {return tt.getDataTypeTokenType(32);}
"longtext" {return tt.getDataTypeTokenType(33);}
"enum" {return tt.getDataTypeTokenType(34);}
// MARKER_END_DATATYPES


// MARKER_BEGIN_KEYWORDS
"accessible" {return tt.getKeywordTokenType(0);}
"add" {return tt.getKeywordTokenType(1);}
"all" {return tt.getKeywordTokenType(2);}
"alter" {return tt.getKeywordTokenType(3);}
"analyze" {return tt.getKeywordTokenType(4);}
"and" {return tt.getKeywordTokenType(5);}
"as" {return tt.getKeywordTokenType(6);}
"asc" {return tt.getKeywordTokenType(7);}
"asensitive" {return tt.getKeywordTokenType(8);}
"before" {return tt.getKeywordTokenType(9);}
"between" {return tt.getKeywordTokenType(10);}
"both" {return tt.getKeywordTokenType(11);}
"by" {return tt.getKeywordTokenType(12);}
"call" {return tt.getKeywordTokenType(13);}
"cascade" {return tt.getKeywordTokenType(14);}
"case" {return tt.getKeywordTokenType(15);}
"change" {return tt.getKeywordTokenType(16);}
"character" {return tt.getKeywordTokenType(17);}
"check" {return tt.getKeywordTokenType(18);}
"close" {return tt.getKeywordTokenType(19);}
"collate" {return tt.getKeywordTokenType(20);}
"column" {return tt.getKeywordTokenType(21);}
"columns" {return tt.getKeywordTokenType(22);}
"concurrent" {return tt.getKeywordTokenType(23);}
"condition" {return tt.getKeywordTokenType(24);}
"constraint" {return tt.getKeywordTokenType(25);}
"continue" {return tt.getKeywordTokenType(26);}
"convert" {return tt.getKeywordTokenType(27);}
"create" {return tt.getKeywordTokenType(28);}
"cross" {return tt.getKeywordTokenType(29);}
"current_user" {return tt.getKeywordTokenType(30);}
"cursor" {return tt.getKeywordTokenType(31);}
"data" {return tt.getKeywordTokenType(32);}
"database" {return tt.getKeywordTokenType(33);}
"databases" {return tt.getKeywordTokenType(34);}
"declare" {return tt.getKeywordTokenType(35);}
"default" {return tt.getKeywordTokenType(36);}
"delayed" {return tt.getKeywordTokenType(37);}
"delete" {return tt.getKeywordTokenType(38);}
"desc" {return tt.getKeywordTokenType(39);}
"describe" {return tt.getKeywordTokenType(40);}
"deterministic" {return tt.getKeywordTokenType(41);}
"distinct" {return tt.getKeywordTokenType(42);}
"distinctrow" {return tt.getKeywordTokenType(43);}
"div" {return tt.getKeywordTokenType(44);}
"do" {return tt.getKeywordTokenType(45);}
"drop" {return tt.getKeywordTokenType(46);}
"dual" {return tt.getKeywordTokenType(47);}
"dumpfile" {return tt.getKeywordTokenType(48);}
"duplicate" {return tt.getKeywordTokenType(49);}
"each" {return tt.getKeywordTokenType(50);}
"else" {return tt.getKeywordTokenType(51);}
"elseif" {return tt.getKeywordTokenType(52);}
"enclosed" {return tt.getKeywordTokenType(53);}
"end" {return tt.getKeywordTokenType(54);}
"escaped" {return tt.getKeywordTokenType(55);}
"exists" {return tt.getKeywordTokenType(56);}
"exit" {return tt.getKeywordTokenType(57);}
"expansion" {return tt.getKeywordTokenType(58);}
"explain" {return tt.getKeywordTokenType(59);}
"fetch" {return tt.getKeywordTokenType(60);}
"fields" {return tt.getKeywordTokenType(61);}
"first" {return tt.getKeywordTokenType(62);}
"float4" {return tt.getKeywordTokenType(63);}
"float8" {return tt.getKeywordTokenType(64);}
"for" {return tt.getKeywordTokenType(65);}
"force" {return tt.getKeywordTokenType(66);}
"foreign" {return tt.getKeywordTokenType(67);}
"from" {return tt.getKeywordTokenType(68);}
"fulltext" {return tt.getKeywordTokenType(69);}
"grant" {return tt.getKeywordTokenType(70);}
"group" {return tt.getKeywordTokenType(71);}
"handler" {return tt.getKeywordTokenType(72);}
"having" {return tt.getKeywordTokenType(73);}
"high_priority" {return tt.getKeywordTokenType(74);}
"if" {return tt.getKeywordTokenType(75);}
"ignore" {return tt.getKeywordTokenType(76);}
"in" {return tt.getKeywordTokenType(77);}
"index" {return tt.getKeywordTokenType(78);}
"infile" {return tt.getKeywordTokenType(79);}
"inner" {return tt.getKeywordTokenType(80);}
"inout" {return tt.getKeywordTokenType(81);}
"insensitive" {return tt.getKeywordTokenType(82);}
"insert" {return tt.getKeywordTokenType(83);}
"int1" {return tt.getKeywordTokenType(84);}
"int2" {return tt.getKeywordTokenType(85);}
"int3" {return tt.getKeywordTokenType(86);}
"int4" {return tt.getKeywordTokenType(87);}
"int8" {return tt.getKeywordTokenType(88);}
"interval" {return tt.getKeywordTokenType(89);}
"into" {return tt.getKeywordTokenType(90);}
"is" {return tt.getKeywordTokenType(91);}
"iterate" {return tt.getKeywordTokenType(92);}
"join" {return tt.getKeywordTokenType(93);}
"key" {return tt.getKeywordTokenType(94);}
"keys" {return tt.getKeywordTokenType(95);}
"kill" {return tt.getKeywordTokenType(96);}
"language" {return tt.getKeywordTokenType(97);}
"last" {return tt.getKeywordTokenType(98);}
"leading" {return tt.getKeywordTokenType(99);}
"leave" {return tt.getKeywordTokenType(100);}
"left" {return tt.getKeywordTokenType(101);}
"level" {return tt.getKeywordTokenType(102);}
"like" {return tt.getKeywordTokenType(103);}
"limit" {return tt.getKeywordTokenType(104);}
"linear" {return tt.getKeywordTokenType(105);}
"lines" {return tt.getKeywordTokenType(106);}
"load" {return tt.getKeywordTokenType(107);}
"local" {return tt.getKeywordTokenType(108);}
"lock" {return tt.getKeywordTokenType(109);}
"long" {return tt.getKeywordTokenType(110);}
"loop" {return tt.getKeywordTokenType(111);}
"low_ignore" {return tt.getKeywordTokenType(112);}
"low_priority" {return tt.getKeywordTokenType(113);}
"master_ssl_verify_server_cert" {return tt.getKeywordTokenType(114);}
"match" {return tt.getKeywordTokenType(115);}
"microsecond" {return tt.getKeywordTokenType(116);}
"middleint" {return tt.getKeywordTokenType(117);}
"mod" {return tt.getKeywordTokenType(118);}
"mode" {return tt.getKeywordTokenType(119);}
"modifies" {return tt.getKeywordTokenType(120);}
"natural" {return tt.getKeywordTokenType(121);}
"next" {return tt.getKeywordTokenType(122);}
"no_write_to_binlog" {return tt.getKeywordTokenType(123);}
"not" {return tt.getKeywordTokenType(124);}
"null" {return tt.getKeywordTokenType(125);}
"offset" {return tt.getKeywordTokenType(126);}
"oj" {return tt.getKeywordTokenType(127);}
"on" {return tt.getKeywordTokenType(128);}
"open" {return tt.getKeywordTokenType(129);}
"optimize" {return tt.getKeywordTokenType(130);}
"option" {return tt.getKeywordTokenType(131);}
"optionally" {return tt.getKeywordTokenType(132);}
"or" {return tt.getKeywordTokenType(133);}
"order" {return tt.getKeywordTokenType(134);}
"out" {return tt.getKeywordTokenType(135);}
"outer" {return tt.getKeywordTokenType(136);}
"outfile" {return tt.getKeywordTokenType(137);}
"precision" {return tt.getKeywordTokenType(138);}
"prev" {return tt.getKeywordTokenType(139);}
"primary" {return tt.getKeywordTokenType(140);}
"procedure" {return tt.getKeywordTokenType(141);}
"purge" {return tt.getKeywordTokenType(142);}
"query" {return tt.getKeywordTokenType(143);}
"quick" {return tt.getKeywordTokenType(144);}
"range" {return tt.getKeywordTokenType(145);}
"read" {return tt.getKeywordTokenType(146);}
"read_only" {return tt.getKeywordTokenType(147);}
"read_write" {return tt.getKeywordTokenType(148);}
"reads" {return tt.getKeywordTokenType(149);}
"references" {return tt.getKeywordTokenType(150);}
"regexp" {return tt.getKeywordTokenType(151);}
"release" {return tt.getKeywordTokenType(152);}
"rename" {return tt.getKeywordTokenType(153);}
"repeat" {return tt.getKeywordTokenType(154);}
"replace" {return tt.getKeywordTokenType(155);}
"require" {return tt.getKeywordTokenType(156);}
"restrict" {return tt.getKeywordTokenType(157);}
"return" {return tt.getKeywordTokenType(158);}
"reverse" {return tt.getKeywordTokenType(159);}
"revoke" {return tt.getKeywordTokenType(160);}
"right" {return tt.getKeywordTokenType(161);}
"rlike" {return tt.getKeywordTokenType(162);}
"rollup" {return tt.getKeywordTokenType(163);}
"schema" {return tt.getKeywordTokenType(164);}
"schemas" {return tt.getKeywordTokenType(165);}
"select" {return tt.getKeywordTokenType(166);}
"sensitive" {return tt.getKeywordTokenType(167);}
"separator" {return tt.getKeywordTokenType(168);}
"set" {return tt.getKeywordTokenType(169);}
"share" {return tt.getKeywordTokenType(170);}
"show" {return tt.getKeywordTokenType(171);}
"spatial" {return tt.getKeywordTokenType(172);}
"specific" {return tt.getKeywordTokenType(173);}
"sql" {return tt.getKeywordTokenType(174);}
"sql_big_result" {return tt.getKeywordTokenType(175);}
"sql_buffer_result" {return tt.getKeywordTokenType(176);}
"sql_cache" {return tt.getKeywordTokenType(177);}
"sql_calc_found_rows" {return tt.getKeywordTokenType(178);}
"sql_no_cache" {return tt.getKeywordTokenType(179);}
"sql_small_result" {return tt.getKeywordTokenType(180);}
"sqlexception" {return tt.getKeywordTokenType(181);}
"sqlstate" {return tt.getKeywordTokenType(182);}
"sqlwarning" {return tt.getKeywordTokenType(183);}
"ssl" {return tt.getKeywordTokenType(184);}
"starting" {return tt.getKeywordTokenType(185);}
"straight_join" {return tt.getKeywordTokenType(186);}
"table" {return tt.getKeywordTokenType(187);}
"terminated" {return tt.getKeywordTokenType(188);}
"then" {return tt.getKeywordTokenType(189);}
"to" {return tt.getKeywordTokenType(190);}
"trailing" {return tt.getKeywordTokenType(191);}
"trigger" {return tt.getKeywordTokenType(192);}
"truncate" {return tt.getKeywordTokenType(193);}
"undo" {return tt.getKeywordTokenType(194);}
"union" {return tt.getKeywordTokenType(195);}
"unique" {return tt.getKeywordTokenType(196);}
"unlock" {return tt.getKeywordTokenType(197);}
"unsigned" {return tt.getKeywordTokenType(198);}
"update" {return tt.getKeywordTokenType(199);}
"usage" {return tt.getKeywordTokenType(200);}
"use" {return tt.getKeywordTokenType(201);}
"using" {return tt.getKeywordTokenType(202);}
"value" {return tt.getKeywordTokenType(203);}
"values" {return tt.getKeywordTokenType(204);}
"varcharacter" {return tt.getKeywordTokenType(205);}
"varying" {return tt.getKeywordTokenType(206);}
"when" {return tt.getKeywordTokenType(207);}
"where" {return tt.getKeywordTokenType(208);}
"while" {return tt.getKeywordTokenType(209);}
"with" {return tt.getKeywordTokenType(210);}
"write" {return tt.getKeywordTokenType(211);}
"xor" {return tt.getKeywordTokenType(212);}
"zerofill" {return tt.getKeywordTokenType(213);}
"false" {return tt.getKeywordTokenType(214);}
"true" {return tt.getKeywordTokenType(215);}
// MARKER_END_KEYWORDS


// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.getFunctionTokenType(0);}
"acos" {return tt.getFunctionTokenType(1);}
"adddate" {return tt.getFunctionTokenType(2);}
"addtime" {return tt.getFunctionTokenType(3);}
"aes_decrypt" {return tt.getFunctionTokenType(4);}
"aes_encrypt" {return tt.getFunctionTokenType(5);}
"against" {return tt.getFunctionTokenType(6);}
"ascii" {return tt.getFunctionTokenType(7);}
"asin" {return tt.getFunctionTokenType(8);}
"atan" {return tt.getFunctionTokenType(9);}
"atan2" {return tt.getFunctionTokenType(10);}
"avg" {return tt.getFunctionTokenType(11);}
"benchmark" {return tt.getFunctionTokenType(12);}
"bin" {return tt.getFunctionTokenType(13);}
"bit_and" {return tt.getFunctionTokenType(14);}
"bit_length" {return tt.getFunctionTokenType(15);}
"bit_or" {return tt.getFunctionTokenType(16);}
"bit_xor" {return tt.getFunctionTokenType(17);}
"boolean"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(18);}
"ceil" {return tt.getFunctionTokenType(19);}
"ceiling" {return tt.getFunctionTokenType(20);}
"char" {return tt.getFunctionTokenType(21);}
"char_length" {return tt.getFunctionTokenType(22);}
"character_length" {return tt.getFunctionTokenType(23);}
"charset" {return tt.getFunctionTokenType(24);}
"coercibility" {return tt.getFunctionTokenType(25);}
"collation" {return tt.getFunctionTokenType(26);}
"compress" {return tt.getFunctionTokenType(27);}
"concat" {return tt.getFunctionTokenType(28);}
"concat_ws" {return tt.getFunctionTokenType(29);}
"connection_id" {return tt.getFunctionTokenType(30);}
"conv" {return tt.getFunctionTokenType(31);}
"convert_tz" {return tt.getFunctionTokenType(32);}
"cos" {return tt.getFunctionTokenType(33);}
"cot" {return tt.getFunctionTokenType(34);}
"count" {return tt.getFunctionTokenType(35);}
"crc32" {return tt.getFunctionTokenType(36);}
"curdate" {return tt.getFunctionTokenType(37);}
"current_date" {return tt.getFunctionTokenType(38);}
"current_time" {return tt.getFunctionTokenType(39);}
"current_timestamp" {return tt.getFunctionTokenType(40);}
"current_user"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(41);}
"curtime" {return tt.getFunctionTokenType(42);}
"database"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(43);}
"date"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(44);}
"date_add" {return tt.getFunctionTokenType(45);}
"date_format" {return tt.getFunctionTokenType(46);}
"date_sub" {return tt.getFunctionTokenType(47);}
"datediff" {return tt.getFunctionTokenType(48);}
"day"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(49);}
"day_hour" {return tt.getFunctionTokenType(50);}
"day_microsecond" {return tt.getFunctionTokenType(51);}
"day_minute" {return tt.getFunctionTokenType(52);}
"day_second" {return tt.getFunctionTokenType(53);}
"dayname" {return tt.getFunctionTokenType(54);}
"dayofmonth" {return tt.getFunctionTokenType(55);}
"dayofweek" {return tt.getFunctionTokenType(56);}
"dayofyear" {return tt.getFunctionTokenType(57);}
"decode" {return tt.getFunctionTokenType(58);}
"default"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(59);}
"degrees" {return tt.getFunctionTokenType(60);}
"des_decrypt" {return tt.getFunctionTokenType(61);}
"des_encrypt" {return tt.getFunctionTokenType(62);}
"elt" {return tt.getFunctionTokenType(63);}
"encode" {return tt.getFunctionTokenType(64);}
"encrypt" {return tt.getFunctionTokenType(65);}
"exp" {return tt.getFunctionTokenType(66);}
"expansion"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(67);}
"export_set" {return tt.getFunctionTokenType(68);}
"extract" {return tt.getFunctionTokenType(69);}
"field" {return tt.getFunctionTokenType(70);}
"find_in_set" {return tt.getFunctionTokenType(71);}
"floor" {return tt.getFunctionTokenType(72);}
"fn_second_microsecond" {return tt.getFunctionTokenType(73);}
"format" {return tt.getFunctionTokenType(74);}
"found_rows" {return tt.getFunctionTokenType(75);}
"from_days" {return tt.getFunctionTokenType(76);}
"from_unixtime" {return tt.getFunctionTokenType(77);}
"get_format" {return tt.getFunctionTokenType(78);}
"get_lock" {return tt.getFunctionTokenType(79);}
"group_concat" {return tt.getFunctionTokenType(80);}
"hex" {return tt.getFunctionTokenType(81);}
"hour" {return tt.getFunctionTokenType(82);}
"hour_microsecond" {return tt.getFunctionTokenType(83);}
"hour_minute" {return tt.getFunctionTokenType(84);}
"hour_second" {return tt.getFunctionTokenType(85);}
"if"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(86);}
"ifnull" {return tt.getFunctionTokenType(87);}
"inet_aton" {return tt.getFunctionTokenType(88);}
"inet_ntoa" {return tt.getFunctionTokenType(89);}
"insert"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(90);}
"instr" {return tt.getFunctionTokenType(91);}
"is_free_lock" {return tt.getFunctionTokenType(92);}
"is_used_lock" {return tt.getFunctionTokenType(93);}
"language"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(94);}
"last_day" {return tt.getFunctionTokenType(95);}
"last_insert_id" {return tt.getFunctionTokenType(96);}
"lcase" {return tt.getFunctionTokenType(97);}
"left"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(98);}
"length" {return tt.getFunctionTokenType(99);}
"like"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(100);}
"ln" {return tt.getFunctionTokenType(101);}
"load_file" {return tt.getFunctionTokenType(102);}
"localtime" {return tt.getFunctionTokenType(103);}
"localtimestamp" {return tt.getFunctionTokenType(104);}
"locate" {return tt.getFunctionTokenType(105);}
"log" {return tt.getFunctionTokenType(106);}
"log10" {return tt.getFunctionTokenType(107);}
"log2" {return tt.getFunctionTokenType(108);}
"lower" {return tt.getFunctionTokenType(109);}
"lpad" {return tt.getFunctionTokenType(110);}
"ltrim" {return tt.getFunctionTokenType(111);}
"make_set" {return tt.getFunctionTokenType(112);}
"makedate" {return tt.getFunctionTokenType(113);}
"maketime" {return tt.getFunctionTokenType(114);}
"master_pos_wait" {return tt.getFunctionTokenType(115);}
"match"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(116);}
"max" {return tt.getFunctionTokenType(117);}
"md5" {return tt.getFunctionTokenType(118);}
"microsecond"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(119);}
"mid" {return tt.getFunctionTokenType(120);}
"min" {return tt.getFunctionTokenType(121);}
"minute" {return tt.getFunctionTokenType(122);}
"minute_microsecond" {return tt.getFunctionTokenType(123);}
"minute_second" {return tt.getFunctionTokenType(124);}
"mod"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(125);}
"month" {return tt.getFunctionTokenType(126);}
"monthname" {return tt.getFunctionTokenType(127);}
"name_const" {return tt.getFunctionTokenType(128);}
"not_like" {return tt.getFunctionTokenType(129);}
"not_regexp" {return tt.getFunctionTokenType(130);}
"now" {return tt.getFunctionTokenType(131);}
"nullif" {return tt.getFunctionTokenType(132);}
"oct" {return tt.getFunctionTokenType(133);}
"octet_length" {return tt.getFunctionTokenType(134);}
"old_password" {return tt.getFunctionTokenType(135);}
"ord" {return tt.getFunctionTokenType(136);}
"password" {return tt.getFunctionTokenType(137);}
"period_add" {return tt.getFunctionTokenType(138);}
"period_diff" {return tt.getFunctionTokenType(139);}
"pi" {return tt.getFunctionTokenType(140);}
"position" {return tt.getFunctionTokenType(141);}
"pow" {return tt.getFunctionTokenType(142);}
"power" {return tt.getFunctionTokenType(143);}
"quarter" {return tt.getFunctionTokenType(144);}
"query"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(145);}
"quote" {return tt.getFunctionTokenType(146);}
"radians" {return tt.getFunctionTokenType(147);}
"rand" {return tt.getFunctionTokenType(148);}
"regexp"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(149);}
"release_lock" {return tt.getFunctionTokenType(150);}
"repeat"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(151);}
"replace"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(152);}
"reverse"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(153);}
"right"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(154);}
"rlike"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(155);}
"round" {return tt.getFunctionTokenType(156);}
"row_count" {return tt.getFunctionTokenType(157);}
"rpad" {return tt.getFunctionTokenType(158);}
"rtrim" {return tt.getFunctionTokenType(159);}
"schema"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(160);}
"sec_to_time" {return tt.getFunctionTokenType(161);}
"second" {return tt.getFunctionTokenType(162);}
"second_microsecond" {return tt.getFunctionTokenType(163);}
"session_user" {return tt.getFunctionTokenType(164);}
"sha" {return tt.getFunctionTokenType(165);}
"sha1" {return tt.getFunctionTokenType(166);}
"sha2" {return tt.getFunctionTokenType(167);}
"sign" {return tt.getFunctionTokenType(168);}
"sin" {return tt.getFunctionTokenType(169);}
"sleep" {return tt.getFunctionTokenType(170);}
"soundex" {return tt.getFunctionTokenType(171);}
"sounds_like" {return tt.getFunctionTokenType(172);}
"space" {return tt.getFunctionTokenType(173);}
"sqrt" {return tt.getFunctionTokenType(174);}
"std" {return tt.getFunctionTokenType(175);}
"stddev" {return tt.getFunctionTokenType(176);}
"stddev_pop" {return tt.getFunctionTokenType(177);}
"stddev_samp" {return tt.getFunctionTokenType(178);}
"str_to_date" {return tt.getFunctionTokenType(179);}
"strcmp" {return tt.getFunctionTokenType(180);}
"subdate" {return tt.getFunctionTokenType(181);}
"substr" {return tt.getFunctionTokenType(182);}
"substring" {return tt.getFunctionTokenType(183);}
"substring_index" {return tt.getFunctionTokenType(184);}
"subtime" {return tt.getFunctionTokenType(185);}
"sum" {return tt.getFunctionTokenType(186);}
"sysdate" {return tt.getFunctionTokenType(187);}
"system_user" {return tt.getFunctionTokenType(188);}
"tan" {return tt.getFunctionTokenType(189);}
"time"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(190);}
"time_format" {return tt.getFunctionTokenType(191);}
"time_to_sec" {return tt.getFunctionTokenType(192);}
"timediff" {return tt.getFunctionTokenType(193);}
"timestamp"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(194);}
"timestampadd" {return tt.getFunctionTokenType(195);}
"timestampdiff" {return tt.getFunctionTokenType(196);}
"to_days" {return tt.getFunctionTokenType(197);}
"trim" {return tt.getFunctionTokenType(198);}
"truncate"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(199);}
"ucase" {return tt.getFunctionTokenType(200);}
"uncompress" {return tt.getFunctionTokenType(201);}
"uncompressed_length" {return tt.getFunctionTokenType(202);}
"unhex" {return tt.getFunctionTokenType(203);}
"unix_timestamp" {return tt.getFunctionTokenType(204);}
"upper" {return tt.getFunctionTokenType(205);}
"user" {return tt.getFunctionTokenType(206);}
"utc_date" {return tt.getFunctionTokenType(207);}
"utc_time" {return tt.getFunctionTokenType(208);}
"utc_timestamp" {return tt.getFunctionTokenType(209);}
"uuid" {return tt.getFunctionTokenType(210);}
"uuid_short" {return tt.getFunctionTokenType(211);}
"var_pop" {return tt.getFunctionTokenType(212);}
"var_samp" {return tt.getFunctionTokenType(213);}
"variance" {return tt.getFunctionTokenType(214);}
"version" {return tt.getFunctionTokenType(215);}
"week" {return tt.getFunctionTokenType(216);}
"weekday" {return tt.getFunctionTokenType(217);}
"weekofyear" {return tt.getFunctionTokenType(218);}
"weight_string" {return tt.getFunctionTokenType(219);}
"year"{wso}"(" { yypushback(1); return tt.getFunctionTokenType(220);}
"year_month" {return tt.getFunctionTokenType(221);}
"yearweek" {return tt.getFunctionTokenType(222);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}    { return stt.getQuotedIdentifier(); }
.                      { return stt.getIdentifier(); }

