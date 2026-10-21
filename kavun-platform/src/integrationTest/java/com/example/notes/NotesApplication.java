package com.example.notes;

import com.kavun.KavunPlatform;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * An example application built on the platform, in its own package as a real one would be: one
 * resource ({@link Note}), its table, page and messages. The integration tests run the platform
 * as part of it, so they also check that an application can extend the platform.
 */
@SpringBootApplication
@Import(KavunPlatform.class)
public class NotesApplication {}
