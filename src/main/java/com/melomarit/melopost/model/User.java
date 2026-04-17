package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Indexed;
import lombok.Data;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Data
@Table("users")
public class User {
    @PrimaryKey
    @Column("uuid")
    private UUID uuid = UUID.randomUUID();

    @Indexed
    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("email")
    private String email;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("active")
    private Boolean active = true;

    @Column("roles")
    private Set<String> roles = new HashSet<>();

    @Column("allowed_departments")
    private Set<String> allowedDepartments = new HashSet<>();

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public Set<String> getAllowedDepartments() { return allowedDepartments; }
    public void setAllowedDepartments(Set<String> allowedDepartments) { this.allowedDepartments = allowedDepartments; }
}
