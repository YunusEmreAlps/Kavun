package com.example.notes;

import com.kavun.backend.service.AbstractService;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoteService
    extends AbstractService<NoteRequest, Note, NoteDto, NoteRepository, NoteMapper> {

  public NoteService(NoteMapper mapper, NoteRepository repository) {
    super(mapper, repository);
  }

  @Override
  protected Set<String> filterableFields() {
    return Set.of("title", "createdAt");
  }
}
