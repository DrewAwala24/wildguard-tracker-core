package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.AnimalDto;
import com.kws.backend.backend.model.Animal;
import com.kws.backend.backend.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalService {
    private final AnimalRepository animalRepository;

    public List<AnimalDto> getAllAnimals() {
        return animalRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public AnimalDto registerAnimal(AnimalDto dto) {
        Animal animal = new Animal();
        animal.setName(dto.getName());
        animal.setSpecies(dto.getSpecies());
        animal.setCollarId(dto.getCollarId());
        Animal saved = animalRepository.save(animal);
        return toDto(saved);
    }

    private AnimalDto toDto(Animal animal) {
        AnimalDto dto = new AnimalDto();
        dto.setId(animal.getId());
        dto.setName(animal.getName());
        dto.setSpecies(animal.getSpecies());
        dto.setCollarId(animal.getCollarId());
        return dto;
    }
}