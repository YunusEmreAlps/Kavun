package com.kavun.backend.serializer;

import com.kavun.backend.persistent.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom JSON serializer that converts Long to user object containing both id and name
 * Used specifically for createdBy, updatedBy, and deletedBy fields via @JsonSerialize annotation
 */
@Slf4j
@Component
public class UserInfoObjectSerializer extends JsonSerializer<Long> {

    private final UserRepository userRepository;

    public UserInfoObjectSerializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void serialize(Long userId, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (userId == null) {
            gen.writeNull();
            return;
        }

        try {
            // Get user info from database
            var userInfo = userRepository.findUserInfoById(userId);
            LOG.info("Serializing userId {}: found userInfo {}", userId, userInfo);
            if (userInfo.isPresent()) {
                gen.writeStartObject();
                gen.writeStringField("id", userId.toString());
                gen.writeStringField("name", userInfo.get().getFullName());
                gen.writeEndObject();
            } else {
                // If user not found, write object with Long only
                gen.writeStartObject();
                gen.writeStringField("id", userId.toString());
                gen.writeNullField("name");
                gen.writeEndObject();
            }
        } catch (Exception e) {
            // In case of any error, fallback to object with Long only
            gen.writeStartObject();
            gen.writeStringField("id", userId.toString());
            gen.writeNullField("name");
            gen.writeEndObject();
        }
    }
}
