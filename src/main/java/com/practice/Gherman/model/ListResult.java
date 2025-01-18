package com.practice.Gherman.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ListResult<T> {
    private List<T> resources;
    private String cursor;
}
