package com.algaworks.algashop.product.catalog.application.product.event;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDelistedIntegrationEvent {
	private UUID productId;
	private OffsetDateTime delistedAt;
}