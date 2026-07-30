package com.sealhackathon.api.kits.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.kits.dto.request.*;
import com.sealhackathon.api.kits.dto.response.*;

import java.util.List;

public interface KitService {

    List<KitItemResponse> listItems(Integer hackathonId);

    KitItemResponse createItem(Integer hackathonId, CreateKitItemRequest req);

    KitItemResponse updateItem(Integer itemId, UpdateKitItemRequest req);

    void deleteItem(Integer itemId);

    KitStockResponse upsertStock(Integer itemId, UpsertKitStockRequest req);

    List<KitRecipientResponse> listRecipients(Integer hackathonId, String query);

    IssueResult issue(Integer hackathonId, IssueKitRequest req);

    KitAllocationResponse revoke(Integer allocationId, RevokeKitRequest req);

    List<KitReconciliationLineResponse> reconciliation(Integer hackathonId);

    ShirtSizeResponse updateMyShirtSize(Integer hackathonId, UpdateShirtSizeRequest req);

    List<ShirtSizeResponse> updateMyShirtSizeAll(UpdateShirtSizeRequest req);

    List<ShirtSizeResponse> listMyShirtSizes();

    record IssueResult(KitAllocationResponse allocation, List<Warning> warnings) {}
}
