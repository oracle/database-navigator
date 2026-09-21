package com.dbn.language.psql.dialect.oracle;

import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageLexerBase;
import com.intellij.psi.tree.IElementType;

%%

%class OraclePLSQLParserFlexLexer
%extends DBLanguageLexerBase
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    public OraclePLSQLParserFlexLexer(TokenTypeBundle tt) {
      super(tt);
  }
%}

%include ../../../common/lexer/shared_elements.flext
%include ../../../common/lexer/shared_elements_oracle.flext

VARIABLE = ":"{INTEGER}
SQLP_VARIABLE = "&""&"?{IDENTIFIER}
DBLINK_QUALIFIER = "@"({IDENTIFIER}|{QUOTED_IDENTIFIER})("."({IDENTIFIER}|{QUOTED_IDENTIFIER}))*

%state WRAPPED
%state CONDITIONAL
%%

<WRAPPED> {
    {WHITE_SPACE}   { return stt.whiteSpace; }
    .*              { return stt.lineComment; }
    .               { return stt.lineComment; }
}
<CONDITIONAL> {
    "$end"          { yybegin(YYINITIAL); }
}

{BLOCK_COMMENT}  { return stt.blockComment; }
{LINE_COMMENT}   { return stt.lineComment; }

"wrapped"          { yybegin(WRAPPED); return tt.getTokenType("KW_WRAPPED");}
"$if"(~"$then")    { yybegin(CONDITIONAL);}
"$elsif"(~"$then") { }
"$else"            { }
"$then"            { }

{VARIABLE}         { return stt.variable; }
{SQLP_VARIABLE}    { return stt.variable; }
{DBLINK_QUALIFIER} { return tt.getTokenType("CT_DBLINK_QUALIFIER"); }


{INTEGER}     { return stt.integer; }
{NUMBER}      { return stt.number; }
{STRING}      { return stt.string; }

"("{wso}"+"{wso}")"  {return tt.getTokenType("CT_OUTER_JOIN");}

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
"bfile" {return tt.dtt(0);}
"binary_double" {return tt.dtt(1);}
"binary_float" {return tt.dtt(2);}
"binary_integer" {return tt.dtt(3);}
"blob" {return tt.dtt(4);}
"boolean" {return tt.dtt(5);}
"byte" {return tt.dtt(6);}
"char" {return tt.dtt(7);}
"character" {return tt.dtt(8);}
"clob" {return tt.dtt(9);}
"date" {return tt.dtt(10);}
"decimal" {return tt.dtt(11);}
"float" {return tt.dtt(12);}
"float32" {return tt.dtt(13);}
"float64" {return tt.dtt(14);}
"int" {return tt.dtt(15);}
"int8" {return tt.dtt(16);}
"integer" {return tt.dtt(17);}
"long" {return tt.dtt(18);}
"nchar" {return tt.dtt(19);}
"nclob" {return tt.dtt(20);}
"number" {return tt.dtt(21);}
"numeric" {return tt.dtt(22);}
"nvarchar2" {return tt.dtt(23);}
"pls_integer" {return tt.dtt(24);}
"raw" {return tt.dtt(25);}
"real" {return tt.dtt(26);}
"rowid" {return tt.dtt(27);}
"smallint" {return tt.dtt(28);}
"string" {return tt.dtt(29);}
"timestamp" {return tt.dtt(30);}
"urowid" {return tt.dtt(31);}
"varchar" {return tt.dtt(32);}
"varchar2" {return tt.dtt(33);}
"vector" {return tt.dtt(34);}
// MARKER_END_DATATYPES



