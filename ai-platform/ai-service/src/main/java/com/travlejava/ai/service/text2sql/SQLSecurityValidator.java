package com.travlejava.ai.service.text2sql;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Text2SQL 的 SQL 安全校验。
 *
 * 早期实现是「关键字黑名单 + String.contains」，有两个问题：
 *   1. 误杀：sql.contains("CREATE") 会把 create_time 这样的列名也拦掉，
 *      而 Text2SQL 内置的默认 schema 里恰好就有 create_time / update_time；
 *   2. 易绕过：大小写变形、夹杂注释的写法容易漏判。
 *
 * 现在改为先用 JSqlParser 解析成 AST，再按语法结构判断：
 *   - 必须能解析通过（解析失败即拒绝，fail-closed）；
 *   - 必须且只能有一条语句（多语句 / 分号注入直接被拒）；
 *   - 根节点必须是 Select（WITH ... SELECT 也归为 Select，天然支持 CTE，
 *     而 INSERT / UPDATE / DELETE / DDL 一律拒绝）；
 *   - 之后再过一遍「危险函数 + 文件写入」的词边界黑名单作为第二道防线。
 *
 * 第二道防线用词边界 \b 匹配，保证 create_time、delete_flag 这类标识符不会被误判。
 */
@Service
public class SQLSecurityValidator {

    private static final Logger logger = LoggerFactory.getLogger(SQLSecurityValidator.class);

    /** 第二道防线：危险函数与文件写入 */
    private static final Pattern DANGEROUS = Pattern.compile(
            "\\b(sleep|benchmark|load_file)\\s*\\("
                    + "|\\binto\\s+(outfile|dumpfile)\\b"
                    + "|\\bsys_exec\\b",
            Pattern.CASE_INSENSITIVE);

    public void validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SecurityException("SQL为空");
        }

        // 1) 解析 AST，解析不过直接拒绝
        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sql);
        } catch (Exception e) {
            throw new SecurityException("SQL语法解析失败: " + e.getMessage());
        }

        // 2) 只允许单条语句
        List<Statement> list = statements.getStatements();
        if (list == null || list.isEmpty()) {
            throw new SecurityException("未解析到任何SQL语句");
        }
        if (list.size() > 1) {
            throw new SecurityException("不允许多条SQL语句（解析到 " + list.size() + " 条）");
        }

        // 3) 根节点必须是查询
        Statement statement = list.get(0);
        if (!(statement instanceof Select)) {
            throw new SecurityException("只允许 SELECT 查询，当前语句类型: "
                    + statement.getClass().getSimpleName());
        }

        // 4) 危险函数 / 文件写入
        String normalized = sql.replaceAll("\\s+", " ");
        if (DANGEROUS.matcher(normalized).find()) {
            throw new SecurityException("SQL包含禁止的危险函数或文件写入操作");
        }

        List<String> tables = new TablesNamesFinder().getTableList(statement);
        logger.info("SQL安全校验通过(AST): type={}, tables={}",
                statement.getClass().getSimpleName(), tables);
    }
}
