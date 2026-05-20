package org.springframework.samples.weightmonitor.provider;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.samples.weightmonitor.measurement.Measurement;
import org.springframework.samples.weightmonitor.model.Person;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "providers")
public class HealthcareProvider extends Person implements UserDetails {

    @Column(unique = true)
    private String professionalId;

    public String getProfessionalId() {
        return professionalId;
    }

    @Column
    private String password; // 存 BCrypt 加密后的

    // UserDetails 必须实现的方法
    @Override
    public String getUsername() {
        return professionalId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"));
    }

    // 其余 isAccountNonExpired 等全部 return true
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static HealthcareProvider create(String professionalId, String encodedPassword, String firstName,
            String lastName) {
        HealthcareProvider p = new HealthcareProvider();
        p.professionalId = professionalId;
        p.password = encodedPassword;
        p.setFirstName(firstName);
        p.setLastName(lastName);
        return p;
    }

    public void applyUpdate(String encodedPassword, String firstName, String lastName) {
        this.password = encodedPassword;
        this.setFirstName(firstName);
        this.setLastName(lastName);
    }

    @ManyToMany(mappedBy = "providers", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<Measurement> measurements = new HashSet<>();
}
