package com.neosow.infra;

import com.neosow.infra.dto.admin.UserUpdateRequest;
import com.neosow.infra.model.User;
import com.neosow.infra.repository.UserRepository;
import com.neosow.infra.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public class UpdateUserTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "superadmin@neosowinfra.com", roles = {"SUPER_ADMIN"})
    public void testUpdateUser() {
        User user = userRepository.findByEmail("superadmin@neosowinfra.com").get();
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("superadmin@neosowinfra.com");
        req.setRoles(List.of("SUPER_ADMIN"));
        req.setPassword("SuperAdmin@1234");
        req.setEnabled(true);
        adminUserService.updateUser(user.getId(), req);
    }
}
