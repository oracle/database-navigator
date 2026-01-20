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





"bigint" {return tt.getDataTypeTokenType(0);}
"binary" {return tt.getDataTypeTokenType(1);}
"bit" {return tt.getDataTypeTokenType(2);}
"blob" {return tt.getDataTypeTokenType(3);}
"bool" {return tt.getDataTypeTokenType(4);}
"boolean" {return tt.getDataTypeTokenType(5);}
"date" {return tt.getDataTypeTokenType(6);}
"datetime" {return tt.getDataTypeTokenType(7);}
"dec" {return tt.getDataTypeTokenType(8);}
"decimal" {return tt.getDataTypeTokenType(9);}
"double" {return tt.getDataTypeTokenType(10);}
"double_precision" {return tt.getDataTypeTokenType(11);}
"enum" {return tt.getDataTypeTokenType(12);}
"float" {return tt.getDataTypeTokenType(13);}
"int" {return tt.getDataTypeTokenType(14);}
"integer" {return tt.getDataTypeTokenType(15);}
"longblob" {return tt.getDataTypeTokenType(16);}
"longtext" {return tt.getDataTypeTokenType(17);}
"mediumblob" {return tt.getDataTypeTokenType(18);}
"mediumint" {return tt.getDataTypeTokenType(19);}
"mediumtext" {return tt.getDataTypeTokenType(20);}
"national_varchar" {return tt.getDataTypeTokenType(21);}
"numeric" {return tt.getDataTypeTokenType(22);}
"real" {return tt.getDataTypeTokenType(23);}
"smallint" {return tt.getDataTypeTokenType(24);}
"text" {return tt.getDataTypeTokenType(25);}
"time" {return tt.getDataTypeTokenType(26);}
"timestamp" {return tt.getDataTypeTokenType(27);}
"tinyblob" {return tt.getDataTypeTokenType(28);}
"tinyint" {return tt.getDataTypeTokenType(29);}
"tinytext" {return tt.getDataTypeTokenType(30);}
"varbinary" {return tt.getDataTypeTokenType(31);}
"varchar" {return tt.getDataTypeTokenType(32);}
"year" {return tt.getDataTypeTokenType(33);}



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
"check" {return tt.getKeywordTokenType(23);}
"checksum" {return tt.getKeywordTokenType(24);}
"close" {return tt.getKeywordTokenType(25);}
"collate" {return tt.getKeywordTokenType(26);}
"column" {return tt.getKeywordTokenType(27);}
"columns" {return tt.getKeywordTokenType(28);}
"comment" {return tt.getKeywordTokenType(29);}
"compact" {return tt.getKeywordTokenType(30);}
"compressed" {return tt.getKeywordTokenType(31);}
"compression" {return tt.getKeywordTokenType(32);}
"concurrent" {return tt.getKeywordTokenType(33);}
"condition" {return tt.getKeywordTokenType(34);}
"connection" {return tt.getKeywordTokenType(35);}
"constraint" {return tt.getKeywordTokenType(36);}
"continue" {return tt.getKeywordTokenType(37);}
"convert" {return tt.getKeywordTokenType(38);}
"create" {return tt.getKeywordTokenType(39);}
"cross" {return tt.getKeywordTokenType(40);}
"current_user" {return tt.getKeywordTokenType(41);}
"cursor" {return tt.getKeywordTokenType(42);}
"data" {return tt.getKeywordTokenType(43);}
"database" {return tt.getKeywordTokenType(44);}
"databases" {return tt.getKeywordTokenType(45);}
"declare" {return tt.getKeywordTokenType(46);}
"default" {return tt.getKeywordTokenType(47);}
"definer" {return tt.getKeywordTokenType(48);}
"delayed" {return tt.getKeywordTokenType(49);}
"delete" {return tt.getKeywordTokenType(50);}
"desc" {return tt.getKeywordTokenType(51);}
"describe" {return tt.getKeywordTokenType(52);}
"deterministic" {return tt.getKeywordTokenType(53);}
"directory" {return tt.getKeywordTokenType(54);}
"disk" {return tt.getKeywordTokenType(55);}
"distinct" {return tt.getKeywordTokenType(56);}
"distinctrow" {return tt.getKeywordTokenType(57);}
"div" {return tt.getKeywordTokenType(58);}
"do" {return tt.getKeywordTokenType(59);}
"drop" {return tt.getKeywordTokenType(60);}
"dual" {return tt.getKeywordTokenType(61);}
"dumpfile" {return tt.getKeywordTokenType(62);}
"duplicate" {return tt.getKeywordTokenType(63);}
"dynamic" {return tt.getKeywordTokenType(64);}
"each" {return tt.getKeywordTokenType(65);}
"else" {return tt.getKeywordTokenType(66);}
"elseif" {return tt.getKeywordTokenType(67);}
"enclosed" {return tt.getKeywordTokenType(68);}
"encryption" {return tt.getKeywordTokenType(69);}
"end" {return tt.getKeywordTokenType(70);}
"enforced" {return tt.getKeywordTokenType(71);}
"engine" {return tt.getKeywordTokenType(72);}
"escaped" {return tt.getKeywordTokenType(73);}
"exists" {return tt.getKeywordTokenType(74);}
"exit" {return tt.getKeywordTokenType(75);}
"expansion" {return tt.getKeywordTokenType(76);}
"explain" {return tt.getKeywordTokenType(77);}
"fetch" {return tt.getKeywordTokenType(78);}
"fields" {return tt.getKeywordTokenType(79);}
"first" {return tt.getKeywordTokenType(80);}
"fixed" {return tt.getKeywordTokenType(81);}
"float4" {return tt.getKeywordTokenType(82);}
"float8" {return tt.getKeywordTokenType(83);}
"for" {return tt.getKeywordTokenType(84);}
"force" {return tt.getKeywordTokenType(85);}
"foreign" {return tt.getKeywordTokenType(86);}
"from" {return tt.getKeywordTokenType(87);}
"full" {return tt.getKeywordTokenType(88);}
"fulltext" {return tt.getKeywordTokenType(89);}
"generated" {return tt.getKeywordTokenType(90);}
"grant" {return tt.getKeywordTokenType(91);}
"group" {return tt.getKeywordTokenType(92);}
"handler" {return tt.getKeywordTokenType(93);}
"hash" {return tt.getKeywordTokenType(94);}
"having" {return tt.getKeywordTokenType(95);}
"high_priority" {return tt.getKeywordTokenType(96);}
"if" {return tt.getKeywordTokenType(97);}
"ignore" {return tt.getKeywordTokenType(98);}
"in" {return tt.getKeywordTokenType(99);}
"index" {return tt.getKeywordTokenType(100);}
"infile" {return tt.getKeywordTokenType(101);}
"inner" {return tt.getKeywordTokenType(102);}
"inout" {return tt.getKeywordTokenType(103);}
"insensitive" {return tt.getKeywordTokenType(104);}
"insert" {return tt.getKeywordTokenType(105);}
"int1" {return tt.getKeywordTokenType(106);}
"int2" {return tt.getKeywordTokenType(107);}
"int3" {return tt.getKeywordTokenType(108);}
"int4" {return tt.getKeywordTokenType(109);}
"int8" {return tt.getKeywordTokenType(110);}
"interval" {return tt.getKeywordTokenType(111);}
"into" {return tt.getKeywordTokenType(112);}
"invisible" {return tt.getKeywordTokenType(113);}
"invoker" {return tt.getKeywordTokenType(114);}
"is" {return tt.getKeywordTokenType(115);}
"iterate" {return tt.getKeywordTokenType(116);}
"join" {return tt.getKeywordTokenType(117);}
"key" {return tt.getKeywordTokenType(118);}
"keys" {return tt.getKeywordTokenType(119);}
"kill" {return tt.getKeywordTokenType(120);}
"language" {return tt.getKeywordTokenType(121);}
"last" {return tt.getKeywordTokenType(122);}
"leading" {return tt.getKeywordTokenType(123);}
"leave" {return tt.getKeywordTokenType(124);}
"left" {return tt.getKeywordTokenType(125);}
"less" {return tt.getKeywordTokenType(126);}
"level" {return tt.getKeywordTokenType(127);}
"like" {return tt.getKeywordTokenType(128);}
"limit" {return tt.getKeywordTokenType(129);}
"linear" {return tt.getKeywordTokenType(130);}
"lines" {return tt.getKeywordTokenType(131);}
"list" {return tt.getKeywordTokenType(132);}
"load" {return tt.getKeywordTokenType(133);}
"local" {return tt.getKeywordTokenType(134);}
"lock" {return tt.getKeywordTokenType(135);}
"long" {return tt.getKeywordTokenType(136);}
"loop" {return tt.getKeywordTokenType(137);}
"match" {return tt.getKeywordTokenType(138);}
"maxvalue" {return tt.getKeywordTokenType(139);}
"memory" {return tt.getKeywordTokenType(140);}
"merge" {return tt.getKeywordTokenType(141);}
"microsecond" {return tt.getKeywordTokenType(142);}
"middleint" {return tt.getKeywordTokenType(143);}
"mod" {return tt.getKeywordTokenType(144);}
"mode" {return tt.getKeywordTokenType(145);}
"modifies" {return tt.getKeywordTokenType(146);}
"natural" {return tt.getKeywordTokenType(147);}
"next" {return tt.getKeywordTokenType(148);}
"no" {return tt.getKeywordTokenType(149);}
"not" {return tt.getKeywordTokenType(150);}
"null" {return tt.getKeywordTokenType(151);}
"offset" {return tt.getKeywordTokenType(152);}
"oj" {return tt.getKeywordTokenType(153);}
"on" {return tt.getKeywordTokenType(154);}
"open" {return tt.getKeywordTokenType(155);}
"optimize" {return tt.getKeywordTokenType(156);}
"option" {return tt.getKeywordTokenType(157);}
"optionally" {return tt.getKeywordTokenType(158);}
"or" {return tt.getKeywordTokenType(159);}
"order" {return tt.getKeywordTokenType(160);}
"out" {return tt.getKeywordTokenType(161);}
"outer" {return tt.getKeywordTokenType(162);}
"outfile" {return tt.getKeywordTokenType(163);}
"parser" {return tt.getKeywordTokenType(164);}
"partial" {return tt.getKeywordTokenType(165);}
"partition" {return tt.getKeywordTokenType(166);}
"partitions" {return tt.getKeywordTokenType(167);}
"password" {return tt.getKeywordTokenType(168);}
"precision" {return tt.getKeywordTokenType(169);}
"prev" {return tt.getKeywordTokenType(170);}
"primary" {return tt.getKeywordTokenType(171);}
"procedure" {return tt.getKeywordTokenType(172);}
"purge" {return tt.getKeywordTokenType(173);}
"query" {return tt.getKeywordTokenType(174);}
"quick" {return tt.getKeywordTokenType(175);}
"range" {return tt.getKeywordTokenType(176);}
"read" {return tt.getKeywordTokenType(177);}
"read_only" {return tt.getKeywordTokenType(178);}
"read_write" {return tt.getKeywordTokenType(179);}
"reads" {return tt.getKeywordTokenType(180);}
"redundant" {return tt.getKeywordTokenType(181);}
"references" {return tt.getKeywordTokenType(182);}
"regexp" {return tt.getKeywordTokenType(183);}
"release" {return tt.getKeywordTokenType(184);}
"rename" {return tt.getKeywordTokenType(185);}
"repeat" {return tt.getKeywordTokenType(186);}
"replace" {return tt.getKeywordTokenType(187);}
"require" {return tt.getKeywordTokenType(188);}
"restrict" {return tt.getKeywordTokenType(189);}
"return" {return tt.getKeywordTokenType(190);}
"reverse" {return tt.getKeywordTokenType(191);}
"revoke" {return tt.getKeywordTokenType(192);}
"right" {return tt.getKeywordTokenType(193);}
"rlike" {return tt.getKeywordTokenType(194);}
"rollback" {return tt.getKeywordTokenType(195);}
"rollup" {return tt.getKeywordTokenType(196);}
"schema" {return tt.getKeywordTokenType(197);}
"schemas" {return tt.getKeywordTokenType(198);}
"security" {return tt.getKeywordTokenType(199);}
"select" {return tt.getKeywordTokenType(200);}
"sensitive" {return tt.getKeywordTokenType(201);}
"separator" {return tt.getKeywordTokenType(202);}
"set" {return tt.getKeywordTokenType(203);}
"share" {return tt.getKeywordTokenType(204);}
"show" {return tt.getKeywordTokenType(205);}
"simple" {return tt.getKeywordTokenType(206);}
"spatial" {return tt.getKeywordTokenType(207);}
"specific" {return tt.getKeywordTokenType(208);}
"sql" {return tt.getKeywordTokenType(209);}
"sql_big_result" {return tt.getKeywordTokenType(210);}
"sql_buffer_result" {return tt.getKeywordTokenType(211);}
"sql_cache" {return tt.getKeywordTokenType(212);}
"sql_calc_found_rows" {return tt.getKeywordTokenType(213);}
"sql_no_cache" {return tt.getKeywordTokenType(214);}
"sql_small_result" {return tt.getKeywordTokenType(215);}
"sqlexception" {return tt.getKeywordTokenType(216);}
"sqlstate" {return tt.getKeywordTokenType(217);}
"sqlwarning" {return tt.getKeywordTokenType(218);}
"ssl" {return tt.getKeywordTokenType(219);}
"start" {return tt.getKeywordTokenType(220);}
"starting" {return tt.getKeywordTokenType(221);}
"storage" {return tt.getKeywordTokenType(222);}
"stored" {return tt.getKeywordTokenType(223);}
"straight_join" {return tt.getKeywordTokenType(224);}
"subpartition" {return tt.getKeywordTokenType(225);}
"table" {return tt.getKeywordTokenType(226);}
"tablespace" {return tt.getKeywordTokenType(227);}
"temporary" {return tt.getKeywordTokenType(228);}
"temptable" {return tt.getKeywordTokenType(229);}
"terminated" {return tt.getKeywordTokenType(230);}
"than" {return tt.getKeywordTokenType(231);}
"then" {return tt.getKeywordTokenType(232);}
"to" {return tt.getKeywordTokenType(233);}
"trailing" {return tt.getKeywordTokenType(234);}
"transaction" {return tt.getKeywordTokenType(235);}
"trigger" {return tt.getKeywordTokenType(236);}
"truncate" {return tt.getKeywordTokenType(237);}
"undefined" {return tt.getKeywordTokenType(238);}
"undo" {return tt.getKeywordTokenType(239);}
"union" {return tt.getKeywordTokenType(240);}
"unique" {return tt.getKeywordTokenType(241);}
"unlock" {return tt.getKeywordTokenType(242);}
"unsigned" {return tt.getKeywordTokenType(243);}
"update" {return tt.getKeywordTokenType(244);}
"usage" {return tt.getKeywordTokenType(245);}
"use" {return tt.getKeywordTokenType(246);}
"using" {return tt.getKeywordTokenType(247);}
"value" {return tt.getKeywordTokenType(248);}
"values" {return tt.getKeywordTokenType(249);}
"varcharacter" {return tt.getKeywordTokenType(250);}
"varying" {return tt.getKeywordTokenType(251);}
"view" {return tt.getKeywordTokenType(252);}
"visible" {return tt.getKeywordTokenType(253);}
"when" {return tt.getKeywordTokenType(254);}
"where" {return tt.getKeywordTokenType(255);}
"while" {return tt.getKeywordTokenType(256);}
"with" {return tt.getKeywordTokenType(257);}
"write" {return tt.getKeywordTokenType(258);}
"xor" {return tt.getKeywordTokenType(259);}
"zerofill" {return tt.getKeywordTokenType(260);}
"false" {return tt.getKeywordTokenType(261);}
"true" {return tt.getKeywordTokenType(262);}





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
"char" {return tt.getFunctionTokenType(20);}
"char_length" {return tt.getFunctionTokenType(21);}
"character_length" {return tt.getFunctionTokenType(22);}
"charset" {return tt.getFunctionTokenType(23);}
"coercibility" {return tt.getFunctionTokenType(24);}
"collation" {return tt.getFunctionTokenType(25);}
"compress" {return tt.getFunctionTokenType(26);}
"concat" {return tt.getFunctionTokenType(27);}
"concat_ws" {return tt.getFunctionTokenType(28);}
"connection_id" {return tt.getFunctionTokenType(29);}
"conv" {return tt.getFunctionTokenType(30);}
"convert_tz" {return tt.getFunctionTokenType(31);}
"cos" {return tt.getFunctionTokenType(32);}
"cot" {return tt.getFunctionTokenType(33);}
"count" {return tt.getFunctionTokenType(34);}
"crc32" {return tt.getFunctionTokenType(35);}
"curdate" {return tt.getFunctionTokenType(36);}
"current_date" {return tt.getFunctionTokenType(37);}
"current_time" {return tt.getFunctionTokenType(38);}
"current_timestamp" {return tt.getFunctionTokenType(39);}
"curtime" {return tt.getFunctionTokenType(40);}
"date_add" {return tt.getFunctionTokenType(41);}
"date_format" {return tt.getFunctionTokenType(42);}
"date_sub" {return tt.getFunctionTokenType(43);}
"datediff" {return tt.getFunctionTokenType(44);}
"day" {return tt.getFunctionTokenType(45);}
"day_hour" {return tt.getFunctionTokenType(46);}
"day_microsecond" {return tt.getFunctionTokenType(47);}
"day_minute" {return tt.getFunctionTokenType(48);}
"day_second" {return tt.getFunctionTokenType(49);}
"dayname" {return tt.getFunctionTokenType(50);}
"dayofmonth" {return tt.getFunctionTokenType(51);}
"dayofweek" {return tt.getFunctionTokenType(52);}
"dayofyear" {return tt.getFunctionTokenType(53);}
"decode" {return tt.getFunctionTokenType(54);}
"degrees" {return tt.getFunctionTokenType(55);}
"des_decrypt" {return tt.getFunctionTokenType(56);}
"des_encrypt" {return tt.getFunctionTokenType(57);}
"elt" {return tt.getFunctionTokenType(58);}
"encode" {return tt.getFunctionTokenType(59);}
"encrypt" {return tt.getFunctionTokenType(60);}
"exp" {return tt.getFunctionTokenType(61);}
"export_set" {return tt.getFunctionTokenType(62);}
"extract" {return tt.getFunctionTokenType(63);}
"field" {return tt.getFunctionTokenType(64);}
"find_in_set" {return tt.getFunctionTokenType(65);}
"floor" {return tt.getFunctionTokenType(66);}
"fn_second_microsecond" {return tt.getFunctionTokenType(67);}
"format" {return tt.getFunctionTokenType(68);}
"found_rows" {return tt.getFunctionTokenType(69);}
"from_days" {return tt.getFunctionTokenType(70);}
"from_unixtime" {return tt.getFunctionTokenType(71);}
"get_format" {return tt.getFunctionTokenType(72);}
"get_lock" {return tt.getFunctionTokenType(73);}
"group_concat" {return tt.getFunctionTokenType(74);}
"hex" {return tt.getFunctionTokenType(75);}
"hour" {return tt.getFunctionTokenType(76);}
"hour_microsecond" {return tt.getFunctionTokenType(77);}
"hour_minute" {return tt.getFunctionTokenType(78);}
"hour_second" {return tt.getFunctionTokenType(79);}
"ifnull" {return tt.getFunctionTokenType(80);}
"inet_aton" {return tt.getFunctionTokenType(81);}
"inet_ntoa" {return tt.getFunctionTokenType(82);}
"instr" {return tt.getFunctionTokenType(83);}
"is_free_lock" {return tt.getFunctionTokenType(84);}
"is_used_lock" {return tt.getFunctionTokenType(85);}
"last_day" {return tt.getFunctionTokenType(86);}
"last_insert_id" {return tt.getFunctionTokenType(87);}
"lcase" {return tt.getFunctionTokenType(88);}
"length" {return tt.getFunctionTokenType(89);}
"ln" {return tt.getFunctionTokenType(90);}
"load_file" {return tt.getFunctionTokenType(91);}
"localtime" {return tt.getFunctionTokenType(92);}
"localtimestamp" {return tt.getFunctionTokenType(93);}
"locate" {return tt.getFunctionTokenType(94);}
"log" {return tt.getFunctionTokenType(95);}
"log10" {return tt.getFunctionTokenType(96);}
"log2" {return tt.getFunctionTokenType(97);}
"lower" {return tt.getFunctionTokenType(98);}
"lpad" {return tt.getFunctionTokenType(99);}
"ltrim" {return tt.getFunctionTokenType(100);}
"make_set" {return tt.getFunctionTokenType(101);}
"makedate" {return tt.getFunctionTokenType(102);}
"maketime" {return tt.getFunctionTokenType(103);}
"master_pos_wait" {return tt.getFunctionTokenType(104);}
"max" {return tt.getFunctionTokenType(105);}
"md5" {return tt.getFunctionTokenType(106);}
"mid" {return tt.getFunctionTokenType(107);}
"min" {return tt.getFunctionTokenType(108);}
"minute" {return tt.getFunctionTokenType(109);}
"minute_microsecond" {return tt.getFunctionTokenType(110);}
"minute_second" {return tt.getFunctionTokenType(111);}
"month" {return tt.getFunctionTokenType(112);}
"monthname" {return tt.getFunctionTokenType(113);}
"name_const" {return tt.getFunctionTokenType(114);}
"not_like" {return tt.getFunctionTokenType(115);}
"not_regexp" {return tt.getFunctionTokenType(116);}
"now" {return tt.getFunctionTokenType(117);}
"nullif" {return tt.getFunctionTokenType(118);}
"oct" {return tt.getFunctionTokenType(119);}
"octet_length" {return tt.getFunctionTokenType(120);}
"old_password" {return tt.getFunctionTokenType(121);}
"ord" {return tt.getFunctionTokenType(122);}
"period_add" {return tt.getFunctionTokenType(123);}
"period_diff" {return tt.getFunctionTokenType(124);}
"pi" {return tt.getFunctionTokenType(125);}
"position" {return tt.getFunctionTokenType(126);}
"pow" {return tt.getFunctionTokenType(127);}
"power" {return tt.getFunctionTokenType(128);}
"quarter" {return tt.getFunctionTokenType(129);}
"quote" {return tt.getFunctionTokenType(130);}
"radians" {return tt.getFunctionTokenType(131);}
"rand" {return tt.getFunctionTokenType(132);}
"release_lock" {return tt.getFunctionTokenType(133);}
"round" {return tt.getFunctionTokenType(134);}
"row_count" {return tt.getFunctionTokenType(135);}
"rpad" {return tt.getFunctionTokenType(136);}
"rtrim" {return tt.getFunctionTokenType(137);}
"sec_to_time" {return tt.getFunctionTokenType(138);}
"second" {return tt.getFunctionTokenType(139);}
"second_microsecond" {return tt.getFunctionTokenType(140);}
"session_user" {return tt.getFunctionTokenType(141);}
"sha" {return tt.getFunctionTokenType(142);}
"sha1" {return tt.getFunctionTokenType(143);}
"sha2" {return tt.getFunctionTokenType(144);}
"sign" {return tt.getFunctionTokenType(145);}
"sin" {return tt.getFunctionTokenType(146);}
"sleep" {return tt.getFunctionTokenType(147);}
"soundex" {return tt.getFunctionTokenType(148);}
"sounds_like" {return tt.getFunctionTokenType(149);}
"space" {return tt.getFunctionTokenType(150);}
"sqrt" {return tt.getFunctionTokenType(151);}
"std" {return tt.getFunctionTokenType(152);}
"stddev" {return tt.getFunctionTokenType(153);}
"stddev_pop" {return tt.getFunctionTokenType(154);}
"stddev_samp" {return tt.getFunctionTokenType(155);}
"str_to_date" {return tt.getFunctionTokenType(156);}
"strcmp" {return tt.getFunctionTokenType(157);}
"subdate" {return tt.getFunctionTokenType(158);}
"substr" {return tt.getFunctionTokenType(159);}
"substring" {return tt.getFunctionTokenType(160);}
"substring_index" {return tt.getFunctionTokenType(161);}
"subtime" {return tt.getFunctionTokenType(162);}
"sum" {return tt.getFunctionTokenType(163);}
"sysdate" {return tt.getFunctionTokenType(164);}
"system_user" {return tt.getFunctionTokenType(165);}
"tan" {return tt.getFunctionTokenType(166);}
"time_format" {return tt.getFunctionTokenType(167);}
"time_to_sec" {return tt.getFunctionTokenType(168);}
"timediff" {return tt.getFunctionTokenType(169);}
"timestampadd" {return tt.getFunctionTokenType(170);}
"timestampdiff" {return tt.getFunctionTokenType(171);}
"to_days" {return tt.getFunctionTokenType(172);}
"trim" {return tt.getFunctionTokenType(173);}
"ucase" {return tt.getFunctionTokenType(174);}
"uncompress" {return tt.getFunctionTokenType(175);}
"uncompressed_length" {return tt.getFunctionTokenType(176);}
"unhex" {return tt.getFunctionTokenType(177);}
"unix_timestamp" {return tt.getFunctionTokenType(178);}
"upper" {return tt.getFunctionTokenType(179);}
"user" {return tt.getFunctionTokenType(180);}
"utc_date" {return tt.getFunctionTokenType(181);}
"utc_time" {return tt.getFunctionTokenType(182);}
"utc_timestamp" {return tt.getFunctionTokenType(183);}
"uuid" {return tt.getFunctionTokenType(184);}
"uuid_short" {return tt.getFunctionTokenType(185);}
"var_pop" {return tt.getFunctionTokenType(186);}
"var_samp" {return tt.getFunctionTokenType(187);}
"variance" {return tt.getFunctionTokenType(188);}
"version" {return tt.getFunctionTokenType(189);}
"week" {return tt.getFunctionTokenType(190);}
"weekday" {return tt.getFunctionTokenType(191);}
"weekofyear" {return tt.getFunctionTokenType(192);}
"weight_string" {return tt.getFunctionTokenType(193);}
"year_month" {return tt.getFunctionTokenType(194);}
"yearweek" {return tt.getFunctionTokenType(195);}


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
"secondary_engine_attribut" {return tt.getParameterTokenType(16);}
"stats_auto_recalc" {return tt.getParameterTokenType(17);}
"stats_persistent" {return tt.getParameterTokenType(18);}
"stats_sample_pages" {return tt.getParameterTokenType(19);}


{IDENTIFIER}           { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}    { return stt.getQuotedIdentifier(); }
.                      { return stt.getIdentifier(); }
