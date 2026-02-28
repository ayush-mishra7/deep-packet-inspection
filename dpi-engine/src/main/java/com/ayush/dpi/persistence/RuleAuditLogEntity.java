package com.ayush.dpi.persistence;

import com.ayush.dpi.decision.Decision;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "rule_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;

    @Column(name = "src_ip")
    private String srcIp;

    @Column(name = "dest_ip")
    private String destIp;

    @Column(name = "dest_port")
    private int destPort;

    @Enumerated(EnumType.STRING)
    private Decision decision;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "matched_sni")
    private String matchedSni;
}
