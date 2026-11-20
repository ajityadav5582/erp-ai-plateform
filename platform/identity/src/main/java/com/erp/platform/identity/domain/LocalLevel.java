package com.erp.platform.identity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "master_local_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class LocalLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "smallint")
    private Short id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "nepali_name", length = 200)
    private String nepaliName;

    @Column(name = "district_id", nullable = false)
    private Short districtId;

    @Column(name = "local_level_type_id", nullable = false)
    private Short localLevelTypeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
