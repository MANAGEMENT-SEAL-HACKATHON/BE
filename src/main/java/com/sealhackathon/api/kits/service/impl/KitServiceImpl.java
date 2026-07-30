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
import com.sealhackathon.api.kits.entity.KitItem;
import com.sealhackathon.api.kits.entity.KitStock;
import com.sealhackathon.api.kits.mapper.KitMapper;
import com.sealhackathon.api.kits.repository.KitAllocationRepository;
import com.sealhackathon.api.kits.repository.KitItemRepository;
import com.sealhackathon.api.kits.repository.KitStockRepository;
import com.sealhackathon.api.kits.service.KitService;
import com.sealhackathon.api.kits.value_object.KitAllocationStatus;
import com.sealhackathon.api.kits.value_object.KitItemType;
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

        boolean hasSize = resolveHasSize(req.getType(), req.getHasSize());
        KitItem saved = kitItemRepository.save(KitItem.builder()
                .hackathon(hackathon)
                .name(req.getName().trim())
                .type(req.getType())
                .hasSize(hasSize)
                .build());
        return kitMapper.toItemResponse(saved, List.of());
    }

    @Override
    public KitItemResponse updateItem(Integer itemId, UpdateKitItemRequest req) {
        KitItem item = requireItem(itemId);
        archiveGuard.assertNotArchived(item.getHackathon());

        item.setName(req.getName().trim());
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

        KitStock stock = kitStockRepository.findByKitItem_IdAndSizeKey(itemId, sizeKey)
                .orElseGet(() -> KitStock.builder().kitItem(item).size(size).sizeKey(sizeKey).build());

        int issued = stock.getQuantityIssued() == null ? 0 : stock.getQuantityIssued();
        if (req.getQuantityTotal() < issued) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "quantityTotal không được nhỏ hơn số đã phát (" + issued + ")",
                    Map.of("quantityIssued", issued, "quantityTotal", req.getQuantityTotal()));
        }
        stock.setQuantityTotal(req.getQuantityTotal());
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

        Map<Integer, String> shirtByUser = loadShirtSizes(hackathonId,
                rows.stream().map(r -> r.user().getId()).toList());

        List<Integer> userIds = rows.stream().map(r -> r.user().getId()).toList();
        Map<Integer, List<KitAllocation>> allocByUser = userIds.isEmpty()
                ? Map.of()
                : kitAllocationRepository.findByHackathon_IdAndUser_IdIn(hackathonId, userIds).stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()));

        List<KitRecipientResponse> result = new ArrayList<>();
        for (RecipientRow row : rows) {
            Integer uid = row.user().getId();
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
                    .preferredShirtSize(shirtByUser.get(uid))
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
        KitStock stock = requireStockForIssue(item, size);

        if (stock.remaining() <= 0) {
            throw new BusinessRuleException(ErrorCode.KIT_OUT_OF_STOCK,
                    "Hết tồn kho cho món/size này",
                    Map.of(
                            "kitItemId", item.getId(),
                            "size", size == null ? "" : size,
                            "quantityTotal", stock.getQuantityTotal(),
                            "quantityIssued", stock.getQuantityIssued()));
        }

        stock.setQuantityIssued(stock.getQuantityIssued() + 1);
        kitStockRepository.save(stock);

        User issuer = userRepository.findById(currentUserAccessor.currentUserId()).orElse(null);
        allocation.setSize(size);
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
        audit.put("size", size);
        audit.put("issuedAt", saved.getIssuedAt());
        auditService.log(AuditAction.KIT_ISSUED, "kit_allocations", saved.getId(), audit);

        List<Warning> warnings = new ArrayList<>();
        if (!isInsideKickoffWindow(hackathonId)) {
            Warning w = Warning.of(WARNING_OUTSIDE_KICKOFF,
                    "Phát kit ngoài khung giờ Kickoff — đã ghi audit, không chặn thao tác");
            warnings.add(w);
            auditService.log(AuditAction.KIT_ISSUED, "kit_allocations", saved.getId(), Map.of(
                    "warning", WARNING_OUTSIDE_KICKOFF,
                    "hackathonId", hackathonId,
                    "userId", recipient.getId(),
                    "kitItemId", item.getId()));
        }

        return new IssueResult(kitMapper.toAllocationResponse(saved), warnings);
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
        KitStock stock = kitStockRepository.findByKitItem_IdAndSizeKey(item.getId(), sizeKey)
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
                "size", allocation.getSize() == null ? "" : allocation.getSize(),
                "reason", req.getReason().trim()));

        return kitMapper.toAllocationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitReconciliationLineResponse> reconciliation(Integer hackathonId) {
        requireHackathon(hackathonId);
        List<RecipientRow> recipients = collectEligibleRecipients(hackathonId);
        Map<Integer, String> shirtByUser = loadShirtSizes(hackathonId,
                recipients.stream().map(r -> r.user().getId()).toList());

        int eligibleTotal = recipients.size();
        Map<String, Long> sizeCounts = shirtByUser.values().stream()
                .collect(Collectors.groupingBy(s -> s == null ? "" : s, Collectors.counting()));

        List<KitItem> items = kitItemRepository.findByHackathon_IdOrderByIdAsc(hackathonId);
        List<KitReconciliationLineResponse> lines = new ArrayList<>();
        for (KitItem item : items) {
            List<KitStock> stocks = kitStockRepository.findByKitItem_IdOrderBySizeAsc(item.getId());
            if (stocks.isEmpty()) {
                int eligible = Boolean.TRUE.equals(item.getHasSize()) ? 0 : eligibleTotal;
                lines.add(KitReconciliationLineResponse.builder()
                        .kitItemId(item.getId())
                        .kitItemName(item.getName())
                        .size(null)
                        .quantityTotal(0)
                        .quantityIssued(0)
                        .remaining(0)
                        .eligibleCount(eligible)
                        .variance(-eligible)
                        .build());
                continue;
            }
            for (KitStock stock : stocks) {
                int eligible;
                if (Boolean.TRUE.equals(item.getHasSize())) {
                    String key = stock.getSize() == null ? "" : stock.getSize();
                    eligible = sizeCounts.getOrDefault(key, 0L).intValue();
                } else {
                    eligible = eligibleTotal;
                }
                int issued = stock.getQuantityIssued() == null ? 0 : stock.getQuantityIssued();
                lines.add(KitReconciliationLineResponse.builder()
                        .kitItemId(item.getId())
                        .kitItemName(item.getName())
                        .size(stock.getSize())
                        .quantityTotal(stock.getQuantityTotal())
                        .quantityIssued(issued)
                        .remaining(stock.remaining())
                        .eligibleCount(eligible)
                        .variance(issued - eligible)
                        .build());
            }
        }
        return lines;
    }

    @Override
    public ShirtSizeResponse updateMyShirtSize(Integer hackathonId, UpdateShirtSizeRequest req) {
        Integer userId = currentUserAccessor.currentUserId();
        String size = parseShirtSize(req.getPreferredShirtSize());
        HackathonRegistration reg = hackathonRegistrationRepository
                .findByHackathon_IdAndUser_Id(hackathonId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("HackathonRegistration", hackathonId));
        reg.setPreferredShirtSize(size);
        hackathonRegistrationRepository.save(reg);
        return ShirtSizeResponse.builder().hackathonId(hackathonId).preferredShirtSize(size).build();
    }

    @Override
    public List<ShirtSizeResponse> updateMyShirtSizeAll(UpdateShirtSizeRequest req) {
        Integer userId = currentUserAccessor.currentUserId();
        String size = parseShirtSize(req.getPreferredShirtSize());
        List<HackathonRegistration> regs = hackathonRegistrationRepository.findAllByUser_Id(userId);
        for (HackathonRegistration reg : regs) {
            reg.setPreferredShirtSize(size);
        }
        hackathonRegistrationRepository.saveAll(regs);
        return regs.stream()
                .map(r -> ShirtSizeResponse.builder()
                        .hackathonId(r.getHackathon().getId())
                        .preferredShirtSize(size)
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
                        .build())
                .toList();
    }

    // ---------- helpers ----------

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

    private boolean resolveHasSize(KitItemType type, Boolean hasSize) {
        if (hasSize != null) {
            return hasSize;
        }
        return type == KitItemType.SHIRT;
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

    private Map<Integer, String> loadShirtSizes(Integer hackathonId, List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> map = new HashMap<>();
        for (HackathonRegistration reg : hackathonRegistrationRepository.findAllByHackathon_Id(hackathonId)) {
            if (reg.getUser() != null && userIds.contains(reg.getUser().getId())) {
                map.put(reg.getUser().getId(), reg.getPreferredShirtSize());
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

    private KitStock requireStockForIssue(KitItem item, String size) {
        if (Boolean.TRUE.equals(item.getHasSize()) && !StringUtils.hasText(size)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Chưa có size áo — chọn size tại quầy trước khi phát");
        }
        String sizeKey = size == null ? "" : size;
        return kitStockRepository.findByKitItem_IdAndSizeKey(item.getId(), sizeKey)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.KIT_OUT_OF_STOCK,
                        "Chưa khai báo tồn kho cho món/size này",
                        Map.of("kitItemId", item.getId(), "size", sizeKey)));
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

    private record RecipientRow(User user, Team team) {}
}
