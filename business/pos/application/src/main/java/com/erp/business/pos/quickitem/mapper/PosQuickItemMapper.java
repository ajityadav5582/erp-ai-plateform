package com.erp.business.pos.quickitem.mapper;

import com.erp.business.pos.domain.PosQuickItem;
import com.erp.business.pos.quickitem.dto.PosQuickItemResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Maps the {@link PosQuickItem} aggregate to its read model.
 */
@Component
public class PosQuickItemMapper {

    public PosQuickItemResponse toResponse(PosQuickItem quickItem) {
        return new PosQuickItemResponse(
                quickItem.getId(),
                quickItem.getTenantId(),
                quickItem.getCompanyId(),
                quickItem.getRegisterId(),
                quickItem.getItemId(),
                quickItem.getItemCode(),
                quickItem.getItemName(),
                quickItem.getUomCode(),
                quickItem.getDefaultPrice(),
                quickItem.getGridRow(),
                quickItem.getGridCol(),
                quickItem.getDisplayColor(),
                quickItem.getDisplayIcon(),
                quickItem.getSortOrder(),
                quickItem.isActive(),
                toLocalDateTime(quickItem.getCreatedAt()),
                toLocalDateTime(quickItem.getUpdatedAt()),
                quickItem.getVersion()
        );
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
