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

{WHITE_SPACE}+   { return stt.whiteSpace; }

{BLOCK_COMMENT}  { return stt.blockComment; }
{LINE_COMMENT}   { return stt.lineComment; }

{VARIABLE}       { return stt.variable; }
{INTEGER}        { return stt.integer; }
{NUMBER}         { return stt.number; }
{STRING}         { return stt.string; }

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
"bigint" {return tt.dtt(0);}
"binary" {return tt.dtt(1);}
"bit" {return tt.dtt(2);}
"blob" {return tt.dtt(3);}
"bool" {return tt.dtt(4);}
"boolean" {return tt.dtt(5);}
"char" {return tt.dtt(6);}
"date" {return tt.dtt(7);}
"datetime" {return tt.dtt(8);}
"dec" {return tt.dtt(9);}
"decimal" {return tt.dtt(10);}
"double" {return tt.dtt(11);}
"enum" {return tt.dtt(12);}
"float" {return tt.dtt(13);}
"geometry" {return tt.dtt(14);}
"geometrycollection" {return tt.dtt(15);}
"int" {return tt.dtt(16);}
"integer" {return tt.dtt(17);}
"json" {return tt.dtt(18);}
"linestring" {return tt.dtt(19);}
"longblob" {return tt.dtt(20);}
"longtext" {return tt.dtt(21);}
"mediumblob" {return tt.dtt(22);}
"mediumint" {return tt.dtt(23);}
"mediumtext" {return tt.dtt(24);}
"multilinestring" {return tt.dtt(25);}
"multipoint" {return tt.dtt(26);}
"multipolygon" {return tt.dtt(27);}
"numeric" {return tt.dtt(28);}
"point" {return tt.dtt(29);}
"polygon" {return tt.dtt(30);}
"real" {return tt.dtt(31);}
"smallint" {return tt.dtt(32);}
"text" {return tt.dtt(33);}
"time" {return tt.dtt(34);}
"timestamp" {return tt.dtt(35);}
"tinyblob" {return tt.dtt(36);}
"tinyint" {return tt.dtt(37);}
"tinytext" {return tt.dtt(38);}
"varbinary" {return tt.dtt(39);}
"varchar" {return tt.dtt(40);}
"year" {return tt.dtt(41);}
// MARKER_END_DATATYPES


