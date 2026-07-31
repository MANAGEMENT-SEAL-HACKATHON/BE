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

    List<KitStockResponse> batchUpsertStock(Integer itemId, BatchUpsertKitStockRequest req);

    KitCloneSourcesResponse listCloneSources(Integer targetHackathonId);

    CloneKitsResponse cloneFromSource(Integer targetHackathonId, CloneKitsRequest req);

    List<KitRecipientResponse> listRecipients(Integer hackathonId, String query);

    IssueResult issue(Integer hackathonId, IssueKitRequest req);

    BundleIssueResult issueBundle(Integer hackathonId, IssueKitBundleRequest req);

    KitAllocationResponse revoke(Integer allocationId, RevokeKitRequest req);

    KitReconciliationResponse reconciliation(Integer hackathonId);

    List<KitBundleResponse> listBundles(Integer hackathonId);

    KitBundleResponse createBundle(Integer hackathonId, UpsertKitBundleRequest req);

    KitBundleResponse updateBundle(Integer bundleId, UpsertKitBundleRequest req);

    void deleteBundle(Integer bundleId);

    ShirtSizeResponse updateMyShirtSize(Integer hackathonId, UpdateShirtSizeRequest req);

    List<ShirtSizeResponse> updateMyShirtSizeAll(UpdateShirtSizeRequest req);

    List<ShirtSizeResponse> listMyShirtSizes();

    record IssueResult(KitAllocationResponse allocation, List<Warning> warnings) {}

    record BundleIssueResult(IssueKitBundleResponse body, List<Warning> warnings) {}
}
