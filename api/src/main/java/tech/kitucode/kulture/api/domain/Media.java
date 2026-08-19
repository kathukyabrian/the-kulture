package tech.kitucode.kulture.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "media")
public class Media {
	@Id private UUID id;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_id", nullable = false) private Vehicle vehicle;
	@Column(name = "original_name", nullable = false) private String originalName;
	@Column(name = "content_type", nullable = false) private String contentType;
	@Column(nullable = false) private long size;
	@Column(name = "storage_key", nullable = false, unique = true) private String storageKey;
	@Column(name = "sort_order", nullable = false) private int sortOrder;
	@Column(nullable = false) private boolean approved;
	@Column(name = "created_at", nullable = false) private Instant createdAt;
}
