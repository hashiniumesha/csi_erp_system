package erp_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "AppUser")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @ManyToOne
    @JoinColumn(name = "RoleID", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    private Status status;

    public enum Status {
        Active,
        Inactive
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

}
