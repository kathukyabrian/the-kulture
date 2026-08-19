package tech.kitucode.kulture.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaStorageService {
	private final Path root;

	public MediaStorageService(@Value("${app.media.storage-directory:./data/media}") String storageDirectory) {
		this.root = Path.of(storageDirectory).toAbsolutePath().normalize();
	}

	public String store(UUID mediaId, MultipartFile file) {
		String extension = switch (file.getContentType()) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> "";
		};
		String storageKey = mediaId + extension;
		Path destination = resolve(storageKey);
		try {
			Files.createDirectories(root);
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
			}
			return storageKey;
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store media", ex);
		}
	}

	public Resource load(String storageKey) {
		try {
			Resource resource = new UrlResource(resolve(storageKey).toUri());
			if (!resource.exists() || !resource.isReadable()) throw new IOException("Media file is unavailable");
			return resource;
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found", ex);
		}
	}

	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolve(storageKey));
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete media", ex);
		}
	}

	private Path resolve(String storageKey) {
		Path path = root.resolve(storageKey).normalize();
		if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid media storage key");
		return path;
	}
}
