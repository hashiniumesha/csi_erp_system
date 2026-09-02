package erp_backend.controller;

import erp_backend.entity.AppUser;
import erp_backend.entity.Role;
import erp_backend.repository.AppUserRepository;
import erp_backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public static class UserSummary {
        public Integer userId;
        public String username;
        public String fullName;
        public String roleName;
        public String status;
    }

    public static class CreateUserRequest {
        public String fullName;
        public String username;
        public String password;
        public Integer roleId;
    }

    @GetMapping
    public List<UserSummary> listUsers() {
        return appUserRepository.findAll().stream().map(UserController::toSummary).toList();
    }

    @PostMapping
    public UserSummary createUser(@RequestBody CreateUserRequest request) {
        Role role = roleRepository.findById(request.roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        AppUser user = new AppUser();
        user.setFullName(request.fullName);
        user.setUsername(request.username);
        user.setPasswordHash(passwordEncoder.encode(request.password));
        user.setRole(role);
        user.setStatus(AppUser.Status.Active);

        return toSummary(appUserRepository.save(user));
    }

    private static UserSummary toSummary(AppUser user) {
        UserSummary summary = new UserSummary();
        summary.userId = user.getUserId();
        summary.username = user.getUsername();
        summary.fullName = user.getFullName();
        summary.roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        summary.status = user.getStatus() != null ? user.getStatus().name() : null;
        return summary;
    }
}