// MARKER_BEGIN_KEYWORDS
"$else" {return tt.ktt(0);}
"$elsif" {return tt.ktt(1);}
"$end" {return tt.ktt(2);}
"$if" {return tt.ktt(3);}
"$then" {return tt.ktt(4);}
"a set" {return tt.ktt(5);}
"absent" {return tt.ktt(6);}
"accessible" {return tt.ktt(7);}
"after" {return tt.ktt(8);}
"agent" {return tt.ktt(9);}
"aggregate" {return tt.ktt(10);}
"all" {return tt.ktt(11);}
"alter" {return tt.ktt(12);}
"analyze" {return tt.ktt(13);}
"and" {return tt.ktt(14);}
"any" {return tt.ktt(15);}
"apply" {return tt.ktt(16);}
"array" {return tt.ktt(17);}
"asc" {return tt.ktt(18);}
"associate" {return tt.ktt(19);}
"as" {return tt.ktt(20);}
"at" {return tt.ktt(21);}
"audit" {return tt.ktt(22);}
"authid" {return tt.ktt(23);}
"automatic" {return tt.ktt(24);}
"autonomous_transaction" {return tt.ktt(25);}
"before" {return tt.ktt(26);}
"begin" {return tt.ktt(27);}
"between" {return tt.ktt(28);}
"block" {return tt.ktt(29);}
"body" {return tt.ktt(30);}
"both" {return tt.ktt(31);}
"bulk" {return tt.ktt(32);}
"bulk_exceptions" {return tt.ktt(33);}
"bulk_rowcount" {return tt.ktt(34);}
"by" {return tt.ktt(35);}
"c" {return tt.ktt(36);}
"call" {return tt.ktt(37);}
"canonical" {return tt.ktt(38);}
"case" {return tt.ktt(39);}
"charsetform" {return tt.ktt(40);}
"charsetid" {return tt.ktt(41);}
"char_base" {return tt.ktt(42);}
"char_cs" {return tt.ktt(43);}
"check" {return tt.ktt(44);}
"chisq_df" {return tt.ktt(45);}
"chisq_obs" {return tt.ktt(46);}
"chisq_sig" {return tt.ktt(47);}
"clone" {return tt.ktt(48);}
"close" {return tt.ktt(49);}
"cluster" {return tt.ktt(50);}
"coalesce" {return tt.ktt(51);}
"coefficient" {return tt.ktt(52);}
"cohens_k" {return tt.ktt(53);}
"collation" {return tt.ktt(54);}
"collect" {return tt.ktt(55);}
"columns" {return tt.ktt(56);}
"comment" {return tt.ktt(57);}
"commit" {return tt.ktt(58);}
"committed" {return tt.ktt(59);}
"compatibility" {return tt.ktt(60);}
"compound" {return tt.ktt(61);}
"compress" {return tt.ktt(62);}
"conditional" {return tt.ktt(63);}
"connect" {return tt.ktt(64);}
"constant" {return tt.ktt(65);}
"constraint" {return tt.ktt(66);}
"constructor" {return tt.ktt(67);}
"container" {return tt.ktt(68);}
"content" {return tt.ktt(69);}
"context" {return tt.ktt(70);}
"continue" {return tt.ktt(71);}
"cont_coefficient" {return tt.ktt(72);}
"conversion" {return tt.ktt(73);}
"count" {return tt.ktt(74);}
"cramers_v" {return tt.ktt(75);}
"create" {return tt.ktt(76);}
"cross" {return tt.ktt(77);}
"crossedition" {return tt.ktt(78);}
"cube" {return tt.ktt(79);}
"current_user" {return tt.ktt(80);}
"current" {return tt.ktt(81);}
"currval" {return tt.ktt(82);}
"cursor" {return tt.ktt(83);}
"database" {return tt.ktt(84);}
"day" {return tt.ktt(85);}
"db_role_change" {return tt.ktt(86);}
"ddl" {return tt.ktt(87);}
"declare" {return tt.ktt(88);}
"decrement" {return tt.ktt(89);}
"defaults" {return tt.ktt(90);}
"default" {return tt.ktt(91);}
"definer" {return tt.ktt(92);}
"delete" {return tt.ktt(93);}
"deleting" {return tt.ktt(94);}
"dense_rank" {return tt.ktt(95);}
"deprecate" {return tt.ktt(96);}
"desc" {return tt.ktt(97);}
"deterministic" {return tt.ktt(98);}
"df" {return tt.ktt(99);}
"df_between" {return tt.ktt(100);}
"df_den" {return tt.ktt(101);}
"df_num" {return tt.ktt(102);}
"df_within" {return tt.ktt(103);}
"dimension" {return tt.ktt(104);}
"disable" {return tt.ktt(105);}
"disassociate" {return tt.ktt(106);}
"distinct" {return tt.ktt(107);}
"do" {return tt.ktt(108);}
"document" {return tt.ktt(109);}
"double" {return tt.ktt(110);}
"drop" {return tt.ktt(111);}
"dump" {return tt.ktt(112);}
"duration" {return tt.ktt(113);}
"each" {return tt.ktt(114);}
"editionable" {return tt.ktt(115);}
"else" {return tt.ktt(116);}
"elsif" {return tt.ktt(117);}
"empty" {return tt.ktt(118);}
"enable" {return tt.ktt(119);}
"encoding" {return tt.ktt(120);}
"end" {return tt.ktt(121);}
"entityescaping" {return tt.ktt(122);}
"equals_path" {return tt.ktt(123);}
"error" {return tt.ktt(124);}
"errors" {return tt.ktt(125);}
"error_code" {return tt.ktt(126);}
"error_index" {return tt.ktt(127);}
"escape" {return tt.ktt(128);}
"evalname" {return tt.ktt(129);}
"exact_prob" {return tt.ktt(130);}
"except" {return tt.ktt(131);}
"exceptions" {return tt.ktt(132);}
"exception_init" {return tt.ktt(133);}
"exception" {return tt.ktt(134);}
"exclude" {return tt.ktt(135);}
"exclusive" {return tt.ktt(136);}
"execute" {return tt.ktt(137);}
"exists" {return tt.ktt(138);}
"exit" {return tt.ktt(139);}
"extend" {return tt.ktt(140);}
"extends" {return tt.ktt(141);}
"external" {return tt.ktt(142);}
"false" {return tt.ktt(143);}
"fetch" {return tt.ktt(144);}
"final" {return tt.ktt(145);}
"first" {return tt.ktt(146);}
"following" {return tt.ktt(147);}
"follows" {return tt.ktt(148);}
"forall" {return tt.ktt(149);}
"force" {return tt.ktt(150);}
"format" {return tt.ktt(151);}
"forward" {return tt.ktt(152);}
"for" {return tt.ktt(153);}
"found" {return tt.ktt(154);}
"from" {return tt.ktt(155);}
"full" {return tt.ktt(156);}
"function" {return tt.ktt(157);}
"f_ratio" {return tt.ktt(158);}
"goto" {return tt.ktt(159);}
"grant" {return tt.ktt(160);}
"group" {return tt.ktt(161);}
"hash" {return tt.ktt(162);}
"having" {return tt.ktt(163);}
"heap" {return tt.ktt(164);}
"hide" {return tt.ktt(165);}
"hour" {return tt.ktt(166);}
"if" {return tt.ktt(167);}
"ignore" {return tt.ktt(168);}
"immediate" {return tt.ktt(169);}
"include" {return tt.ktt(170);}
"increment" {return tt.ktt(171);}
"indent" {return tt.ktt(172);}
"index" {return tt.ktt(173);}
"indicator" {return tt.ktt(174);}
"indices" {return tt.ktt(175);}
"infinite" {return tt.ktt(176);}
"inline" {return tt.ktt(177);}
"inner" {return tt.ktt(178);}
"inserting" {return tt.ktt(179);}
"insert" {return tt.ktt(180);}
"instantiable" {return tt.ktt(181);}
"instead" {return tt.ktt(182);}
"interface" {return tt.ktt(183);}
"intersect" {return tt.ktt(184);}
"interval" {return tt.ktt(185);}
"into" {return tt.ktt(186);}
"in" {return tt.ktt(187);}
"isolation" {return tt.ktt(188);}
"isopen" {return tt.ktt(189);}
"is" {return tt.ktt(190);}
"iterate" {return tt.ktt(191);}
"java" {return tt.ktt(192);}
"join" {return tt.ktt(193);}
"json" {return tt.ktt(194);}
"keep" {return tt.ktt(195);}
"key" {return tt.ktt(196);}
"keys" {return tt.ktt(197);}
"language" {return tt.ktt(198);}
"last" {return tt.ktt(199);}
"leading" {return tt.ktt(200);}
"left" {return tt.ktt(201);}
"level" {return tt.ktt(202);}
"library" {return tt.ktt(203);}
"like2" {return tt.ktt(204);}
"like4" {return tt.ktt(205);}
"likec" {return tt.ktt(206);}
"like" {return tt.ktt(207);}
"limit" {return tt.ktt(208);}
"limited" {return tt.ktt(209);}
"local" {return tt.ktt(210);}
"locked" {return tt.ktt(211);}
"lock" {return tt.ktt(212);}
"log" {return tt.ktt(213);}
"logoff" {return tt.ktt(214);}
"logon" {return tt.ktt(215);}
"loop" {return tt.ktt(216);}
"main" {return tt.ktt(217);}
"map" {return tt.ktt(218);}
"matched" {return tt.ktt(219);}
"maxlen" {return tt.ktt(220);}
"maxvalue" {return tt.ktt(221);}
"mean_squares_between" {return tt.ktt(222);}
"mean_squares_within" {return tt.ktt(223);}
"measures" {return tt.ktt(224);}
"member" {return tt.ktt(225);}
"merge" {return tt.ktt(226);}
"metadata" {return tt.ktt(227);}
"minus" {return tt.ktt(228);}
"minute" {return tt.ktt(229);}
"minvalue" {return tt.ktt(230);}
"mismatch" {return tt.ktt(231);}
"mlslabel" {return tt.ktt(232);}
"model" {return tt.ktt(233);}
"mode" {return tt.ktt(234);}
"month" {return tt.ktt(235);}
"multiset" {return tt.ktt(236);}
"name" {return tt.ktt(237);}
"nan" {return tt.ktt(238);}
"national" {return tt.ktt(239);}
"natural" {return tt.ktt(240);}
"naturaln" {return tt.ktt(241);}
"nav" {return tt.ktt(242);}
"nchar_cs" {return tt.ktt(243);}
"nested" {return tt.ktt(244);}
"new" {return tt.ktt(245);}
"next" {return tt.ktt(246);}
"nextval" {return tt.ktt(247);}
"no" {return tt.ktt(248);}
"noaudit" {return tt.ktt(249);}
"nocopy" {return tt.ktt(250);}
"nocycle" {return tt.ktt(251);}
"noentityescaping" {return tt.ktt(252);}
"none" {return tt.ktt(253);}
"noneditionable" {return tt.ktt(254);}
"noschemacheck" {return tt.ktt(255);}
"notfound" {return tt.ktt(256);}
"not" {return tt.ktt(257);}
"nowait" {return tt.ktt(258);}
"nulls" {return tt.ktt(259);}
"null" {return tt.ktt(260);}
"number_base" {return tt.ktt(261);}
"object" {return tt.ktt(262);}
"ocirowid" {return tt.ktt(263);}
"offset" {return tt.ktt(264);}
"of" {return tt.ktt(265);}
"oid" {return tt.ktt(266);}
"old" {return tt.ktt(267);}
"one_sided_prob_or_less" {return tt.ktt(268);}
"one_sided_prob_or_more" {return tt.ktt(269);}
"one_sided_sig" {return tt.ktt(270);}
"only" {return tt.ktt(271);}
"on" {return tt.ktt(272);}
"opaque" {return tt.ktt(273);}
"open" {return tt.ktt(274);}
"operator" {return tt.ktt(275);}
"option" {return tt.ktt(276);}
"order" {return tt.ktt(277);}
"ordinality" {return tt.ktt(278);}
"organization" {return tt.ktt(279);}
"or" {return tt.ktt(280);}
"others" {return tt.ktt(281);}
"out" {return tt.ktt(282);}
"outer" {return tt.ktt(283);}
"over" {return tt.ktt(284);}
"overflow" {return tt.ktt(285);}
"overlaps" {return tt.ktt(286);}
"overriding" {return tt.ktt(287);}
"package" {return tt.ktt(288);}
"parallel_enable" {return tt.ktt(289);}
"parameters" {return tt.ktt(290);}
"parent" {return tt.ktt(291);}
"partition" {return tt.ktt(292);}
"passing" {return tt.ktt(293);}
"path" {return tt.ktt(294);}
"pctfree" {return tt.ktt(295);}
"percent" {return tt.ktt(296);}
"period" {return tt.ktt(297);}
"phi_coefficient" {return tt.ktt(298);}
"pipe" {return tt.ktt(299);}
"pipelined" {return tt.ktt(300);}
"pivot" {return tt.ktt(301);}
"pluggable" {return tt.ktt(302);}
"positive" {return tt.ktt(303);}
"positiven" {return tt.ktt(304);}
"power" {return tt.ktt(305);}
"pragma" {return tt.ktt(306);}
"precedes" {return tt.ktt(307);}
"preceding" {return tt.ktt(308);}
"precision" {return tt.ktt(309);}
"present" {return tt.ktt(310);}
"pretty" {return tt.ktt(311);}
"prior" {return tt.ktt(312);}
"private" {return tt.ktt(313);}
"procedure" {return tt.ktt(314);}
"public" {return tt.ktt(315);}
"raise" {return tt.ktt(316);}
"range" {return tt.ktt(317);}
"read" {return tt.ktt(318);}
"record" {return tt.ktt(319);}
"ref" {return tt.ktt(320);}
"reference" {return tt.ktt(321);}
"referencing" {return tt.ktt(322);}
"regexp_like" {return tt.ktt(323);}
"reject" {return tt.ktt(324);}
"release" {return tt.ktt(325);}
"relies_on" {return tt.ktt(326);}
"remainder" {return tt.ktt(327);}
"rename" {return tt.ktt(328);}
"replace" {return tt.ktt(329);}
"restrict_references" {return tt.ktt(330);}
"result" {return tt.ktt(331);}
"result_cache" {return tt.ktt(332);}
"return" {return tt.ktt(333);}
"returning" {return tt.ktt(334);}
"reverse" {return tt.ktt(335);}
"revoke" {return tt.ktt(336);}
"right" {return tt.ktt(337);}
"rnds" {return tt.ktt(338);}
"rnps" {return tt.ktt(339);}
"rollback" {return tt.ktt(340);}
"rollup" {return tt.ktt(341);}
"rowcount" {return tt.ktt(342);}
"rownum" {return tt.ktt(343);}
"rows" {return tt.ktt(344);}
"rowtype" {return tt.ktt(345);}
"row" {return tt.ktt(346);}
"rules" {return tt.ktt(347);}
"sample" {return tt.ktt(348);}
"save" {return tt.ktt(349);}
"savepoint" {return tt.ktt(350);}
"schema" {return tt.ktt(351);}
"schemacheck" {return tt.ktt(352);}
"scn" {return tt.ktt(353);}
"second" {return tt.ktt(354);}
"seed" {return tt.ktt(355);}
"segment" {return tt.ktt(356);}
"select" {return tt.ktt(357);}
"self" {return tt.ktt(358);}
"separate" {return tt.ktt(359);}
"sequence" {return tt.ktt(360);}
"sequential" {return tt.ktt(361);}
"serializable" {return tt.ktt(362);}
"serially_reusable" {return tt.ktt(363);}
"servererror" {return tt.ktt(364);}
"sets" {return tt.ktt(365);}
"set" {return tt.ktt(366);}
"share" {return tt.ktt(367);}
"sharing" {return tt.ktt(368);}
"show" {return tt.ktt(369);}
"shutdown" {return tt.ktt(370);}
"siblings" {return tt.ktt(371);}
"sig" {return tt.ktt(372);}
"single" {return tt.ktt(373);}
"size" {return tt.ktt(374);}
"skip" {return tt.ktt(375);}
"some" {return tt.ktt(376);}
"space" {return tt.ktt(377);}
"sqlcode" {return tt.ktt(378);}
"sqlerrm" {return tt.ktt(379);}
"sql" {return tt.ktt(380);}
"standalone" {return tt.ktt(381);}
"startup" {return tt.ktt(382);}
"start" {return tt.ktt(383);}
"statement" {return tt.ktt(384);}
"static" {return tt.ktt(385);}
"statistic" {return tt.ktt(386);}
"statistics" {return tt.ktt(387);}
"strict" {return tt.ktt(388);}
"struct" {return tt.ktt(389);}
"submultiset" {return tt.ktt(390);}
"subpartition" {return tt.ktt(391);}
"subtype" {return tt.ktt(392);}
"successful" {return tt.ktt(393);}
"sum_squares_between" {return tt.ktt(394);}
"sum_squares_within" {return tt.ktt(395);}
"suspend" {return tt.ktt(396);}
"synonym" {return tt.ktt(397);}
"table" {return tt.ktt(398);}
"tdo" {return tt.ktt(399);}
"then" {return tt.ktt(400);}
"ties" {return tt.ktt(401);}
"time" {return tt.ktt(402);}
"timezone_abbr" {return tt.ktt(403);}
"timezone_hour" {return tt.ktt(404);}
"timezone_minute" {return tt.ktt(405);}
"timezone_region" {return tt.ktt(406);}
"to" {return tt.ktt(407);}
"trailing" {return tt.ktt(408);}
"transaction" {return tt.ktt(409);}
"trigger" {return tt.ktt(410);}
"true" {return tt.ktt(411);}
"truncate" {return tt.ktt(412);}
"trust" {return tt.ktt(413);}
"two_sided_prob" {return tt.ktt(414);}
"two_sided_sig" {return tt.ktt(415);}
"type" {return tt.ktt(416);}
"unbounded" {return tt.ktt(417);}
"unconditional" {return tt.ktt(418);}
"under" {return tt.ktt(419);}
"under_path" {return tt.ktt(420);}
"union" {return tt.ktt(421);}
"unique" {return tt.ktt(422);}
"unlimited" {return tt.ktt(423);}
"unpivot" {return tt.ktt(424);}
"unplug" {return tt.ktt(425);}
"until" {return tt.ktt(426);}
"updated" {return tt.ktt(427);}
"update" {return tt.ktt(428);}
"updating" {return tt.ktt(429);}
"upsert" {return tt.ktt(430);}
"user" {return tt.ktt(431);}
"use" {return tt.ktt(432);}
"using" {return tt.ktt(433);}
"u_statistic" {return tt.ktt(434);}
"validate" {return tt.ktt(435);}
"value" {return tt.ktt(436);}
"values" {return tt.ktt(437);}
"variable" {return tt.ktt(438);}
"varray" {return tt.ktt(439);}
"varying" {return tt.ktt(440);}
"version" {return tt.ktt(441);}
"versions" {return tt.ktt(442);}
"view" {return tt.ktt(443);}
"wait" {return tt.ktt(444);}
"wellformed" {return tt.ktt(445);}
"whenever" {return tt.ktt(446);}
"when" {return tt.ktt(447);}
"where" {return tt.ktt(448);}
"while" {return tt.ktt(449);}
"within" {return tt.ktt(450);}
"without" {return tt.ktt(451);}
"with" {return tt.ktt(452);}
"wnds" {return tt.ktt(453);}
"wnps" {return tt.ktt(454);}
"work" {return tt.ktt(455);}
"wrapped" {return tt.ktt(456);}
"wrapper" {return tt.ktt(457);}
"write" {return tt.ktt(458);}
"xml" {return tt.ktt(459);}
"xmlnamespaces" {return tt.ktt(460);}
"xmltype" {return tt.ktt(461);}
"year" {return tt.ktt(462);}
"yes" {return tt.ktt(463);}
"zone" {return tt.ktt(464);}
// MARKER_END_KEYWORDS



// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.ftt(0);}
"acos" {return tt.ftt(1);}
"add_months" {return tt.ftt(2);}
"appendchildxml" {return tt.ftt(3);}
"ascii" {return tt.ftt(4);}
"asciistr" {return tt.ftt(5);}
"asin" {return tt.ftt(6);}
"atan" {return tt.ftt(7);}
"atan2" {return tt.ftt(8);}
"avg" {return tt.ftt(9);}
"bfilename" {return tt.ftt(10);}
"bin_to_num" {return tt.ftt(11);}
"bitand" {return tt.ftt(12);}
"cardinality" {return tt.ftt(13);}
"cast" {return tt.ftt(14);}
"ceil" {return tt.ftt(15);}
"chartorowid" {return tt.ftt(16);}
"chr" {return tt.ftt(17);}
"compose" {return tt.ftt(18);}
"concat" {return tt.ftt(19);}
"convert" {return tt.ftt(20);}
"corr" {return tt.ftt(21);}
"corr_k" {return tt.ftt(22);}
"corr_s" {return tt.ftt(23);}
"cos" {return tt.ftt(24);}
"cosh" {return tt.ftt(25);}
"covar_pop" {return tt.ftt(26);}
"covar_samp" {return tt.ftt(27);}
"cume_dist" {return tt.ftt(28);}
"current_date" {return tt.ftt(29);}
"current_timestamp" {return tt.ftt(30);}
"cv" {return tt.ftt(31);}
"dbtimezone" {return tt.ftt(32);}
"dbtmezone" {return tt.ftt(33);}
"decode" {return tt.ftt(34);}
"decompose" {return tt.ftt(35);}
"deletexml" {return tt.ftt(36);}
"depth" {return tt.ftt(37);}
"deref" {return tt.ftt(38);}
"empty_blob" {return tt.ftt(39);}
"empty_clob" {return tt.ftt(40);}
"existsnode" {return tt.ftt(41);}
"exp" {return tt.ftt(42);}
"extract" {return tt.ftt(43);}
"extractvalue" {return tt.ftt(44);}
"first_value" {return tt.ftt(45);}
"floor" {return tt.ftt(46);}
"from_tz" {return tt.ftt(47);}
"greatest" {return tt.ftt(48);}
"grouping" {return tt.ftt(49);}
"grouping_id" {return tt.ftt(50);}
"group_id" {return tt.ftt(51);}
"hextoraw" {return tt.ftt(52);}
"initcap" {return tt.ftt(53);}
"insertchildxml" {return tt.ftt(54);}
"insertchildxmlafter" {return tt.ftt(55);}
"insertchildxmlbefore" {return tt.ftt(56);}
"insertxmlafter" {return tt.ftt(57);}
"insertxmlbefore" {return tt.ftt(58);}
"instr" {return tt.ftt(59);}
"instr2" {return tt.ftt(60);}
"instr4" {return tt.ftt(61);}
"instrb" {return tt.ftt(62);}
"instrc" {return tt.ftt(63);}
"iteration_number" {return tt.ftt(64);}
"json_array" {return tt.ftt(65);}
"json_arrayagg" {return tt.ftt(66);}
"json_dataguide" {return tt.ftt(67);}
"json_object" {return tt.ftt(68);}
"json_objectagg" {return tt.ftt(69);}
"json_query" {return tt.ftt(70);}
"json_table" {return tt.ftt(71);}
"json_value" {return tt.ftt(72);}
"lag" {return tt.ftt(73);}
"last_day" {return tt.ftt(74);}
"last_value" {return tt.ftt(75);}
"lateral" {return tt.ftt(76);}
"lead" {return tt.ftt(77);}
"least" {return tt.ftt(78);}
"length" {return tt.ftt(79);}
"length2" {return tt.ftt(80);}
"length4" {return tt.ftt(81);}
"lengthb" {return tt.ftt(82);}
"lengthc" {return tt.ftt(83);}
"listagg" {return tt.ftt(84);}
"ln" {return tt.ftt(85);}
"lnnvl" {return tt.ftt(86);}
"localtimestamp" {return tt.ftt(87);}
"lower" {return tt.ftt(88);}
"lpad" {return tt.ftt(89);}
"ltrim" {return tt.ftt(90);}
"make_ref" {return tt.ftt(91);}
"max" {return tt.ftt(92);}
"median" {return tt.ftt(93);}
"min" {return tt.ftt(94);}
"mod" {return tt.ftt(95);}
"months_between" {return tt.ftt(96);}
"nanvl" {return tt.ftt(97);}
"nchr" {return tt.ftt(98);}
"new_time" {return tt.ftt(99);}
"next_day" {return tt.ftt(100);}
"nlssort" {return tt.ftt(101);}
"nls_charset_decl_len" {return tt.ftt(102);}
"nls_charset_id" {return tt.ftt(103);}
"nls_charset_name" {return tt.ftt(104);}
"nls_initcap" {return tt.ftt(105);}
"nls_lower" {return tt.ftt(106);}
"nls_upper" {return tt.ftt(107);}
"ntile" {return tt.ftt(108);}
"nullif" {return tt.ftt(109);}
"numtodsinterval" {return tt.ftt(110);}
"numtoyminterval" {return tt.ftt(111);}
"nvl" {return tt.ftt(112);}
"nvl2" {return tt.ftt(113);}
"ora_hash" {return tt.ftt(114);}
"percentile_cont" {return tt.ftt(115);}
"percentile_disc" {return tt.ftt(116);}
"percent_rank" {return tt.ftt(117);}
"powermultiset" {return tt.ftt(118);}
"powermultiset_by_cardinality" {return tt.ftt(119);}
"presentnnv" {return tt.ftt(120);}
"presentv" {return tt.ftt(121);}
"previous" {return tt.ftt(122);}
"rank" {return tt.ftt(123);}
"ratio_to_report" {return tt.ftt(124);}
"rawtohex" {return tt.ftt(125);}
"rawtonhex" {return tt.ftt(126);}
"reftohex" {return tt.ftt(127);}
"regexp_instr" {return tt.ftt(128);}
"regexp_replace" {return tt.ftt(129);}
"regexp_substr" {return tt.ftt(130);}
"regr_avgx" {return tt.ftt(131);}
"regr_avgy" {return tt.ftt(132);}
"regr_count" {return tt.ftt(133);}
"regr_intercept" {return tt.ftt(134);}
"regr_r2" {return tt.ftt(135);}
"regr_slope" {return tt.ftt(136);}
"regr_sxx" {return tt.ftt(137);}
"regr_sxy" {return tt.ftt(138);}
"regr_syy" {return tt.ftt(139);}
"round" {return tt.ftt(140);}
"rowidtochar" {return tt.ftt(141);}
"rowidtonchar" {return tt.ftt(142);}
"row_number" {return tt.ftt(143);}
"rpad" {return tt.ftt(144);}
"rtrim" {return tt.ftt(145);}
"scn_to_timestamp" {return tt.ftt(146);}
"sessiontimezone" {return tt.ftt(147);}
"sign" {return tt.ftt(148);}
"sin" {return tt.ftt(149);}
"sinh" {return tt.ftt(150);}
"soundex" {return tt.ftt(151);}
"sqrt" {return tt.ftt(152);}
"stats_binomial_test" {return tt.ftt(153);}
"stats_crosstab" {return tt.ftt(154);}
"stats_f_test" {return tt.ftt(155);}
"stats_ks_test" {return tt.ftt(156);}
"stats_mode" {return tt.ftt(157);}
"stats_mw_test" {return tt.ftt(158);}
"stats_one_way_anova" {return tt.ftt(159);}
"stats_t_test_indep" {return tt.ftt(160);}
"stats_t_test_indepu" {return tt.ftt(161);}
"stats_t_test_one" {return tt.ftt(162);}
"stats_t_test_paired" {return tt.ftt(163);}
"stats_wsr_test" {return tt.ftt(164);}
"stddev" {return tt.ftt(165);}
"stddev_pop" {return tt.ftt(166);}
"stddev_samp" {return tt.ftt(167);}
"substr" {return tt.ftt(168);}
"substr2" {return tt.ftt(169);}
"substr4" {return tt.ftt(170);}
"substrb" {return tt.ftt(171);}
"substrc" {return tt.ftt(172);}
"sum" {return tt.ftt(173);}
"sysdate" {return tt.ftt(174);}
"systimestamp" {return tt.ftt(175);}
"sys_connect_by_path" {return tt.ftt(176);}
"sys_context" {return tt.ftt(177);}
"sys_dburigen" {return tt.ftt(178);}
"sys_extract_utc" {return tt.ftt(179);}
"sys_guid" {return tt.ftt(180);}
"sys_typeid" {return tt.ftt(181);}
"sys_xmlagg" {return tt.ftt(182);}
"sys_xmlgen" {return tt.ftt(183);}
"tan" {return tt.ftt(184);}
"tanh" {return tt.ftt(185);}
"timestamp_to_scn" {return tt.ftt(186);}
"to_binary_double" {return tt.ftt(187);}
"to_binary_float" {return tt.ftt(188);}
"to_char" {return tt.ftt(189);}
"to_clob" {return tt.ftt(190);}
"to_date" {return tt.ftt(191);}
"to_dsinterval" {return tt.ftt(192);}
"to_lob" {return tt.ftt(193);}
"to_multi_byte" {return tt.ftt(194);}
"to_nchar" {return tt.ftt(195);}
"to_nclob" {return tt.ftt(196);}
"to_number" {return tt.ftt(197);}
"to_single_byte" {return tt.ftt(198);}
"to_timestamp" {return tt.ftt(199);}
"to_timestamp_tz" {return tt.ftt(200);}
"to_yminterval" {return tt.ftt(201);}
"translate" {return tt.ftt(202);}
"treat" {return tt.ftt(203);}
"trim" {return tt.ftt(204);}
"trunc" {return tt.ftt(205);}
"tz_offset" {return tt.ftt(206);}
"uid" {return tt.ftt(207);}
"unistr" {return tt.ftt(208);}
"updatexml" {return tt.ftt(209);}
"upper" {return tt.ftt(210);}
"userenv" {return tt.ftt(211);}
"validate_conversion" {return tt.ftt(212);}
"variance" {return tt.ftt(213);}
"var_pop" {return tt.ftt(214);}
"var_samp" {return tt.ftt(215);}
"vsize" {return tt.ftt(216);}
"width_bucket" {return tt.ftt(217);}
"xmlagg" {return tt.ftt(218);}
"xmlattributes" {return tt.ftt(219);}
"xmlcast" {return tt.ftt(220);}
"xmlcdata" {return tt.ftt(221);}
"xmlcolattval" {return tt.ftt(222);}
"xmlcomment" {return tt.ftt(223);}
"xmlconcat" {return tt.ftt(224);}
"xmldiff" {return tt.ftt(225);}
"xmlelement" {return tt.ftt(226);}
"xmlexists" {return tt.ftt(227);}
"xmlforest" {return tt.ftt(228);}
"xmlisvalid" {return tt.ftt(229);}
"xmlparse" {return tt.ftt(230);}
"xmlpatch" {return tt.ftt(231);}
"xmlpi" {return tt.ftt(232);}
"xmlquery" {return tt.ftt(233);}
"xmlroot" {return tt.ftt(234);}
"xmlsequence" {return tt.ftt(235);}
"xmlserialize" {return tt.ftt(236);}
"xmltable" {return tt.ftt(237);}
"xmltransform" {return tt.ftt(238);}
// MARKER_END_FUNCTIONS



