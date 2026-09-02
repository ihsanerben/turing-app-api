package com.turing.app.api.document.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.turing.app.api.application.entity.Application;
import com.turing.app.api.application.entity.ApplicationStatus;
import com.turing.app.api.application.repository.ApplicationRepository;
import com.turing.app.api.document.entity.DocumentRequirement;
import com.turing.app.api.document.repository.DocumentRequirementRepository;
import com.turing.app.api.document.repository.StoredFileRepository;
import com.turing.app.api.document.storage.ObjectStorage;
import com.turing.app.api.scholarship.entity.ApplicationPeriod;
import com.turing.app.api.scholarship.entity.PeriodStatus;
import com.turing.app.api.scholarship.repository.ApplicationPeriodRepository;
import com.turing.app.api.user.entity.User;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

class DocumentServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void removesUploadedObjectWhenMetadataPersistenceFails() {
        DocumentRequirementRepository requirements=mock(DocumentRequirementRepository.class);StoredFileRepository files=mock(StoredFileRepository.class);ApplicationRepository applications=mock(ApplicationRepository.class);ApplicationPeriodRepository periods=mock(ApplicationPeriodRepository.class);UserRepository users=mock(UserRepository.class);ObjectProvider<ObjectStorage> provider=mock(ObjectProvider.class);ObjectStorage storage=mock(ObjectStorage.class);Clock clock=Clock.fixed(Instant.parse("2026-09-02T09:00:00Z"),ZoneOffset.UTC);
        UUID userId=UUID.randomUUID(),applicationId=UUID.randomUUID(),requirementId=UUID.randomUUID();ApplicationPeriod period=mock(ApplicationPeriod.class);Application application=mock(Application.class);DocumentRequirement requirement=mock(DocumentRequirement.class);User user=mock(User.class);
        when(applications.findByIdAndProfileUserId(applicationId,userId)).thenReturn(Optional.of(application));when(application.getStatus()).thenReturn(ApplicationStatus.DRAFT);when(application.getPeriod()).thenReturn(period);when(period.getId()).thenReturn(UUID.randomUUID());when(period.getStatus()).thenReturn(PeriodStatus.OPEN);when(period.getStartsAt()).thenReturn(clock.instant().minusSeconds(60));when(period.getEndsAt()).thenReturn(clock.instant().plusSeconds(60));when(requirements.findById(requirementId)).thenReturn(Optional.of(requirement));when(requirement.getPeriod()).thenReturn(period);when(requirement.getMaxSizeBytes()).thenReturn(1024L);when(requirement.getAllowedMimeTypes()).thenReturn(List.of("application/pdf"));when(provider.getIfAvailable()).thenReturn(storage);when(users.findById(userId)).thenReturn(Optional.of(user));when(files.findByApplicationIdAndRequirementIdAndStatus(any(),any(),any())).thenReturn(Optional.empty());when(files.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("forced"));
        DocumentService service=new DocumentService(requirements,files,applications,periods,users,provider,clock);

        assertThatThrownBy(()->service.upload(userId,applicationId,requirementId,new MockMultipartFile("file","proof.pdf","application/pdf","%PDF-1.4 proof".getBytes())))
                .hasMessageContaining("Dosya kaydı tamamlanamadı");
        verify(storage).put(anyString(),any(),eq("application/pdf"));
        verify(storage).delete(anyString());
    }
}
