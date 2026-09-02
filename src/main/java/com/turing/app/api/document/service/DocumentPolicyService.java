package com.turing.app.api.document.service;

import com.turing.app.api.application.entity.Application;
import com.turing.app.api.document.entity.*;
import com.turing.app.api.document.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentPolicyService {
    private final DocumentRequirementRepository requirements;private final StoredFileRepository files;
    public DocumentPolicyService(DocumentRequirementRepository requirements,StoredFileRepository files){this.requirements=requirements;this.files=files;}
    @Transactional(readOnly=true)
    public List<String> missingRequired(Application app){List<DocumentRequirement> required=requirements.findByPeriodIdOrderByDisplayOrderAsc(app.getPeriod().getId()).stream().filter(DocumentRequirement::isRequired).toList();Set<UUID> present=new HashSet<>(files.findByApplicationIdAndStatusOrderByRequirementDisplayOrderAsc(app.getId(),FileStatus.ACTIVE).stream().map(file->file.getRequirement().getId()).toList());return required.stream().filter(value->!present.contains(value.getId())).map(DocumentRequirement::getName).toList();}
}
