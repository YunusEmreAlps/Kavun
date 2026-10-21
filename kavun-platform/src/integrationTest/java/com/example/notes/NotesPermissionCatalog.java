package com.example.notes;

import com.kavun.backend.service.security.permission.PermissionCatalog;
import com.kavun.backend.service.security.permission.PermissionCatalogContributor;
import com.kavun.constant.PermissionConstants.Actions;
import org.springframework.stereotype.Component;

@Component
public class NotesPermissionCatalog implements PermissionCatalogContributor {

  public static final String NOTES = "NOTES";

  @Override
  public void contribute(PermissionCatalog.Builder catalog) {
    catalog
        .page(NOTES)
        .name("Notes")
        .url("/notes")
        .icon("fa-solid fa-note-sticky")
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
  }
}
