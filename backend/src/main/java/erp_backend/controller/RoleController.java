package erp_backend.controller;

import erp_backend.entity.Role;
import erp_backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired private RoleRepository roleRepository;

    @GetMapping
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }
}