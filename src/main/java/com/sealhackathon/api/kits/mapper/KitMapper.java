package com.sealhackathon.api.kits.mapper;

import com.sealhackathon.api.kits.dto.response.KitAllocationResponse;
import com.sealhackathon.api.kits.dto.response.KitBundleResponse;
import com.sealhackathon.api.kits.dto.response.KitItemResponse;
import com.sealhackathon.api.kits.dto.response.KitStockResponse;
import com.sealhackathon.api.kits.entity.KitAllocation;
import com.sealhackathon.api.kits.entity.KitBundle;
import com.sealhackathon.api.kits.entity.KitBundleItem;
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
                .fit(s.getFit())
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
                .fit(a.getFit())
                .status(a.getStatus())
                .issuedAt(a.getIssuedAt())
                .issuedById(a.getIssuedBy() == null ? null : a.getIssuedBy().getId())
                .note(a.getNote())
                .build();
    }

    public KitBundleResponse toBundleResponse(KitBundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<KitBundleResponse.KitBundleItemResponse> items = bundle.getItems() == null
                ? List.of()
                : bundle.getItems().stream().map(this::toBundleItemResponse).toList();
        return KitBundleResponse.builder()
                .id(bundle.getId())
                .hackathonId(bundle.getHackathon() == null ? null : bundle.getHackathon().getId())
                .name(bundle.getName())
                .isDefault(bundle.getIsDefault())
                .items(items)
                .build();
    }

    private KitBundleResponse.KitBundleItemResponse toBundleItemResponse(KitBundleItem bi) {
        KitItem item = bi.getKitItem();
        return KitBundleResponse.KitBundleItemResponse.builder()
                .id(bi.getId())
                .kitItemId(item == null ? null : item.getId())
                .kitItemName(item == null ? null : item.getName())
                .kitItemType(item == null || item.getType() == null ? null : item.getType().name())
                .quantity(bi.getQuantity())
                .build();
    }
}
