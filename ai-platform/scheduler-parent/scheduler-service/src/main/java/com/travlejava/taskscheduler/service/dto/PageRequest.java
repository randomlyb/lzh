package com.travlejava.taskscheduler.service.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int pageNum = 1; // Current page number, 1-indexed
    private int pageSize = 10; // Number of items per page
    private String orderBy; // Field to order by
    private String orderDirection; // asc or desc
}
