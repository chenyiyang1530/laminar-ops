package com.laminar.ops.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;

    public PageResult() {
    }

    public PageResult(List<T> records, long total) {
        this.records = records;
        this.total = total;
    }
}
