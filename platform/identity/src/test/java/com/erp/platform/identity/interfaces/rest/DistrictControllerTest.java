package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.dto.DistrictListResponse;
import com.erp.platform.identity.domain.District;
import com.erp.platform.identity.infrastructure.persistence.DistrictRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistrictControllerTest {

    @Mock
    private DistrictRepository districtRepository;

    private DistrictController districtController;

    @BeforeEach
    void setUp() {
        districtController = new DistrictController(districtRepository);
    }

    @Test
    @DisplayName("Should list districts by province ordered by name")
    void shouldListDistrictsByProvinceOrderedByName() {
        Long provinceId = 3L;
        District kathmandu = District.builder()
                .name("Kathmandu")
                .nepaliName("काठमाडौं")
                .provinceId(provinceId)
                .build();

        District lalitpur = District.builder()
                .name("Lalitpur")
                .nepaliName("ललितपुर")
                .provinceId(provinceId)
                .build();

        when(districtRepository.findByProvinceIdOrderByName(provinceId))
                .thenReturn(List.of(kathmandu, lalitpur));

        List<DistrictListResponse> result = districtController.listByProvince(provinceId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).districtName()).isEqualTo("Kathmandu");
        assertThat(result.get(0).nepaliName()).isEqualTo("काठमाडौं");
        assertThat(result.get(0).provinceId()).isEqualTo(provinceId);
        assertThat(result.get(1).districtName()).isEqualTo("Lalitpur");
        assertThat(result.get(1).nepaliName()).isEqualTo("ललितपुर");
        assertThat(result.get(1).provinceId()).isEqualTo(provinceId);

        verify(districtRepository).findByProvinceIdOrderByName(provinceId);
    }
}

