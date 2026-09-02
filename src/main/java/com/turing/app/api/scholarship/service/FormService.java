package com.turing.app.api.scholarship.service;

import com.turing.app.api.audit.service.AuditService;
import com.turing.app.api.scholarship.dto.*;
import com.turing.app.api.scholarship.entity.*;
import com.turing.app.api.scholarship.exception.ScholarshipException;
import com.turing.app.api.scholarship.repository.*;
import com.turing.app.api.document.repository.DocumentRequirementRepository;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class FormService {
    private static final Set<FormFieldType> OPTION_TYPES=Set.of(FormFieldType.SELECT,FormFieldType.MULTI_SELECT,FormFieldType.RADIO);
    private static final Set<String> RULES=Set.of("minLength","maxLength","min","max","pattern");
    private static final Set<PeriodStatus> CONFIGURABLE_PERIODS=Set.of(PeriodStatus.DRAFT,PeriodStatus.SCHEDULED);
    private final FormDefinitionRepository forms; private final ApplicationPeriodRepository periods; private final DocumentRequirementRepository requirements;
    private final AuditService audit; private final ObjectMapper json; private final Clock clock;
    public FormService(FormDefinitionRepository forms,ApplicationPeriodRepository periods,DocumentRequirementRepository requirements,AuditService audit,ObjectMapper json,Clock clock){this.forms=forms;this.periods=periods;this.requirements=requirements;this.audit=audit;this.json=json;this.clock=clock;}

    @Transactional(readOnly=true)
    public List<FormSummaryResponse> list(UUID periodId){findPeriod(periodId);return forms.findByPeriodIdOrderByVersionNumberDesc(periodId).stream().map(FormSummaryResponse::from).toList();}

    @Transactional(readOnly=true)
    public FormResponse get(UUID id){return FormResponse.from(findForm(id));}

    @Transactional
    public FormResponse create(UUID actor,UUID periodId,FormCreateRequest request,String ip){ApplicationPeriod period=findPeriod(periodId);ensureConfigurable(period);if(forms.findByPeriodIdOrderByVersionNumberDesc(periodId).stream().anyMatch(f->f.getStatus()==FormStatus.DRAFT))throw conflict("FORM_DRAFT_ALREADY_EXISTS","Bu dönem için zaten bir taslak form var.");FormDefinition saved=forms.saveAndFlush(FormDefinition.create(period,request.name().trim(),forms.maxVersion(periodId)+1,clock.instant()));audit.record(actor,"FORM_CREATED","FORM",saved.getId(),"{}",snapshot(saved),ip);return FormResponse.from(saved);}

    @Transactional
    public FormResponse saveSchema(UUID actor,UUID id,FormSchemaRequest request,String ip){FormDefinition form=findForm(id);ensureDraft(form);ensureConfigurable(form.getPeriod());checkVersion(form.getVersion(),request.version());validateSchema(form.getPeriod().getId(),request.sections());String before=snapshot(form);form.replaceSchema(request.name().trim(),toSections(request.sections()),clock.instant());forms.flush();audit.record(actor,"FORM_SCHEMA_UPDATED","FORM",id,before,snapshot(form),ip);return FormResponse.from(form);}

    @Transactional
    public FormResponse publish(UUID actor,UUID id,VersionRequest request,String ip){FormDefinition form=findForm(id);ensureDraft(form);ensureConfigurable(form.getPeriod());checkVersion(form.getVersion(),request.version());if(form.getSections().isEmpty())throw bad("FORM_SCHEMA_EMPTY","Boş form yayınlanamaz.");String before=snapshot(form);forms.findByPeriodIdAndStatus(form.getPeriod().getId(),FormStatus.PUBLISHED).ifPresent(previous->{previous.retire(clock.instant());forms.flush();});form.publish(clock.instant());forms.flush();audit.record(actor,"FORM_PUBLISHED","FORM",id,before,snapshot(form),ip);return FormResponse.from(form);}

    @Transactional
    public FormResponse newVersion(UUID actor,UUID id,VersionRequest request,String ip){FormDefinition source=findForm(id);ensureConfigurable(source.getPeriod());checkVersion(source.getVersion(),request.version());if(source.getStatus()==FormStatus.DRAFT)throw conflict("FORM_IS_DRAFT","Taslak formdan yeni versiyon oluşturulamaz.");if(forms.findByPeriodIdOrderByVersionNumberDesc(source.getPeriod().getId()).stream().anyMatch(f->f.getStatus()==FormStatus.DRAFT))throw conflict("FORM_DRAFT_ALREADY_EXISTS","Bu dönem için zaten bir taslak form var.");FormDefinition copy=FormDefinition.create(source.getPeriod(),source.getName(),forms.maxVersion(source.getPeriod().getId())+1,clock.instant());copy.replaceSchema(source.getName(),copySections(source),clock.instant());forms.saveAndFlush(copy);audit.record(actor,"FORM_VERSION_CREATED","FORM",copy.getId(),"{}",snapshot(copy),ip);return FormResponse.from(copy);}

    private List<FormSection> toSections(List<FormSectionRequest> sections){Instant now=clock.instant();List<FormSection> values=new ArrayList<>();for(int sectionOrder=0;sectionOrder<sections.size();sectionOrder++){FormSectionRequest section=sections.get(sectionOrder);List<FormField> fields=new ArrayList<>();for(int fieldOrder=0;fieldOrder<section.fields().size();fieldOrder++){FormFieldRequest field=section.fields().get(fieldOrder);List<FormFieldOption> options=new ArrayList<>();for(int optionOrder=0;optionOrder<field.options().size();optionOrder++){FormOptionRequest option=field.options().get(optionOrder);options.add(FormFieldOption.create(option.label().trim(),option.value(),optionOrder,now));}fields.add(FormField.create(field.key(),field.label().trim(),field.type(),field.required(),fieldOrder,blankToNull(field.placeholder()),field.requirementId(),field.validationRules(),options,now));}values.add(FormSection.create(section.title().trim(),blankToNull(section.description()),sectionOrder,fields,now));}return values;}
    private List<FormSection> copySections(FormDefinition source){List<FormSectionRequest> requests=source.getSections().stream().map(section->new FormSectionRequest(section.getTitle(),section.getDescription(),section.getFields().stream().map(field->new FormFieldRequest(field.getKey(),field.getLabel(),field.getType(),field.isRequired(),field.getPlaceholder(),field.getRequirementId(),field.getValidationRules(),field.getOptions().stream().map(option->new FormOptionRequest(option.getLabel(),option.getValue())).toList())).toList())).toList();return toSections(requests);}
    private void validateSchema(UUID periodId,List<FormSectionRequest> sections){Set<String> keys=new HashSet<>();for(FormSectionRequest section:sections){for(FormFieldRequest field:section.fields()){if(!keys.add(field.key()))throw bad("DUPLICATE_FIELD_KEY","Alan anahtarları form içinde benzersiz olmalıdır: "+field.key());boolean optionType=OPTION_TYPES.contains(field.type());if(optionType&&field.options().isEmpty())throw bad("FIELD_OPTIONS_REQUIRED","Seçimli alanlarda en az bir seçenek olmalıdır: "+field.key());if(!optionType&&!field.options().isEmpty())throw bad("FIELD_OPTIONS_NOT_ALLOWED","Bu alan türü seçenek kabul etmez: "+field.key());if(field.type()==FormFieldType.FILE){if(field.requirementId()==null)throw bad("FILE_REQUIREMENT_REQUIRED","Dosya alanı bir belge gereksinimine bağlanmalıdır: "+field.key());var requirement=requirements.findById(field.requirementId()).orElseThrow(()->bad("INVALID_FILE_REQUIREMENT","Belge gereksinimi bulunamadı: "+field.key()));if(!requirement.getPeriod().getId().equals(periodId))throw bad("INVALID_FILE_REQUIREMENT","Belge gereksinimi bu döneme ait değil: "+field.key());}else if(field.requirementId()!=null)throw bad("FILE_REQUIREMENT_NOT_ALLOWED","Yalnız FILE alanı belge gereksinimine bağlanabilir: "+field.key());Set<String> optionValues=new HashSet<>();if(field.options().stream().anyMatch(option->!optionValues.add(option.value())))throw bad("DUPLICATE_OPTION_VALUE","Seçenek değerleri alan içinde benzersiz olmalıdır: "+field.key());if(!RULES.containsAll(field.validationRules().keySet()))throw bad("UNSUPPORTED_VALIDATION_RULE","Desteklenmeyen validation kuralı var: "+field.key());validateRules(field);}}}
    private void validateRules(FormFieldRequest field){Map<String,Object> rules=field.validationRules();Integer minLength=integerRule(rules,"minLength");Integer maxLength=integerRule(rules,"maxLength");if(minLength!=null&&minLength<0||maxLength!=null&&maxLength<0||minLength!=null&&maxLength!=null&&minLength>maxLength)throw bad("INVALID_VALIDATION_RULE","Metin uzunluk kuralları geçersiz: "+field.key());if(rules.containsKey("pattern")){if(!(rules.get("pattern") instanceof String regex))throw bad("INVALID_VALIDATION_RULE","Pattern metin olmalıdır: "+field.key());try{Pattern.compile(regex);}catch(PatternSyntaxException exception){throw bad("INVALID_VALIDATION_RULE","Pattern geçerli bir düzenli ifade olmalıdır: "+field.key());}}numberRule(rules,"min");numberRule(rules,"max");}
    private Integer integerRule(Map<String,Object> rules,String key){Object value=rules.get(key);if(value==null)return null;if(!(value instanceof Number number)||number.doubleValue()%1!=0)throw bad("INVALID_VALIDATION_RULE",key+" tam sayı olmalıdır.");return number.intValue();}
    private void numberRule(Map<String,Object> rules,String key){Object value=rules.get(key);if(value!=null&&!(value instanceof Number))throw bad("INVALID_VALIDATION_RULE",key+" sayı olmalıdır.");}
    private void ensureDraft(FormDefinition form){if(form.getStatus()!=FormStatus.DRAFT)throw conflict("FORM_IMMUTABLE","Yayınlanmış veya kullanım dışı form değiştirilemez. Yeni versiyon oluşturun.");}
    private void ensureConfigurable(ApplicationPeriod period){if(!CONFIGURABLE_PERIODS.contains(period.getStatus()))throw conflict("PERIOD_FORM_LOCKED","Form yalnız taslak veya planlanmış dönemde düzenlenebilir.");}
    private ApplicationPeriod findPeriod(UUID id){return periods.findById(id).orElseThrow(()->notFound("PERIOD_NOT_FOUND","Başvuru dönemi bulunamadı."));}
    private FormDefinition findForm(UUID id){return forms.findById(id).orElseThrow(()->notFound("FORM_NOT_FOUND","Form bulunamadı."));}
    private void checkVersion(long current,Long requested){if(requested==null||current!=requested)throw conflict("VERSION_CONFLICT","Kayıt başka bir işlem tarafından güncellendi. Sayfayı yenileyin.");}
    private String snapshot(FormDefinition form){return json.writeValueAsString(Map.of("name",form.getName(),"versionNumber",form.getVersionNumber(),"status",form.getStatus(),"schemaSections",form.getSections().size(),"version",form.getVersion()));}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private ScholarshipException bad(String code,String message){return new ScholarshipException(HttpStatus.BAD_REQUEST,code,message);} private ScholarshipException conflict(String code,String message){return new ScholarshipException(HttpStatus.CONFLICT,code,message);} private ScholarshipException notFound(String code,String message){return new ScholarshipException(HttpStatus.NOT_FOUND,code,message);}
}
