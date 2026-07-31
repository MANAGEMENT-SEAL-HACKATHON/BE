package com.sealhackathon.api.kits.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.kits.dto.request.*;
import com.sealhackathon.api.kits.dto.response.*;
import com.sealhackathon.api.kits.entity.KitAllocation;
import com.sealhackathon.api.kits.entity.KitBundle;
import com.sealhackathon.api.kits.entity.KitBundleItem;
import com.sealhackathon.api.kits.entity.KitItem;
import com.sealhackathon.api.kits.entity.KitStock;
import com.sealhackathon.api.kits.mapper.KitMapper;
import com.sealhackathon.api.kits.repository.KitAllocationRepository;
import com.sealhackathon.api.kits.repository.KitBundleItemRepository;
import com.sealhackathon.api.kits.repository.KitBundleRepository;
import com.sealhackathon.api.kits.repository.KitItemRepository;
import com.sealhackathon.api.kits.repository.KitStockRepository;
import com.sealhackathon.api.kits.service.KitService;
import com.sealhackathon.api.kits.value_object.KitAllocationStatus;
import com.sealhackathon.api.kits.value_object.KitItemType;
import com.sealhackathon.api.kits.value_object.ShirtFit;
import com.sealhackathon.api.kits.value_object.ShirtSize;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class KitServiceImpl implements KitService {

    public static final String WARNING_OUTSIDE_KICKOFF = "KIT_OUTSIDE_KICKOFF_WINDOW";

    private final KitItemRepository kitItemRepository;
    private final KitStockRepository kitStockRepository;
    private final KitAllocationRepository kitAllocationRepository;
    private final KitBundleRepository kitBundleRepository;
    private final KitBundleItemRepository kitBundleItemRepository;
    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final KitMapper kitMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final HackathonArchiveGuard archiveGuard;

    @Override
    @Transactional(readOnly = true)
    public List<KitItemResponse> listItems(Integer hackathonId) {
        requireHackathon(hackathonId);
        List<KitItem> items = kitItemRepository.findByHackathon_IdOrderByIdAsc(hackathonId);
        return toItemResponses(items);
    }

    @Override
    public KitItemResponse createItem(Integer hackathonId, CreateKitItemRequest req) {
        Hackathon hackathon = requireHackathon(hackathonId);
        archiveGuard.assertNotArchived(hackathon);

        String name = validateItemName(req.getType(), req.getName());
        boolean hasSize = resolveHasSize(req.getType(), req.getHasSize());
        KitItem saved = kitItemRepository.save(KitItem.builder()
                .hackathon(hackathon)
                .name(name)
                .type(req.getType())
                .hasSize(hasSize)
                .build());
        return kitMapper.toItemResponse(saved, List.of());
    }

    @Override
    public KitItemResponse updateItem(Integer itemId, UpdateKitItemRequest req) {
        KitItem item = requireItem(itemId);
        archiveGuard.assertNotArchived(item.getHackathon());

        item.setName(validateItemName(req.getType(), req.getName()));
        item.setType(req.getType());
        item.setHasSize(resolveHasSize(req.getType(), req.getHasSize()));
        KitItem saved = kitItemRepository.save(item);
        List<KitStock> stocks = kitStockRepository.findByKitItem_IdOrderBySizeAsc(saved.getId());
        return kitMapper.toItemResponse(saved, stocks);
    }

    @Override
    public void deleteItem(Integer itemId) {
        KitItem item = requireItem(itemId);
        archiveGuard.assertNotArchived(item.getHackathon());
        if (kitBundleItemRepository.existsByKitItem_Id(itemId)) {
            throw new BusinessRuleException(ErrorCode.KIT_ITEM_IN_BUNDLE,
                    "Không thể xóa món đang nằm trong combo kit",
                    Map.of("kitItemId", itemId));
        }
        kitAllocationRepository.deleteByKitItem_Id(itemId);
        kitStockRepository.deleteByKitItem_Id(itemId);
        kitItemRepository.delete(item);
    }

    @Override
    public KitStockResponse upsertStock(Integer itemId, UpsertKitStockRequest req) {
        KitItem item = requireItem(itemId);
        archiveGuard.assertNotArchived(item.getHackathon());

        String size = normalizeStockSize(item, req.getSize());
        String sizeKey = size == null ? "" : size;
        String fit = normalizeStockFit(item, req.getFit());
        String fitKey = fit == null ? "" : fit;

        KitStock stock = kitStockRepository.findByKitItem_IdAndFitKeyAndSizeKey(itemId, fitKey, sizeKey)
                .orElseGet(() -> KitStock.builder()
                        .kitItem(item)
                        .fit(fit)
                        .fitKey(fitKey)
                        .size(size)
                        .sizeKey(sizeKey)
                        .build());

        int issued = stock.getQuantityIssued() == null ? 0 : stock.getQuantityIssued();
        if (req.getQuantityTotal() < issued) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "quantityTotal không được nhỏ hơn số đã phát (" + issued + ")",
                    Map.of("quantityIssued", issued, "quantityTotal", req.getQuantityTotal()));
        }
        stock.setQuantityTotal(req.getQuantityTotal());
        stock.setFit(fit);
        stock.setFitKey(fitKey);
        stock.setSize(size);
        stock.setSizeKey(sizeKey);
        return kitMapper.toStockResponse(kitStockRepository.save(stock));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitRecipientResponse> listRecipients(Integer hackathonId, String query) {
        requireHackathon(hackathonId);
        List<RecipientRow> rows = collectEligibleRecipients(hackathonId);
        if (StringUtils.hasText(query)) {
            String q = query.trim().toLowerCase(Locale.ROOT);
            rows = rows.stream().filter(r -> matchesQuery(r, q)).toList();
        }

        Map<Integer, ShirtPrefs> shirtByUser = loadShirtPrefs(hackathonId,
                rows.stream().map(r -> r.user().getId()).toList());

        List<Integer> userIds = rows.stream().map(r -> r.user().getId()).toList();
        Map<Integer, List<KitAllocation>> allocByUser = userIds.isEmpty()
                ? Map.of()
                : kitAllocationRepository.findByHackathon_IdAndUser_IdIn(hackathonId, userIds).stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()));

        List<KitRecipientResponse> result = new ArrayList<>();
        for (RecipientRow row : rows) {
            Integer uid = row.user().getId();
            ShirtPrefs prefs = shirtByUser.getOrDefault(uid, ShirtPrefs.empty());
            List<KitAllocationResponse> allocations = allocByUser.getOrDefault(uid, List.of()).stream()
                    .map(kitMapper::toAllocationResponse)
                    .toList();
            result.add(KitRecipientResponse.builder()
                    .userId(uid)
                    .fullName(row.user().getFullName())
                    .studentCode(row.user().getStudentCode())
                    .email(row.user().getEmail())
                    .phone(row.user().getPhone())
                    .teamId(row.team().getId())
                    .teamName(row.team().getTeamName())
                    .preferredShirtSize(prefs.size())
                    .preferredShirtFit(prefs.fit())
                    .allocations(allocations)
                    .build());
        }
        result.sort(Comparator
                .comparing((KitRecipientResponse r) -> r.getTeamName() == null ? "" : r.getTeamName())
                .thenComparing(r -> r.getFullName() == null ? "" : r.getFullName()));
        return result;
    }

    @Override
    public IssueResult issue(Integer hackathonId, IssueKitRequest req) {
        Hackathon hackathon = requireHackathon(hackathonId);
        archiveGuard.assertNotArchived(hackathon);

        User recipient = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        assertEligibleRecipient(hackathonId, recipient.getId());

        KitItem item = requireItem(req.getKitItemId());
        if (!Objects.equals(item.getHackathon().getId(), hackathonId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Kit item không thuộc hackathon này");
        }

        KitAllocation allocation = kitAllocationRepository
                .findByHackathon_IdAndUser_IdAndKitItem_Id(hackathonId, recipient.getId(), item.getId())
                .orElseGet(() -> KitAllocation.builder()
                        .hackathon(hackathon)
                        .user(recipient)
                        .kitItem(item)
                        .status(KitAllocationStatus.PENDING)
                        .build());

        if (allocation.getStatus() == KitAllocationStatus.ISSUED) {
            throw new ConflictException(ErrorCode.KIT_ALREADY_ISSUED,
                    "Sinh viên đã nhận món kit này rồi",
                    Map.of("userId", recipient.getId(), "kitItemId", item.getId()));
        }

        String size = resolveIssueSize(item, recipient.getId(), hackathonId, req.getSize());
        String fit = resolveIssueFit(item, recipient.getId(), hackathonId, req.getFit());
        KitStock stock = requireStockForIssue(item, fit, size);

        if (stock.remaining() <= 0) {
            throw new BusinessRuleException(ErrorCode.KIT_OUT_OF_STOCK,
                    "Hết tồn kho cho món/size này",
                    Map.of(
                            "kitItemId", item.getId(),
                            "fit", fit == null ? "" : fit,
                            "size", size == null ? "" : size,
                            "quantityTotal", stock.getQuantityTotal(),
                            "quantityIssued", stock.getQuantityIssued()));
        }

        stock.setQuantityIssued(stock.getQuantityIssued() + 1);
        kitStockRepository.save(stock);

        User issuer = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        allocation.setSize(size);
        allocation.setFit(fit);
        allocation.setStatus(KitAllocationStatus.ISSUED);
        allocation.setIssuedAt(LocalDateTime.now());
        allocation.setIssuedBy(issuer);
        if (StringUtils.hasText(req.getNote())) {
            allocation.setNote(req.getNote().trim());
        }
        KitAllocation saved = kitAllocationRepository.save(allocation);

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("hackathonId", hackathonId);
        audit.put("userId", recipient.getId());
        audit.put("kitItemId", item.getId());
        audit.put("fit", fit);
        audit.put("size", size);
        audit.put("issuedAt", saved.getIssuedAt());
        auditService.log(AuditAction.KIT_ISSUED, "kit_allocations", saved.getId(), audit);

        List<Warning> warnings = kickoffWarnings(hackathonId, saved.getId(), recipient.getId(), item.getId());
        return new IssueResult(kitMapper.toAllocationResponse(saved), warnings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BundleIssueResult issueBundle(Integer hackathonId, IssueKitBundleRequest req) {
        Hackathon hackathon = requireHackathon(hackathonId);
        archiveGuard.assertNotArchived(hackathon);

        User recipient = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        assertEligibleRecipient(hackathonId, recipient.getId());

        KitBundle bundle = requireBundle(req.getBundleId());
        if (!Objects.equals(bundle.getHackathon().getId(), hackathonId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Combo không thuộc hackathon này");
        }
        if (bundle.getItems() == null || bundle.getItems().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.KIT_BUNDLE_EMPTY, "Combo không có món nào");
        }

        List<KitBundleItem> toIssue = new ArrayList<>();
        List<KitAllocation> skipped = new ArrayList<>();
        for (KitBundleItem bi : bundle.getItems()) {
            KitItem item = bi.getKitItem();
            KitAllocation existing = kitAllocationRepository
                    .findByHackathon_IdAndUser_IdAndKitItem_Id(hackathonId, recipient.getId(), item.getId())
                    .orElse(null);
            if (existing != null && existing.getStatus() == KitAllocationStatus.ISSUED) {
                skipped.add(existing);
            } else {
                toIssue.add(bi);
            }
        }

        if (toIssue.isEmpty()) {
            throw new ConflictException(ErrorCode.KIT_ALREADY_ISSUED,
                    "Sinh viên đã nhận đủ mọi món trong combo",
                    Map.of("userId", recipient.getId(), "bundleId", bundle.getId()));
        }

        List<Map<String, Object>> missing = new ArrayList<>();
        Map<Integer, PreparedIssue> prepared = new LinkedHashMap<>();
        for (KitBundleItem bi : toIssue) {
            KitItem item = bi.getKitItem();
            int qty = bi.getQuantity() == null || bi.getQuantity() < 1 ? 1 : bi.getQuantity();
            String size = item.getType() == KitItemType.SHIRT
                    ? resolveIssueSize(item, recipient.getId(), hackathonId, req.getSize())
                    : null;
            String fit = item.getType() == KitItemType.SHIRT
                    ? resolveIssueFit(item, recipient.getId(), hackathonId, req.getFit())
                    : null;
            try {
                KitStock stock = requireStockForIssue(item, fit, size);
                if (stock.remaining() < qty) {
                    missing.add(missingEntry(item, fit, size, stock, qty));
                } else {
                    prepared.put(item.getId(), new PreparedIssue(bi, item, stock, fit, size, qty));
                }
            } catch (BusinessRuleException ex) {
                if (ErrorCode.KIT_OUT_OF_STOCK.equals(ex.getCode())
                        || ErrorCode.VALIDATION_FAILED.equals(ex.getCode())) {
                    missing.add(Map.of(
                            "kitItemId", item.getId(),
                            "kitItemName", item.getName(),
                            "fit", fit == null ? "" : fit,
                            "size", size == null ? "" : size,
                            "needed", qty,
                            "reason", ex.getMessage()));
                } else {
                    throw ex;
                }
            }
        }

        if (!missing.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.KIT_OUT_OF_STOCK,
                    "Thiếu tồn kho cho một hoặc nhiều món trong combo — không trừ kho",
                    Map.of("missing", missing));
        }

        User issuer = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        List<KitAllocationResponse> issuedResponses = new ArrayList<>();
        List<Integer> issuedIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (PreparedIssue prep : prepared.values()) {
            KitStock stock = prep.stock();
            stock.setQuantityIssued(stock.getQuantityIssued() + prep.qty());
            kitStockRepository.save(stock);

            KitAllocation allocation = kitAllocationRepository
                    .findByHackathon_IdAndUser_IdAndKitItem_Id(hackathonId, recipient.getId(), prep.item().getId())
                    .orElseGet(() -> KitAllocation.builder()
                            .hackathon(hackathon)
                            .user(recipient)
                            .kitItem(prep.item())
                            .status(KitAllocationStatus.PENDING)
                            .build());
            allocation.setSize(prep.size());
            allocation.setFit(prep.fit());
            allocation.setStatus(KitAllocationStatus.ISSUED);
            allocation.setIssuedAt(now);
            allocation.setIssuedBy(issuer);
            if (StringUtils.hasText(req.getNote())) {
                allocation.setNote(req.getNote().trim());
            }
            KitAllocation saved = kitAllocationRepository.save(allocation);
            issuedIds.add(saved.getId());
            issuedResponses.add(kitMapper.toAllocationResponse(saved));
        }

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("hackathonId", hackathonId);
        audit.put("userId", recipient.getId());
        audit.put("bundleId", bundle.getId());
        audit.put("issuedAllocationIds", issuedIds);
        audit.put("skippedItemIds", skipped.stream().map(a -> a.getKitItem().getId()).toList());
        auditService.log(AuditAction.KIT_BUNDLE_ISSUED, "kit_bundles", bundle.getId(), audit);

        List<Warning> warnings = new ArrayList<>();
        if (!isInsideKickoffWindow(hackathonId)) {
            Warning w = Warning.of(WARNING_OUTSIDE_KICKOFF,
                    "Phát kit ngoài khung giờ Kickoff — đã ghi audit, không chặn thao tác");
            warnings.add(w);
            auditService.log(AuditAction.KIT_BUNDLE_ISSUED, "kit_bundles", bundle.getId(), Map.of(
                    "warning", WARNING_OUTSIDE_KICKOFF,
                    "hackathonId", hackathonId,
                    "userId", recipient.getId(),
                    "bundleId", bundle.getId()));
        }

        IssueKitBundleResponse body = IssueKitBundleResponse.builder()
                .issued(issuedResponses)
                .skipped(skipped.stream().map(kitMapper::toAllocationResponse).toList())
                .build();
        return new BundleIssueResult(body, warnings);
    }

    @Override
    public KitAllocationResponse revoke(Integer allocationId, RevokeKitRequest req) {
        KitAllocation allocation = kitAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("KitAllocation", allocationId));
        archiveGuard.assertNotArchived(allocation.getHackathon());

        if (allocation.getStatus() != KitAllocationStatus.ISSUED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ thu hồi được allocation đang ISSUED");
        }

        KitItem item = allocation.getKitItem();
        String sizeKey = allocation.getSize() == null ? "" : allocation.getSize();
        String fitKey = allocation.getFit() == null ? "" : allocation.getFit();
        KitStock stock = kitStockRepository.findByKitItem_IdAndFitKeyAndSizeKey(item.getId(), fitKey, sizeKey)
                .orElse(null);
        if (stock != null && stock.getQuantityIssued() != null && stock.getQuantityIssued() > 0) {
            stock.setQuantityIssued(stock.getQuantityIssued() - 1);
            kitStockRepository.save(stock);
        }

        allocation.setStatus(KitAllocationStatus.REVOKED);
        allocation.setNote(req.getReason().trim());
        KitAllocation saved = kitAllocationRepository.save(allocation);

        auditService.log(AuditAction.KIT_REVOKED, "kit_allocations", saved.getId(), Map.of(
                "hackathonId", allocation.getHackathon().getId(),
                "userId", allocation.getUser().getId(),
                "kitItemId", item.getId(),
                "fit", allocation.getFit() == null ? "" : allocation.getFit(),
                "size", allocation.getSize() == null ? "" : allocation.getSize(),
                "reason", req.getReason().trim()));

        return kitMapper.toAllocationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public KitReconciliationResponse reconciliation(Integer hackathonId) {
        requireHackathon(hackathonId);
        List<RecipientRow> recipients = collectEligibleRecipients(hackathonId);
        Map<Integer, ShirtPrefs> shirtByUser = loadShirtPrefs(hackathonId,
                recipients.stream().map(r -> r.user().getId()).toList());

        int eligibleTotal = recipients.size();
        Map<String, Long> fitSizeCounts = shirtByUser.values().stream()
                .collect(Collectors.groupingBy(
                        p -> fitSizeKey(p.fitOrDefault(), p.size() == null ? "" : p.size()),
                        Collectors.counting()));
        Map<String, Long> sizeCounts = shirtByUser.values().stream()
                .collect(Collectors.groupingBy(
                        p -> p.size() == null ? "" : p.size(),
                        Collectors.counting()));

        List<KitItem> items = kitItemRepository.findByHackathon_IdOrderByIdAsc(hackathonId);
        List<KitReconciliationLineResponse> lines = new ArrayList<>();
        for (KitItem item : items) {
            List<KitStock> stocks = kitStockRepository.findByKitItem_IdOrderBySizeAsc(item.getId());
            if (stocks.isEmpty()) {
                int eligible = Boolean.TRUE.equals(item.getHasSize()) ? 0 : eligibleTotal;
                lines.add(line(item, null, null, 0, 0, 0, eligible));
                continue;
            }
            for (KitStock stock : stocks) {
                int eligible;
                if (item.getType() == KitItemType.SHIRT) {
                    String fit = stock.getFit() == null ? "" : stock.getFit();
                    String size = stock.getSize() == null ? "" : stock.getSize();
                    eligible = fitSizeCounts.getOrDefault(fitSizeKey(fit, size), 0L).intValue();
                } else if (Boolean.TRUE.equals(item.getHasSize())) {
                    String key = stock.getSize() == null ? "" : stock.getSize();
                    eligible = sizeCounts.getOrDefault(key, 0L).intValue();
                } else {
                    eligible = eligibleTotal;
                }
                int issued = stock.getQuantityIssued() == null ? 0 : stock.getQuantityIssued();
                int total = stock.getQuantityTotal() == null ? 0 : stock.getQuantityTotal();
                lines.add(line(item, stock.getFit(), stock.getSize(), total, issued, stock.remaining(), eligible));
            }
        }

        LocalDateTime kickoffStartsAt = earliestKickoffStartsAt(hackathonId);
        boolean beforeKickoff = kickoffStartsAt == null || LocalDateTime.now().isBefore(kickoffStartsAt);
        return KitReconciliationResponse.builder()
                .lines(lines)
                .kickoffStartsAt(kickoffStartsAt)
                .beforeKickoff(beforeKickoff)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitBundleResponse> listBundles(Integer hackathonId) {
        requireHackathon(hackathonId);
        return kitBundleRepository.findByHackathon_IdOrderByIdAsc(hackathonId).stream()
                .map(kitMapper::toBundleResponse)
                .toList();
    }

    @Override
    public KitBundleResponse createBundle(Integer hackathonId, UpsertKitBundleRequest req) {
        Hackathon hackathon = requireHackathon(hackathonId);
        archiveGuard.assertNotArchived(hackathon);
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.KIT_BUNDLE_EMPTY, "Combo phải có ít nhất một món");
        }

        KitBundle bundle = KitBundle.builder()
                .hackathon(hackathon)
                .name(req.getName().trim())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();
        applyBundleItems(bundle, hackathonId, req.getItems());
        if (Boolean.TRUE.equals(bundle.getIsDefault())) {
            clearOtherDefaults(hackathonId, null);
        }
        return kitMapper.toBundleResponse(kitBundleRepository.save(bundle));
    }

    @Override
    public KitBundleResponse updateBundle(Integer bundleId, UpsertKitBundleRequest req) {
        KitBundle bundle = requireBundle(bundleId);
        archiveGuard.assertNotArchived(bundle.getHackathon());
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessRuleException(ErrorCode.KIT_BUNDLE_EMPTY, "Combo phải có ít nhất một món");
        }

        bundle.setName(req.getName().trim());
        if (req.getIsDefault() != null) {
            bundle.setIsDefault(req.getIsDefault());
        }
        bundle.getItems().clear();
        applyBundleItems(bundle, bundle.getHackathon().getId(), req.getItems());
        if (Boolean.TRUE.equals(bundle.getIsDefault())) {
            clearOtherDefaults(bundle.getHackathon().getId(), bundle.getId());
        }
        return kitMapper.toBundleResponse(kitBundleRepository.save(bundle));
    }

    @Override
    public void deleteBundle(Integer bundleId) {
        KitBundle bundle = requireBundle(bundleId);
        archiveGuard.assertNotArchived(bundle.getHackathon());
        kitBundleRepository.delete(bundle);
    }

    @Override
    public ShirtSizeResponse updateMyShirtSize(Integer hackathonId, UpdateShirtSizeRequest req) {
        Integer userId = currentUserAccessor.currentUserId();
        String size = parseShirtSize(req.getPreferredShirtSize());
        String fit = parseShirtFit(req.getPreferredShirtFit());
        HackathonRegistration reg = hackathonRegistrationRepository
                .findByHackathon_IdAndUser_Id(hackathonId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("HackathonRegistration", hackathonId));
        reg.setPreferredShirtSize(size);
        reg.setPreferredShirtFit(fit);
        hackathonRegistrationRepository.save(reg);
        return ShirtSizeResponse.builder()
                .hackathonId(hackathonId)
                .preferredShirtSize(size)
                .preferredShirtFit(fit)
                .build();
    }

    @Override
    public List<ShirtSizeResponse> updateMyShirtSizeAll(UpdateShirtSizeRequest req) {
        Integer userId = currentUserAccessor.currentUserId();
        String size = parseShirtSize(req.getPreferredShirtSize());
        String fit = parseShirtFit(req.getPreferredShirtFit());
        List<HackathonRegistration> regs = hackathonRegistrationRepository.findAllByUser_Id(userId);
        for (HackathonRegistration reg : regs) {
            reg.setPreferredShirtSize(size);
            reg.setPreferredShirtFit(fit);
        }
        hackathonRegistrationRepository.saveAll(regs);
        return regs.stream()
                .map(r -> ShirtSizeResponse.builder()
                        .hackathonId(r.getHackathon().getId())
                        .preferredShirtSize(size)
                        .preferredShirtFit(fit)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShirtSizeResponse> listMyShirtSizes() {
        Integer userId = currentUserAccessor.currentUserId();
        return hackathonRegistrationRepository.findAllByUser_Id(userId).stream()
                .map(r -> ShirtSizeResponse.builder()
                        .hackathonId(r.getHackathon().getId())
                        .preferredShirtSize(r.getPreferredShirtSize())
                        .preferredShirtFit(r.getPreferredShirtFit() == null
                                ? ShirtFit.DEFAULT : r.getPreferredShirtFit())
                        .build())
                .toList();
    }

    // ---------- helpers ----------

    private void applyBundleItems(KitBundle bundle, Integer hackathonId,
                                  List<UpsertKitBundleRequest.BundleItemRequest> items) {
        Set<Integer> seen = new HashSet<>();
        for (UpsertKitBundleRequest.BundleItemRequest row : items) {
            if (!seen.add(row.getKitItemId())) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Mỗi món chỉ được thêm một lần trong combo",
                        Map.of("kitItemId", row.getKitItemId()));
            }
            KitItem item = requireItem(row.getKitItemId());
            if (!Objects.equals(item.getHackathon().getId(), hackathonId)) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                        "Kit item không thuộc hackathon này",
                        Map.of("kitItemId", item.getId()));
            }
            int qty = row.getQuantity() == null || row.getQuantity() < 1 ? 1 : row.getQuantity();
            KitBundleItem bi = KitBundleItem.builder()
                    .bundle(bundle)
                    .kitItem(item)
                    .quantity(qty)
                    .build();
            bundle.getItems().add(bi);
        }
    }

    private void clearOtherDefaults(Integer hackathonId, Integer keepBundleId) {
        for (KitBundle other : kitBundleRepository.findByHackathon_IdAndIsDefaultTrue(hackathonId)) {
            if (keepBundleId != null && Objects.equals(other.getId(), keepBundleId)) {
                continue;
            }
            other.setIsDefault(false);
            kitBundleRepository.save(other);
        }
    }

    private List<KitItemResponse> toItemResponses(List<KitItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = items.stream().map(KitItem::getId).toList();
        Map<Integer, List<KitStock>> byItem = kitStockRepository.findByKitItem_IdIn(ids).stream()
                .collect(Collectors.groupingBy(s -> s.getKitItem().getId()));
        return items.stream()
                .map(i -> kitMapper.toItemResponse(i, byItem.getOrDefault(i.getId(), List.of())))
                .toList();
    }

    private Hackathon requireHackathon(Integer hackathonId) {
        return hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
    }

    private KitItem requireItem(Integer itemId) {
        return kitItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("KitItem", itemId));
    }

    private KitBundle requireBundle(Integer bundleId) {
        return kitBundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("KitBundle", bundleId));
    }

    private boolean resolveHasSize(KitItemType type, Boolean hasSize) {
        if (hasSize != null) {
            return hasSize;
        }
        return type == KitItemType.SHIRT;
    }

    private String validateItemName(KitItemType type, String raw) {
        String name = raw == null ? "" : raw.trim();
        if (type == KitItemType.OTHER) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!StringUtils.hasText(name) || "khác".equals(lower) || "other".equals(lower)) {
                throw new BusinessRuleException(ErrorCode.KIT_ITEM_NAME_REQUIRED,
                        "Món loại OTHER cần tên cụ thể (không dùng \"khác\"/\"other\")");
            }
        } else if (!StringUtils.hasText(name)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Tên món kit bắt buộc");
        }
        return name;
    }

    private String normalizeStockSize(KitItem item, String raw) {
        if (!Boolean.TRUE.equals(item.getHasSize())) {
            return null;
        }
        if (!StringUtils.hasText(raw)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Món có size bắt buộc chọn size (" + ShirtSize.allowedList() + ")");
        }
        try {
            return ShirtSize.normalizeOrNull(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Size không hợp lệ. Cho phép: " + ShirtSize.allowedList());
        }
    }

    private String normalizeStockFit(KitItem item, String raw) {
        if (item.getType() != KitItemType.SHIRT) {
            return null;
        }
        try {
            return ShirtFit.normalizeOrDefault(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Dáng áo không hợp lệ. Cho phép: " + ShirtFit.allowedList());
        }
    }

    private String parseShirtSize(String raw) {
        try {
            String size = ShirtSize.normalizeOrNull(raw);
            if (size == null) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "preferredShirtSize bắt buộc. Cho phép: " + ShirtSize.allowedList());
            }
            return size;
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Size không hợp lệ. Cho phép: " + ShirtSize.allowedList());
        }
    }

    private String parseShirtFit(String raw) {
        try {
            return ShirtFit.normalizeOrDefault(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Dáng áo không hợp lệ. Cho phép: " + ShirtFit.allowedList());
        }
    }

    private List<RecipientRow> collectEligibleRecipients(Integer hackathonId) {
        List<Team> teams = teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.ACTIVE);
        if (teams.isEmpty()) {
            return List.of();
        }
        Map<Integer, Team> teamById = teams.stream().collect(Collectors.toMap(Team::getId, t -> t));
        List<Integer> teamIds = teams.stream().map(Team::getId).toList();
        Map<Integer, RecipientRow> byUser = new LinkedHashMap<>();
        for (TeamMember m : teamMemberRepository.findByTeam_IdIn(teamIds)) {
            if (m.getStatus() != TeamMemberStatus.ACCEPTED || m.getUser() == null) {
                continue;
            }
            Team team = teamById.get(m.getTeam().getId());
            if (team == null) {
                continue;
            }
            byUser.putIfAbsent(m.getUser().getId(), new RecipientRow(m.getUser(), team));
        }
        return new ArrayList<>(byUser.values());
    }

    private void assertEligibleRecipient(Integer hackathonId, Integer userId) {
        boolean ok = collectEligibleRecipients(hackathonId).stream()
                .anyMatch(r -> Objects.equals(r.user().getId(), userId));
        if (!ok) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                    "Chỉ phát kit cho thành viên ACCEPTED của đội ACTIVE",
                    Map.of("userId", userId, "hackathonId", hackathonId));
        }
    }

    private Map<Integer, ShirtPrefs> loadShirtPrefs(Integer hackathonId, List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, ShirtPrefs> map = new HashMap<>();
        for (HackathonRegistration reg : hackathonRegistrationRepository.findAllByHackathon_Id(hackathonId)) {
            if (reg.getUser() != null && userIds.contains(reg.getUser().getId())) {
                map.put(reg.getUser().getId(), new ShirtPrefs(reg.getPreferredShirtSize(), reg.getPreferredShirtFit()));
            }
        }
        return map;
    }

    private boolean matchesQuery(RecipientRow row, String q) {
        User u = row.user();
        Team t = row.team();
        return contains(u.getFullName(), q)
                || contains(u.getStudentCode(), q)
                || contains(u.getEmail(), q)
                || contains(t.getTeamName(), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private String resolveIssueSize(KitItem item, Integer userId, Integer hackathonId, String override) {
        if (!Boolean.TRUE.equals(item.getHasSize())) {
            return null;
        }
        if (StringUtils.hasText(override)) {
            try {
                return ShirtSize.normalizeOrNull(override);
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Size không hợp lệ. Cho phép: " + ShirtSize.allowedList());
            }
        }
        return hackathonRegistrationRepository.findByHackathon_IdAndUser_Id(hackathonId, userId)
                .map(HackathonRegistration::getPreferredShirtSize)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private String resolveIssueFit(KitItem item, Integer userId, Integer hackathonId, String override) {
        if (item.getType() != KitItemType.SHIRT) {
            return null;
        }
        if (StringUtils.hasText(override)) {
            try {
                return ShirtFit.normalizeOrNull(override);
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                        "Dáng áo không hợp lệ. Cho phép: " + ShirtFit.allowedList());
            }
        }
        return hackathonRegistrationRepository.findByHackathon_IdAndUser_Id(hackathonId, userId)
                .map(HackathonRegistration::getPreferredShirtFit)
                .filter(StringUtils::hasText)
                .map(v -> {
                    try {
                        return ShirtFit.normalizeOrNull(v);
                    } catch (IllegalArgumentException ex) {
                        return ShirtFit.DEFAULT;
                    }
                })
                .orElse(ShirtFit.DEFAULT);
    }

    private KitStock requireStockForIssue(KitItem item, String fit, String size) {
        if (Boolean.TRUE.equals(item.getHasSize()) && !StringUtils.hasText(size)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Chưa có size áo — chọn size tại quầy trước khi phát");
        }
        String sizeKey = size == null ? "" : size;
        String fitKey = fit == null ? "" : fit;
        return kitStockRepository.findByKitItem_IdAndFitKeyAndSizeKey(item.getId(), fitKey, sizeKey)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.KIT_OUT_OF_STOCK,
                        "Chưa khai báo tồn kho cho món/dáng/size này",
                        Map.of("kitItemId", item.getId(), "fit", fitKey, "size", sizeKey)));
    }

    private List<Warning> kickoffWarnings(Integer hackathonId, Integer allocationId,
                                          Integer userId, Integer kitItemId) {
        List<Warning> warnings = new ArrayList<>();
        if (!isInsideKickoffWindow(hackathonId)) {
            Warning w = Warning.of(WARNING_OUTSIDE_KICKOFF,
                    "Phát kit ngoài khung giờ Kickoff — đã ghi audit, không chặn thao tác");
            warnings.add(w);
            auditService.log(AuditAction.KIT_ISSUED, "kit_allocations", allocationId, Map.of(
                    "warning", WARNING_OUTSIDE_KICKOFF,
                    "hackathonId", hackathonId,
                    "userId", userId,
                    "kitItemId", kitItemId));
        }
        return warnings;
    }

    private boolean isInsideKickoffWindow(Integer hackathonId) {
        List<Event> kickoffs = eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF);
        if (kickoffs.isEmpty()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Event e : kickoffs) {
            LocalDateTime start = e.getStartsAt();
            LocalDateTime end = e.getEndsAt() != null ? e.getEndsAt() : start.plusHours(8);
            if (start != null && !now.isBefore(start) && !now.isAfter(end)) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime earliestKickoffStartsAt(Integer hackathonId) {
        return eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF).stream()
                .map(Event::getStartsAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private static KitReconciliationLineResponse line(KitItem item, String fit, String size,
                                                      int total, int issued, int remaining, int eligible) {
        return KitReconciliationLineResponse.builder()
                .kitItemId(item.getId())
                .kitItemName(item.getName())
                .fit(fit)
                .size(size)
                .quantityTotal(total)
                .quantityIssued(issued)
                .remaining(remaining)
                .eligibleCount(eligible)
                .variance(issued - eligible)
                .shortfall(eligible - total)
                .build();
    }

    private static String fitSizeKey(String fit, String size) {
        return (fit == null ? "" : fit) + "|" + (size == null ? "" : size);
    }

    private static Map<String, Object> missingEntry(KitItem item, String fit, String size,
                                                    KitStock stock, int needed) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kitItemId", item.getId());
        m.put("kitItemName", item.getName());
        m.put("fit", fit == null ? "" : fit);
        m.put("size", size == null ? "" : size);
        m.put("needed", needed);
        m.put("remaining", stock.remaining());
        return m;
    }

    private record RecipientRow(User user, Team team) {}

    private record ShirtPrefs(String size, String fit) {
        static ShirtPrefs empty() {
            return new ShirtPrefs(null, null);
        }

        String fitOrDefault() {
            return StringUtils.hasText(fit) ? fit : ShirtFit.DEFAULT;
        }
    }

    private record PreparedIssue(KitBundleItem bundleItem, KitItem item, KitStock stock,
                                 String fit, String size, int qty) {}
}
