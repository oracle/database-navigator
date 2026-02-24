package com.dbn.language.sql.dialect.oracle;

import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageCompoundLexerBase;
import com.intellij.psi.tree.IElementType;

import static com.dbn.language.sql.dialect.oracle.OraclePLSQLBlockMonitor.Marker;

%%

%class OracleSQLParserFlexLexer
%extends DBLanguageCompoundLexerBase
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    private final OraclePLSQLBlockMonitor pbm = new OraclePLSQLBlockMonitor(this, YYINITIAL, PSQL_BLOCK);

    public OracleSQLParserFlexLexer(TokenTypeBundle tt) {
        super(tt, DBLanguageDialectIdentifier.ORACLE_PLSQL);
    }

    public void setTokenStart(int tokenStart) {
        zzStartRead = tokenStart;
    }

    public int getCurrentPosition() {
        return zzCurrentPos;
    }

    public String getCurrentToken() {
        return ((String) zzBuffer).substring(zzStartRead, zzMarkedPos);
    }
%}


%include ../../../common/lexer/shared_elements.flext
%include ../../../common/lexer/shared_elements_oracle.flext

NON_PSQL_BLOCK_ENTER = ("grant"|"revoke"){ws}"create"
NON_PSQL_BLOCK_EXIT = "to"|"from"|";"

PSQL_STUB_OR_REPLACE = ({ws}"or"{ws}"replace")?
PSQL_STUB_EDITIONABLE = ({ws}("editionable"|"editioning"|'noneditionable'))?
PSQL_STUB_FORCE = ({ws}("no"{ws})?"force")?
PSQL_STUB_PUBLIC = ({ws}"public")?
PSQL_STUB_PROGRAM = {ws}("package"|"trigger"|"function"|"procedure"|"type")
PSQL_STUB_IDENTIFIER = ({ws}({IDENTIFIER}|{QUOTED_IDENTIFIER}))*

PSQL_BLOCK_START_CREATE = "create"{PSQL_STUB_OR_REPLACE}{PSQL_STUB_FORCE}{PSQL_STUB_EDITIONABLE}{PSQL_STUB_PUBLIC}{PSQL_STUB_PROGRAM}
PSQL_BLOCK_START_DECLARE = "declare"
PSQL_BLOCK_START_BEGIN = "begin"
//PSQL_BLOCK_END_IGNORE = "end"{ws}("if"|"loop"|"case"){PSQL_STUB_IDENTIFIER}{wso}";"
PSQL_BLOCK_END_IGNORE = "end"{ws}("if"|"loop"){PSQL_STUB_IDENTIFIER}{wso}";"
PSQL_BLOCK_END = "end"{PSQL_STUB_IDENTIFIER}({wso}";"({wso}"/")?)?

CT_SIZE_CLAUSE = {INTEGER}{wso}("k"|"m"|"g"|"t"|"p"|"e"){ws}
SELECT_AI_START = "select"{ws}"ai"

VARIABLE = ":"({IDENTIFIER}|{INTEGER})
SQLP_VARIABLE = "&""&"?({IDENTIFIER}|{INTEGER})
VARIABLE_IDENTIFIER={IDENTIFIER}"&""&"?({IDENTIFIER}|{INTEGER})|"<"{IDENTIFIER}({ws}{IDENTIFIER})*">"

%state PSQL_BLOCK
%state NON_PSQL_BLOCK
%state SELECT_AI
%%

<PSQL_BLOCK> {
    {BLOCK_COMMENT}                 {}
    {LINE_COMMENT}                  {}

    {PSQL_BLOCK_START_CREATE}       {if (pbm.isBlockStarted()) { pbm.pushBack(); pbm.end(true); return getChameleon(); }}
    {PSQL_BLOCK_END_IGNORE}         { pbm.ignore();}
    {PSQL_BLOCK_END}                { if (pbm.end(false)) return getChameleon();}

    "begin"                         { pbm.mark(Marker.BEGIN); }
    "type"{ws}{IDENTIFIER}          { pbm.mark(Marker.PROGRAM); }
    "function"{ws}{IDENTIFIER}      { pbm.mark(Marker.PROGRAM); }
    "procedure"{ws}{IDENTIFIER}     { pbm.mark(Marker.PROGRAM); }
    "trigger"{ws}{IDENTIFIER}       { pbm.mark(Marker.PROGRAM); }
    "case"                          { pbm.mark(Marker.CASE); }

    {IDENTIFIER}                    {}
    {INTEGER}                       {}
    {NUMBER}                        {}
    {STRING}                        {}
    {WHITE_SPACE}                   {}
    .                               {}
    <<EOF>>                         { pbm.end(true); return getChameleon(); }
}

<NON_PSQL_BLOCK> {
    {NON_PSQL_BLOCK_EXIT}          { yybegin(YYINITIAL); pbm.pushBack(); }
}


<YYINITIAL> {
    {NON_PSQL_BLOCK_ENTER}         { yybegin(NON_PSQL_BLOCK); pbm.pushBack(); }

    {PSQL_BLOCK_START_CREATE}      { pbm.start(Marker.CREATE); }
    {PSQL_BLOCK_START_DECLARE}     { pbm.start(Marker.DECLARE); }
    {PSQL_BLOCK_START_BEGIN}       { pbm.start(Marker.BEGIN); }

    {SELECT_AI_START}              { yybegin(SELECT_AI); yypushback(yylength()); }
}

<SELECT_AI> {
    "select"           { return tt.getTokenType("KW_SELECT"); }
    "ai"               { return tt.getTokenType("KW_AI"); }
    "showprompt"       { return tt.getTokenType("KW_SHOWPROMPT"); }
    "showsql"          { return tt.getTokenType("KW_SHOWSQL"); }
    "explainsql"       { return tt.getTokenType("KW_EXPLAINSQL"); }
    "executesql"       { return tt.getTokenType("KW_EXECUTESQL"); }
    "narrate"          { return tt.getTokenType("KW_NARRATE"); }
    "chat"             { return tt.getTokenType("KW_CHAT"); }
    {STRING}           { yybegin(YYINITIAL); return stt.getString(); }   // string is allowed to have eols
    {eol}              { yybegin(YYINITIAL); return stt.getWhiteSpace();} // end of line -> exit the SELECT_AI block
    ";"                { yybegin(YYINITIAL); return stt.getChrSemicolon();}
    "/"                { yybegin(YYINITIAL); return stt.getChrSlash();}
    [^\r\n\t\f ;/]+     { return stt.getIdentifier();}
    {wsc}+             { return stt.getWhiteSpace(); }
}

