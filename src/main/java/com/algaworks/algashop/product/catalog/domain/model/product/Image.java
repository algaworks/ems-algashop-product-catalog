package com.algaworks.algashop.product.catalog.domain.model.product;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

    @EqualsAndHashCode.Include
    private UUID id;

    private String name;

    public Image(String name) {
        Objects.requireNonNull(name);

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException();
        }

        this.id = UUID.randomUUID();
        this.name = name;
    }
}
