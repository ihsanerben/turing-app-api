package com.turing.app.api.evaluation.entity;

import com.turing.app.api.scholarship.entity.ApplicationPeriod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="evaluation_criteria")
public class EvaluationCriterion {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="period_id") private ApplicationPeriod period;
    @Column(nullable=false,length=160) private String name;
    @Column(length=1000) private String description;
    @Column(name="max_score",nullable=false,precision=8,scale=2) private BigDecimal maxScore;
    @Column(nullable=false,precision=8,scale=2) private BigDecimal weight;
    @Column(name="display_order",nullable=false) private int displayOrder;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false) private long version;
    protected EvaluationCriterion() {}
    public static EvaluationCriterion create(ApplicationPeriod period,String name,String description,BigDecimal maxScore,BigDecimal weight,int order,Instant now){EvaluationCriterion v=new EvaluationCriterion();v.id=UUID.randomUUID();v.period=period;v.update(name,description,maxScore,weight,order,now);v.createdAt=now;return v;}
    public void update(String name,String description,BigDecimal maxScore,BigDecimal weight,int order,Instant now){this.name=name;this.description=description;this.maxScore=maxScore;this.weight=weight;this.displayOrder=order;this.updatedAt=now;}
    public UUID getId(){return id;} public ApplicationPeriod getPeriod(){return period;} public String getName(){return name;} public String getDescription(){return description;} public BigDecimal getMaxScore(){return maxScore;} public BigDecimal getWeight(){return weight;} public int getDisplayOrder(){return displayOrder;} public long getVersion(){return version;}
}
