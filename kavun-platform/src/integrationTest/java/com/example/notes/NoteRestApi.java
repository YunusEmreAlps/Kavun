package com.example.notes;

import com.kavun.annotation.RequirePermission;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/note")
@Tag(name = "Notes", description = "APIs for managing notes")
@RequirePermission(page = NotesPermissionCatalog.NOTES)
public class NoteRestApi extends AbstractCrudRestApi<NoteRequest, NoteDto> {

  public NoteRestApi(NoteService service) {
    super(service);
  }
}
