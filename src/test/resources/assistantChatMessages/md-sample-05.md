Images

Inline image (remote):

![Oracle Database](https://www.oracle.com/a/ocom/img/db19c-og-image.jpg)

Reference-style image:

![Oracle Logo][oracle-logo]

[oracle-logo]: https://www.oracle.com/a/ocom/img/oracle-logo.png

---

## HTML inside Markdown (renderer-dependent)

<div style="padding:10px;border:1px solid #ccc;border-radius:6px">
  <strong>HTML block:</strong> If your chat supports raw HTML, you should see a bordered box.
  <br/>
  <em>Line breaks</em> and <span style="color:#b00">inline styling</span> may or may not render.
</div>

---

## Collapsible section (`<details>`; renderer-dependent)

<details>
  <summary>Click to expand: sample payload</summary>

### Hidden header inside details

- You should be able to expand/collapse this section.
- Nested markdown may or may not be supported depending on the client.

```sql
  SELECT 'details section' AS demo
  FROM dual;
```
</details>

---

## Footnotes (renderer-dependent)

Here is a statement with a footnote reference.[^plsql]

Another sentence with a second footnote.[^render]

[^plsql]: PL/SQL is Oracle’s procedural extension to SQL.
[^render]: Many chat clients don’t render footnotes; they may show as plain text.

---

## Math (LaTeX; renderer-dependent)

Inline math: \( a^2 + b^2 = c^2 \)

Block math:

\[
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
\]

---

## Syntax Highlighting Tests

### SQL
```sql
WITH t AS (
  SELECT 10 AS n FROM dual
)
SELECT n,
       n * (n + 1) / 2 AS triangular
FROM t;
```

### PL/SQL
```plsql
CREATE OR REPLACE PROCEDURE demo_math(p_n IN PLS_INTEGER) AS
  l_sum PLS_INTEGER;
BEGIN
  l_sum := p_n * (p_n + 1) / 2;
  DBMS_OUTPUT.PUT_LINE('sum=' || l_sum);
END;
/
```

### JSON
```json
{
  "schema": "HR",
  "object": "EMPLOYEES",
  "filters": [
    { "field": "DEPARTMENT_ID", "op": "=", "value": 50 }
  ]
}
```

### Bash
```bash
echo "hello"
sqlplus user/pass@//host:1521/service <<'SQL'
select sysdate from dual;
SQL
```

---

## Bonus: Links + Inline HTML

- [Oracle PL/SQL Language Reference (19c)](https://docs.oracle.com/en/database/oracle/oracle-database/19/lnpls/)
- <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/DBMS_OUTPUT.html">DBMS_OUTPUT (HTML link tag)</a>

---

## “Kitchen Sink” Table

| Feature | Markdown | Might Fail If… |
|---|---|---|
| Image | `![alt](url)` | remote images blocked |
| HTML | `<div>...</div>` | HTML sanitized |
| Collapsible | `<details><summary>..` | tags stripped |
| Footnotes | `[^1]` | no footnote support |
| Math | `\(..\)` / `\[..]` | no LaTeX renderer |
| Highlighting | ```sql | no fenced code support |

If you tell me what chat platform/renderer you’re testing (e.g., VS Code, GitHub, Slack, Teams, a custom web view), I can tailor the syntax to what it supports best.