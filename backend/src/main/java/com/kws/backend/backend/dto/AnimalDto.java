package com.kws.backend.backend.dto;

import lombok.Data;

@Data
public class AnimalDto {
    private Long id;
    private String name;
    private String species;
    private String collarId;
}