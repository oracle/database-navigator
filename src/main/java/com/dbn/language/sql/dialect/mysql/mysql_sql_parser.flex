package com.dbn.language.sql.dialect.mysql;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

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
string_double_quoted      = \"([^\"]|"\"\""|{WHITE_SPACE})*\"?
STRING = ("n"|"_"{CHARSET})?{wso}({string_simple_quoted}|{string_double_quoted})

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

BIND_VARIABLE = ":"({IDENTIFIER}|{INTEGER})
USER_VARIABLE = "@"({IDENTIFIER}|{INTEGER})
SYSTEM_VARIABLE = "@@"({IDENTIFIER}|{INTEGER})
VARIABLE = {BIND_VARIABLE}|{SYSTEM_VARIABLE}|{USER_VARIABLE}

%state DIV
%%

{WHITE_SPACE}+   { return stt.whiteSpace; }

{BLOCK_COMMENT}  { return stt.blockComment; }
{LINE_COMMENT}   { return stt.lineComment; }

{VARIABLE}       { return stt.variable; }
{INTEGER}        { return stt.integer; }
{NUMBER}         { return stt.number; }
{STRING}         { return stt.string; }

"="{wso}"="         {return tt.getOperatorTokenType(0);}
"|"{wso}"|"         {return tt.getOperatorTokenType(1);}
"<"{wso}"="         {return tt.getOperatorTokenType(2);}
">"{wso}"="         {return tt.getOperatorTokenType(3);}
"<"{wso}">"         {return tt.getOperatorTokenType(4);}
"!"{wso}"="         {return tt.getOperatorTokenType(5);}
":"{wso}"="         {return tt.getOperatorTokenType(6);}
"="{wso}">"         {return tt.getOperatorTokenType(7);}
".."                {return tt.getOperatorTokenType(8);}
"::"                {return tt.getOperatorTokenType(9);}
"<"{wso}"="{wso}">" {return tt.getOperatorTokenType(16);}
"<"{wso}"<"         {return tt.getOperatorTokenType(17);}
">"{wso}">"         {return tt.getOperatorTokenType(18);}

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
"^" {return tt.getCharacterTokenType(24);}




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
"active" {return tt.ktt(3);}
"add" {return tt.ktt(4);}
"after" {return tt.ktt(5);}
"algorithm" {return tt.ktt(6);}
"all" {return tt.ktt(7);}
"alter" {return tt.ktt(8);}
"always" {return tt.ktt(9);}
"analyze" {return tt.ktt(10);}
"and" {return tt.ktt(11);}
"any" {return tt.ktt(12);}
"as" {return tt.ktt(13);}
"asc" {return tt.ktt(14);}
"asensitive" {return tt.ktt(15);}
"at" {return tt.ktt(16);}
"attribute" {return tt.ktt(17);}
"authentication" {return tt.ktt(18);}
"before" {return tt.ktt(19);}
"begin" {return tt.ktt(20);}
"between" {return tt.ktt(21);}
"binlog" {return tt.ktt(22);}
"both" {return tt.ktt(23);}
"btree" {return tt.ktt(24);}
"by" {return tt.ktt(25);}
"call" {return tt.ktt(26);}
"cascade" {return tt.ktt(27);}
"cascaded" {return tt.ktt(28);}
"cast" {return tt.ktt(29);}
"case" {return tt.ktt(30);}
"change" {return tt.ktt(31);}
"channel" {return tt.ktt(32);}
"character" {return tt.ktt(33);}
"charset" {return tt.ktt(34);}
"check" {return tt.ktt(35);}
"checksum" {return tt.ktt(36);}
"cipher" {return tt.ktt(37);}
"close" {return tt.ktt(38);}
"collate" {return tt.ktt(39);}
"column" {return tt.ktt(40);}
"columns" {return tt.ktt(41);}
"comment" {return tt.ktt(42);}
"compile" {return tt.ktt(43);}
"compact" {return tt.ktt(44);}
"completion" {return tt.ktt(45);}
"compressed" {return tt.ktt(46);}
"compression" {return tt.ktt(47);}
"concurrent" {return tt.ktt(48);}
"condition" {return tt.ktt(49);}
"connection" {return tt.ktt(50);}
"constraint" {return tt.ktt(51);}
"contains" {return tt.ktt(52);}
"continue" {return tt.ktt(53);}
"convert" {return tt.ktt(54);}
"copy" {return tt.ktt(55);}
"create" {return tt.ktt(56);}
"cross" {return tt.ktt(57);}
"current" {return tt.ktt(58);}
"current_user" {return tt.ktt(59);}
"cursor" {return tt.ktt(60);}
"data" {return tt.ktt(61);}
"datafile" {return tt.ktt(62);}
"database" {return tt.ktt(63);}
"databases" {return tt.ktt(64);}
"declare" {return tt.ktt(65);}
"default" {return tt.ktt(66);}
"definer" {return tt.ktt(67);}
"delayed" {return tt.ktt(68);}
"delete" {return tt.ktt(69);}
"desc" {return tt.ktt(70);}
"describe" {return tt.ktt(71);}
"deterministic" {return tt.ktt(72);}
"directory" {return tt.ktt(73);}
"disable" {return tt.ktt(74);}
"disk" {return tt.ktt(75);}
"distinct" {return tt.ktt(76);}
"distinctrow" {return tt.ktt(77);}
"div" {return tt.ktt(78);}
"do" {return tt.ktt(79);}
"drop" {return tt.ktt(80);}
"dual" {return tt.ktt(81);}
"duality" {return tt.ktt(82);}
"discard" {return tt.ktt(83);}
"dumpfile" {return tt.ktt(84);}
"duplicate" {return tt.ktt(85);}
"dynamic" {return tt.ktt(86);}
"each" {return tt.ktt(87);}
"else" {return tt.ktt(88);}
"elseif" {return tt.ktt(89);}
"enclosed" {return tt.ktt(90);}
"encryption" {return tt.ktt(91);}
"end" {return tt.ktt(92);}
"enable" {return tt.ktt(93);}
"enforced" {return tt.ktt(94);}
"engine" {return tt.ktt(95);}
"ends" {return tt.ktt(96);}
"error" {return tt.ktt(97);}
"escaped" {return tt.ktt(98);}
"event" {return tt.ktt(99);}
"every" {return tt.ktt(100);}
"except" {return tt.ktt(101);}
"exchange" {return tt.ktt(102);}
"exists" {return tt.ktt(103);}
"exit" {return tt.ktt(104);}
"exclusive" {return tt.ktt(105);}
"expire" {return tt.ktt(106);}
"expansion" {return tt.ktt(107);}
"explain" {return tt.ktt(108);}
"factor" {return tt.ktt(109);}
"false" {return tt.ktt(110);}
"fetch" {return tt.ktt(111);}
"fields" {return tt.ktt(112);}
"finish" {return tt.ktt(113);}
"first" {return tt.ktt(114);}
"fixed" {return tt.ktt(115);}
"float4" {return tt.ktt(116);}
"float8" {return tt.ktt(117);}
"following" {return tt.ktt(118);}
"follows" {return tt.ktt(119);}
"for" {return tt.ktt(120);}
"force" {return tt.ktt(121);}
"foreign" {return tt.ktt(122);}
"from" {return tt.ktt(123);}
"full" {return tt.ktt(124);}
"fulltext" {return tt.ktt(125);}
"function" {return tt.ktt(126);}
"generated" {return tt.ktt(127);}
"grant" {return tt.ktt(128);}
"group" {return tt.ktt(129);}
"handler" {return tt.ktt(130);}
"hash" {return tt.ktt(131);}
"having" {return tt.ktt(132);}
"high_priority" {return tt.ktt(133);}
"history" {return tt.ktt(134);}
"if" {return tt.ktt(135);}
"ignore" {return tt.ktt(136);}
"import" {return tt.ktt(137);}
"identified" {return tt.ktt(138);}
"in" {return tt.ktt(139);}
"index" {return tt.ktt(140);}
"infile" {return tt.ktt(141);}
"inactive" {return tt.ktt(142);}
"initial" {return tt.ktt(143);}
"initiate" {return tt.ktt(144);}
"inner" {return tt.ktt(145);}
"inplace" {return tt.ktt(146);}
"instant" {return tt.ktt(147);}
"inout" {return tt.ktt(148);}
"innodb" {return tt.ktt(149);}
"insensitive" {return tt.ktt(150);}
"insert" {return tt.ktt(151);}
"intersect" {return tt.ktt(152);}
"int1" {return tt.ktt(153);}
"int2" {return tt.ktt(154);}
"int3" {return tt.ktt(155);}
"int4" {return tt.ktt(156);}
"int8" {return tt.ktt(157);}
"interval" {return tt.ktt(158);}
"into" {return tt.ktt(159);}
"invisible" {return tt.ktt(160);}
"instance" {return tt.ktt(161);}
"invoker" {return tt.ktt(162);}
"is" {return tt.ktt(163);}
"issuer" {return tt.ktt(164);}
"iterate" {return tt.ktt(165);}
"javascript" {return tt.ktt(166);}
"join" {return tt.ktt(167);}
"key" {return tt.ktt(168);}
"keyring" {return tt.ktt(169);}
"keys" {return tt.ktt(170);}
"kill" {return tt.ktt(171);}
"language" {return tt.ktt(172);}
"last" {return tt.ktt(173);}
"leading" {return tt.ktt(174);}
"leave" {return tt.ktt(175);}
"left" {return tt.ktt(176);}
"less" {return tt.ktt(177);}
"level" {return tt.ktt(178);}
"library" {return tt.ktt(179);}
"like" {return tt.ktt(180);}
"limit" {return tt.ktt(181);}
"linear" {return tt.ktt(182);}
"lines" {return tt.ktt(183);}
"list" {return tt.ktt(184);}
"load" {return tt.ktt(185);}
"local" {return tt.ktt(186);}
"logfile" {return tt.ktt(187);}
"locked" {return tt.ktt(188);}
"lock" {return tt.ktt(189);}
"long" {return tt.ktt(190);}
"loop" {return tt.ktt(191);}
"master" {return tt.ktt(192);}
"match" {return tt.ktt(193);}
"materialized" {return tt.ktt(194);}
"maxvalue" {return tt.ktt(195);}
"member" {return tt.ktt(196);}
"memory" {return tt.ktt(197);}
"merge" {return tt.ktt(198);}
"microsecond" {return tt.ktt(199);}
"middleint" {return tt.ktt(200);}
"mod" {return tt.ktt(201);}
"mode" {return tt.ktt(202);}
"modify" {return tt.ktt(203);}
"modifies" {return tt.ktt(204);}
"national" {return tt.ktt(205);}
"natural" {return tt.ktt(206);}
"never" {return tt.ktt(207);}
"new" {return tt.ktt(208);}
"next" {return tt.ktt(209);}
"no" {return tt.ktt(210);}
"none" {return tt.ktt(211);}
"not" {return tt.ktt(212);}
"nowait" {return tt.ktt(213);}
"null" {return tt.ktt(214);}
"offset" {return tt.ktt(215);}
"of" {return tt.ktt(216);}
"oj" {return tt.ktt(217);}
"old" {return tt.ktt(218);}
"on" {return tt.ktt(219);}
"open" {return tt.ktt(220);}
"optimize" {return tt.ktt(221);}
"option" {return tt.ktt(222);}
"options" {return tt.ktt(223);}
"optional" {return tt.ktt(224);}
"optionally" {return tt.ktt(225);}
"only" {return tt.ktt(226);}
"or" {return tt.ktt(227);}
"order" {return tt.ktt(228);}
"out" {return tt.ktt(229);}
"outer" {return tt.ktt(230);}
"outfile" {return tt.ktt(231);}
"off" {return tt.ktt(232);}
"owner" {return tt.ktt(233);}
"over" {return tt.ktt(234);}
"parser" {return tt.ktt(235);}
"partial" {return tt.ktt(236);}
"partition" {return tt.ktt(237);}
"partitioning" {return tt.ktt(238);}
"partitions" {return tt.ktt(239);}
"password" {return tt.ktt(240);}
"precision" {return tt.ktt(241);}
"preceding" {return tt.ktt(242);}
"precedes" {return tt.ktt(243);}
"preserve" {return tt.ktt(244);}
"prev" {return tt.ktt(245);}
"primary" {return tt.ktt(246);}
"procedure" {return tt.ktt(247);}
"purge" {return tt.ktt(248);}
"query" {return tt.ktt(249);}
"quick" {return tt.ktt(250);}
"random" {return tt.ktt(251);}
"range" {return tt.ktt(252);}
"read" {return tt.ktt(253);}
"reads" {return tt.ktt(254);}
"read_only" {return tt.ktt(255);}
"read_write" {return tt.ktt(256);}
"rebuild" {return tt.ktt(257);}
"recursive" {return tt.ktt(258);}
"redundant" {return tt.ktt(259);}
"redo_log" {return tt.ktt(260);}
"references" {return tt.ktt(261);}
"regexp" {return tt.ktt(262);}
"release" {return tt.ktt(263);}
"reload" {return tt.ktt(264);}
"rename" {return tt.ktt(265);}
"repeat" {return tt.ktt(266);}
"replace" {return tt.ktt(267);}
"registration" {return tt.ktt(268);}
"require" {return tt.ktt(269);}
"restrict" {return tt.ktt(270);}
"retain" {return tt.ktt(271);}
"return" {return tt.ktt(272);}
"reverse" {return tt.ktt(273);}
"replica" {return tt.ktt(274);}
"resource" {return tt.ktt(275);}
"remove" {return tt.ktt(276);}
"reorganize" {return tt.ktt(277);}
"repair" {return tt.ktt(278);}
"reuse" {return tt.ktt(279);}
"revoke" {return tt.ktt(280);}
"right" {return tt.ktt(281);}
"rotate" {return tt.ktt(282);}
"rlike" {return tt.ktt(283);}
"rollback" {return tt.ktt(284);}
"rollup" {return tt.ktt(285);}
"role" {return tt.ktt(286);}
"row" {return tt.ktt(287);}
"rows" {return tt.ktt(288);}
"schedule" {return tt.ktt(289);}
"schema" {return tt.ktt(290);}
"schemas" {return tt.ktt(291);}
"security" {return tt.ktt(292);}
"select" {return tt.ktt(293);}
"sensitive" {return tt.ktt(294);}
"separator" {return tt.ktt(295);}
"server" {return tt.ktt(296);}
"set" {return tt.ktt(297);}
"share" {return tt.ktt(298);}
"shared" {return tt.ktt(299);}
"show" {return tt.ktt(300);}
"signed" {return tt.ktt(301);}
"simple" {return tt.ktt(302);}
"skip" {return tt.ktt(303);}
"slave" {return tt.ktt(304);}
"spatial" {return tt.ktt(305);}
"specific" {return tt.ktt(306);}
"sql" {return tt.ktt(307);}
"sqlexception" {return tt.ktt(308);}
"sqlstate" {return tt.ktt(309);}
"sqlwarning" {return tt.ktt(310);}
"sql_big_result" {return tt.ktt(311);}
"sql_buffer_result" {return tt.ktt(312);}
"sql_cache" {return tt.ktt(313);}
"sql_calc_found_rows" {return tt.ktt(314);}
"sql_no_cache" {return tt.ktt(315);}
"sql_small_result" {return tt.ktt(316);}
"ssl" {return tt.ktt(317);}
"start" {return tt.ktt(318);}
"starting" {return tt.ktt(319);}
"starts" {return tt.ktt(320);}
"storage" {return tt.ktt(321);}
"stored" {return tt.ktt(322);}
"straight_join" {return tt.ktt(323);}
"subject" {return tt.ktt(324);}
"subpartition" {return tt.ktt(325);}
"subpartitions" {return tt.ktt(326);}
"table" {return tt.ktt(327);}
"tablespace" {return tt.ktt(328);}
"temporary" {return tt.ktt(329);}
"temptable" {return tt.ktt(330);}
"terminated" {return tt.ktt(331);}
"than" {return tt.ktt(332);}
"then" {return tt.ktt(333);}
"tls" {return tt.ktt(334);}
"to" {return tt.ktt(335);}
"trailing" {return tt.ktt(336);}
"transaction" {return tt.ktt(337);}
"trigger" {return tt.ktt(338);}
"true" {return tt.ktt(339);}
"truncate" {return tt.ktt(340);}
"undefined" {return tt.ktt(341);}
"undo" {return tt.ktt(342);}
"undofile" {return tt.ktt(343);}
"union" {return tt.ktt(344);}
"unique" {return tt.ktt(345);}
"unlock" {return tt.ktt(346);}
"unbounded" {return tt.ktt(347);}
"unregister" {return tt.ktt(348);}
"unsigned" {return tt.ktt(349);}
"update" {return tt.ktt(350);}
"upgrade" {return tt.ktt(351);}
"usage" {return tt.ktt(352);}
"use" {return tt.ktt(353);}
"using" {return tt.ktt(354);}
"validation" {return tt.ktt(355);}
"value" {return tt.ktt(356);}
"values" {return tt.ktt(357);}
"varcharacter" {return tt.ktt(358);}
"varying" {return tt.ktt(359);}
"view" {return tt.ktt(360);}
"virtual" {return tt.ktt(361);}
"visible" {return tt.ktt(362);}
"wait" {return tt.ktt(363);}
"when" {return tt.ktt(364);}
"where" {return tt.ktt(365);}
"while" {return tt.ktt(366);}
"window" {return tt.ktt(367);}
"with" {return tt.ktt(368);}
"without" {return tt.ktt(369);}
"write" {return tt.ktt(370);}
"x509" {return tt.ktt(371);}
"xor" {return tt.ktt(372);}
"zerofill" {return tt.ktt(373);}
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
"coalesce" {return tt.ftt(22);}
"coercibility" {return tt.ftt(23);}
"collation" {return tt.ftt(24);}
"compress" {return tt.ftt(25);}
"concat" {return tt.ftt(26);}
"concat_ws" {return tt.ftt(27);}
"connection_id" {return tt.ftt(28);}
"conv" {return tt.ftt(29);}
"convert_tz" {return tt.ftt(30);}
"cos" {return tt.ftt(31);}
"cot" {return tt.ftt(32);}
"count" {return tt.ftt(33);}
"crc32" {return tt.ftt(34);}
"curdate" {return tt.ftt(35);}
"current_date" {return tt.ftt(36);}
"current_time" {return tt.ftt(37);}
"current_timestamp" {return tt.ftt(38);}
"curtime" {return tt.ftt(39);}
"datediff" {return tt.ftt(40);}
"date_add" {return tt.ftt(41);}
"date_format" {return tt.ftt(42);}
"date_sub" {return tt.ftt(43);}
"day" {return tt.ftt(44);}
"dayname" {return tt.ftt(45);}
"dayofmonth" {return tt.ftt(46);}
"dayofweek" {return tt.ftt(47);}
"dayofyear" {return tt.ftt(48);}
"day_hour" {return tt.ftt(49);}
"day_microsecond" {return tt.ftt(50);}
"day_minute" {return tt.ftt(51);}
"day_second" {return tt.ftt(52);}
"decode" {return tt.ftt(53);}
"degrees" {return tt.ftt(54);}
"des_decrypt" {return tt.ftt(55);}
"des_encrypt" {return tt.ftt(56);}
"elt" {return tt.ftt(57);}
"encode" {return tt.ftt(58);}
"encrypt" {return tt.ftt(59);}
"exp" {return tt.ftt(60);}
"export_set" {return tt.ftt(61);}
"extract" {return tt.ftt(62);}
"field" {return tt.ftt(63);}
"find_in_set" {return tt.ftt(64);}
"floor" {return tt.ftt(65);}
"fn_second_microsecond" {return tt.ftt(66);}
"format" {return tt.ftt(67);}
"found_rows" {return tt.ftt(68);}
"from_days" {return tt.ftt(69);}
"from_unixtime" {return tt.ftt(70);}
"get_format" {return tt.ftt(71);}
"get_lock" {return tt.ftt(72);}
"group_concat" {return tt.ftt(73);}
"hex" {return tt.ftt(74);}
"hour" {return tt.ftt(75);}
"hour_microsecond" {return tt.ftt(76);}
"hour_minute" {return tt.ftt(77);}
"hour_second" {return tt.ftt(78);}
"ifnull" {return tt.ftt(79);}
"inet_aton" {return tt.ftt(80);}
"inet_ntoa" {return tt.ftt(81);}
"instr" {return tt.ftt(82);}
"is_free_lock" {return tt.ftt(83);}
"is_used_lock" {return tt.ftt(84);}
"json_duality_object" {return tt.ftt(85);}
"last_day" {return tt.ftt(86);}
"last_insert_id" {return tt.ftt(87);}
"lcase" {return tt.ftt(88);}
"length" {return tt.ftt(89);}
"ln" {return tt.ftt(90);}
"load_file" {return tt.ftt(91);}
"localtime" {return tt.ftt(92);}
"localtimestamp" {return tt.ftt(93);}
"locate" {return tt.ftt(94);}
"log" {return tt.ftt(95);}
"log10" {return tt.ftt(96);}
"log2" {return tt.ftt(97);}
"lower" {return tt.ftt(98);}
"lpad" {return tt.ftt(99);}
"ltrim" {return tt.ftt(100);}
"makedate" {return tt.ftt(101);}
"maketime" {return tt.ftt(102);}
"make_set" {return tt.ftt(103);}
"master_pos_wait" {return tt.ftt(104);}
"max" {return tt.ftt(105);}
"md5" {return tt.ftt(106);}
"mid" {return tt.ftt(107);}
"min" {return tt.ftt(108);}
"minute" {return tt.ftt(109);}
"minute_microsecond" {return tt.ftt(110);}
"minute_second" {return tt.ftt(111);}
"month" {return tt.ftt(112);}
"monthname" {return tt.ftt(113);}
"name_const" {return tt.ftt(114);}
"not_like" {return tt.ftt(115);}
"not_regexp" {return tt.ftt(116);}
"now" {return tt.ftt(117);}
"nullif" {return tt.ftt(118);}
"oct" {return tt.ftt(119);}
"octet_length" {return tt.ftt(120);}
"old_password" {return tt.ftt(121);}
"ord" {return tt.ftt(122);}
"period_add" {return tt.ftt(123);}
"period_diff" {return tt.ftt(124);}
"pi" {return tt.ftt(125);}
"position" {return tt.ftt(126);}
"pow" {return tt.ftt(127);}
"power" {return tt.ftt(128);}
"quarter" {return tt.ftt(129);}
"quote" {return tt.ftt(130);}
"radians" {return tt.ftt(131);}
"rand" {return tt.ftt(132);}
"release_lock" {return tt.ftt(133);}
"round" {return tt.ftt(134);}
"row_count" {return tt.ftt(135);}
"rpad" {return tt.ftt(136);}
"rtrim" {return tt.ftt(137);}
"second" {return tt.ftt(138);}
"second_microsecond" {return tt.ftt(139);}
"sec_to_time" {return tt.ftt(140);}
"session_user" {return tt.ftt(141);}
"sha" {return tt.ftt(142);}
"sha1" {return tt.ftt(143);}
"sha2" {return tt.ftt(144);}
"sign" {return tt.ftt(145);}
"sin" {return tt.ftt(146);}
"sleep" {return tt.ftt(147);}
"soundex" {return tt.ftt(148);}
"sounds_like" {return tt.ftt(149);}
"space" {return tt.ftt(150);}
"sqrt" {return tt.ftt(151);}
"std" {return tt.ftt(152);}
"stddev" {return tt.ftt(153);}
"stddev_pop" {return tt.ftt(154);}
"stddev_samp" {return tt.ftt(155);}
"strcmp" {return tt.ftt(156);}
"str_to_date" {return tt.ftt(157);}
"subdate" {return tt.ftt(158);}
"substr" {return tt.ftt(159);}
"substring" {return tt.ftt(160);}
"substring_index" {return tt.ftt(161);}
"subtime" {return tt.ftt(162);}
"sum" {return tt.ftt(163);}
"sysdate" {return tt.ftt(164);}
"system_user" {return tt.ftt(165);}
"tan" {return tt.ftt(166);}
"timediff" {return tt.ftt(167);}
"timestampadd" {return tt.ftt(168);}
"timestampdiff" {return tt.ftt(169);}
"time_format" {return tt.ftt(170);}
"time_to_sec" {return tt.ftt(171);}
"to_days" {return tt.ftt(172);}
"trim" {return tt.ftt(173);}
"ucase" {return tt.ftt(174);}
"uncompress" {return tt.ftt(175);}
"uncompressed_length" {return tt.ftt(176);}
"unhex" {return tt.ftt(177);}
"unix_timestamp" {return tt.ftt(178);}
"upper" {return tt.ftt(179);}
"user" {return tt.ftt(180);}
"utc_date" {return tt.ftt(181);}
"utc_time" {return tt.ftt(182);}
"utc_timestamp" {return tt.ftt(183);}
"uuid" {return tt.ftt(184);}
"uuid_short" {return tt.ftt(185);}
"variance" {return tt.ftt(186);}
"var_pop" {return tt.ftt(187);}
"var_samp" {return tt.ftt(188);}
"version" {return tt.ftt(189);}
"week" {return tt.ftt(190);}
"weekday" {return tt.ftt(191);}
"weekofyear" {return tt.ftt(192);}
"weight_string" {return tt.ftt(193);}
"yearweek" {return tt.ftt(194);}
"year_month" {return tt.ftt(195);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
"autoextend_size" {return tt.ptt(0);}
"auto_increment" {return tt.ptt(1);}
"avg_row_length" {return tt.ptt(2);}
"challenge_response" {return tt.ptt(3);}
"column_format" {return tt.ptt(4);}
"delay_key_write" {return tt.ptt(5);}
"engine_attribute" {return tt.ptt(6);}
"failed_login_attempts" {return tt.ptt(7);}
"insert_method" {return tt.ptt(8);}
"initial_size" {return tt.ptt(9);}
"key_block_size" {return tt.ptt(10);}
"low_ignore" {return tt.ptt(11);}
"low_priority" {return tt.ptt(12);}
"master_ssl_verify_server_cert" {return tt.ptt(13);}
"max_connections_per_hour" {return tt.ptt(14);}
"max_rows" {return tt.ptt(15);}
"max_queries_per_hour" {return tt.ptt(16);}
"max_updates_per_hour" {return tt.ptt(17);}
"max_user_connections" {return tt.ptt(18);}
"min_rows" {return tt.ptt(19);}
"no_write_to_binlog" {return tt.ptt(20);}
"pack_keys" {return tt.ptt(21);}
"password_lock_time" {return tt.ptt(22);}
"row_format" {return tt.ptt(23);}
"secondary_engine_attribute" {return tt.ptt(24);}
"stats_auto_recalc" {return tt.ptt(25);}
"stats_persistent" {return tt.ptt(26);}
"stats_sample_pages" {return tt.ptt(27);}
"thread_priority" {return tt.ptt(28);}
"vcpu" {return tt.ptt(29);}
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }
