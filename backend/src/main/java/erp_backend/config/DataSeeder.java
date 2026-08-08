package erp_backend.config;

import erp_backend.entity.AppUser;
import erp_backend.entity.Role;
import erp_backend.repository.AppUserRepository;
import erp_backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository, AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findAll().stream()
                    .filter(r -> r.getRoleName().equals("Admin"))
                    .findFirst()
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setRoleName("Admin");
                        return roleRepository.save(r);
                    });

            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFullName("System Admin");
            admin.setRole(adminRole);
            admin.setStatus(AppUser.Status.Active);
            appUserRepository.save(admin);
            System.out.println("Seeded default admin user: admin / admin123");
        }
    }
}
