package com.kavun.backend.serializer;

import com.kavun.backend.persistent.repository.UserRepository;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * Custom JSON serializer that converts Long to user object containing both id and name
 */
public class UserInfoObjectSerializer extends JsonSerializer<Long> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void serialize(Long userId, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (userId == null) {
            gen.writeNull();
            return;
        }

        try {
            // Get user info from database
            var userInfo = userRepository.findUserInfoById(userId);
            if (userInfo.isPresent()) {
                gen.writeStartObject();
                gen.writeStringField("id", userId.toString());
                gen.writeStringField("name", userInfo.get().getFullName());
                gen.writeEndObject();
            } else {
                // If user not found, write object with UUID only
                gen.writeStartObject();
                gen.writeStringField("id", userId.toString());
                gen.writeNullField("name");
                gen.writeEndObject();
            }
        } catch (Exception e) {
            // In case of any error, fallback to object with UUID only
            gen.writeStartObject();
            gen.writeStringField("id", userId.toString());
            gen.writeNullField("name");
            gen.writeEndObject();
        }
    }
}
