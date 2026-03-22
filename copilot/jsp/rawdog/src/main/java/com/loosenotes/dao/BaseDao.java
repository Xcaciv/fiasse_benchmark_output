package com.loosenotes.dao;

import java.time.LocalDateTime;

abstract class BaseDao {
    protected String now() {
        return LocalDateTime.now().toString();
    }

    protected LocalDateTime parseTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }

    protected String excerpt(String content) {
        if (content == null) {
            return "";
        }
        String collapsed = content.replaceAll("\s+", " ").trim();
        return collapsed.length() <= 200 ? collapsed : collapsed.substring(0, 200) + "...";
    }
}
