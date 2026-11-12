package dev.huff.hexaquot.auth;

import dev.huff.hexaquot.persistence.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Stores profile photos as files on the host (app.avatars.dir, a mounted
 * volume in Docker deployments). The database only keeps a content hash in
 * users.avatar_version, which versions the public URL so browsers can cache it
 * forever.
 */
@ApplicationScoped
public class AvatarService {
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    public enum Format {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        final String extension;
        final String contentType;

        Format(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public String contentType() {
            return contentType;
        }
    }

    public record Avatar(byte[] bytes, Format format) {
    }

    @ConfigProperty(name = "app.avatars.dir")
    String directory;

    @Inject
    UserService userService;

    /** Public URL of a user's photo, or null when they have none. */
    public static String url(String userId, String version) {
        return version == null || version.isBlank() ? null : "/api/avatars/" + userId + "?v=" + version;
    }

    @Transactional
    public AppUser store(String userId, byte[] bytes, boolean authenticated) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotAuthorizedException("Utente non trovato.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new WebApplicationException("Nessuna immagine ricevuta.", Response.Status.BAD_REQUEST);
        }
        if (bytes.length > MAX_BYTES) {
            throw new WebApplicationException("La foto può pesare al massimo 2 MB.", Response.Status.REQUEST_ENTITY_TOO_LARGE);
        }
        Format format = detect(bytes)
            .orElseThrow(() -> new WebApplicationException("Formato non supportato: usa JPEG, PNG o WebP.", Response.Status.UNSUPPORTED_MEDIA_TYPE));

        Path target = file(userId, format);
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            try {
                Files.write(temp, bytes);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            for (Format other : Format.values()) {
                if (other != format) {
                    Files.deleteIfExists(file(userId, other));
                }
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Impossibile salvare la foto profilo.", error);
        }

        user.avatarVersion = version(bytes);
        return user.toAppUser(authenticated, userService.adminPrivileges(userId));
    }

    @Transactional
    public AppUser delete(String userId, boolean authenticated) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotAuthorizedException("Utente non trovato.");
        }
        deleteFiles(userId);
        user.avatarVersion = null;
        return user.toAppUser(authenticated, userService.adminPrivileges(userId));
    }

    /** Removes the files only; callers own the database row. */
    public void deleteFiles(String userId) {
        for (Format format : Format.values()) {
            try {
                Files.deleteIfExists(file(userId, format));
            } catch (IOException error) {
                throw new UncheckedIOException("Impossibile eliminare la foto profilo.", error);
            }
        }
    }

    public Avatar read(String userId) {
        for (Format format : Format.values()) {
            Path path = file(userId, format);
            if (Files.isRegularFile(path)) {
                try {
                    return new Avatar(Files.readAllBytes(path), format);
                } catch (IOException error) {
                    throw new UncheckedIOException("Impossibile leggere la foto profilo.", error);
                }
            }
        }
        throw new NotFoundException("Foto profilo non trovata.");
    }

    static Optional<Format> detect(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(Format.JPEG);
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        if (startsWith(bytes, png, 0)) {
            return Optional.of(Format.PNG);
        }
        if (startsWith(bytes, "RIFF".getBytes(), 0) && startsWith(bytes, "WEBP".getBytes(), 8)) {
            return Optional.of(Format.WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix, int offset) {
        if (bytes.length < offset + prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[offset + index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /** Files are named after a hash of the user id, so any id maps to a safe name. */
    private Path file(String userId, Format format) {
        if (userId == null || userId.isBlank()) {
            throw new NotFoundException("Foto profilo non trovata.");
        }
        return Path.of(directory).toAbsolutePath().normalize().resolve(fileStem(userId) + "." + format.extension);
    }

    private static String fileStem(String userId) {
        return sha256(userId.getBytes(StandardCharsets.UTF_8)).substring(0, 32);
    }

    private static String version(byte[] bytes) {
        return sha256(bytes).substring(0, 12);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
