package com.turing.app.api.evaluation.entity;

import com.turing.app.api.application.entity.Application;
import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="evaluation_scores")
public class EvaluationScore {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id") private Application application;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="criterion_id") private EvaluationCriterion criterion;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reviewer_id") private User reviewer;
    @Column(nullable=false,precision=8,scale=2) private BigDecimal score;
    @Column(length=2000) private String comment;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false) private long version;
    protected EvaluationScore() {}
    public static EvaluationScore create(Application application,EvaluationCriterion criterion,User reviewer,BigDecimal score,String comment,Instant now){EvaluationScore v=new EvaluationScore();v.id=UUID.randomUUID();v.application=application;v.criterion=criterion;v.reviewer=reviewer;v.score=score;v.comment=comment;v.createdAt=now;v.updatedAt=now;return v;}
    public void update(BigDecimal score,String comment,Instant now){this.score=score;this.comment=comment;this.updatedAt=now;}
    public UUID getId(){return id;} public Application getApplication(){return application;} public EvaluationCriterion getCriterion(){return criterion;} public User getReviewer(){return reviewer;} public BigDecimal getScore(){return score;} public String getComment(){return comment;} public long getVersion(){return version;}
}
