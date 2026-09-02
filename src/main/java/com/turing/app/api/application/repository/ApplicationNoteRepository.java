package com.turing.app.api.application.repository;
import com.turing.app.api.application.entity.ApplicationNote;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote,UUID>{List<ApplicationNote> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);}
