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

    @PostMapping("/login")
    public AppUser login(@RequestBody LoginRequest request) {
        return appUserService.login(request.getUsername(), request.getPassword());
    }
}