<YYINITIAL, NON_PSQL_BLOCK> {

{BLOCK_COMMENT}        { return stt.getBlockComment(); }
{LINE_COMMENT}         { return stt.getLineComment(); }

{VARIABLE}             { return stt.getVariable(); }
{VARIABLE_IDENTIFIER}  { return stt.getIdentifier(); }
{SQLP_VARIABLE}        { return stt.getVariable(); }

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



"varchar2" {return tt.dtt(0);}
"bfile" {return tt.dtt(1);}
"binary_double" {return tt.dtt(2);}
"binary_float" {return tt.dtt(3);}
"blob" {return tt.dtt(4);}
"boolean" {return tt.dtt(5);}
"byte" {return tt.dtt(6);}
"char" {return tt.dtt(7);}
"character" {return tt.dtt(8);}
"character"{ws}"varying" {return tt.dtt(9);}
"clob" {return tt.dtt(10);}
"date" {return tt.dtt(11);}
"decimal" {return tt.dtt(12);}
"double"{ws}"precision" {return tt.dtt(13);}
"float" {return tt.dtt(14);}
"int" {return tt.dtt(15);}
"integer" {return tt.dtt(16);}
"interval" {return tt.dtt(17);}
"long" {return tt.dtt(18);}
"long"{ws}"raw" {return tt.dtt(19);}
"long"{ws}"varchar" {return tt.dtt(20);}
"national"{ws}"char" {return tt.dtt(21);}
"national"{ws}"char"{ws}"varying" {return tt.dtt(22);}
"national"{ws}"character" {return tt.dtt(23);}
"national"{ws}"character"{ws}"varying" {return tt.dtt(24);}
"nchar" {return tt.dtt(25);}
"nchar"{ws}"varying" {return tt.dtt(26);}
"nclob" {return tt.dtt(27);}
"number" {return tt.dtt(28);}
"numeric" {return tt.dtt(29);}
"nvarchar2" {return tt.dtt(30);}
"raw" {return tt.dtt(31);}
"real" {return tt.dtt(32);}
"rowid" {return tt.dtt(33);}
"smallint" {return tt.dtt(34);}
"timestamp" {return tt.dtt(35);}
"urowid" {return tt.dtt(36);}
"varchar" {return tt.dtt(37);}
"with"{ws}"local"{ws}"time"{ws}"zone" {return tt.dtt(38);}
"with"{ws}"time"{ws}"zone" {return tt.dtt(39);}



"a set" {return tt.ktt(0);}
"abort" {return tt.ktt(1);}
"absent" {return tt.ktt(2);}
"access" {return tt.ktt(3);}
"accessed" {return tt.ktt(4);}
"accessible" {return tt.ktt(5);}
"account" {return tt.ktt(6);}
"anyschema" {return tt.ktt(7);}
"archival" {return tt.ktt(8);}
"activate" {return tt.ktt(9);}
"active" {return tt.ktt(10);}
"add" {return tt.ktt(11);}
"admin" {return tt.ktt(12);}
"administer" {return tt.ktt(13);}
"advanced" {return tt.ktt(14);}
"advise" {return tt.ktt(15);}
"advisor" {return tt.ktt(16);}
"after" {return tt.ktt(17);}
"agent" {return tt.ktt(18);}
"ai" {return tt.ktt(19);}
"alias" {return tt.ktt(20);}
"all" {return tt.ktt(21);}
"allocate" {return tt.ktt(22);}
"allow" {return tt.ktt(23);}
"alter" {return tt.ktt(24);}
"always" {return tt.ktt(25);}
"analyze" {return tt.ktt(26);}
"ancillary" {return tt.ktt(27);}
"and" {return tt.ktt(28);}
"annotations" {return tt.ktt(29);}
"any" {return tt.ktt(30);}
"apply" {return tt.ktt(31);}
"archive" {return tt.ktt(32);}
"archivelog" {return tt.ktt(33);}
"array" {return tt.ktt(34);}
"as" {return tt.ktt(35);}
"asc" {return tt.ktt(36);}
"asynchronous" {return tt.ktt(37);}
"assembly" {return tt.ktt(38);}
"at" {return tt.ktt(39);}
"attribute" {return tt.ktt(40);}
"attributes" {return tt.ktt(41);}
"audit" {return tt.ktt(42);}
"authid" {return tt.ktt(43);}
"authentication" {return tt.ktt(44);}
"auto" {return tt.ktt(45);}
"autoextend" {return tt.ktt(46);}
"automatic" {return tt.ktt(47);}
"availability" {return tt.ktt(48);}
"backup" {return tt.ktt(49);}
"basic" {return tt.ktt(50);}
"basicfile" {return tt.ktt(51);}
"become" {return tt.ktt(52);}
"before" {return tt.ktt(53);}
"begin" {return tt.ktt(54);}
"beginning" {return tt.ktt(55);}
"bequeath" {return tt.ktt(56);}
"between" {return tt.ktt(57);}
"bigfile" {return tt.ktt(58);}
"binary" {return tt.ktt(59);}
"binding" {return tt.ktt(60);}
"bitmap" {return tt.ktt(61);}
"block" {return tt.ktt(62);}
"blockchain" {return tt.ktt(63);}
"body" {return tt.ktt(64);}
"both" {return tt.ktt(65);}
"buffer_cache" {return tt.ktt(66);}
"buffer_pool" {return tt.ktt(67);}
"build" {return tt.ktt(68);}
"by" {return tt.ktt(69);}
"cache" {return tt.ktt(70);}
"cancel" {return tt.ktt(71);}
"canonical" {return tt.ktt(72);}
"capacity" {return tt.ktt(73);}
"cascade" {return tt.ktt(74);}
"case" {return tt.ktt(75);}
"category" {return tt.ktt(76);}
"cell_flash_cache" {return tt.ktt(77);}
"change" {return tt.ktt(78);}
"char_cs" {return tt.ktt(79);}
"chat" {return tt.ktt(80);}
"check" {return tt.ktt(81);}
"checkpoint" {return tt.ktt(82);}
"child" {return tt.ktt(83);}
"chisq_df" {return tt.ktt(84);}
"chisq_obs" {return tt.ktt(85);}
"chisq_sig" {return tt.ktt(86);}
"chunk" {return tt.ktt(87);}
"class" {return tt.ktt(88);}
"clear" {return tt.ktt(89);}
"clone" {return tt.ktt(90);}
"close" {return tt.ktt(91);}
"cluster" {return tt.ktt(92);}
"clustering" {return tt.ktt(93);}
"coalesce" {return tt.ktt(94);}
"coarse" {return tt.ktt(95);}
"coefficient" {return tt.ktt(96);}
"cohens_k" {return tt.ktt(97);}
"collate" {return tt.ktt(98);}
"collation" {return tt.ktt(99);}
"column" {return tt.ktt(100);}
"column_value" {return tt.ktt(101);}
"columns" {return tt.ktt(102);}
"comment" {return tt.ktt(103);}
"commit" {return tt.ktt(104);}
"committed" {return tt.ktt(105);}
"compact" {return tt.ktt(106);}
"compatibility" {return tt.ktt(107);}
"compile" {return tt.ktt(108);}
"complete" {return tt.ktt(109);}
"compress" {return tt.ktt(110);}
"computation" {return tt.ktt(111);}
"compute" {return tt.ktt(112);}
"conditional" {return tt.ktt(113);}
"connect" {return tt.ktt(114);}
"consider" {return tt.ktt(115);}
"consistent" {return tt.ktt(116);}
"constraint" {return tt.ktt(117);}
"constraints" {return tt.ktt(118);}
"cont_coefficient" {return tt.ktt(119);}
"container" {return tt.ktt(120);}
"container_map" {return tt.ktt(121);}
"containers_default" {return tt.ktt(122);}
"content" {return tt.ktt(123);}
"contents" {return tt.ktt(124);}
"context" {return tt.ktt(125);}
"continue" {return tt.ktt(126);}
"controlfile" {return tt.ktt(127);}
"conversion" {return tt.ktt(128);}
"corruption" {return tt.ktt(129);}
"cost" {return tt.ktt(130);}
"cramers_v" {return tt.ktt(131);}
"create" {return tt.ktt(132);}
"creation" {return tt.ktt(133);}
"credential" {return tt.ktt(134);}
"critical" {return tt.ktt(135);}
"cross" {return tt.ktt(136);}
"cube" {return tt.ktt(137);}
"current" {return tt.ktt(138);}
"current_user" {return tt.ktt(139);}
"currval" {return tt.ktt(140);}
"cursor" {return tt.ktt(141);}
"cycle" {return tt.ktt(142);}
"data" {return tt.ktt(143);}
"database" {return tt.ktt(144);}
"datafile" {return tt.ktt(145);}
"datafiles" {return tt.ktt(146);}
"day" {return tt.ktt(147);}
"days" {return tt.ktt(148);}
"ddl" {return tt.ktt(149);}
"deallocate" {return tt.ktt(150);}
"debug" {return tt.ktt(151);}
"decrement" {return tt.ktt(152);}
"decrypt" {return tt.ktt(153);}
"default" {return tt.ktt(154);}
"defaults" {return tt.ktt(155);}
"deferrable" {return tt.ktt(156);}
"deferred" {return tt.ktt(157);}
"definer" {return tt.ktt(158);}
"definition" {return tt.ktt(159);}
"delay" {return tt.ktt(160);}
"delegate" {return tt.ktt(161);}
"delete" {return tt.ktt(162);}
"delete_all" {return tt.ktt(163);}
"demand" {return tt.ktt(164);}
"dense_rank" {return tt.ktt(165);}
"dequeue" {return tt.ktt(166);}
"desc" {return tt.ktt(167);}
"determines" {return tt.ktt(168);}
"df" {return tt.ktt(169);}
"df_between" {return tt.ktt(170);}
"df_den" {return tt.ktt(171);}
"df_num" {return tt.ktt(172);}
"df_within" {return tt.ktt(173);}
"dictionary" {return tt.ktt(174);}
"digest" {return tt.ktt(175);}
"dimension" {return tt.ktt(176);}
"directory" {return tt.ktt(177);}
"disable" {return tt.ktt(178);}
"disable_all" {return tt.ktt(179);}
"disallow" {return tt.ktt(180);}
"disconnect" {return tt.ktt(181);}
"disk" {return tt.ktt(182);}
"diskgroup" {return tt.ktt(183);}
"disks" {return tt.ktt(184);}
"dismount" {return tt.ktt(185);}
"distinct" {return tt.ktt(186);}
"distribute" {return tt.ktt(187);}
"distributed" {return tt.ktt(188);}
"dml" {return tt.ktt(189);}
"document" {return tt.ktt(190);}
"domain" {return tt.ktt(191);}
"downgrade" {return tt.ktt(192);}
"drop" {return tt.ktt(193);}
"duality" {return tt.ktt(194);}
"dump" {return tt.ktt(195);}
"duplicate" {return tt.ktt(196);}
"duplicated" {return tt.ktt(197);}
"edition" {return tt.ktt(198);}
"editions" {return tt.ktt(199);}
"editionable" {return tt.ktt(200);}
"editioning" {return tt.ktt(201);}
"element" {return tt.ktt(202);}
"else" {return tt.ktt(203);}
"empty" {return tt.ktt(204);}
"enable" {return tt.ktt(205);}
"enable_all" {return tt.ktt(206);}
"encoding" {return tt.ktt(207);}
"encrypt" {return tt.ktt(208);}
"end" {return tt.ktt(209);}
"enforced" {return tt.ktt(210);}
"entityescaping" {return tt.ktt(211);}
"entry" {return tt.ktt(212);}
"equals_path" {return tt.ktt(213);}
"error" {return tt.ktt(214);}
"errors" {return tt.ktt(215);}
"escape" {return tt.ktt(216);}
"etag" {return tt.ktt(217);}
"evalname" {return tt.ktt(218);}
"evaluate" {return tt.ktt(219);}
"evaluation" {return tt.ktt(220);}
"exact_prob" {return tt.ktt(221);}
"except" {return tt.ktt(222);}
"exceptions" {return tt.ktt(223);}
"exchange" {return tt.ktt(224);}
"exclude" {return tt.ktt(225);}
"excluding" {return tt.ktt(226);}
"exclusive" {return tt.ktt(227);}
"execute" {return tt.ktt(228);}
"executesql" {return tt.ktt(229);}
"exempt" {return tt.ktt(230);}
"exists" {return tt.ktt(231);}
"expire" {return tt.ktt(232);}
"explain" {return tt.ktt(233);}
"explainsql" {return tt.ktt(234);}
"export" {return tt.ktt(235);}
"extended" {return tt.ktt(236);}
"extends" {return tt.ktt(237);}
"extensions" {return tt.ktt(238);}
"extent" {return tt.ktt(239);}
"external" {return tt.ktt(240);}
"externally" {return tt.ktt(241);}
"f_ratio" {return tt.ktt(242);}
"failed" {return tt.ktt(243);}
"failgroup" {return tt.ktt(244);}
"fast" {return tt.ktt(245);}
"fetch" {return tt.ktt(246);}
"file" {return tt.ktt(247);}
"final" {return tt.ktt(248);}
"fine" {return tt.ktt(249);}
"finish" {return tt.ktt(250);}
"first" {return tt.ktt(251);}
"flash_cache" {return tt.ktt(252);}
"flashback" {return tt.ktt(253);}
"flex" {return tt.ktt(254);}
"flush" {return tt.ktt(255);}
"folder" {return tt.ktt(256);}
"following" {return tt.ktt(257);}
"for" {return tt.ktt(258);}
"force" {return tt.ktt(259);}
"foreign" {return tt.ktt(260);}
"format" {return tt.ktt(261);}
"freelist" {return tt.ktt(262);}
"freelists" {return tt.ktt(263);}
"freepools" {return tt.ktt(264);}
"fresh" {return tt.ktt(265);}
"from" {return tt.ktt(266);}
"full" {return tt.ktt(267);}
"function" {return tt.ktt(268);}
"generated" {return tt.ktt(269);}
"global" {return tt.ktt(270);}
"global_name" {return tt.ktt(271);}
"globally" {return tt.ktt(272);}
"grant" {return tt.ktt(273);}
"group" {return tt.ktt(274);}
"groups" {return tt.ktt(275);}
"guard" {return tt.ktt(276);}
"hash" {return tt.ktt(277);}
"hashing" {return tt.ktt(278);}
"having" {return tt.ktt(279);}
"heap" {return tt.ktt(280);}
"hide" {return tt.ktt(281);}
"hierarchy" {return tt.ktt(282);}
"high" {return tt.ktt(283);}
"history" {return tt.ktt(284);}
"hour" {return tt.ktt(285);}
"http" {return tt.ktt(286);}
"id" {return tt.ktt(287);}
"identified" {return tt.ktt(288);}
"identifier" {return tt.ktt(289);}
"identity" {return tt.ktt(290);}
"idle" {return tt.ktt(291);}
"if" {return tt.ktt(292);}
"ignore" {return tt.ktt(293);}
"ilm" {return tt.ktt(294);}
"immediate" {return tt.ktt(295);}
"immutable" {return tt.ktt(296);}
"import" {return tt.ktt(297);}
"in" {return tt.ktt(298);}
"include" {return tt.ktt(299);}
"including" {return tt.ktt(300);}
"increment" {return tt.ktt(301);}
"indent" {return tt.ktt(302);}
"index" {return tt.ktt(303);}
"indexes" {return tt.ktt(304);}
"indexing" {return tt.ktt(305);}
"indextype" {return tt.ktt(306);}
"infinite" {return tt.ktt(307);}
"initial" {return tt.ktt(308);}
"initialized" {return tt.ktt(309);}
"initially" {return tt.ktt(310);}
"initrans" {return tt.ktt(311);}
"inmemory" {return tt.ktt(312);}
"inner" {return tt.ktt(313);}
"insert" {return tt.ktt(314);}
"instance" {return tt.ktt(315);}
"instantiable" {return tt.ktt(316);}
"intermediate" {return tt.ktt(317);}
"intersect" {return tt.ktt(318);}
"intervaled" {return tt.ktt(319);}
"into" {return tt.ktt(320);}
"invalidate" {return tt.ktt(321);}
"invalidation" {return tt.ktt(322);}
"invisible" {return tt.ktt(323);}
"is" {return tt.ktt(324);}
"iterate" {return tt.ktt(325);}
"java" {return tt.ktt(326);}
"job" {return tt.ktt(327);}
"lobs" {return tt.ktt(328);}
"join" {return tt.ktt(329);}
"json" {return tt.ktt(330);}
"keep" {return tt.ktt(331);}
"key" {return tt.ktt(332);}
"keys" {return tt.ktt(333);}
"kill" {return tt.ktt(334);}
"last" {return tt.ktt(335);}
"leading" {return tt.ktt(336);}
"left" {return tt.ktt(337);}
"less" {return tt.ktt(338);}
"level" {return tt.ktt(339);}
"levels" {return tt.ktt(340);}
"library" {return tt.ktt(341);}
"like" {return tt.ktt(342);}
"like2" {return tt.ktt(343);}
"like4" {return tt.ktt(344);}
"likec" {return tt.ktt(345);}
"limit" {return tt.ktt(346);}
"linear" {return tt.ktt(347);}
"link" {return tt.ktt(348);}
"load" {return tt.ktt(349);}
"lob" {return tt.ktt(350);}
"local" {return tt.ktt(351);}
"location" {return tt.ktt(352);}
"locator" {return tt.ktt(353);}
"lock" {return tt.ktt(354);}
"lockdown" {return tt.ktt(355);}
"locked" {return tt.ktt(356);}
"locking" {return tt.ktt(357);}
"log" {return tt.ktt(358);}
"logfile" {return tt.ktt(359);}
"logging" {return tt.ktt(360);}
"logical" {return tt.ktt(361);}
"low" {return tt.ktt(362);}
"low_cost_tbs" {return tt.ktt(363);}
"main" {return tt.ktt(364);}
"manage" {return tt.ktt(365);}
"managed" {return tt.ktt(366);}
"manager" {return tt.ktt(367);}
"management" {return tt.ktt(368);}
"manual" {return tt.ktt(369);}
"mapping" {return tt.ktt(370);}
"master" {return tt.ktt(371);}
"matched" {return tt.ktt(372);}
"materialized" {return tt.ktt(373);}
"maxextents" {return tt.ktt(374);}
"maximize" {return tt.ktt(375);}
"maxsize" {return tt.ktt(376);}
"maxtrans" {return tt.ktt(377);}
"maxvalue" {return tt.ktt(378);}
"mean_squares_between" {return tt.ktt(379);}
"mean_squares_within" {return tt.ktt(380);}
"measure" {return tt.ktt(381);}
"measures" {return tt.ktt(382);}
"medium" {return tt.ktt(383);}
"member" {return tt.ktt(384);}
"memcompress" {return tt.ktt(385);}
"memoptimize" {return tt.ktt(386);}
"memory" {return tt.ktt(387);}
"memspeed" {return tt.ktt(388);}
"merge" {return tt.ktt(389);}
"metadata" {return tt.ktt(390);}
"minextents" {return tt.ktt(391);}
"minimize" {return tt.ktt(392);}
"mining" {return tt.ktt(393);}
"minus" {return tt.ktt(394);}
"minute" {return tt.ktt(395);}
"minutes" {return tt.ktt(396);}
"minvalue" {return tt.ktt(397);}
"mirror" {return tt.ktt(398);}
"mismatch" {return tt.ktt(399);}
"mlslabel" {return tt.ktt(400);}
"mode" {return tt.ktt(401);}
"model" {return tt.ktt(402);}
"modification" {return tt.ktt(403);}
"modify" {return tt.ktt(404);}
"monitoring" {return tt.ktt(405);}
"month" {return tt.ktt(406);}
"months" {return tt.ktt(407);}
"mount" {return tt.ktt(408);}
"move" {return tt.ktt(409);}
"movement" {return tt.ktt(410);}
"multiset" {return tt.ktt(411);}
"multivalue" {return tt.ktt(412);}
"name" {return tt.ktt(413);}
"nan" {return tt.ktt(414);}
"narrate" {return tt.ktt(415);}
"natural" {return tt.ktt(416);}
"nav" {return tt.ktt(417);}
"nchar_cs" {return tt.ktt(418);}
"nested" {return tt.ktt(419);}
"never" {return tt.ktt(420);}
"new" {return tt.ktt(421);}
"next" {return tt.ktt(422);}
"nextval" {return tt.ktt(423);}
"no" {return tt.ktt(424);}
"noarchivelog" {return tt.ktt(425);}
"noaudit" {return tt.ktt(426);}
"nocache" {return tt.ktt(427);}
"nocheck" {return tt.ktt(428);}
"nocompress" {return tt.ktt(429);}
"nocycle" {return tt.ktt(430);}
"nodelay" {return tt.ktt(431);}
"nodelete" {return tt.ktt(432);}
"noentityescaping" {return tt.ktt(433);}
"noforce" {return tt.ktt(434);}
"noinsert" {return tt.ktt(435);}
"nologging" {return tt.ktt(436);}
"nomapping" {return tt.ktt(437);}
"nomaxvalue" {return tt.ktt(438);}
"nominimize" {return tt.ktt(439);}
"nominvalue" {return tt.ktt(440);}
"nomonitoring" {return tt.ktt(441);}
"none" {return tt.ktt(442);}
"noneditionable" {return tt.ktt(443);}
"nonpartitioned" {return tt.ktt(444);}
"noorder" {return tt.ktt(445);}
"noparallel" {return tt.ktt(446);}
"norely" {return tt.ktt(447);}
"norepair" {return tt.ktt(448);}
"noresetlogs" {return tt.ktt(449);}
"noreverse" {return tt.ktt(450);}
"norowdependencies" {return tt.ktt(451);}
"noschema" {return tt.ktt(452);}
"noschemacheck" {return tt.ktt(453);}
"nosort" {return tt.ktt(454);}
"noswitch" {return tt.ktt(455);}
"not" {return tt.ktt(456);}
"nothing" {return tt.ktt(457);}
"notification" {return tt.ktt(458);}
"notimeout" {return tt.ktt(459);}
"noupdate" {return tt.ktt(460);}
"novalidate" {return tt.ktt(461);}
"nowait" {return tt.ktt(462);}
"null" {return tt.ktt(463);}
"nulls" {return tt.ktt(464);}
"object" {return tt.ktt(465);}
"of" {return tt.ktt(466);}
"off" {return tt.ktt(467);}
"offline" {return tt.ktt(468);}
"offset" {return tt.ktt(469);}
"oidindex" {return tt.ktt(470);}
"on" {return tt.ktt(471);}
"one_sided_prob_or_less" {return tt.ktt(472);}
"one_sided_prob_or_more" {return tt.ktt(473);}
"one_sided_sig" {return tt.ktt(474);}
"online" {return tt.ktt(475);}
"only" {return tt.ktt(476);}
"opaque" {return tt.ktt(477);}
"open" {return tt.ktt(478);}
"operator" {return tt.ktt(479);}
"optimal" {return tt.ktt(480);}
"optimize" {return tt.ktt(481);}
"option" {return tt.ktt(482);}
"or" {return tt.ktt(483);}
"order" {return tt.ktt(484);}
"ordered" {return tt.ktt(485);}
"ordinality" {return tt.ktt(486);}
"organization" {return tt.ktt(487);}
"outer" {return tt.ktt(488);}
"outline" {return tt.ktt(489);}
"over" {return tt.ktt(490);}
"overflow" {return tt.ktt(491);}
"overlaps" {return tt.ktt(492);}
"package" {return tt.ktt(493);}
"parallel" {return tt.ktt(494);}
"parameters" {return tt.ktt(495);}
"parent" {return tt.ktt(496);}
"partial" {return tt.ktt(497);}
"partition" {return tt.ktt(498);}
"partitions" {return tt.ktt(499);}
"partitionset" {return tt.ktt(500);}
"passing" {return tt.ktt(501);}
"password" {return tt.ktt(502);}
"path" {return tt.ktt(503);}
"pctfree" {return tt.ktt(504);}
"pctincrease" {return tt.ktt(505);}
"pctthreshold" {return tt.ktt(506);}
"pctused" {return tt.ktt(507);}
"pctversion" {return tt.ktt(508);}
"percent" {return tt.ktt(509);}
"performance" {return tt.ktt(510);}
"period" {return tt.ktt(511);}
"phi_coefficient" {return tt.ktt(512);}
"physical" {return tt.ktt(513);}
"pivot" {return tt.ktt(514);}
"plan" {return tt.ktt(515);}
"pluggable" {return tt.ktt(516);}
"policy" {return tt.ktt(517);}
"post_transaction" {return tt.ktt(518);}
"power" {return tt.ktt(519);}
"prebuilt" {return tt.ktt(520);}
"preceding" {return tt.ktt(521);}
"precision" {return tt.ktt(522);}
"prepare" {return tt.ktt(523);}
"present" {return tt.ktt(524);}
"preserve" {return tt.ktt(525);}
"pretty" {return tt.ktt(526);}
"primary" {return tt.ktt(527);}
"prior" {return tt.ktt(528);}
"priority" {return tt.ktt(529);}
"private" {return tt.ktt(530);}
"privilege" {return tt.ktt(531);}
"privileges" {return tt.ktt(532);}
"procedure" {return tt.ktt(533);}
"process" {return tt.ktt(534);}
"profile" {return tt.ktt(535);}
"program" {return tt.ktt(536);}
"project" {return tt.ktt(537);}
"protection" {return tt.ktt(538);}
"public" {return tt.ktt(539);}
"purge" {return tt.ktt(540);}
"query" {return tt.ktt(541);}
"queue" {return tt.ktt(542);}
"quiesce" {return tt.ktt(543);}
"quota" {return tt.ktt(544);}
"range" {return tt.ktt(545);}
"read" {return tt.ktt(546);}
"reads" {return tt.ktt(547);}
"rebalance" {return tt.ktt(548);}
"rebuild" {return tt.ktt(549);}
"recover" {return tt.ktt(550);}
"recovery" {return tt.ktt(551);}
"recycle" {return tt.ktt(552);}
"redefine" {return tt.ktt(553);}
"reduced" {return tt.ktt(554);}
"ref" {return tt.ktt(555);}
"reference" {return tt.ktt(556);}
"referenced" {return tt.ktt(557);}
"references" {return tt.ktt(558);}
"refresh" {return tt.ktt(559);}
"regexp_like" {return tt.ktt(560);}
"register" {return tt.ktt(561);}
"reject" {return tt.ktt(562);}
"rekey" {return tt.ktt(563);}
"relational" {return tt.ktt(564);}
"rely" {return tt.ktt(565);}
"remainder" {return tt.ktt(566);}
"rename" {return tt.ktt(567);}
"repair" {return tt.ktt(568);}
"repeat" {return tt.ktt(569);}
"replace" {return tt.ktt(570);}
"replication" {return tt.ktt(571);}
"reservable" {return tt.ktt(572);}
"reset" {return tt.ktt(573);}
"resetlogs" {return tt.ktt(574);}
"resize" {return tt.ktt(575);}
"resolve" {return tt.ktt(576);}
"resolver" {return tt.ktt(577);}
"resource" {return tt.ktt(578);}
"restrict" {return tt.ktt(579);}
"restricted" {return tt.ktt(580);}
"result_cache" {return tt.ktt(581);}
"resumable" {return tt.ktt(582);}
"resume" {return tt.ktt(583);}
"retention" {return tt.ktt(584);}
"return" {return tt.ktt(585);}
"returning" {return tt.ktt(586);}
"reuse" {return tt.ktt(587);}
"reverse" {return tt.ktt(588);}
"revoke" {return tt.ktt(589);}
"rewrite" {return tt.ktt(590);}
"right" {return tt.ktt(591);}
"role" {return tt.ktt(592);}
"rollback" {return tt.ktt(593);}
"rollover" {return tt.ktt(594);}
"rollup" {return tt.ktt(595);}
"row" {return tt.ktt(596);}
"rowdependencies" {return tt.ktt(597);}
"rownum" {return tt.ktt(598);}
"rows" {return tt.ktt(599);}
"rule" {return tt.ktt(600);}
"rules" {return tt.ktt(601);}
"salt" {return tt.ktt(602);}
"sample" {return tt.ktt(603);}
"savepoint" {return tt.ktt(604);}
"scan" {return tt.ktt(605);}
"scheduler" {return tt.ktt(606);}
"schemacheck" {return tt.ktt(607);}
"scn" {return tt.ktt(608);}
"scope" {return tt.ktt(609);}
"second" {return tt.ktt(610);}
"securefile" {return tt.ktt(611);}
"seed" {return tt.ktt(612);}
"segment" {return tt.ktt(613);}
"select" {return tt.ktt(614);}
"sequence" {return tt.ktt(615);}
"sequential" {return tt.ktt(616);}
"serializable" {return tt.ktt(617);}
"service" {return tt.ktt(618);}
"session" {return tt.ktt(619);}
"set" {return tt.ktt(620);}
"sets" {return tt.ktt(621);}
"settings" {return tt.ktt(622);}
"share" {return tt.ktt(623);}
"shared" {return tt.ktt(624);}
"shared_pool" {return tt.ktt(625);}
"sharing" {return tt.ktt(626);}
"show" {return tt.ktt(627);}
"showprompt" {return tt.ktt(628);}
"showsql" {return tt.ktt(629);}
"shrink" {return tt.ktt(630);}
"shutdown" {return tt.ktt(631);}
"siblings" {return tt.ktt(632);}
"sid" {return tt.ktt(633);}
"sig" {return tt.ktt(634);}
"single" {return tt.ktt(635);}
"size" {return tt.ktt(636);}
"skip" {return tt.ktt(637);}
"smallfile" {return tt.ktt(638);}
"snapshot" {return tt.ktt(639);}
"some" {return tt.ktt(640);}
"sort" {return tt.ktt(641);}
"source" {return tt.ktt(642);}
"space" {return tt.ktt(643);}
"spatial" {return tt.ktt(644);}
"specification" {return tt.ktt(645);}
"spfile" {return tt.ktt(646);}
"split" {return tt.ktt(647);}
"sql" {return tt.ktt(648);}
"staging" {return tt.ktt(649);}
"standalone" {return tt.ktt(650);}
"standby" {return tt.ktt(651);}
"start" {return tt.ktt(652);}
"statement" {return tt.ktt(653);}
"statistic" {return tt.ktt(654);}
"statistics" {return tt.ktt(655);}
"stop" {return tt.ktt(656);}
"storage" {return tt.ktt(657);}
"store" {return tt.ktt(658);}
"strict" {return tt.ktt(659);}
"submultiset" {return tt.ktt(660);}
"subpartition" {return tt.ktt(661);}
"subpartitions" {return tt.ktt(662);}
"substitutable" {return tt.ktt(663);}
"successful" {return tt.ktt(664);}
"sum_squares_between" {return tt.ktt(665);}
"sum_squares_within" {return tt.ktt(666);}
"supplemental" {return tt.ktt(667);}
"suspend" {return tt.ktt(668);}
"switch" {return tt.ktt(669);}
"switchover" {return tt.ktt(670);}
"synchronous" {return tt.ktt(671);}
"synonym" {return tt.ktt(672);}
"sysbackup" {return tt.ktt(673);}
"sysdba" {return tt.ktt(674);}
"sysdg" {return tt.ktt(675);}
"syskm" {return tt.ktt(676);}
"sysoper" {return tt.ktt(677);}
"system" {return tt.ktt(678);}
"table" {return tt.ktt(679);}
"tables" {return tt.ktt(680);}
"tablespace" {return tt.ktt(681);}
"tempfile" {return tt.ktt(682);}
"template" {return tt.ktt(683);}
"temporary" {return tt.ktt(684);}
"test" {return tt.ktt(685);}
"than" {return tt.ktt(686);}
"then" {return tt.ktt(687);}
"thread" {return tt.ktt(688);}
"through" {return tt.ktt(689);}
"tier" {return tt.ktt(690);}
"ties" {return tt.ktt(691);}
"time" {return tt.ktt(692);}
"time_zone" {return tt.ktt(693);}
"timeout" {return tt.ktt(694);}
"timezone_abbr" {return tt.ktt(695);}
"timezone_hour" {return tt.ktt(696);}
"timezone_minute" {return tt.ktt(697);}
"timezone_region" {return tt.ktt(698);}
"to" {return tt.ktt(699);}
"trace" {return tt.ktt(700);}
"tracking" {return tt.ktt(701);}
"trailing" {return tt.ktt(702);}
"transaction" {return tt.ktt(703);}
"translation" {return tt.ktt(704);}
"trigger" {return tt.ktt(705);}
"triggers" {return tt.ktt(706);}
"truncate" {return tt.ktt(707);}
"trusted" {return tt.ktt(708);}
"tuning" {return tt.ktt(709);}
"two_sided_prob" {return tt.ktt(710);}
"two_sided_sig" {return tt.ktt(711);}
"type" {return tt.ktt(712);}
"u_statistic" {return tt.ktt(713);}
"uid" {return tt.ktt(714);}
"unarchived" {return tt.ktt(715);}
"unbounded" {return tt.ktt(716);}
"unconditional" {return tt.ktt(717);}
"under" {return tt.ktt(718);}
"under_path" {return tt.ktt(719);}
"undrop" {return tt.ktt(720);}
"union" {return tt.ktt(721);}
"unique" {return tt.ktt(722);}
"unlimited" {return tt.ktt(723);}
"unlock" {return tt.ktt(724);}
"unnest" {return tt.ktt(725);}
"unpacked" {return tt.ktt(726);}
"unpivot" {return tt.ktt(727);}
"unprotected" {return tt.ktt(728);}
"unquiesce" {return tt.ktt(729);}
"unrecoverable" {return tt.ktt(730);}
"until" {return tt.ktt(731);}
"unusable" {return tt.ktt(732);}
"unused" {return tt.ktt(733);}
"update" {return tt.ktt(734);}
"updated" {return tt.ktt(735);}
"upgrade" {return tt.ktt(736);}
"upsert" {return tt.ktt(737);}
"usage" {return tt.ktt(738);}
"use" {return tt.ktt(739);}
"user" {return tt.ktt(740);}
"using" {return tt.ktt(741);}
"validate" {return tt.ktt(742);}
"validation" {return tt.ktt(743);}
"value" {return tt.ktt(744);}
"values" {return tt.ktt(745);}
"variants" {return tt.ktt(746);}
"varray" {return tt.ktt(747);}
"varrays" {return tt.ktt(748);}
"version" {return tt.ktt(749);}
"versions" {return tt.ktt(750);}
"view" {return tt.ktt(751);}
"virtual" {return tt.ktt(752);}
"visible" {return tt.ktt(753);}
"wait" {return tt.ktt(754);}
"wellformed" {return tt.ktt(755);}
"when" {return tt.ktt(756);}
"whenever" {return tt.ktt(757);}
"where" {return tt.ktt(758);}
"with" {return tt.ktt(759);}
"within" {return tt.ktt(760);}
"without" {return tt.ktt(761);}
"work" {return tt.ktt(762);}
"wrapper" {return tt.ktt(763);}
"write" {return tt.ktt(764);}
"xml" {return tt.ktt(765);}
"xmlnamespaces" {return tt.ktt(766);}
"xmlschema" {return tt.ktt(767);}
"xmltype" {return tt.ktt(768);}
"year" {return tt.ktt(769);}
"years" {return tt.ktt(770);}
"yes" {return tt.ktt(771);}
"zone" {return tt.ktt(772);}
"zonemap" {return tt.ktt(773);}
"false" {return tt.ktt(774);}
"true" {return tt.ktt(775);}



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
"collect" {return tt.ftt(18);}
"compose" {return tt.ftt(19);}
"concat" {return tt.ftt(20);}
"convert" {return tt.ftt(21);}
"corr" {return tt.ftt(22);}
"corr_k" {return tt.ftt(23);}
"corr_s" {return tt.ftt(24);}
"cos" {return tt.ftt(25);}
"cosh" {return tt.ftt(26);}
"count" {return tt.ftt(27);}
"covar_pop" {return tt.ftt(28);}
"covar_samp" {return tt.ftt(29);}
"cume_dist" {return tt.ftt(30);}
"current_date" {return tt.ftt(31);}
"current_timestamp" {return tt.ftt(32);}
"cv" {return tt.ftt(33);}
"dbtimezone" {return tt.ftt(34);}
"dbtmezone" {return tt.ftt(35);}
"decode" {return tt.ftt(36);}
"decompose" {return tt.ftt(37);}
"deletexml" {return tt.ftt(38);}
"depth" {return tt.ftt(39);}
"deref" {return tt.ftt(40);}
"empty_blob" {return tt.ftt(41);}
"empty_clob" {return tt.ftt(42);}
"existsnode" {return tt.ftt(43);}
"exp" {return tt.ftt(44);}
"extract" {return tt.ftt(45);}
"extractvalue" {return tt.ftt(46);}
"first_value" {return tt.ftt(47);}
"floor" {return tt.ftt(48);}
"from_tz" {return tt.ftt(49);}
"greatest" {return tt.ftt(50);}
"group_id" {return tt.ftt(51);}
"grouping" {return tt.ftt(52);}
"grouping_id" {return tt.ftt(53);}
"hextoraw" {return tt.ftt(54);}
"initcap" {return tt.ftt(55);}
"insertchildxml" {return tt.ftt(56);}
"insertchildxmlafter" {return tt.ftt(57);}
"insertchildxmlbefore" {return tt.ftt(58);}
"insertxmlafter" {return tt.ftt(59);}
"insertxmlbefore" {return tt.ftt(60);}
"instr" {return tt.ftt(61);}
"instr2" {return tt.ftt(62);}
"instr4" {return tt.ftt(63);}
"instrb" {return tt.ftt(64);}
"instrc" {return tt.ftt(65);}
"iteration_number" {return tt.ftt(66);}
"json_array" {return tt.ftt(67);}
"json_arrayagg" {return tt.ftt(68);}
"json_dataguide" {return tt.ftt(69);}
"json_object" {return tt.ftt(70);}
"json_objectagg" {return tt.ftt(71);}
"json_query" {return tt.ftt(72);}
"json_serialize" {return tt.ftt(73);}
"json_table" {return tt.ftt(74);}
"json_value" {return tt.ftt(75);}
"lag" {return tt.ftt(76);}
"last_day" {return tt.ftt(77);}
"last_value" {return tt.ftt(78);}
"lateral" {return tt.ftt(79);}
"lead" {return tt.ftt(80);}
"least" {return tt.ftt(81);}
"length" {return tt.ftt(82);}
"length2" {return tt.ftt(83);}
"length4" {return tt.ftt(84);}
"lengthb" {return tt.ftt(85);}
"lengthc" {return tt.ftt(86);}
"listagg" {return tt.ftt(87);}
"ln" {return tt.ftt(88);}
"lnnvl" {return tt.ftt(89);}
"localtimestamp" {return tt.ftt(90);}
"lower" {return tt.ftt(91);}
"lpad" {return tt.ftt(92);}
"ltrim" {return tt.ftt(93);}
"make_ref" {return tt.ftt(94);}
"max" {return tt.ftt(95);}
"median" {return tt.ftt(96);}
"min" {return tt.ftt(97);}
"mod" {return tt.ftt(98);}
"months_between" {return tt.ftt(99);}
"nanvl" {return tt.ftt(100);}
"nchr" {return tt.ftt(101);}
"new_time" {return tt.ftt(102);}
"next_day" {return tt.ftt(103);}
"nls_charset_decl_len" {return tt.ftt(104);}
"nls_charset_id" {return tt.ftt(105);}
"nls_charset_name" {return tt.ftt(106);}
"nls_initcap" {return tt.ftt(107);}
"nls_lower" {return tt.ftt(108);}
"nls_upper" {return tt.ftt(109);}
"nlssort" {return tt.ftt(110);}
"ntile" {return tt.ftt(111);}
"nullif" {return tt.ftt(112);}
"numtodsinterval" {return tt.ftt(113);}
"numtoyminterval" {return tt.ftt(114);}
"nvl" {return tt.ftt(115);}
"nvl2" {return tt.ftt(116);}
"ora_hash" {return tt.ftt(117);}
"percent_rank" {return tt.ftt(118);}
"percentile_cont" {return tt.ftt(119);}
"percentile_disc" {return tt.ftt(120);}
"powermultiset" {return tt.ftt(121);}
"powermultiset_by_cardinality" {return tt.ftt(122);}
"presentnnv" {return tt.ftt(123);}
"presentv" {return tt.ftt(124);}
"previous" {return tt.ftt(125);}
"rank" {return tt.ftt(126);}
"ratio_to_report" {return tt.ftt(127);}
"rawtohex" {return tt.ftt(128);}
"rawtonhex" {return tt.ftt(129);}
"reftohex" {return tt.ftt(130);}
"regexp_instr" {return tt.ftt(131);}
"regexp_replace" {return tt.ftt(132);}
"regexp_substr" {return tt.ftt(133);}
"regr_avgx" {return tt.ftt(134);}
"regr_avgy" {return tt.ftt(135);}
"regr_count" {return tt.ftt(136);}
"regr_intercept" {return tt.ftt(137);}
"regr_r2" {return tt.ftt(138);}
"regr_slope" {return tt.ftt(139);}
"regr_sxx" {return tt.ftt(140);}
"regr_sxy" {return tt.ftt(141);}
"regr_syy" {return tt.ftt(142);}
"round" {return tt.ftt(143);}
"row_number" {return tt.ftt(144);}
"rowidtochar" {return tt.ftt(145);}
"rowidtonchar" {return tt.ftt(146);}
"rpad" {return tt.ftt(147);}
"rtrim" {return tt.ftt(148);}
"scn_to_timestamp" {return tt.ftt(149);}
"sessiontimezone" {return tt.ftt(150);}
"sign" {return tt.ftt(151);}
"sin" {return tt.ftt(152);}
"sinh" {return tt.ftt(153);}
"soundex" {return tt.ftt(154);}
"sqrt" {return tt.ftt(155);}
"stats_binomial_test" {return tt.ftt(156);}
"stats_crosstab" {return tt.ftt(157);}
"stats_f_test" {return tt.ftt(158);}
"stats_ks_test" {return tt.ftt(159);}
"stats_mode" {return tt.ftt(160);}
"stats_mw_test" {return tt.ftt(161);}
"stats_one_way_anova" {return tt.ftt(162);}
"stats_t_test_indep" {return tt.ftt(163);}
"stats_t_test_indepu" {return tt.ftt(164);}
"stats_t_test_one" {return tt.ftt(165);}
"stats_t_test_paired" {return tt.ftt(166);}
"stats_wsr_test" {return tt.ftt(167);}
"stddev" {return tt.ftt(168);}
"stddev_pop" {return tt.ftt(169);}
"stddev_samp" {return tt.ftt(170);}
"substr" {return tt.ftt(171);}
"substr2" {return tt.ftt(172);}
"substr4" {return tt.ftt(173);}
"substrb" {return tt.ftt(174);}
"substrc" {return tt.ftt(175);}
"sum" {return tt.ftt(176);}
"sys_connect_by_path" {return tt.ftt(177);}
"sys_context" {return tt.ftt(178);}
"sys_dburigen" {return tt.ftt(179);}
"sys_extract_utc" {return tt.ftt(180);}
"sys_guid" {return tt.ftt(181);}
"sys_typeid" {return tt.ftt(182);}
"sys_xmlagg" {return tt.ftt(183);}
"sys_xmlgen" {return tt.ftt(184);}
"sysdate" {return tt.ftt(185);}
"systimestamp" {return tt.ftt(186);}
"tan" {return tt.ftt(187);}
"tanh" {return tt.ftt(188);}
"timestamp_to_scn" {return tt.ftt(189);}
"to_binary_double" {return tt.ftt(190);}
"to_binary_float" {return tt.ftt(191);}
"to_char" {return tt.ftt(192);}
"to_clob" {return tt.ftt(193);}
"to_date" {return tt.ftt(194);}
"to_dsinterval" {return tt.ftt(195);}
"to_lob" {return tt.ftt(196);}
"to_multi_byte" {return tt.ftt(197);}
"to_nchar" {return tt.ftt(198);}
"to_nclob" {return tt.ftt(199);}
"to_number" {return tt.ftt(200);}
"to_single_byte" {return tt.ftt(201);}
"to_timestamp" {return tt.ftt(202);}
"to_timestamp_tz" {return tt.ftt(203);}
"to_yminterval" {return tt.ftt(204);}
"translate" {return tt.ftt(205);}
"treat" {return tt.ftt(206);}
"trim" {return tt.ftt(207);}
"trunc" {return tt.ftt(208);}
"tz_offset" {return tt.ftt(209);}
"unistr" {return tt.ftt(210);}
"updatexml" {return tt.ftt(211);}
"upper" {return tt.ftt(212);}
"userenv" {return tt.ftt(213);}
"validate_conversion" {return tt.ftt(214);}
"var_pop" {return tt.ftt(215);}
"var_samp" {return tt.ftt(216);}
"variance" {return tt.ftt(217);}
"vsize" {return tt.ftt(218);}
"width_bucket" {return tt.ftt(219);}
"xmlagg" {return tt.ftt(220);}
"xmlattributes" {return tt.ftt(221);}
"xmlcast" {return tt.ftt(222);}
"xmlcdata" {return tt.ftt(223);}
"xmlcolattval" {return tt.ftt(224);}
"xmlcomment" {return tt.ftt(225);}
"xmlconcat" {return tt.ftt(226);}
"xmldiff" {return tt.ftt(227);}
"xmlelement" {return tt.ftt(228);}
"xmlforest" {return tt.ftt(229);}
"xmlisvalid" {return tt.ftt(230);}
"xmlparse" {return tt.ftt(231);}
"xmlpatch" {return tt.ftt(232);}
"xmlpi" {return tt.ftt(233);}
"xmlquery" {return tt.ftt(234);}
"xmlroot" {return tt.ftt(235);}
"xmlsequence" {return tt.ftt(236);}
"xmlserialize" {return tt.ftt(237);}
"xmltable" {return tt.ftt(238);}
"xmltransform" {return tt.ftt(239);}





"aq_tm_processes" {return tt.ptt(0);}
"archive_lag_target" {return tt.ptt(1);}
"audit_file_dest" {return tt.ptt(2);}
"audit_sys_operations" {return tt.ptt(3);}
"audit_trail" {return tt.ptt(4);}
"background_core_dump" {return tt.ptt(5);}
"background_dump_dest" {return tt.ptt(6);}
"backup_tape_io_slaves" {return tt.ptt(7);}
"bitmap_merge_area_size" {return tt.ptt(8);}
"blank_trimming" {return tt.ptt(9);}
"circuits" {return tt.ptt(10);}
"cluster_database" {return tt.ptt(11);}
"cluster_database_instances" {return tt.ptt(12);}
"cluster_interconnects" {return tt.ptt(13);}
"commit_point_strength" {return tt.ptt(14);}
"compatible" {return tt.ptt(15);}
"composite_limit" {return tt.ptt(16);}
"connect_time" {return tt.ptt(17);}
"control_file_record_keep_time" {return tt.ptt(18);}
"control_files" {return tt.ptt(19);}
"core_dump_dest"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(20);}
"cpu_count" {return tt.ptt(21);}
"cpu_per_call" {return tt.ptt(22);}
"cpu_per_session" {return tt.ptt(23);}
"create_bitmap_area_size" {return tt.ptt(24);}
"create_stored_outlines" {return tt.ptt(25);}
"current_schema" {return tt.ptt(26);}
"cursor_sharing" {return tt.ptt(27);}
"cursor_space_for_time" {return tt.ptt(28);}
"db_block_checking" {return tt.ptt(29);}
"db_block_checksum" {return tt.ptt(30);}
"db_block_size" {return tt.ptt(31);}
"db_cache_advice" {return tt.ptt(32);}
"db_cache_size" {return tt.ptt(33);}
"db_create_file_dest" {return tt.ptt(34);}
"db_create_online_log_dest_"{digit}+ {return tt.ptt(35);}
"db_domain" {return tt.ptt(36);}
"db_file_multiblock_read_count" {return tt.ptt(37);}
"db_file_name_convert" {return tt.ptt(38);}
"db_files" {return tt.ptt(39);}
"db_flashback_retention_target" {return tt.ptt(40);}
"db_keep_cache_size" {return tt.ptt(41);}
"db_name" {return tt.ptt(42);}
"db_nk_cache_size" {return tt.ptt(43);}
"db_recovery_file_dest" {return tt.ptt(44);}
"db_recovery_file_dest_size" {return tt.ptt(45);}
"db_recycle_cache_size" {return tt.ptt(46);}
"db_unique_name" {return tt.ptt(47);}
"db_writer_processes" {return tt.ptt(48);}
"dbwr_io_slaves" {return tt.ptt(49);}
"ddl_wait_for_locks" {return tt.ptt(50);}
"dg_broker_config_filen" {return tt.ptt(51);}
"dg_broker_start" {return tt.ptt(52);}
"disk_asynch_io" {return tt.ptt(53);}
"dispatchers" {return tt.ptt(54);}
"distributed_lock_timeout" {return tt.ptt(55);}
"dml_locks" {return tt.ptt(56);}
"enqueue_resources" {return tt.ptt(57);}
"error_on_overlap_time" {return tt.ptt(58);}
"event" {return tt.ptt(59);}
"failed_login_attempts" {return tt.ptt(60);}
"fal_client" {return tt.ptt(61);}
"fal_server" {return tt.ptt(62);}
"fast_start_mttr_target" {return tt.ptt(63);}
"fast_start_parallel_rollback" {return tt.ptt(64);}
"file_mapping" {return tt.ptt(65);}
"fileio_network_adapters" {return tt.ptt(66);}
"filesystem_like_logging" {return tt.ptt(67);}
"filesystemio_options" {return tt.ptt(68);}
"fixed_date" {return tt.ptt(69);}
"flagger" {return tt.ptt(70);}
"gc_files_to_locks" {return tt.ptt(71);}
"gcs_server_processes" {return tt.ptt(72);}
"global_names" {return tt.ptt(73);}
"hash_area_size" {return tt.ptt(74);}
"hi_shared_memory_address" {return tt.ptt(75);}
"hs_autoregister" {return tt.ptt(76);}
"idle_time" {return tt.ptt(77);}
"ifile" {return tt.ptt(78);}
"instance"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(79);}
"instance_groups" {return tt.ptt(80);}
"instance_name" {return tt.ptt(81);}
"instance_number" {return tt.ptt(82);}
"instance_type" {return tt.ptt(83);}
"isolation_level" {return tt.ptt(84);}
"java_max_sessionspace_size" {return tt.ptt(85);}
"java_pool_size" {return tt.ptt(86);}
"java_soft_sessionspace_limit" {return tt.ptt(87);}
"job_queue_processes" {return tt.ptt(88);}
"large_pool_size" {return tt.ptt(89);}
"ldap_directory_access" {return tt.ptt(90);}
"license_max_sessions" {return tt.ptt(91);}
"license_max_users" {return tt.ptt(92);}
"license_sessions_warning" {return tt.ptt(93);}
"local_listener" {return tt.ptt(94);}
"lock_sga" {return tt.ptt(95);}
"log_archive_config" {return tt.ptt(96);}
"log_archive_dest" {return tt.ptt(97);}
"log_archive_dest_"{digit}+ {return tt.ptt(98);}
"log_archive_dest_state_"{digit}+ {return tt.ptt(99);}
"log_archive_duplex_dest" {return tt.ptt(100);}
"log_archive_format" {return tt.ptt(101);}
"log_archive_local_first" {return tt.ptt(102);}
"log_archive_max_processes" {return tt.ptt(103);}
"log_archive_min_succeed_dest" {return tt.ptt(104);}
"log_archive_trace" {return tt.ptt(105);}
"log_buffer" {return tt.ptt(106);}
"log_checkpoint_interval" {return tt.ptt(107);}
"log_checkpoint_timeout" {return tt.ptt(108);}
"log_checkpoints_to_alert" {return tt.ptt(109);}
"log_file_name_convert" {return tt.ptt(110);}
"logical_reads_per_call" {return tt.ptt(111);}
"logical_reads_per_session" {return tt.ptt(112);}
"logmnr_max_persistent_sessions" {return tt.ptt(113);}
"max_commit_propagation_delay" {return tt.ptt(114);}
"max_dispatchers" {return tt.ptt(115);}
"max_dump_file_size" {return tt.ptt(116);}
"max_shared_servers" {return tt.ptt(117);}
"nls_calendar" {return tt.ptt(118);}
"nls_comp" {return tt.ptt(119);}
"nls_currency" {return tt.ptt(120);}
"nls_date_format" {return tt.ptt(121);}
"nls_date_language" {return tt.ptt(122);}
"nls_dual_currency" {return tt.ptt(123);}
"nls_iso_currency" {return tt.ptt(124);}
"nls_language" {return tt.ptt(125);}
"nls_length_semantics" {return tt.ptt(126);}
"nls_nchar_conv_excp" {return tt.ptt(127);}
"nls_numeric_characters" {return tt.ptt(128);}
"nls_sort" {return tt.ptt(129);}
"nls_territory" {return tt.ptt(130);}
"nls_timestamp_format" {return tt.ptt(131);}
"nls_timestamp_tz_format" {return tt.ptt(132);}
"o7_dictionary_accessibility" {return tt.ptt(133);}
"object_cache_max_size_percent" {return tt.ptt(134);}
"object_cache_optimal_size" {return tt.ptt(135);}
"olap_page_pool_size" {return tt.ptt(136);}
"open_cursors" {return tt.ptt(137);}
"open_links" {return tt.ptt(138);}
"open_links_per_instance" {return tt.ptt(139);}
"optimizer_dynamic_sampling" {return tt.ptt(140);}
"optimizer_features_enable" {return tt.ptt(141);}
"optimizer_index_caching" {return tt.ptt(142);}
"optimizer_index_cost_adj" {return tt.ptt(143);}
"optimizer_mode" {return tt.ptt(144);}
"os_authent_prefix" {return tt.ptt(145);}
"os_roles" {return tt.ptt(146);}
"osm_diskgroups" {return tt.ptt(147);}
"osm_diskstring" {return tt.ptt(148);}
"osm_power_limit" {return tt.ptt(149);}
"parallel_adaptive_multi_user" {return tt.ptt(150);}
"parallel_execution_message_size" {return tt.ptt(151);}
"parallel_instance_group" {return tt.ptt(152);}
"parallel_max_servers" {return tt.ptt(153);}
"parallel_min_percent" {return tt.ptt(154);}
"parallel_min_servers" {return tt.ptt(155);}
"parallel_threads_per_cpu" {return tt.ptt(156);}
"password_grace_time" {return tt.ptt(157);}
"password_life_time" {return tt.ptt(158);}
"password_lock_time" {return tt.ptt(159);}
"password_reuse_max" {return tt.ptt(160);}
"password_reuse_time" {return tt.ptt(161);}
"password_verify_function" {return tt.ptt(162);}
"pga_aggregate_target" {return tt.ptt(163);}
"plsql_code_type" {return tt.ptt(164);}
"plsql_compiler_flags" {return tt.ptt(165);}
"plsql_debug" {return tt.ptt(166);}
"plsql_native_library_dir" {return tt.ptt(167);}
"plsql_native_library_subdir_count" {return tt.ptt(168);}
"plsql_optimize_level" {return tt.ptt(169);}
"plsql_v2_compatibility" {return tt.ptt(170);}
"plsql_warnings" {return tt.ptt(171);}
"pre_page_sga" {return tt.ptt(172);}
"private_sga" {return tt.ptt(173);}
"processes" {return tt.ptt(174);}
"query_rewrite_enabled" {return tt.ptt(175);}
"query_rewrite_integrity" {return tt.ptt(176);}
"rdbms_server_dn" {return tt.ptt(177);}
"read_only_open_delayed" {return tt.ptt(178);}
"recovery_parallelism" {return tt.ptt(179);}
"remote_archive_enable" {return tt.ptt(180);}
"remote_dependencies_mode" {return tt.ptt(181);}
"remote_listener" {return tt.ptt(182);}
"remote_login_passwordfile" {return tt.ptt(183);}
"remote_os_authent" {return tt.ptt(184);}
"remote_os_roles" {return tt.ptt(185);}
"replication_dependency_tracking" {return tt.ptt(186);}
"resource_limit" {return tt.ptt(187);}
"resource_manager_plan" {return tt.ptt(188);}
"resumable_timeout" {return tt.ptt(189);}
"rollback_segments" {return tt.ptt(190);}
"serial_reuse" {return tt.ptt(191);}
"service_names" {return tt.ptt(192);}
"session_cached_cursors" {return tt.ptt(193);}
"session_max_open_files" {return tt.ptt(194);}
"sessions" {return tt.ptt(195);}
"sessions_per_user" {return tt.ptt(196);}
"sga_max_size" {return tt.ptt(197);}
"sga_target" {return tt.ptt(198);}
"shadow_core_dump" {return tt.ptt(199);}
"shared_memory_address" {return tt.ptt(200);}
"shared_pool_reserved_size" {return tt.ptt(201);}
"shared_pool_size" {return tt.ptt(202);}
"shared_server_sessions" {return tt.ptt(203);}
"shared_servers" {return tt.ptt(204);}
"skip_unusable_indexes" {return tt.ptt(205);}
"smtp_out_server" {return tt.ptt(206);}
"sort_area_retained_size" {return tt.ptt(207);}
"sort_area_size" {return tt.ptt(208);}
"spfile"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(209);}
"sql_trace" {return tt.ptt(210);}
"sql92_security" {return tt.ptt(211);}
"sqltune_category" {return tt.ptt(212);}
"standby_archive_dest" {return tt.ptt(213);}
"standby_file_management" {return tt.ptt(214);}
"star_transformation_enabled" {return tt.ptt(215);}
"statement_id" {return tt.ptt(216);}
"statistics_level" {return tt.ptt(217);}
"streams_pool_size" {return tt.ptt(218);}
"tape_asynch_io" {return tt.ptt(219);}
"thread"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(220);}
"timed_os_statistics" {return tt.ptt(221);}
"timed_statistics" {return tt.ptt(222);}
"trace_enabled" {return tt.ptt(223);}
"tracefile_identifier" {return tt.ptt(224);}
"transactions" {return tt.ptt(225);}
"transactions_per_rollback_segment" {return tt.ptt(226);}
"undo_management" {return tt.ptt(227);}
"undo_retention" {return tt.ptt(228);}
"undo_tablespace" {return tt.ptt(229);}
"use_indirect_data_buffers" {return tt.ptt(230);}
"use_private_outlines" {return tt.ptt(231);}
"use_stored_outlines" {return tt.ptt(232);}
"user_dump_dest" {return tt.ptt(233);}
"utl_file_dir" {return tt.ptt(234);}
"workarea_size_policy" {return tt.ptt(235);}


{CT_SIZE_CLAUSE} {return tt.getTokenType("CT_SIZE_CLAUSE");}

{INTEGER}     { return stt.getInteger(); }
{NUMBER}      { return stt.getNumber(); }
{STRING}      { return stt.getString(); }

{IDENTIFIER}         { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}  { return stt.getQuotedIdentifier(); }

{WHITE_SPACE}        { return stt.getWhiteSpace(); }
.                    { return stt.getIdentifier(); }
}
