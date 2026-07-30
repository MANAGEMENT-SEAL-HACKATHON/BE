package com.sealhackathon.api.showcase.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.showcase.dto.request.CreateShowcaseArticleRequest;
import com.sealhackathon.api.showcase.dto.request.ShowcaseArticleBlockRequest;
import com.sealhackathon.api.showcase.dto.request.UpdateShowcaseArticleRequest;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleSummaryResponse;
import com.sealhackathon.api.showcase.entity.HallOfFameEntry;
import com.sealhackathon.api.showcase.entity.ShowcaseArticle;
import com.sealhackathon.api.showcase.entity.ShowcaseArticleBlock;
import com.sealhackathon.api.showcase.mapper.ShowcaseMapper;
import com.sealhackathon.api.showcase.repository.HallOfFameEntryRepository;
import com.sealhackathon.api.showcase.repository.ShowcaseArticleRepository;
import com.sealhackathon.api.showcase.service.ShowcaseArticleService;
import com.sealhackathon.api.showcase.support.ShowcaseCoverStorageService;
import com.sealhackathon.api.showcase.value_object.ShowcaseArticleStatus;
import com.sealhackathon.api.showcase.value_object.ShowcaseBlockType;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Showcase article CRUD intentionally bypasses {@code HackathonArchiveGuard}.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.showcase.enabled", havingValue = "true", matchIfMissing = true)
public class ShowcaseArticleServiceImpl implements ShowcaseArticleService {

