package com.example.notes;

import com.kavun.IntegrationTestUtils;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.TenantContext;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** An application's own resource, table, page and messages work next to the platform's. */
@TestInstance(Lifecycle.PER_CLASS)
class NotesIntegrationTest extends IntegrationTestUtils {

  private static final String NOTES_URL = "/api/v1/note";

  @Autowired private transient NoteService noteService;
  @Autowired private transient MessageSource messageSource;

  private transient UserDto admin;

  @BeforeAll
  void beforeAll() {
    admin = createAndAssertAdmin(UserUtils.createUserDto(true));
  }

  @Test
  void adminManagesNotesThroughTheGenericEndpoints() throws Exception {
    String title = uniqueTitle();

    perform(
            MockMvcRequestBuilders.post(NOTES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"content\":\"Buy milk\"}"),
            admin)
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value(title));
    perform(MockMvcRequestBuilders.get(NOTES_URL).param("title", title), admin)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].content").value("Buy milk"));
  }

  @Test
  void notesNeedTheirPagePermission() throws Exception {
    var reader = createUserWithRoles(createRoleWithPermissions("NOTES:VIEW"));

    perform(MockMvcRequestBuilders.get(NOTES_URL), createUserWithRoles())
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    perform(MockMvcRequestBuilders.get(NOTES_URL), reader)
        .andExpect(MockMvcResultMatchers.status().isOk());
    perform(
            MockMvcRequestBuilders.post(NOTES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + uniqueTitle() + "\"}"),
            reader)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void notesBelongToTheTenantThatCreatedThem() {
    String title = uniqueTitle();
    TenantContext.runAs(
        "notes-acme", () -> noteService.create(NoteRequest.builder().title(title).build()));

    Assertions.assertEquals(
        1, TenantContext.callAs("notes-acme", () -> noteService.count(Map.of("title", title))));
    Assertions.assertEquals(
        0, TenantContext.callAs("notes-globex", () -> noteService.count(Map.of("title", title))));
  }

  @Test
  void applicationMessagesResolveNextToThePlatformMessages() {
    var turkish = Locale.forLanguageTag("tr");

    Assertions.assertEquals("Notes", messageSource.getMessage("notes.title", null, Locale.ENGLISH));
    Assertions.assertEquals("Notlar", messageSource.getMessage("notes.title", null, turkish));
    Assertions.assertEquals(
        "You do not have permission to perform this action.",
        messageSource.getMessage("error.access.denied", null, Locale.ENGLISH));
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, UserDto user)
      throws Exception {
    return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerTokenFor(user)));
  }

  private static String uniqueTitle() {
    return "Note " + UUID.randomUUID();
  }
}
