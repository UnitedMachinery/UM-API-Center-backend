package com.um.apicenter.apidefinition;

import org.springframework.stereotype.Component;

@Component
public class NamedParameterSqlConverter {

    public String toMyBatisSql(String sql) {
        StringBuilder result = new StringBuilder(sql.length() + 32);
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (lineComment) {
                result.append(current);
                if (current == '\n' || current == '\r') lineComment = false;
                continue;
            }
            if (blockComment) {
                result.append(current);
                if (current == '*' && next == '/') {
                    result.append(next);
                    index++;
                    blockComment = false;
                }
                continue;
            }
            if (!singleQuoted && !doubleQuoted && current == '-' && next == '-') {
                result.append(current).append(next);
                index++;
                lineComment = true;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && current == '/' && next == '*') {
                result.append(current).append(next);
                index++;
                blockComment = true;
                continue;
            }
            if (!doubleQuoted && current == '\'') {
                result.append(current);
                if (singleQuoted && next == '\'') {
                    result.append(next);
                    index++;
                } else {
                    singleQuoted = !singleQuoted;
                }
                continue;
            }
            if (!singleQuoted && current == '"') {
                result.append(current);
                doubleQuoted = !doubleQuoted;
                continue;
            }
            if (!singleQuoted && !doubleQuoted && current == ':' && isNameStart(next)) {
                int end = index + 2;
                while (end < sql.length() && isNamePart(sql.charAt(end))) end++;
                result.append("#{").append(sql, index + 1, end).append('}');
                index = end - 1;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private boolean isNameStart(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    private boolean isNamePart(char character) {
        return isNameStart(character) || (character >= '0' && character <= '9') || character == '_';
    }
}
