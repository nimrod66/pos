package com.example.pos.core.pharmacy.mapper;

import com.example.pos.core.pharmacy.dto.PharmacyRequestDto;
import com.example.pos.core.pharmacy.dto.PharmacyResponseDto;
import com.example.pos.core.pharmacy.model.Pharmacy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PharmacyMapper {

    PharmacyMapper INSTANCE = Mappers.getMapper(PharmacyMapper.class);

    Pharmacy toEntity(PharmacyRequestDto dto);

    @Mapping(target = "id", source = "id")
    PharmacyResponseDto toDto(Pharmacy pharmacy);
}
