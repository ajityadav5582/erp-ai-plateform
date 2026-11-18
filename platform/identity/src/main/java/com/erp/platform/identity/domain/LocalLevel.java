package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_local_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class LocalLevel extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "nepali_name", length = 200)
    private String nepaliName;

    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Column(name = "local_level_type_id", nullable = false)
    private Long localLevelTypeId;
}
