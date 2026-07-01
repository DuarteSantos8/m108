package ch.duartesantos.m108.user;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lädt Benutzer aus der Datenbank für Spring Security. Übersetzt die
 * gespeicherten Rollen in {@link org.springframework.security.core.GrantedAuthority}.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unbekannter Benutzer"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(AuthorityUtils.commaSeparatedStringToAuthorityList(user.getRoles()))
                .build();
    }
}