// MARKER_BEGIN_PARAMETERS
"using_nls_comp" {return tt.ptt(0);}
// MARKER_END_PARAMETERS

// MARKER_BEGIN_EXCEPTIONS
"access_into_null" {return tt.ett(0);}
"case_not_found" {return tt.ett(1);}
"collection_is_null" {return tt.ett(2);}
"cursor_already_open" {return tt.ett(3);}
"dup_val_on_index" {return tt.ett(4);}
"invalid_cursor" {return tt.ett(5);}
"invalid_number" {return tt.ett(6);}
"login_denied" {return tt.ett(7);}
"not_logged_on" {return tt.ett(8);}
"no_data_found" {return tt.ett(9);}
"program_error" {return tt.ett(10);}
"rowtype_mismatch" {return tt.ett(11);}
"self_is_null" {return tt.ett(12);}
"storage_error" {return tt.ett(13);}
"subscript_beyond_count" {return tt.ett(14);}
"subscript_outside_limit" {return tt.ett(15);}
"sys_invalid_rowid" {return tt.ett(16);}
"timeout_on_resource" {return tt.ett(17);}
"too_many_rows" {return tt.ett(18);}
"value_error" {return tt.ett(19);}
"zero_divide" {return tt.ett(20);}
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
{WHITE_SPACE}          { return stt.whiteSpace; }
.                      { return stt.identifier; }

