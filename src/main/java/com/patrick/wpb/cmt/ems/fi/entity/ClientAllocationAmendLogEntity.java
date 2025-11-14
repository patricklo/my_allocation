package com.patrick.wpb.cmt.ems.fi.entity;

import com.patrick.wpb.cmt.ems.fi.entity.base.BaseAuditEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentObjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client_allocation_amend_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ClientAllocationAmendLogEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Column(name = "ref_id", nullable = false, length = 64)
    private String refId;

    @Enumerated(EnumType.STRING)
    @Column(name = "obj_type", nullable = false, length = 32)
    private AmendmentObjectType objectType;

    @Lob
    @Column(name = "before_obj")
    private String beforeObjectJson;

    @Lob
    @Column(name = "after_obj")
    private String afterObjectJson;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;
}

