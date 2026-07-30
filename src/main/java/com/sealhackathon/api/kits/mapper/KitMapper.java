package com.sealhackathon.api.kits.mapper;

import com.sealhackathon.api.kits.dto.response.KitAllocationResponse;
import com.sealhackathon.api.kits.dto.response.KitItemResponse;
import com.sealhackathon.api.kits.dto.response.KitStockResponse;
import com.sealhackathon.api.kits.entity.KitAllocation;
import com.sealhackathon.api.kits.entity.KitItem;
import com.sealhackathon.api.kits.entity.KitStock;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KitMapper {

    public KitStockResponse toStockResponse(KitStock s) {
        if (s == null) {
            return null;
        }
        return KitStockResponse.builder()
                .id(s.getId())
                .size(s.getSize())
                .quantityTotal(s.getQuantityTotal())
                .quantityIssued(s.getQuantityIssued())
                .remaining(s.remaining())
                .version(s.getVersion())
                .build();
    }

    public KitItemResponse toItemResponse(KitItem item, List<KitStock> stocks) {
        return KitItemResponse.builder()
                .id(item.getId())
                .hackathonId(item.getHackathon() == null ? null : item.getHackathon().getId())
                .name(item.getName())
                .type(item.getType())
                .hasSize(item.getHasSize())
                .stocks(stocks == null ? List.of() : stocks.stream().map(this::toStockResponse).toList())
                .build();
    }

    public KitAllocationResponse toAllocationResponse(KitAllocation a) {
        if (a == null) {
            return null;
        }
        return KitAllocationResponse.builder()
                .id(a.getId())
                .hackathonId(a.getHackathon() == null ? null : a.getHackathon().getId())
                .userId(a.getUser() == null ? null : a.getUser().getId())
                .kitItemId(a.getKitItem() == null ? null : a.getKitItem().getId())
                .kitItemName(a.getKitItem() == null ? null : a.getKitItem().getName())
                .kitItemType(a.getKitItem() == null ? null : a.getKitItem().getType())
                .size(a.getSize())
                .status(a.getStatus())
                .issuedAt(a.getIssuedAt())
                .issuedById(a.getIssuedBy() == null ? null : a.getIssuedBy().getId())
                .note(a.getNote())
                .build();
    }
}
