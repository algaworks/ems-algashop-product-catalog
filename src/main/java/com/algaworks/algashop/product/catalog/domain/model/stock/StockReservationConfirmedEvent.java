package com.algaworks.algashop.product.catalog.domain.model.stock;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@ToString
@Builder
public class StockReservationConfirmedEvent {
	private UUID reservationId;
	private String orderId;

	@Builder.Default
	private OffsetDateTime confirmed = OffsetDateTime.now();
}