    private final ShowcaseArticleRepository articleRepository;
    private final HallOfFameEntryRepository hallOfFameEntryRepository;
    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final ShowcaseMapper showcaseMapper;
    private final ShowcaseCoverStorageService coverStorageService;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    @Transactional(readOnly = true)
    public List<ShowcaseArticleSummaryResponse> listPublished() {
        return articleRepository.findByStatusOrderByPublishedAtDesc(ShowcaseArticleStatus.PUBLISHED).stream()
                .map(showcaseMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShowcaseArticleResponse getPublishedBySlug(String slug) {
        ShowcaseArticle article = articleRepository
                .findBySlugAndStatus(slug, ShowcaseArticleStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("ShowcaseArticle", slug));
        return showcaseMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowcaseArticleResponse> listByHackathon(Integer hackathonId) {
        assertHackathonExists(hackathonId);
        return articleRepository.findByHackathonIdOrderByUpdatedAtDesc(hackathonId).stream()
                .map(showcaseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShowcaseArticleResponse getById(Integer id) {
        return showcaseMapper.toResponse(requireArticle(id));
    }

    @Override
    @Transactional
    public ShowcaseArticleResponse create(Integer hackathonId, CreateShowcaseArticleRequest req) {
        assertHackathonExists(hackathonId);
        String slug = normalizeSlug(req.getSlug());
        assertSlugAvailable(slug, null);

        User author = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));

        ShowcaseArticle article = ShowcaseArticle.builder()
                .hackathonId(hackathonId)
                .slug(slug)
                .title(req.getTitle().trim())
                .summary(req.getSummary())
                .status(ShowcaseArticleStatus.DRAFT)
                .author(author)
                .build();
        replaceBlocks(article, req.getBlocks());
        return showcaseMapper.toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ShowcaseArticleResponse update(Integer id, UpdateShowcaseArticleRequest req) {
        ShowcaseArticle article = requireArticle(id);
        if (StringUtils.hasText(req.getSlug())) {
            String slug = normalizeSlug(req.getSlug());
            assertSlugAvailable(slug, id);
            article.setSlug(slug);
        }
        if (StringUtils.hasText(req.getTitle())) {
            article.setTitle(req.getTitle().trim());
        }
        if (req.getSummary() != null) {
            article.setSummary(req.getSummary());
        }
        if (req.getBlocks() != null) {
            replaceBlocks(article, req.getBlocks());
        }
        return showcaseMapper.toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        ShowcaseArticle article = requireArticle(id);
        if (StringUtils.hasText(article.getCoverImageKey())) {
            coverStorageService.deleteQuietly(article.getCoverImageKey());
        }
        if (article.getBlocks() != null) {
            for (ShowcaseArticleBlock block : article.getBlocks()) {
                if (StringUtils.hasText(block.getImageKey())) {
                    coverStorageService.deleteQuietly(block.getImageKey());
                }
            }
        }
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public ShowcaseArticleResponse publish(Integer id) {
        ShowcaseArticle article = requireArticle(id);
        article.setStatus(ShowcaseArticleStatus.PUBLISHED);
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        return showcaseMapper.toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ShowcaseArticleResponse unpublish(Integer id) {
        ShowcaseArticle article = requireArticle(id);
        article.setStatus(ShowcaseArticleStatus.DRAFT);
        return showcaseMapper.toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ShowcaseArticleResponse generateDraftFromChampions(Integer hackathonId) {
        Hackathon hackathon = assertHackathonExists(hackathonId);
        HallOfFameEntry hof = hallOfFameEntryRepository.findByHackathonId(hackathonId)
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCode.VALIDATION_FAILED,
                        "Chưa có bản ghi bảng vàng — cần có giải Nhất trước khi tạo nháp"));

        String baseSlug = normalizeSlug(hackathon.getSlug() + "-champion");
        String slug = uniqueSlug(baseSlug);

        User author = userRepository.findById(currentUserAccessor.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserAccessor.currentUserId()));

        ShowcaseArticle article = ShowcaseArticle.builder()
                .hackathonId(hackathonId)
                .slug(slug)
                .title("Vinh danh quán quân " + hackathon.getName())
                .summary("Đội " + hof.getTeamName() + " — nhà vô địch mùa " + hof.getYear())
                .status(ShowcaseArticleStatus.DRAFT)
                .author(author)
                .build();

        List<ShowcaseArticleBlockRequest> blocks = new ArrayList<>();
        blocks.add(block(ShowcaseBlockType.HEADING, 0, "Quán quân " + hof.getTeamName(), null));
        blocks.add(block(ShowcaseBlockType.PARAGRAPH, 1,
                "Tại " + hof.getHackathonName() + " (" + hof.getSeason() + " " + hof.getYear() + "), "
                        + "đội " + hof.getTeamName()
                        + (StringUtils.hasText(hof.getTrackName()) ? " thuộc bảng " + hof.getTrackName() : "")
                        + " đã giành giải " + (hof.getPrizeName() != null ? hof.getPrizeName() : "Nhất") + ".",
                null));
        if (StringUtils.hasText(hof.getMemberNames())) {
            blocks.add(block(ShowcaseBlockType.PARAGRAPH, 2,
                    "Thành viên: " + hof.getMemberNames() + ".", null));
        }
        if (StringUtils.hasText(hof.getPrizeValue())) {
            blocks.add(block(ShowcaseBlockType.QUOTE, 3,
                    "Giá trị giải thưởng: " + hof.getPrizeValue(), null));
        }
        replaceBlocks(article, blocks);
        return showcaseMapper.toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ShowcaseArticleResponse uploadCover(Integer id, MultipartFile file) {
        ShowcaseArticle article = requireArticle(id);
        String key = coverStorageService.storeCover(id, file, article.getCoverImageKey());
        article.setCoverImageKey(key);
        return showcaseMapper.toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public Map<String, String> uploadBlockImage(Integer id, MultipartFile file) {
        requireArticle(id);
        String key = coverStorageService.storeBlockImage(id, file);
        return Map.of("imageKey", key);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadCoverBySlug(String slug) {
        ShowcaseArticle article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("ShowcaseArticle", slug));
        if (article.getStatus() != ShowcaseArticleStatus.PUBLISHED
                && !isCoordinatorContext()) {
            throw new ResourceNotFoundException("ShowcaseArticle", slug);
        }
        return coverStorageService.loadAsResource(article.getCoverImageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadBlockImageBySlug(String slug, Integer blockId) {
        ShowcaseArticle article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("ShowcaseArticle", slug));
        if (article.getStatus() != ShowcaseArticleStatus.PUBLISHED
                && !isCoordinatorContext()) {
            throw new ResourceNotFoundException("ShowcaseArticle", slug);
        }
        ShowcaseArticleBlock block = article.getBlocks().stream()
                .filter(b -> blockId.equals(b.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ShowcaseArticleBlock", blockId));
        return coverStorageService.loadAsResource(block.getImageKey());
    }

    private boolean isCoordinatorContext() {
        return currentUserAccessor.currentUserId() != null;
    }

    private Hackathon assertHackathonExists(Integer hackathonId) {
        return hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
    }

    private ShowcaseArticle requireArticle(Integer id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShowcaseArticle", id));
    }

    private void assertSlugAvailable(String slug, Integer excludeId) {
        boolean taken = excludeId == null
                ? articleRepository.existsBySlug(slug)
                : articleRepository.existsBySlugAndIdNot(slug, excludeId);
        if (taken) {
            throw new ConflictException(ErrorCode.HACKATHON_DUPLICATE, "Slug bài viết đã tồn tại",
                    Map.of("slug", slug));
        }
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int i = 2;
        while (articleRepository.existsBySlug(candidate)) {
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }

    private static String normalizeSlug(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Slug không được trống");
        }
        String slug = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(slug)) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Slug không hợp lệ");
        }
        return slug;
    }

    private void replaceBlocks(ShowcaseArticle article, List<ShowcaseArticleBlockRequest> requests) {
        if (article.getBlocks() == null) {
            article.setBlocks(new ArrayList<>());
        }
        article.getBlocks().clear();
        if (requests == null || requests.isEmpty()) {
            return;
        }
        int order = 0;
        for (ShowcaseArticleBlockRequest req : requests) {
            if (req.getType() == null) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Loại khối nội dung bắt buộc");
            }
            ShowcaseArticleBlock block = ShowcaseArticleBlock.builder()
                    .article(article)
                    .type(req.getType())
                    .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : order)
                    .text(req.getText())
                    .imageKey(req.getImageKey())
                    .build();
            article.getBlocks().add(block);
            order++;
        }
    }

    private static ShowcaseArticleBlockRequest block(
            ShowcaseBlockType type, int order, String text, String imageKey) {
        ShowcaseArticleBlockRequest req = new ShowcaseArticleBlockRequest();
        req.setType(type);
        req.setSortOrder(order);
        req.setText(text);
        req.setImageKey(imageKey);
        return req;
    }
}
