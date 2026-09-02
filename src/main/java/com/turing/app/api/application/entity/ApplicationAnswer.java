package com.turing.app.api.application.entity;

import com.turing.app.api.scholarship.entity.FormField;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="application_answers")
public class ApplicationAnswer {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id") private Application application;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="field_id") private FormField field;
    @Column(name="text_value") private String textValue;
    @Column(name="number_value",precision=18,scale=4) private BigDecimal numberValue;
    @Column(name="boolean_value") private Boolean booleanValue;
    @Column(name="date_value") private LocalDate dateValue;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="json_value",columnDefinition="jsonb") private List<String> jsonValue;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false) private long version;
    protected ApplicationAnswer() {}
    public static ApplicationAnswer text(Application app,FormField field,String value,Instant now){ApplicationAnswer answer=base(app,field,now);answer.textValue=value;return answer;}
    public static ApplicationAnswer number(Application app,FormField field,BigDecimal value,Instant now){ApplicationAnswer answer=base(app,field,now);answer.numberValue=value;return answer;}
    public static ApplicationAnswer bool(Application app,FormField field,Boolean value,Instant now){ApplicationAnswer answer=base(app,field,now);answer.booleanValue=value;return answer;}
    public static ApplicationAnswer date(Application app,FormField field,LocalDate value,Instant now){ApplicationAnswer answer=base(app,field,now);answer.dateValue=value;return answer;}
    public static ApplicationAnswer multiple(Application app,FormField field,List<String> value,Instant now){ApplicationAnswer answer=base(app,field,now);answer.jsonValue=List.copyOf(value);return answer;}
    private static ApplicationAnswer base(Application app,FormField field,Instant now){ApplicationAnswer answer=new ApplicationAnswer();answer.id=UUID.randomUUID();answer.application=app;answer.field=field;answer.createdAt=now;answer.updatedAt=now;return answer;}
    public UUID getId(){return id;} public FormField getField(){return field;} public Object getValue(){if(textValue!=null)return textValue;if(numberValue!=null)return numberValue;if(booleanValue!=null)return booleanValue;if(dateValue!=null)return dateValue;return jsonValue;}
}
