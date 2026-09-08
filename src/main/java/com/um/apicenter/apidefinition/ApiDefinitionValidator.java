package com.um.apicenter.apidefinition;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ApiDefinitionValidator {

    private static final Pattern API_PATH = Pattern.compile("^/[A-Za-z0-9][A-Za-z0-9._/-]*$");
    private static final Pattern PARAMETER_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");
    private static final Pattern NAMED_PARAMETER = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)");
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "MERGE", "CREATE", "ALTER", "DROP", "TRUNCATE",
            "CALL", "EXEC", "EXECUTE", "GRANT", "REVOKE", "SET", "USE", "BEGIN", "COMMIT", "ROLLBACK",
            "INTO");

    public void validate(String apiPath, List<ApiParameterDefinition> parameters, String sqlText) {
        validatePath(apiPath);
        Set<String> parameterNames = validateParameters(parameters);
        validateSql(sqlText, parameterNames);
    }

    private void validatePath(String apiPath) {
        if (apiPath == null || !API_PATH.matcher(apiPath).matches() || apiPath.contains("//") || apiPath.contains("..")
                || apiPath.equals("/") || apiPath.startsWith("/admin") || apiPath.startsWith("/health")) {
            throw new ApiDefinitionValidationException("接口路径格式不合法");
        }
    }

    private Set<String> validateParameters(List<ApiParameterDefinition> parameters) {
        if (parameters == null) {
            throw new ApiDefinitionValidationException("参数定义不能为空");
        }
        Set<String> names = new HashSet<>();
        for (ApiParameterDefinition parameter : parameters) {
            if (parameter == null || parameter.name() == null || !PARAMETER_NAME.matcher(parameter.name()).matches()
                    || parameter.type() == null || !names.add(parameter.name())) {
                throw new ApiDefinitionValidationException("参数定义不合法或存在重复名称");
            }
            if (parameter.maxLength() != null && (parameter.maxLength() < 1 || parameter.maxLength() > 1000)) {
                throw new ApiDefinitionValidationException("参数最大长度必须在 1 至 1000 之间");
            }
            if (parameter.type() == ApiParameterType.STRING && parameter.maxLength() == null) {
                throw new ApiDefinitionValidationException("字符串参数必须设置最大长度");
            }
        }
        return names;
    }

    private void validateSql(String sqlText, Set<String> definedParameters) {
        if (sqlText == null || sqlText.isBlank() || sqlText.contains("${")) {
            throw new ApiDefinitionValidationException("SQL 仅允许使用命名参数绑定");
        }
        String executableSql = stripStringsAndComments(sqlText);
        if (executableSql.indexOf(';') >= 0) {
            throw new ApiDefinitionValidationException("SQL 仅允许单条 SELECT 查询");
        }
        List<String> keywords = words(executableSql);
        if (keywords.isEmpty() || !"SELECT".equals(keywords.get(0))
                || keywords.stream().anyMatch(FORBIDDEN_KEYWORDS::contains)) {
            throw new ApiDefinitionValidationException("SQL 仅允许单条 SELECT 查询");
        }
        for (int index = 0; index + 1 < keywords.size(); index++) {
            if ("FOR".equals(keywords.get(index)) && "UPDATE".equals(keywords.get(index + 1))) {
                throw new ApiDefinitionValidationException("SQL 仅允许单条 SELECT 查询");
            }
        }

        Set<String> sqlParameters = new HashSet<>();
        Matcher matcher = NAMED_PARAMETER.matcher(executableSql);
        while (matcher.find()) {
            sqlParameters.add(matcher.group(1));
        }
        if (!definedParameters.equals(sqlParameters)) {
            throw new ApiDefinitionValidationException("SQL 命名参数必须与参数定义完全一致");
        }
    }

    private List<String> words(String sql) {
        Matcher matcher = Pattern.compile("[A-Za-z]+") .matcher(sql.toUpperCase(Locale.ROOT));
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private String stripStringsAndComments(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (lineComment) {
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                    result.append(current);
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    result.append("  ");
                    index++;
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (!singleQuoted && !doubleQuoted && current == '-' && next == '-') {
                lineComment = true;
                result.append("  ");
                index++;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && current == '/' && next == '*') {
                blockComment = true;
                result.append("  ");
                index++;
                continue;
            }
            if (!doubleQuoted && current == '\'') {
                singleQuoted = !singleQuoted;
                result.append(' ');
                continue;
            }
            if (!singleQuoted && current == '"') {
                doubleQuoted = !doubleQuoted;
                result.append(' ');
                continue;
            }
            result.append(singleQuoted || doubleQuoted ? ' ' : current);
        }
        return result.toString();
    }
}