// MARKER_BEGIN_KEYWORDS
"accessible" {return tt.ktt(0);}
"account" {return tt.ktt(1);}
"action" {return tt.ktt(2);}
"add" {return tt.ktt(3);}
"algorithm" {return tt.ktt(4);}
"all" {return tt.ktt(5);}
"alter" {return tt.ktt(6);}
"always" {return tt.ktt(7);}
"analyze" {return tt.ktt(8);}
"and" {return tt.ktt(9);}
"as" {return tt.ktt(10);}
"asc" {return tt.ktt(11);}
"asensitive" {return tt.ktt(12);}
"attribute" {return tt.ktt(13);}
"authentication" {return tt.ktt(14);}
"before" {return tt.ktt(15);}
"between" {return tt.ktt(16);}
"both" {return tt.ktt(17);}
"btree" {return tt.ktt(18);}
"by" {return tt.ktt(19);}
"call" {return tt.ktt(20);}
"cascade" {return tt.ktt(21);}
"cascaded" {return tt.ktt(22);}
"case" {return tt.ktt(23);}
"change" {return tt.ktt(24);}
"character" {return tt.ktt(25);}
"charset" {return tt.ktt(26);}
"check" {return tt.ktt(27);}
"checksum" {return tt.ktt(28);}
"cipher" {return tt.ktt(29);}
"close" {return tt.ktt(30);}
"collate" {return tt.ktt(31);}
"column" {return tt.ktt(32);}
"columns" {return tt.ktt(33);}
"comment" {return tt.ktt(34);}
"compact" {return tt.ktt(35);}
"compressed" {return tt.ktt(36);}
"compression" {return tt.ktt(37);}
"concurrent" {return tt.ktt(38);}
"condition" {return tt.ktt(39);}
"connection" {return tt.ktt(40);}
"constraint" {return tt.ktt(41);}
"continue" {return tt.ktt(42);}
"convert" {return tt.ktt(43);}
"copy" {return tt.ktt(44);}
"create" {return tt.ktt(45);}
"cross" {return tt.ktt(46);}
"current" {return tt.ktt(47);}
"current_user" {return tt.ktt(48);}
"cursor" {return tt.ktt(49);}
"data" {return tt.ktt(50);}
"database" {return tt.ktt(51);}
"databases" {return tt.ktt(52);}
"declare" {return tt.ktt(53);}
"default" {return tt.ktt(54);}
"definer" {return tt.ktt(55);}
"delayed" {return tt.ktt(56);}
"delete" {return tt.ktt(57);}
"desc" {return tt.ktt(58);}
"describe" {return tt.ktt(59);}
"deterministic" {return tt.ktt(60);}
"directory" {return tt.ktt(61);}
"disk" {return tt.ktt(62);}
"distinct" {return tt.ktt(63);}
"distinctrow" {return tt.ktt(64);}
"div" {return tt.ktt(65);}
"do" {return tt.ktt(66);}
"drop" {return tt.ktt(67);}
"dual" {return tt.ktt(68);}
"dumpfile" {return tt.ktt(69);}
"duplicate" {return tt.ktt(70);}
"dynamic" {return tt.ktt(71);}
"each" {return tt.ktt(72);}
"else" {return tt.ktt(73);}
"elseif" {return tt.ktt(74);}
"enclosed" {return tt.ktt(75);}
"encryption" {return tt.ktt(76);}
"end" {return tt.ktt(77);}
"enforced" {return tt.ktt(78);}
"engine" {return tt.ktt(79);}
"escaped" {return tt.ktt(80);}
"exists" {return tt.ktt(81);}
"exit" {return tt.ktt(82);}
"exclusive" {return tt.ktt(83);}
"expire" {return tt.ktt(84);}
"expansion" {return tt.ktt(85);}
"explain" {return tt.ktt(86);}
"false" {return tt.ktt(87);}
"fetch" {return tt.ktt(88);}
"fields" {return tt.ktt(89);}
"first" {return tt.ktt(90);}
"fixed" {return tt.ktt(91);}
"float4" {return tt.ktt(92);}
"float8" {return tt.ktt(93);}
"for" {return tt.ktt(94);}
"force" {return tt.ktt(95);}
"foreign" {return tt.ktt(96);}
"from" {return tt.ktt(97);}
"full" {return tt.ktt(98);}
"fulltext" {return tt.ktt(99);}
"generated" {return tt.ktt(100);}
"grant" {return tt.ktt(101);}
"group" {return tt.ktt(102);}
"handler" {return tt.ktt(103);}
"hash" {return tt.ktt(104);}
"having" {return tt.ktt(105);}
"high_priority" {return tt.ktt(106);}
"history" {return tt.ktt(107);}
"if" {return tt.ktt(108);}
"ignore" {return tt.ktt(109);}
"identified" {return tt.ktt(110);}
"in" {return tt.ktt(111);}
"index" {return tt.ktt(112);}
"infile" {return tt.ktt(113);}
"initial" {return tt.ktt(114);}
"inner" {return tt.ktt(115);}
"inplace" {return tt.ktt(116);}
"inout" {return tt.ktt(117);}
"insensitive" {return tt.ktt(118);}
"insert" {return tt.ktt(119);}
"int1" {return tt.ktt(120);}
"int2" {return tt.ktt(121);}
"int3" {return tt.ktt(122);}
"int4" {return tt.ktt(123);}
"int8" {return tt.ktt(124);}
"interval" {return tt.ktt(125);}
"into" {return tt.ktt(126);}
"invisible" {return tt.ktt(127);}
"invoker" {return tt.ktt(128);}
"is" {return tt.ktt(129);}
"issuer" {return tt.ktt(130);}
"iterate" {return tt.ktt(131);}
"join" {return tt.ktt(132);}
"key" {return tt.ktt(133);}
"keys" {return tt.ktt(134);}
"kill" {return tt.ktt(135);}
"language" {return tt.ktt(136);}
"last" {return tt.ktt(137);}
"leading" {return tt.ktt(138);}
"leave" {return tt.ktt(139);}
"left" {return tt.ktt(140);}
"less" {return tt.ktt(141);}
"level" {return tt.ktt(142);}
"like" {return tt.ktt(143);}
"limit" {return tt.ktt(144);}
"linear" {return tt.ktt(145);}
"lines" {return tt.ktt(146);}
"list" {return tt.ktt(147);}
"load" {return tt.ktt(148);}
"local" {return tt.ktt(149);}
"lock" {return tt.ktt(150);}
"long" {return tt.ktt(151);}
"loop" {return tt.ktt(152);}
"match" {return tt.ktt(153);}
"maxvalue" {return tt.ktt(154);}
"memory" {return tt.ktt(155);}
"merge" {return tt.ktt(156);}
"microsecond" {return tt.ktt(157);}
"middleint" {return tt.ktt(158);}
"mod" {return tt.ktt(159);}
"mode" {return tt.ktt(160);}
"modifies" {return tt.ktt(161);}
"national" {return tt.ktt(162);}
"natural" {return tt.ktt(163);}
"never" {return tt.ktt(164);}
"next" {return tt.ktt(165);}
"no" {return tt.ktt(166);}
"none" {return tt.ktt(167);}
"not" {return tt.ktt(168);}
"null" {return tt.ktt(169);}
"offset" {return tt.ktt(170);}
"oj" {return tt.ktt(171);}
"on" {return tt.ktt(172);}
"open" {return tt.ktt(173);}
"optimize" {return tt.ktt(174);}
"option" {return tt.ktt(175);}
"optional" {return tt.ktt(176);}
"optionally" {return tt.ktt(177);}
"or" {return tt.ktt(178);}
"order" {return tt.ktt(179);}
"out" {return tt.ktt(180);}
"outer" {return tt.ktt(181);}
"outfile" {return tt.ktt(182);}
"parser" {return tt.ktt(183);}
"partial" {return tt.ktt(184);}
"partition" {return tt.ktt(185);}
"partitions" {return tt.ktt(186);}
"password" {return tt.ktt(187);}
"precision" {return tt.ktt(188);}
"prev" {return tt.ktt(189);}
"primary" {return tt.ktt(190);}
"procedure" {return tt.ktt(191);}
"purge" {return tt.ktt(192);}
"query" {return tt.ktt(193);}
"quick" {return tt.ktt(194);}
"random" {return tt.ktt(195);}
"range" {return tt.ktt(196);}
"read" {return tt.ktt(197);}
"reads" {return tt.ktt(198);}
"read_only" {return tt.ktt(199);}
"read_write" {return tt.ktt(200);}
"recursive" {return tt.ktt(201);}
"redundant" {return tt.ktt(202);}
"references" {return tt.ktt(203);}
"regexp" {return tt.ktt(204);}
"release" {return tt.ktt(205);}
"rename" {return tt.ktt(206);}
"repeat" {return tt.ktt(207);}
"replace" {return tt.ktt(208);}
"require" {return tt.ktt(209);}
"restrict" {return tt.ktt(210);}
"return" {return tt.ktt(211);}
"reverse" {return tt.ktt(212);}
"reuse" {return tt.ktt(213);}
"revoke" {return tt.ktt(214);}
"right" {return tt.ktt(215);}
"rlike" {return tt.ktt(216);}
"rollback" {return tt.ktt(217);}
"rollup" {return tt.ktt(218);}
"role" {return tt.ktt(219);}
"row" {return tt.ktt(220);}
"schema" {return tt.ktt(221);}
"schemas" {return tt.ktt(222);}
"security" {return tt.ktt(223);}
"select" {return tt.ktt(224);}
"sensitive" {return tt.ktt(225);}
"separator" {return tt.ktt(226);}
"set" {return tt.ktt(227);}
"share" {return tt.ktt(228);}
"shared" {return tt.ktt(229);}
"show" {return tt.ktt(230);}
"simple" {return tt.ktt(231);}
"spatial" {return tt.ktt(232);}
"specific" {return tt.ktt(233);}
"sql" {return tt.ktt(234);}
"sqlexception" {return tt.ktt(235);}
"sqlstate" {return tt.ktt(236);}
"sqlwarning" {return tt.ktt(237);}
"sql_big_result" {return tt.ktt(238);}
"sql_buffer_result" {return tt.ktt(239);}
"sql_cache" {return tt.ktt(240);}
"sql_calc_found_rows" {return tt.ktt(241);}
"sql_no_cache" {return tt.ktt(242);}
"sql_small_result" {return tt.ktt(243);}
"ssl" {return tt.ktt(244);}
"start" {return tt.ktt(245);}
"starting" {return tt.ktt(246);}
"storage" {return tt.ktt(247);}
"stored" {return tt.ktt(248);}
"straight_join" {return tt.ktt(249);}
"subject" {return tt.ktt(250);}
"subpartition" {return tt.ktt(251);}
"table" {return tt.ktt(252);}
"tablespace" {return tt.ktt(253);}
"temporary" {return tt.ktt(254);}
"temptable" {return tt.ktt(255);}
"terminated" {return tt.ktt(256);}
"than" {return tt.ktt(257);}
"then" {return tt.ktt(258);}
"to" {return tt.ktt(259);}
"trailing" {return tt.ktt(260);}
"transaction" {return tt.ktt(261);}
"trigger" {return tt.ktt(262);}
"true" {return tt.ktt(263);}
"truncate" {return tt.ktt(264);}
"undefined" {return tt.ktt(265);}
"undo" {return tt.ktt(266);}
"union" {return tt.ktt(267);}
"unique" {return tt.ktt(268);}
"unlock" {return tt.ktt(269);}
"unbounded" {return tt.ktt(270);}
"unsigned" {return tt.ktt(271);}
"update" {return tt.ktt(272);}
"usage" {return tt.ktt(273);}
"use" {return tt.ktt(274);}
"using" {return tt.ktt(275);}
"value" {return tt.ktt(276);}
"values" {return tt.ktt(277);}
"varcharacter" {return tt.ktt(278);}
"varying" {return tt.ktt(279);}
"view" {return tt.ktt(280);}
"virtual" {return tt.ktt(281);}
"visible" {return tt.ktt(282);}
"when" {return tt.ktt(283);}
"where" {return tt.ktt(284);}
"while" {return tt.ktt(285);}
"with" {return tt.ktt(286);}
"write" {return tt.ktt(287);}
"x509" {return tt.ktt(288);}
"xor" {return tt.ktt(289);}
"zerofill" {return tt.ktt(290);}
// MARKER_END_KEYWORDS



// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.ftt(0);}
"acos" {return tt.ftt(1);}
"adddate" {return tt.ftt(2);}
"addtime" {return tt.ftt(3);}
"aes_decrypt" {return tt.ftt(4);}
"aes_encrypt" {return tt.ftt(5);}
"against" {return tt.ftt(6);}
"ascii" {return tt.ftt(7);}
"asin" {return tt.ftt(8);}
"atan" {return tt.ftt(9);}
"atan2" {return tt.ftt(10);}
"avg" {return tt.ftt(11);}
"benchmark" {return tt.ftt(12);}
"bin" {return tt.ftt(13);}
"bit_and" {return tt.ftt(14);}
"bit_length" {return tt.ftt(15);}
"bit_or" {return tt.ftt(16);}
"bit_xor" {return tt.ftt(17);}
"ceil" {return tt.ftt(18);}
"ceiling" {return tt.ftt(19);}
"character_length" {return tt.ftt(20);}
"char_length" {return tt.ftt(21);}
"coercibility" {return tt.ftt(22);}
"collation" {return tt.ftt(23);}
"compress" {return tt.ftt(24);}
"concat" {return tt.ftt(25);}
"concat_ws" {return tt.ftt(26);}
"connection_id" {return tt.ftt(27);}
"conv" {return tt.ftt(28);}
"convert_tz" {return tt.ftt(29);}
"cos" {return tt.ftt(30);}
"cot" {return tt.ftt(31);}
"count" {return tt.ftt(32);}
"crc32" {return tt.ftt(33);}
"curdate" {return tt.ftt(34);}
"current_date" {return tt.ftt(35);}
"current_time" {return tt.ftt(36);}
"current_timestamp" {return tt.ftt(37);}
"curtime" {return tt.ftt(38);}
"datediff" {return tt.ftt(39);}
"date_add" {return tt.ftt(40);}
"date_format" {return tt.ftt(41);}
"date_sub" {return tt.ftt(42);}
"day" {return tt.ftt(43);}
"dayname" {return tt.ftt(44);}
"dayofmonth" {return tt.ftt(45);}
"dayofweek" {return tt.ftt(46);}
"dayofyear" {return tt.ftt(47);}
"day_hour" {return tt.ftt(48);}
"day_microsecond" {return tt.ftt(49);}
"day_minute" {return tt.ftt(50);}
"day_second" {return tt.ftt(51);}
"decode" {return tt.ftt(52);}
"degrees" {return tt.ftt(53);}
"des_decrypt" {return tt.ftt(54);}
"des_encrypt" {return tt.ftt(55);}
"elt" {return tt.ftt(56);}
"encode" {return tt.ftt(57);}
"encrypt" {return tt.ftt(58);}
"exp" {return tt.ftt(59);}
"export_set" {return tt.ftt(60);}
"extract" {return tt.ftt(61);}
"field" {return tt.ftt(62);}
"find_in_set" {return tt.ftt(63);}
"floor" {return tt.ftt(64);}
"fn_second_microsecond" {return tt.ftt(65);}
"format" {return tt.ftt(66);}
"found_rows" {return tt.ftt(67);}
"from_days" {return tt.ftt(68);}
"from_unixtime" {return tt.ftt(69);}
"get_format" {return tt.ftt(70);}
"get_lock" {return tt.ftt(71);}
"group_concat" {return tt.ftt(72);}
"hex" {return tt.ftt(73);}
"hour" {return tt.ftt(74);}
"hour_microsecond" {return tt.ftt(75);}
"hour_minute" {return tt.ftt(76);}
"hour_second" {return tt.ftt(77);}
"ifnull" {return tt.ftt(78);}
"inet_aton" {return tt.ftt(79);}
"inet_ntoa" {return tt.ftt(80);}
"instr" {return tt.ftt(81);}
"is_free_lock" {return tt.ftt(82);}
"is_used_lock" {return tt.ftt(83);}
"last_day" {return tt.ftt(84);}
"last_insert_id" {return tt.ftt(85);}
"lcase" {return tt.ftt(86);}
"length" {return tt.ftt(87);}
"ln" {return tt.ftt(88);}
"load_file" {return tt.ftt(89);}
"localtime" {return tt.ftt(90);}
"localtimestamp" {return tt.ftt(91);}
"locate" {return tt.ftt(92);}
"log" {return tt.ftt(93);}
"log10" {return tt.ftt(94);}
"log2" {return tt.ftt(95);}
"lower" {return tt.ftt(96);}
"lpad" {return tt.ftt(97);}
"ltrim" {return tt.ftt(98);}
"makedate" {return tt.ftt(99);}
"maketime" {return tt.ftt(100);}
"make_set" {return tt.ftt(101);}
"master_pos_wait" {return tt.ftt(102);}
"max" {return tt.ftt(103);}
"md5" {return tt.ftt(104);}
"mid" {return tt.ftt(105);}
"min" {return tt.ftt(106);}
"minute" {return tt.ftt(107);}
"minute_microsecond" {return tt.ftt(108);}
"minute_second" {return tt.ftt(109);}
"month" {return tt.ftt(110);}
"monthname" {return tt.ftt(111);}
"name_const" {return tt.ftt(112);}
"not_like" {return tt.ftt(113);}
"not_regexp" {return tt.ftt(114);}
"now" {return tt.ftt(115);}
"nullif" {return tt.ftt(116);}
"oct" {return tt.ftt(117);}
"octet_length" {return tt.ftt(118);}
"old_password" {return tt.ftt(119);}
"ord" {return tt.ftt(120);}
"period_add" {return tt.ftt(121);}
"period_diff" {return tt.ftt(122);}
"pi" {return tt.ftt(123);}
"position" {return tt.ftt(124);}
"pow" {return tt.ftt(125);}
"power" {return tt.ftt(126);}
"quarter" {return tt.ftt(127);}
"quote" {return tt.ftt(128);}
"radians" {return tt.ftt(129);}
"rand" {return tt.ftt(130);}
"release_lock" {return tt.ftt(131);}
"round" {return tt.ftt(132);}
"row_count" {return tt.ftt(133);}
"rpad" {return tt.ftt(134);}
"rtrim" {return tt.ftt(135);}
"second" {return tt.ftt(136);}
"second_microsecond" {return tt.ftt(137);}
"sec_to_time" {return tt.ftt(138);}
"session_user" {return tt.ftt(139);}
"sha" {return tt.ftt(140);}
"sha1" {return tt.ftt(141);}
"sha2" {return tt.ftt(142);}
"sign" {return tt.ftt(143);}
"sin" {return tt.ftt(144);}
"sleep" {return tt.ftt(145);}
"soundex" {return tt.ftt(146);}
"sounds_like" {return tt.ftt(147);}
"space" {return tt.ftt(148);}
"sqrt" {return tt.ftt(149);}
"std" {return tt.ftt(150);}
"stddev" {return tt.ftt(151);}
"stddev_pop" {return tt.ftt(152);}
"stddev_samp" {return tt.ftt(153);}
"strcmp" {return tt.ftt(154);}
"str_to_date" {return tt.ftt(155);}
"subdate" {return tt.ftt(156);}
"substr" {return tt.ftt(157);}
"substring" {return tt.ftt(158);}
"substring_index" {return tt.ftt(159);}
"subtime" {return tt.ftt(160);}
"sum" {return tt.ftt(161);}
"sysdate" {return tt.ftt(162);}
"system_user" {return tt.ftt(163);}
"tan" {return tt.ftt(164);}
"timediff" {return tt.ftt(165);}
"timestampadd" {return tt.ftt(166);}
"timestampdiff" {return tt.ftt(167);}
"time_format" {return tt.ftt(168);}
"time_to_sec" {return tt.ftt(169);}
"to_days" {return tt.ftt(170);}
"trim" {return tt.ftt(171);}
"ucase" {return tt.ftt(172);}
"uncompress" {return tt.ftt(173);}
"uncompressed_length" {return tt.ftt(174);}
"unhex" {return tt.ftt(175);}
"unix_timestamp" {return tt.ftt(176);}
"upper" {return tt.ftt(177);}
"user" {return tt.ftt(178);}
"utc_date" {return tt.ftt(179);}
"utc_time" {return tt.ftt(180);}
"utc_timestamp" {return tt.ftt(181);}
"uuid" {return tt.ftt(182);}
"uuid_short" {return tt.ftt(183);}
"variance" {return tt.ftt(184);}
"var_pop" {return tt.ftt(185);}
"var_samp" {return tt.ftt(186);}
"version" {return tt.ftt(187);}
"week" {return tt.ftt(188);}
"weekday" {return tt.ftt(189);}
"weekofyear" {return tt.ftt(190);}
"weight_string" {return tt.ftt(191);}
"yearweek" {return tt.ftt(192);}
"year_month" {return tt.ftt(193);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
"autoextend_size" {return tt.ptt(0);}
"auto_increment" {return tt.ptt(1);}
"avg_row_length" {return tt.ptt(2);}
"column_format" {return tt.ptt(3);}
"delay_key_write" {return tt.ptt(4);}
"engine_attribute" {return tt.ptt(5);}
"failed_login_attempts" {return tt.ptt(6);}
"insert_method" {return tt.ptt(7);}
"key_block_size" {return tt.ptt(8);}
"low_ignore" {return tt.ptt(9);}
"low_priority" {return tt.ptt(10);}
"master_ssl_verify_server_cert" {return tt.ptt(11);}
"max_connections_per_hour" {return tt.ptt(12);}
"max_rows" {return tt.ptt(13);}
"max_queries_per_hour" {return tt.ptt(14);}
"max_updates_per_hour" {return tt.ptt(15);}
"max_user_connections" {return tt.ptt(16);}
"min_rows" {return tt.ptt(17);}
"no_write_to_binlog" {return tt.ptt(18);}
"pack_keys" {return tt.ptt(19);}
"password_lock_time" {return tt.ptt(20);}
"row_format" {return tt.ptt(21);}
"secondary_engine_attribute" {return tt.ptt(22);}
"stats_auto_recalc" {return tt.ptt(23);}
"stats_persistent" {return tt.ptt(24);}
"stats_sample_pages" {return tt.ptt(25);}
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }
