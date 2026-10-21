package com.example.notes;

import com.kavun.backend.persistent.repository.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRepository extends BaseRepository<Note> {}
