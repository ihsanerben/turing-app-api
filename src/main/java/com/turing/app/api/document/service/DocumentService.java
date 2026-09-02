package com.turing.app.api.document.service;

import com.turing.app.api.application.entity.*;
import com.turing.app.api.application.exception.ApplicationException;
import com.turing.app.api.application.repository.ApplicationRepository;
import com.turing.app.api.document.dto.*;
import com.turing.app.api.document.entity.*;
import com.turing.app.api.document.repository.*;
import com.turing.app.api.document.storage.*;
import com.turing.app.api.scholarship.entity.*;
import com.turing.app.api.scholarship.repository.ApplicationPeriodRepository;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private static final Logger log=LoggerFactory.getLogger(DocumentService.class);
    private static final Set<PeriodStatus> CONFIGURABLE=Set.of(PeriodStatus.DRAFT,PeriodStatus.SCHEDULED);
    private final DocumentRequirementRepository requirements;private final StoredFileRepository files;private final ApplicationRepository applications;private final ApplicationPeriodRepository periods;private final UserRepository users;private final ObjectProvider<ObjectStorage> storageProvider;private final Clock clock;
    public DocumentService(DocumentRequirementRepository requirements,StoredFileRepository files,ApplicationRepository applications,ApplicationPeriodRepository periods,UserRepository users,ObjectProvider<ObjectStorage> storageProvider,Clock clock){this.requirements=requirements;this.files=files;this.applications=applications;this.periods=periods;this.users=users;this.storageProvider=storageProvider;this.clock=clock;}

    @Transactional(readOnly=true) public List<DocumentRequirementResponse> adminRequirements(UUID periodId){findPeriod(periodId);return requirements.findByPeriodIdOrderByDisplayOrderAsc(periodId).stream().map(DocumentRequirementResponse::from).toList();}
    @Transactional
    public DocumentRequirementResponse createRequirement(UUID periodId,DocumentRequirementRequest request){ApplicationPeriod period=findPeriod(periodId);if(!CONFIGURABLE.contains(period.getStatus()))throw conflict("REQUIREMENTS_LOCKED","Belge gereksinimleri yalnız taslak veya planlanmış dönemde değiştirilebilir.");if(requirements.existsByPeriodIdAndNameIgnoreCase(periodId,request.name().trim()))throw conflict("REQUIREMENT_ALREADY_EXISTS","Bu belge gereksinimi zaten var.");List<String> mime=request.allowedMimeTypes().stream().distinct().toList();try{return DocumentRequirementResponse.from(requirements.saveAndFlush(DocumentRequirement.create(period,request.name().trim(),clean(request.description()),request.required(),mime,request.maxSizeBytes(),request.order(),clock.instant())));}catch(DataIntegrityViolationException exception){throw conflict("REQUIREMENT_CONFLICT","Belge adı veya sırası bu dönemde kullanılıyor.");}}

    @Transactional(readOnly=true)
    public List<StoredFileResponse> list(UUID userId,UUID applicationId){findOwned(userId,applicationId);return files.findByApplicationIdAndStatusOrderByRequirementDisplayOrderAsc(applicationId,FileStatus.ACTIVE).stream().map(StoredFileResponse::from).toList();}

    @Transactional
    public StoredFileResponse upload(UUID userId,UUID applicationId,UUID requirementId,MultipartFile multipart){Application app=findEditable(userId,applicationId);DocumentRequirement requirement=requirements.findById(requirementId).orElseThrow(()->notFound("REQUIREMENT_NOT_FOUND","Belge gereksinimi bulunamadı."));if(!requirement.getPeriod().getId().equals(app.getPeriod().getId()))throw bad("REQUIREMENT_PERIOD_MISMATCH","Belge gereksinimi bu başvuru dönemine ait değil.");byte[] content=bytes(multipart);String original=safeName(multipart.getOriginalFilename());String declared=Optional.ofNullable(multipart.getContentType()).orElse("").toLowerCase(Locale.ROOT);validateFile(requirement,original,declared,content);String key="applications/"+applicationId+"/"+requirementId+"/"+UUID.randomUUID();String checksum=sha256(content);ObjectStorage storage=storage();storage.put(key,content,declared);try{files.findByApplicationIdAndRequirementIdAndStatus(applicationId,requirementId,FileStatus.ACTIVE).ifPresent(previous->{previous.replace(clock.instant());files.flush();});User owner=users.findById(userId).orElseThrow(()->notFound("USER_NOT_FOUND","Kullanıcı bulunamadı."));StoredFile saved=files.saveAndFlush(StoredFile.create(owner,app,requirement,original,key,declared,content.length,checksum,clock.instant()));return StoredFileResponse.from(saved);}catch(RuntimeException exception){compensateDelete(storage,key);if(exception instanceof ApplicationException applicationException)throw applicationException;throw new ApplicationException(HttpStatus.CONFLICT,"FILE_METADATA_FAILED","Dosya kaydı tamamlanamadı. Tekrar deneyin.");}}

    @Transactional(readOnly=true)
    public DocumentDownload download(UUID userId,UUID fileId){StoredFile file=findOwnedFile(userId,fileId);if(file.getStatus()==FileStatus.DELETED)throw notFound("FILE_NOT_FOUND","Dosya bulunamadı.");return new DocumentDownload(file.getOriginalName(),file.getMimeType(),storage().get(file.getStorageKey()));}

    @Transactional
    public void delete(UUID userId,UUID fileId){StoredFile file=findOwnedFile(userId,fileId);if(file.getStatus()!=FileStatus.ACTIVE)throw conflict("FILE_NOT_ACTIVE","Yalnız aktif dosya silinebilir.");findEditable(userId,file.getApplication().getId());file.delete(clock.instant());files.flush();String key=file.getStorageKey();ObjectStorage storage=storage();TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){try{storage.delete(key);}catch(RuntimeException exception){log.error("Orphan object cleanup failed key={}",key,exception);}}});}

    @Transactional(readOnly=true)
    public List<DocumentRequirementResponse> applicationRequirements(UUID userId,UUID applicationId){Application app=findOwned(userId,applicationId);return requirements.findByPeriodIdOrderByDisplayOrderAsc(app.getPeriod().getId()).stream().map(DocumentRequirementResponse::from).toList();}

    private Application findEditable(UUID userId,UUID id){Application app=findOwned(userId,id);if(app.getStatus()==ApplicationStatus.MISSING_DOCUMENT)return app;Instant now=clock.instant();if(app.getStatus()!=ApplicationStatus.DRAFT)throw conflict("DOCUMENTS_LOCKED","Bu durumdaki başvurunun belgeleri değiştirilemez.");if(app.getPeriod().getStatus()!=PeriodStatus.OPEN||now.isBefore(app.getPeriod().getStartsAt())||!now.isBefore(app.getPeriod().getEndsAt()))throw conflict("APPLICATION_PERIOD_CLOSED","Başvuru dönemi açık değil.");return app;}
    private Application findOwned(UUID userId,UUID id){return applications.findByIdAndProfileUserId(id,userId).orElseThrow(()->notFound("APPLICATION_NOT_FOUND","Başvuru bulunamadı."));}
    private StoredFile findOwnedFile(UUID userId,UUID id){return files.findByIdAndOwnerId(id,userId).orElseThrow(()->notFound("FILE_NOT_FOUND","Dosya bulunamadı."));}
    private ApplicationPeriod findPeriod(UUID id){return periods.findById(id).orElseThrow(()->notFound("PERIOD_NOT_FOUND","Başvuru dönemi bulunamadı."));}
    private ObjectStorage storage(){ObjectStorage storage=storageProvider.getIfAvailable();if(storage==null)throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE,"STORAGE_UNAVAILABLE","Dosya servisi kullanılamıyor.");return storage;}
    private byte[] bytes(MultipartFile file){if(file==null||file.isEmpty())throw bad("FILE_EMPTY","Dosya boş olamaz.");try{return file.getBytes();}catch(Exception exception){throw bad("FILE_READ_FAILED","Dosya okunamadı.");}}
    private void validateFile(DocumentRequirement requirement,String name,String declared,byte[] content){if(content.length>requirement.getMaxSizeBytes())throw new ApplicationException(HttpStatus.PAYLOAD_TOO_LARGE,"FILE_TOO_LARGE","Dosya izin verilen boyutu aşıyor.");if(!requirement.getAllowedMimeTypes().contains(declared))throw new ApplicationException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"FILE_TYPE_NOT_ALLOWED","Bu dosya türüne izin verilmiyor.");String detected=detect(content);if(!declared.equals(detected)||!extensionMatches(name,detected))throw new ApplicationException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"FILE_SIGNATURE_MISMATCH","Dosya içeriği veya uzantısı bildirilen türle eşleşmiyor.");}
    private String detect(byte[] bytes){if(bytes.length>=5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))return "application/pdf";if(bytes.length>=8&&bytes[0]==(byte)0x89&&bytes[1]==0x50&&bytes[2]==0x4e&&bytes[3]==0x47)return "image/png";if(bytes.length>=3&&bytes[0]==(byte)0xff&&bytes[1]==(byte)0xd8&&bytes[2]==(byte)0xff)return "image/jpeg";return "application/octet-stream";}
    private boolean extensionMatches(String name,String mime){String lower=name.toLowerCase(Locale.ROOT);return mime.equals("application/pdf")&&lower.endsWith(".pdf")||mime.equals("image/png")&&lower.endsWith(".png")||mime.equals("image/jpeg")&&(lower.endsWith(".jpg")||lower.endsWith(".jpeg"));}
    private String safeName(String value){if(value==null||value.isBlank())throw bad("INVALID_FILE_NAME","Dosya adı geçersiz.");String name=value.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).trim();if(name.isBlank()||name.length()>255||name.chars().anyMatch(Character::isISOControl))throw bad("INVALID_FILE_NAME","Dosya adı geçersiz.");return name;}
    private String sha256(byte[] content){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));}catch(Exception exception){throw new IllegalStateException(exception);}}
    private void compensateDelete(ObjectStorage storage,String key){try{storage.delete(key);}catch(RuntimeException cleanup){log.error("Upload compensation failed key={}",key,cleanup);}}
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private ApplicationException bad(String code,String message){return new ApplicationException(HttpStatus.BAD_REQUEST,code,message);}private ApplicationException conflict(String code,String message){return new ApplicationException(HttpStatus.CONFLICT,code,message);}private ApplicationException notFound(String code,String message){return new ApplicationException(HttpStatus.NOT_FOUND,code,message);}
}
