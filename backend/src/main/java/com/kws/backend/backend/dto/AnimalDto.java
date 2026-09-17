package com.kws.backend.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnimalDto {
    private Long id;
    private String name;
    private String species;
    private String collarId;
    private String parkName;
    private String sex;
    private Integer collarBattery;
    private Double latitude;
    private Double longitude;
    private String status;
    private Boolean isBreaching;
    private LocalDateTime lastSeen;
}