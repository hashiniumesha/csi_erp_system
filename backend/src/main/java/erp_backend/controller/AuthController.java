package erp_backend.controller;

import erp_backend.dto.LoginRequest;
import erp_backend.entity.AppUser;
import erp_backend.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AppUserService appUserService;

    // Deliberately not the raw AppUser entity — that would serialize
    // passwordHash straight into the response body. This projection is safe
    // to send to the frontend and carries the role name it needs to route
    // the user to the correct dashboard.
    public static class LoginResponse {
        public Integer userId;
        public String username;
        public String fullName;
        public String roleName;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AppUser user = appUserService.login(request.getUsername(), request.getPassword());
        LoginResponse response = new LoginResponse();
        response.userId = user.getUserId();
        response.username = user.getUsername();
        response.fullName = user.getFullName();
        response.roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        return response;
    }
}