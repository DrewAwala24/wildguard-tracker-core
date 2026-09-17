package com.kws.backend.backend.dto;

import lombok.Data;

@Data
public class PatrolUnitDto {
    private Long id;
    private String name;
    private String callSign;
    private String unitType;
    private Double latitude;
    private Double longitude;
    private String status;
    private String sector;
}
