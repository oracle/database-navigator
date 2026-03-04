package com.dbn.language.sql.dialect.mysql;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

%%

%class MysqlSQLParserFlexLexer
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
    public MysqlSQLParserFlexLexer(TokenTypeBundle tt) {
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
LINE_COMMENT = ("--"|"#"){input_character}*

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = "`"[^\`]*"`"?

CHARSET ="armscii8"|"ascii"|"big5"|"binary"|"cp1250"|"cp1251"|"cp1256"|"cp1257"|"cp850"|"cp852"|"cp866"|"cp932"|"dec8"|"eucjpms"|"euckr"|"gb2312"|"gbk"|"geostd8"|"greek"|"hebrew"|"hp8"|"keybcs2"|"koi8r"|"koi8u"|"latin1"|"latin2"|"latin5"|"latin7"|"macce"|"macroman"|"sjis"|"tis620"|"ucs2"|"ujis"|"utf8"|"utf8mb3"|"utf8mb4"|"utf16"|"utf16le"|"utf32"

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

"="{wso}"=" {return tt.getOperatorTokenType(0);}
"|"{wso}"|" {return tt.getOperatorTokenType(1);}
"<"{wso}"=" {return tt.getOperatorTokenType(2);}
">"{wso}"=" {return tt.getOperatorTokenType(3);}
"<"{wso}">" {return tt.getOperatorTokenType(4);}
"!"{wso}"=" {return tt.getOperatorTokenType(5);}
":"{wso}"=" {return tt.getOperatorTokenType(6);}
"="{wso}">" {return tt.getOperatorTokenType(7);}
".."        {return tt.getOperatorTokenType(8);}
"::"        {return tt.getOperatorTokenType(9);}

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
"binary" {return tt.getDataTypeTokenType(1);}
"bit" {return tt.getDataTypeTokenType(2);}
"blob" {return tt.getDataTypeTokenType(3);}
"bool" {return tt.getDataTypeTokenType(4);}
"boolean" {return tt.getDataTypeTokenType(5);}
"char" {return tt.getDataTypeTokenType(6);}
"date" {return tt.getDataTypeTokenType(7);}
"datetime" {return tt.getDataTypeTokenType(8);}
"dec" {return tt.getDataTypeTokenType(9);}
"decimal" {return tt.getDataTypeTokenType(10);}
"double" {return tt.getDataTypeTokenType(11);}
"enum" {return tt.getDataTypeTokenType(12);}
"float" {return tt.getDataTypeTokenType(13);}
"geometry" {return tt.getDataTypeTokenType(14);}
"geometrycollection" {return tt.getDataTypeTokenType(15);}
"int" {return tt.getDataTypeTokenType(16);}
"integer" {return tt.getDataTypeTokenType(17);}
"json" {return tt.getDataTypeTokenType(18);}
"linestring" {return tt.getDataTypeTokenType(19);}
"longblob" {return tt.getDataTypeTokenType(20);}
"longtext" {return tt.getDataTypeTokenType(21);}
"mediumblob" {return tt.getDataTypeTokenType(22);}
"mediumint" {return tt.getDataTypeTokenType(23);}
"mediumtext" {return tt.getDataTypeTokenType(24);}
"multilinestring" {return tt.getDataTypeTokenType(25);}
"multipoint" {return tt.getDataTypeTokenType(26);}
"multipolygon" {return tt.getDataTypeTokenType(27);}
"numeric" {return tt.getDataTypeTokenType(28);}
"point" {return tt.getDataTypeTokenType(29);}
"polygon" {return tt.getDataTypeTokenType(30);}
"real" {return tt.getDataTypeTokenType(31);}
"smallint" {return tt.getDataTypeTokenType(32);}
"text" {return tt.getDataTypeTokenType(33);}
"time" {return tt.getDataTypeTokenType(34);}
"timestamp" {return tt.getDataTypeTokenType(35);}
"tinyblob" {return tt.getDataTypeTokenType(36);}
"tinyint" {return tt.getDataTypeTokenType(37);}
"tinytext" {return tt.getDataTypeTokenType(38);}
"varbinary" {return tt.getDataTypeTokenType(39);}
"varchar" {return tt.getDataTypeTokenType(40);}
"year" {return tt.getDataTypeTokenType(41);}
// MARKER_END_DATATYPES


// MARKER_BEGIN_KEYWORDS
"accessible" {return tt.getKeywordTokenType(0);}
"action" {return tt.getKeywordTokenType(1);}
"add" {return tt.getKeywordTokenType(2);}
"algorithm" {return tt.getKeywordTokenType(3);}
"all" {return tt.getKeywordTokenType(4);}
"alter" {return tt.getKeywordTokenType(5);}
"always" {return tt.getKeywordTokenType(6);}
"analyze" {return tt.getKeywordTokenType(7);}
"and" {return tt.getKeywordTokenType(8);}
"as" {return tt.getKeywordTokenType(9);}
"asc" {return tt.getKeywordTokenType(10);}
"asensitive" {return tt.getKeywordTokenType(11);}
"before" {return tt.getKeywordTokenType(12);}
"between" {return tt.getKeywordTokenType(13);}
"both" {return tt.getKeywordTokenType(14);}
"btree" {return tt.getKeywordTokenType(15);}
"by" {return tt.getKeywordTokenType(16);}
"call" {return tt.getKeywordTokenType(17);}
"cascade" {return tt.getKeywordTokenType(18);}
"cascaded" {return tt.getKeywordTokenType(19);}
"case" {return tt.getKeywordTokenType(20);}
"change" {return tt.getKeywordTokenType(21);}
"character" {return tt.getKeywordTokenType(22);}
"charset" {return tt.getKeywordTokenType(23);}
"check" {return tt.getKeywordTokenType(24);}
"checksum" {return tt.getKeywordTokenType(25);}
"close" {return tt.getKeywordTokenType(26);}
"collate" {return tt.getKeywordTokenType(27);}
"column" {return tt.getKeywordTokenType(28);}
"columns" {return tt.getKeywordTokenType(29);}
"comment" {return tt.getKeywordTokenType(30);}
"compact" {return tt.getKeywordTokenType(31);}
"compressed" {return tt.getKeywordTokenType(32);}
"compression" {return tt.getKeywordTokenType(33);}
"concurrent" {return tt.getKeywordTokenType(34);}
"condition" {return tt.getKeywordTokenType(35);}
"connection" {return tt.getKeywordTokenType(36);}
"constraint" {return tt.getKeywordTokenType(37);}
"continue" {return tt.getKeywordTokenType(38);}
"convert" {return tt.getKeywordTokenType(39);}
"create" {return tt.getKeywordTokenType(40);}
"cross" {return tt.getKeywordTokenType(41);}
"current_user" {return tt.getKeywordTokenType(42);}
"cursor" {return tt.getKeywordTokenType(43);}
"data" {return tt.getKeywordTokenType(44);}
"database" {return tt.getKeywordTokenType(45);}
"databases" {return tt.getKeywordTokenType(46);}
"declare" {return tt.getKeywordTokenType(47);}
"default" {return tt.getKeywordTokenType(48);}
"definer" {return tt.getKeywordTokenType(49);}
"delayed" {return tt.getKeywordTokenType(50);}
"delete" {return tt.getKeywordTokenType(51);}
"desc" {return tt.getKeywordTokenType(52);}
"describe" {return tt.getKeywordTokenType(53);}
"deterministic" {return tt.getKeywordTokenType(54);}
"directory" {return tt.getKeywordTokenType(55);}
"disk" {return tt.getKeywordTokenType(56);}
"distinct" {return tt.getKeywordTokenType(57);}
"distinctrow" {return tt.getKeywordTokenType(58);}
"div" {return tt.getKeywordTokenType(59);}
"do" {return tt.getKeywordTokenType(60);}
"drop" {return tt.getKeywordTokenType(61);}
"dual" {return tt.getKeywordTokenType(62);}
"dumpfile" {return tt.getKeywordTokenType(63);}
"duplicate" {return tt.getKeywordTokenType(64);}
"dynamic" {return tt.getKeywordTokenType(65);}
"each" {return tt.getKeywordTokenType(66);}
"else" {return tt.getKeywordTokenType(67);}
"elseif" {return tt.getKeywordTokenType(68);}
"enclosed" {return tt.getKeywordTokenType(69);}
"encryption" {return tt.getKeywordTokenType(70);}
"end" {return tt.getKeywordTokenType(71);}
"enforced" {return tt.getKeywordTokenType(72);}
"engine" {return tt.getKeywordTokenType(73);}
"escaped" {return tt.getKeywordTokenType(74);}
"exists" {return tt.getKeywordTokenType(75);}
"exit" {return tt.getKeywordTokenType(76);}
"expansion" {return tt.getKeywordTokenType(77);}
"explain" {return tt.getKeywordTokenType(78);}
"fetch" {return tt.getKeywordTokenType(79);}
"fields" {return tt.getKeywordTokenType(80);}
"first" {return tt.getKeywordTokenType(81);}
"fixed" {return tt.getKeywordTokenType(82);}
"float4" {return tt.getKeywordTokenType(83);}
"float8" {return tt.getKeywordTokenType(84);}
"for" {return tt.getKeywordTokenType(85);}
"force" {return tt.getKeywordTokenType(86);}
"foreign" {return tt.getKeywordTokenType(87);}
"from" {return tt.getKeywordTokenType(88);}
"full" {return tt.getKeywordTokenType(89);}
"fulltext" {return tt.getKeywordTokenType(90);}
"generated" {return tt.getKeywordTokenType(91);}
"grant" {return tt.getKeywordTokenType(92);}
"group" {return tt.getKeywordTokenType(93);}
"handler" {return tt.getKeywordTokenType(94);}
"hash" {return tt.getKeywordTokenType(95);}
"having" {return tt.getKeywordTokenType(96);}
"high_priority" {return tt.getKeywordTokenType(97);}
"if" {return tt.getKeywordTokenType(98);}
"ignore" {return tt.getKeywordTokenType(99);}
"in" {return tt.getKeywordTokenType(100);}
"index" {return tt.getKeywordTokenType(101);}
"infile" {return tt.getKeywordTokenType(102);}
"inner" {return tt.getKeywordTokenType(103);}
"inout" {return tt.getKeywordTokenType(104);}
"insensitive" {return tt.getKeywordTokenType(105);}
"insert" {return tt.getKeywordTokenType(106);}
"int1" {return tt.getKeywordTokenType(107);}
"int2" {return tt.getKeywordTokenType(108);}
"int3" {return tt.getKeywordTokenType(109);}
"int4" {return tt.getKeywordTokenType(110);}
"int8" {return tt.getKeywordTokenType(111);}
"interval" {return tt.getKeywordTokenType(112);}
"into" {return tt.getKeywordTokenType(113);}
"invisible" {return tt.getKeywordTokenType(114);}
"invoker" {return tt.getKeywordTokenType(115);}
"is" {return tt.getKeywordTokenType(116);}
"iterate" {return tt.getKeywordTokenType(117);}
"join" {return tt.getKeywordTokenType(118);}
"key" {return tt.getKeywordTokenType(119);}
"keys" {return tt.getKeywordTokenType(120);}
"kill" {return tt.getKeywordTokenType(121);}
"language" {return tt.getKeywordTokenType(122);}
"last" {return tt.getKeywordTokenType(123);}
"leading" {return tt.getKeywordTokenType(124);}
"leave" {return tt.getKeywordTokenType(125);}
"left" {return tt.getKeywordTokenType(126);}
"less" {return tt.getKeywordTokenType(127);}
"level" {return tt.getKeywordTokenType(128);}
"like" {return tt.getKeywordTokenType(129);}
"limit" {return tt.getKeywordTokenType(130);}
"linear" {return tt.getKeywordTokenType(131);}
"lines" {return tt.getKeywordTokenType(132);}
"list" {return tt.getKeywordTokenType(133);}
"load" {return tt.getKeywordTokenType(134);}
"local" {return tt.getKeywordTokenType(135);}
"lock" {return tt.getKeywordTokenType(136);}
"long" {return tt.getKeywordTokenType(137);}
"loop" {return tt.getKeywordTokenType(138);}
"match" {return tt.getKeywordTokenType(139);}
"maxvalue" {return tt.getKeywordTokenType(140);}
"memory" {return tt.getKeywordTokenType(141);}
"merge" {return tt.getKeywordTokenType(142);}
"microsecond" {return tt.getKeywordTokenType(143);}
"middleint" {return tt.getKeywordTokenType(144);}
"mod" {return tt.getKeywordTokenType(145);}
"mode" {return tt.getKeywordTokenType(146);}
"modifies" {return tt.getKeywordTokenType(147);}
"national" {return tt.getKeywordTokenType(148);}
"natural" {return tt.getKeywordTokenType(149);}
"next" {return tt.getKeywordTokenType(150);}
"no" {return tt.getKeywordTokenType(151);}
"not" {return tt.getKeywordTokenType(152);}
"null" {return tt.getKeywordTokenType(153);}
"offset" {return tt.getKeywordTokenType(154);}
"oj" {return tt.getKeywordTokenType(155);}
"on" {return tt.getKeywordTokenType(156);}
"open" {return tt.getKeywordTokenType(157);}
"optimize" {return tt.getKeywordTokenType(158);}
"option" {return tt.getKeywordTokenType(159);}
"optionally" {return tt.getKeywordTokenType(160);}
"or" {return tt.getKeywordTokenType(161);}
"order" {return tt.getKeywordTokenType(162);}
"out" {return tt.getKeywordTokenType(163);}
"outer" {return tt.getKeywordTokenType(164);}
"outfile" {return tt.getKeywordTokenType(165);}
"parser" {return tt.getKeywordTokenType(166);}
"partial" {return tt.getKeywordTokenType(167);}
"partition" {return tt.getKeywordTokenType(168);}
"partitions" {return tt.getKeywordTokenType(169);}
"password" {return tt.getKeywordTokenType(170);}
"precision" {return tt.getKeywordTokenType(171);}
"prev" {return tt.getKeywordTokenType(172);}
"primary" {return tt.getKeywordTokenType(173);}
"procedure" {return tt.getKeywordTokenType(174);}
"purge" {return tt.getKeywordTokenType(175);}
"query" {return tt.getKeywordTokenType(176);}
"quick" {return tt.getKeywordTokenType(177);}
"range" {return tt.getKeywordTokenType(178);}
"read" {return tt.getKeywordTokenType(179);}
"read_only" {return tt.getKeywordTokenType(180);}
"read_write" {return tt.getKeywordTokenType(181);}
"reads" {return tt.getKeywordTokenType(182);}
"redundant" {return tt.getKeywordTokenType(183);}
"references" {return tt.getKeywordTokenType(184);}
"regexp" {return tt.getKeywordTokenType(185);}
"release" {return tt.getKeywordTokenType(186);}
"rename" {return tt.getKeywordTokenType(187);}
"repeat" {return tt.getKeywordTokenType(188);}
"replace" {return tt.getKeywordTokenType(189);}
"require" {return tt.getKeywordTokenType(190);}
"restrict" {return tt.getKeywordTokenType(191);}
"return" {return tt.getKeywordTokenType(192);}
"reverse" {return tt.getKeywordTokenType(193);}
"revoke" {return tt.getKeywordTokenType(194);}
"right" {return tt.getKeywordTokenType(195);}
"rlike" {return tt.getKeywordTokenType(196);}
"rollback" {return tt.getKeywordTokenType(197);}
"rollup" {return tt.getKeywordTokenType(198);}
"schema" {return tt.getKeywordTokenType(199);}
"schemas" {return tt.getKeywordTokenType(200);}
"security" {return tt.getKeywordTokenType(201);}
"select" {return tt.getKeywordTokenType(202);}
"sensitive" {return tt.getKeywordTokenType(203);}
"separator" {return tt.getKeywordTokenType(204);}
"set" {return tt.getKeywordTokenType(205);}
"share" {return tt.getKeywordTokenType(206);}
"show" {return tt.getKeywordTokenType(207);}
"simple" {return tt.getKeywordTokenType(208);}
"spatial" {return tt.getKeywordTokenType(209);}
"specific" {return tt.getKeywordTokenType(210);}
"sql" {return tt.getKeywordTokenType(211);}
"sql_big_result" {return tt.getKeywordTokenType(212);}
"sql_buffer_result" {return tt.getKeywordTokenType(213);}
"sql_cache" {return tt.getKeywordTokenType(214);}
"sql_calc_found_rows" {return tt.getKeywordTokenType(215);}
"sql_no_cache" {return tt.getKeywordTokenType(216);}
"sql_small_result" {return tt.getKeywordTokenType(217);}
"sqlexception" {return tt.getKeywordTokenType(218);}
"sqlstate" {return tt.getKeywordTokenType(219);}
"sqlwarning" {return tt.getKeywordTokenType(220);}
"ssl" {return tt.getKeywordTokenType(221);}
"start" {return tt.getKeywordTokenType(222);}
"starting" {return tt.getKeywordTokenType(223);}
"storage" {return tt.getKeywordTokenType(224);}
"stored" {return tt.getKeywordTokenType(225);}
"straight_join" {return tt.getKeywordTokenType(226);}
"subpartition" {return tt.getKeywordTokenType(227);}
"table" {return tt.getKeywordTokenType(228);}
"tablespace" {return tt.getKeywordTokenType(229);}
"temporary" {return tt.getKeywordTokenType(230);}
"temptable" {return tt.getKeywordTokenType(231);}
"terminated" {return tt.getKeywordTokenType(232);}
"than" {return tt.getKeywordTokenType(233);}
"then" {return tt.getKeywordTokenType(234);}
"to" {return tt.getKeywordTokenType(235);}
"trailing" {return tt.getKeywordTokenType(236);}
"transaction" {return tt.getKeywordTokenType(237);}
"trigger" {return tt.getKeywordTokenType(238);}
"truncate" {return tt.getKeywordTokenType(239);}
"undefined" {return tt.getKeywordTokenType(240);}
"undo" {return tt.getKeywordTokenType(241);}
"union" {return tt.getKeywordTokenType(242);}
"unique" {return tt.getKeywordTokenType(243);}
"unlock" {return tt.getKeywordTokenType(244);}
"unsigned" {return tt.getKeywordTokenType(245);}
"update" {return tt.getKeywordTokenType(246);}
"usage" {return tt.getKeywordTokenType(247);}
"use" {return tt.getKeywordTokenType(248);}
"using" {return tt.getKeywordTokenType(249);}
"value" {return tt.getKeywordTokenType(250);}
"values" {return tt.getKeywordTokenType(251);}
"varcharacter" {return tt.getKeywordTokenType(252);}
"varying" {return tt.getKeywordTokenType(253);}
"view" {return tt.getKeywordTokenType(254);}
"virtual" {return tt.getKeywordTokenType(255);}
"visible" {return tt.getKeywordTokenType(256);}
"when" {return tt.getKeywordTokenType(257);}
"where" {return tt.getKeywordTokenType(258);}
"while" {return tt.getKeywordTokenType(259);}
"with" {return tt.getKeywordTokenType(260);}
"write" {return tt.getKeywordTokenType(261);}
"xor" {return tt.getKeywordTokenType(262);}
"zerofill" {return tt.getKeywordTokenType(263);}
"false" {return tt.getKeywordTokenType(264);}
"true" {return tt.getKeywordTokenType(265);}
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
"ceil" {return tt.getFunctionTokenType(18);}
"ceiling" {return tt.getFunctionTokenType(19);}
"char_length" {return tt.getFunctionTokenType(20);}
"character_length" {return tt.getFunctionTokenType(21);}
"coercibility" {return tt.getFunctionTokenType(22);}
"collation" {return tt.getFunctionTokenType(23);}
"compress" {return tt.getFunctionTokenType(24);}
"concat" {return tt.getFunctionTokenType(25);}
"concat_ws" {return tt.getFunctionTokenType(26);}
"connection_id" {return tt.getFunctionTokenType(27);}
"conv" {return tt.getFunctionTokenType(28);}
"convert_tz" {return tt.getFunctionTokenType(29);}
"cos" {return tt.getFunctionTokenType(30);}
"cot" {return tt.getFunctionTokenType(31);}
"count" {return tt.getFunctionTokenType(32);}
"crc32" {return tt.getFunctionTokenType(33);}
"curdate" {return tt.getFunctionTokenType(34);}
"current_date" {return tt.getFunctionTokenType(35);}
"current_time" {return tt.getFunctionTokenType(36);}
"current_timestamp" {return tt.getFunctionTokenType(37);}
"curtime" {return tt.getFunctionTokenType(38);}
"date_add" {return tt.getFunctionTokenType(39);}
"date_format" {return tt.getFunctionTokenType(40);}
"date_sub" {return tt.getFunctionTokenType(41);}
"datediff" {return tt.getFunctionTokenType(42);}
"day" {return tt.getFunctionTokenType(43);}
"day_hour" {return tt.getFunctionTokenType(44);}
"day_microsecond" {return tt.getFunctionTokenType(45);}
"day_minute" {return tt.getFunctionTokenType(46);}
"day_second" {return tt.getFunctionTokenType(47);}
"dayname" {return tt.getFunctionTokenType(48);}
"dayofmonth" {return tt.getFunctionTokenType(49);}
"dayofweek" {return tt.getFunctionTokenType(50);}
"dayofyear" {return tt.getFunctionTokenType(51);}
"decode" {return tt.getFunctionTokenType(52);}
"degrees" {return tt.getFunctionTokenType(53);}
"des_decrypt" {return tt.getFunctionTokenType(54);}
"des_encrypt" {return tt.getFunctionTokenType(55);}
"elt" {return tt.getFunctionTokenType(56);}
"encode" {return tt.getFunctionTokenType(57);}
"encrypt" {return tt.getFunctionTokenType(58);}
"exp" {return tt.getFunctionTokenType(59);}
"export_set" {return tt.getFunctionTokenType(60);}
"extract" {return tt.getFunctionTokenType(61);}
"field" {return tt.getFunctionTokenType(62);}
"find_in_set" {return tt.getFunctionTokenType(63);}
"floor" {return tt.getFunctionTokenType(64);}
"fn_second_microsecond" {return tt.getFunctionTokenType(65);}
"format" {return tt.getFunctionTokenType(66);}
"found_rows" {return tt.getFunctionTokenType(67);}
"from_days" {return tt.getFunctionTokenType(68);}
"from_unixtime" {return tt.getFunctionTokenType(69);}
"get_format" {return tt.getFunctionTokenType(70);}
"get_lock" {return tt.getFunctionTokenType(71);}
"group_concat" {return tt.getFunctionTokenType(72);}
"hex" {return tt.getFunctionTokenType(73);}
"hour" {return tt.getFunctionTokenType(74);}
"hour_microsecond" {return tt.getFunctionTokenType(75);}
"hour_minute" {return tt.getFunctionTokenType(76);}
"hour_second" {return tt.getFunctionTokenType(77);}
"ifnull" {return tt.getFunctionTokenType(78);}
"inet_aton" {return tt.getFunctionTokenType(79);}
"inet_ntoa" {return tt.getFunctionTokenType(80);}
"instr" {return tt.getFunctionTokenType(81);}
"is_free_lock" {return tt.getFunctionTokenType(82);}
"is_used_lock" {return tt.getFunctionTokenType(83);}
"last_day" {return tt.getFunctionTokenType(84);}
"last_insert_id" {return tt.getFunctionTokenType(85);}
"lcase" {return tt.getFunctionTokenType(86);}
"length" {return tt.getFunctionTokenType(87);}
"ln" {return tt.getFunctionTokenType(88);}
"load_file" {return tt.getFunctionTokenType(89);}
"localtime" {return tt.getFunctionTokenType(90);}
"localtimestamp" {return tt.getFunctionTokenType(91);}
"locate" {return tt.getFunctionTokenType(92);}
"log" {return tt.getFunctionTokenType(93);}
"log10" {return tt.getFunctionTokenType(94);}
"log2" {return tt.getFunctionTokenType(95);}
"lower" {return tt.getFunctionTokenType(96);}
"lpad" {return tt.getFunctionTokenType(97);}
"ltrim" {return tt.getFunctionTokenType(98);}
"make_set" {return tt.getFunctionTokenType(99);}
"makedate" {return tt.getFunctionTokenType(100);}
"maketime" {return tt.getFunctionTokenType(101);}
"master_pos_wait" {return tt.getFunctionTokenType(102);}
"max" {return tt.getFunctionTokenType(103);}
"md5" {return tt.getFunctionTokenType(104);}
"mid" {return tt.getFunctionTokenType(105);}
"min" {return tt.getFunctionTokenType(106);}
"minute" {return tt.getFunctionTokenType(107);}
"minute_microsecond" {return tt.getFunctionTokenType(108);}
"minute_second" {return tt.getFunctionTokenType(109);}
"month" {return tt.getFunctionTokenType(110);}
"monthname" {return tt.getFunctionTokenType(111);}
"name_const" {return tt.getFunctionTokenType(112);}
"not_like" {return tt.getFunctionTokenType(113);}
"not_regexp" {return tt.getFunctionTokenType(114);}
"now" {return tt.getFunctionTokenType(115);}
"nullif" {return tt.getFunctionTokenType(116);}
"oct" {return tt.getFunctionTokenType(117);}
"octet_length" {return tt.getFunctionTokenType(118);}
"old_password" {return tt.getFunctionTokenType(119);}
"ord" {return tt.getFunctionTokenType(120);}
"period_add" {return tt.getFunctionTokenType(121);}
"period_diff" {return tt.getFunctionTokenType(122);}
"pi" {return tt.getFunctionTokenType(123);}
"position" {return tt.getFunctionTokenType(124);}
"pow" {return tt.getFunctionTokenType(125);}
"power" {return tt.getFunctionTokenType(126);}
"quarter" {return tt.getFunctionTokenType(127);}
"quote" {return tt.getFunctionTokenType(128);}
"radians" {return tt.getFunctionTokenType(129);}
"rand" {return tt.getFunctionTokenType(130);}
"release_lock" {return tt.getFunctionTokenType(131);}
"round" {return tt.getFunctionTokenType(132);}
"row_count" {return tt.getFunctionTokenType(133);}
"rpad" {return tt.getFunctionTokenType(134);}
"rtrim" {return tt.getFunctionTokenType(135);}
"sec_to_time" {return tt.getFunctionTokenType(136);}
"second" {return tt.getFunctionTokenType(137);}
"second_microsecond" {return tt.getFunctionTokenType(138);}
"session_user" {return tt.getFunctionTokenType(139);}
"sha" {return tt.getFunctionTokenType(140);}
"sha1" {return tt.getFunctionTokenType(141);}
"sha2" {return tt.getFunctionTokenType(142);}
"sign" {return tt.getFunctionTokenType(143);}
"sin" {return tt.getFunctionTokenType(144);}
"sleep" {return tt.getFunctionTokenType(145);}
"soundex" {return tt.getFunctionTokenType(146);}
"sounds_like" {return tt.getFunctionTokenType(147);}
"space" {return tt.getFunctionTokenType(148);}
"sqrt" {return tt.getFunctionTokenType(149);}
"std" {return tt.getFunctionTokenType(150);}
"stddev" {return tt.getFunctionTokenType(151);}
"stddev_pop" {return tt.getFunctionTokenType(152);}
"stddev_samp" {return tt.getFunctionTokenType(153);}
"str_to_date" {return tt.getFunctionTokenType(154);}
"strcmp" {return tt.getFunctionTokenType(155);}
"subdate" {return tt.getFunctionTokenType(156);}
"substr" {return tt.getFunctionTokenType(157);}
"substring" {return tt.getFunctionTokenType(158);}
"substring_index" {return tt.getFunctionTokenType(159);}
"subtime" {return tt.getFunctionTokenType(160);}
"sum" {return tt.getFunctionTokenType(161);}
"sysdate" {return tt.getFunctionTokenType(162);}
"system_user" {return tt.getFunctionTokenType(163);}
"tan" {return tt.getFunctionTokenType(164);}
"time_format" {return tt.getFunctionTokenType(165);}
"time_to_sec" {return tt.getFunctionTokenType(166);}
"timediff" {return tt.getFunctionTokenType(167);}
"timestampadd" {return tt.getFunctionTokenType(168);}
"timestampdiff" {return tt.getFunctionTokenType(169);}
"to_days" {return tt.getFunctionTokenType(170);}
"trim" {return tt.getFunctionTokenType(171);}
"ucase" {return tt.getFunctionTokenType(172);}
"uncompress" {return tt.getFunctionTokenType(173);}
"uncompressed_length" {return tt.getFunctionTokenType(174);}
"unhex" {return tt.getFunctionTokenType(175);}
"unix_timestamp" {return tt.getFunctionTokenType(176);}
"upper" {return tt.getFunctionTokenType(177);}
"user" {return tt.getFunctionTokenType(178);}
"utc_date" {return tt.getFunctionTokenType(179);}
"utc_time" {return tt.getFunctionTokenType(180);}
"utc_timestamp" {return tt.getFunctionTokenType(181);}
"uuid" {return tt.getFunctionTokenType(182);}
"uuid_short" {return tt.getFunctionTokenType(183);}
"var_pop" {return tt.getFunctionTokenType(184);}
"var_samp" {return tt.getFunctionTokenType(185);}
"variance" {return tt.getFunctionTokenType(186);}
"version" {return tt.getFunctionTokenType(187);}
"week" {return tt.getFunctionTokenType(188);}
"weekday" {return tt.getFunctionTokenType(189);}
"weekofyear" {return tt.getFunctionTokenType(190);}
"weight_string" {return tt.getFunctionTokenType(191);}
"year_month" {return tt.getFunctionTokenType(192);}
"yearweek" {return tt.getFunctionTokenType(193);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
"autoextend_size" {return tt.getParameterTokenType(0);}
"auto_increment" {return tt.getParameterTokenType(1);}
"avg_row_length" {return tt.getParameterTokenType(2);}
"column_format" {return tt.getParameterTokenType(3);}
"delay_key_write" {return tt.getParameterTokenType(4);}
"engine_attribute" {return tt.getParameterTokenType(5);}
"insert_method" {return tt.getParameterTokenType(6);}
"key_block_size" {return tt.getParameterTokenType(7);}
"low_ignore" {return tt.getParameterTokenType(8);}
"low_priority" {return tt.getParameterTokenType(9);}
"master_ssl_verify_server_cert" {return tt.getParameterTokenType(10);}
"max_rows" {return tt.getParameterTokenType(11);}
"min_rows" {return tt.getParameterTokenType(12);}
"no_write_to_binlog" {return tt.getParameterTokenType(13);}
"pack_keys" {return tt.getParameterTokenType(14);}
"row_format" {return tt.getParameterTokenType(15);}
"secondary_engine_attribute" {return tt.getParameterTokenType(16);}
"stats_auto_recalc" {return tt.getParameterTokenType(17);}
"stats_persistent" {return tt.getParameterTokenType(18);}
"stats_sample_pages" {return tt.getParameterTokenType(19);}
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}    { return stt.getQuotedIdentifier(); }
.                      { return stt.getIdentifier(); }
