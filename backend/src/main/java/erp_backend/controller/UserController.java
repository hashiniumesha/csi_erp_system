package erp_backend.controller;

import erp_backend.entity.AppUser;
import erp_backend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private AppUserRepository appUserRepository;

    // Lightweight projection — deliberately excludes passwordHash, unlike the
    // raw AppUser entity, since this list is used to populate officer-picker
    // dropdowns (QC Officer, Sales Officer) and must be safe to expose broadly.
    public static class UserSummary {
        public Integer userId;
        public String username;
        public String fullName;
        public String roleName;
        public String status;
    }

    @GetMapping
    public List<UserSummary> listUsers() {
        return appUserRepository.findAll().stream().map(UserController::toSummary).toList();
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
