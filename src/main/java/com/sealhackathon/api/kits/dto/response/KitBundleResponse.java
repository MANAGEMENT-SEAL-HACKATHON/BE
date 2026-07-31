package com.sealhackathon.api.kits.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitBundleResponse {

    private Integer id;
    private Integer hackathonId;
    private String name;
    private Boolean isDefault;
    private List<KitBundleItemResponse> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KitBundleItemResponse {
        private Integer id;
        private Integer kitItemId;
        private String kitItemName;
        private String kitItemType;
        private Integer quantity;
    }
}
