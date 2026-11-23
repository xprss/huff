package dev.huff.hexaquot.game;

import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** While Hexatrap is not public, only admins can see and play the generated cases (preview). */
@ApplicationScoped
public class HexatrapAccess {
    @ConfigProperty(name = "app.hexatrap.enabled", defaultValue = "false")
    boolean enabled;

    public boolean available(AppUser user) {
        return enabled || (user != null && user.admin() != null);
    }

    public boolean isPublic() {
        return enabled;
    }
}
