# How to Add a New Endpoint to a Spring Boot Application

Adding new endpoints to a Spring Boot application involves several steps, including defining the endpoint, creating a new method in the controller, implementing the service method, adding a repository method, creating a response DTO, writing unit tests, updating the documentation, testing the endpoint, committing and pushing changes, and creating a pull request.

## Step 1: Define the Endpoint

Decide on the URL, HTTP method, and the purpose of the endpoint. For example, let's create a new endpoint to fetch user details by their username.

## Step 2: Create a New Method in the Controller

Locate the appropriate controller or create a new one if necessary. For this example, we'll add a new method to the `UserRestApi` controller.

```java
package com.kavun.web.rest.v1;

import com.kavun.backend.service.user.UserService;
import com.kavun.web.payload.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminConstants.API_V1_USERS_ROOT_URL)
public class UserRestApi {

    private final UserService userService;

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse userResponse = userService.findByUsername(username);
        return ResponseEntity.ok(userResponse);
    }
}
```

## Step 3: Implement the Service Method

Add the corresponding method in the `UserService` interface and its implementation.

```java
package com.kavun.backend.service.user;

import com.kavun.web.payload.response.UserResponse;

public interface UserService {
    UserResponse findByUsername(String username);
}
```

```java
package com.kavun.backend.service.user.impl;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.user.UserService;
import com.kavun.web.payload.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserResponse(user);
    }
}
```

## Step 4: Add Repository Method

Add the method to the `UserRepository` to fetch user details by username.

```java
package com.kavun.backend.persistent.repository;

import com.kavun.backend.persistent.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

## Step 5: Create a Response DTO

Create a DTO to structure the response data.

```java
package com.kavun.web.payload.response;

import com.kavun.backend.persistent.domain.user.User;
import lombok.Data;

@Data
public class UserResponse {
    private String username;
    private String email;

    public UserResponse(User user) {
        this.username = user.getUsername();
        this.email = user.getEmail();
    }
}
```

## Step 6: Write Unit Tests

Write unit tests to ensure the new endpoint works as expected.

```java
package com.kavun.web.rest.v1;

import com.kavun.backend.service.user.UserService;
import com.kavun.web.payload.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserRestApiTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserRestApi userRestApi;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userRestApi).build();
    }

    @Test
    void getUserByUsername() throws Exception {
        String username = "testuser";
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(username);
        userResponse.setEmail("testuser@example.com");

        when(userService.findByUsername(username)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
```

## Step 7: Update Documentation

Update the API documentation to include the new endpoint.

```md
## Get User by Username Endpoint

Fetches user details by their username.

The URL for the endpoint is:

```bash
GET: /api/v1/users/username/{username}
```

```json
{
  "username": "testuser",
  "email": "testuser@example.com"
}
```

## Step 8: Test the Endpoint

Run the application and test the new endpoint using tools like Postman or cURL.

```sh
curl -X GET "http://localhost:8080/api/v1/users/username/testuser" -H "accept: application/json"
```

## Step 9: Commit and Push Changes

Commit your changes and push them to the repository.

```sh
git add .
git commit -m "feat: add endpoint to fetch user details by username"
git push origin feature/branch-name
```

## Step 10: Create a Pull Request

Create a pull request to merge your changes into the main branch.

That's it! You've successfully added a new endpoint to your project.

Similar code found with 3 license types
