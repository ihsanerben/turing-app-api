package com.turing.app.api.application.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="application_snapshots")
public class ApplicationSnapshot {
    @Id private UUID id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id") private Application application;
    @Column(name="schema_version",nullable=false) private int schemaVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="profile_data",nullable=false,columnDefinition="jsonb") private Map<String,Object> profileData;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected ApplicationSnapshot() {}
    public static ApplicationSnapshot create(Application app,int schemaVersion,Map<String,Object> data,Instant now){ApplicationSnapshot value=new ApplicationSnapshot();value.id=UUID.randomUUID();value.application=app;value.schemaVersion=schemaVersion;value.profileData=new LinkedHashMap<>(data);value.createdAt=now;return value;}
}
