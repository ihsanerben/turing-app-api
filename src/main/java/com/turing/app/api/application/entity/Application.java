package com.turing.app.api.application.entity;

import com.turing.app.api.profile.entity.StudentProfile;
import com.turing.app.api.scholarship.entity.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="applications")
public class Application {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="profile_id") private StudentProfile profile;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="period_id") private ApplicationPeriod period;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="form_id") private FormDefinition form;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private ApplicationStatus status;
    @Column(nullable=false) private int completion;
    @Column(name="calculated_score",precision=8,scale=3) private BigDecimal calculatedScore;
    @Column(name="submitted_at") private Instant submittedAt;
    @Column(name="decision_at") private Instant decisionAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false) private long version;
    protected Application() {}
    public static Application draft(StudentProfile profile,ApplicationPeriod period,FormDefinition form,Instant now){Application value=new Application();value.id=UUID.randomUUID();value.profile=profile;value.period=period;value.form=form;value.status=ApplicationStatus.DRAFT;value.createdAt=now;value.updatedAt=now;return value;}
    public void answersChanged(int value,Instant now){completion=value;updatedAt=now;}
    public void submit(Instant now){status=ApplicationStatus.SUBMITTED;completion=100;submittedAt=now;updatedAt=now;}
    public void resubmit(Instant now){status=ApplicationStatus.SUBMITTED;updatedAt=now;}
    public void withdraw(Instant now){status=ApplicationStatus.WITHDRAWN;updatedAt=now;}
    public void changeStatus(ApplicationStatus next,Instant now){status=next;if(next==ApplicationStatus.APPROVED||next==ApplicationStatus.REJECTED||next==ApplicationStatus.WAITLISTED)decisionAt=now;updatedAt=now;}
    public void updateCalculatedScore(BigDecimal score,Instant now){calculatedScore=score;updatedAt=now;}
    public UUID getId(){return id;} public StudentProfile getProfile(){return profile;} public ApplicationPeriod getPeriod(){return period;} public FormDefinition getForm(){return form;} public ApplicationStatus getStatus(){return status;} public int getCompletion(){return completion;} public BigDecimal getCalculatedScore(){return calculatedScore;} public Instant getSubmittedAt(){return submittedAt;} public Instant getDecisionAt(){return decisionAt;} public Instant getCreatedAt(){return createdAt;} public long getVersion(){return version;}
}
