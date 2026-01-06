package com.algaworks.algashop.product.catalog.application.category.query;

import com.algaworks.algashop.product.catalog.application.utility.SortablePageFilter;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CategoryFilter extends SortablePageFilter<CategoryFilter.SortType> {
    private String name;
    private Boolean enabled = Boolean.TRUE;

    @Override
    public CategoryFilter.SortType getSortByPropertyOrDefault() {
        return getSortByProperty() == null ? CategoryFilter.SortType.NAME: getSortByProperty();
    }

    @Override
    public Sort.Direction getSortDirectionOrDefault() {
        return getSortDirection() == null ? Sort.Direction.ASC : getSortDirection();
    }

    @Getter
    @RequiredArgsConstructor
    public enum SortType {
        NAME("name");

        private final String propertyName;
    }

    public boolean isDefault() {
        return Boolean.TRUE.equals(enabled)
                && StringUtils.isBlank(name)
                && getSortByPropertyOrDefault().equals(SortType.NAME)
                && getSortDirectionOrDefault().equals(Sort.Direction.ASC)
                && getPage() == 0
                && getSize() == 15;
    }
}
