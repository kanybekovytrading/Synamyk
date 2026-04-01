package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.dto.NewsDetailResponse;
import synamyk.dto.NewsListResponse;
import synamyk.dto.admin.CreateNewsRequest;
import synamyk.entities.NewsArticle;
import synamyk.repo.NewsArticleRepository;
import synamyk.util.L10n;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsArticleRepository newsRepository;
    private final MinioService minioService;

    public List<NewsListResponse> getNewsList(String lang) {
        return newsRepository.findByActiveTrueOrderByPublishedAtDesc().stream()
                .map(a -> toListResponse(a, lang))
                .toList();
    }

    public NewsDetailResponse getNewsDetail(Long id, String lang) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News article not found"));

        if (!article.getActive()) {
            throw new RuntimeException("News article not found");
        }

        return NewsDetailResponse.builder()
                .id(article.getId())
                .title(L10n.pick(article.getTitle(), article.getTitleKy(), lang))
                .coverImageUrl(minioService.presign(article.getCoverImageUrl()))
                .content(L10n.pick(article.getContent(), article.getContentKy(), lang))
                .publishedAt(article.getPublishedAt())
                .build();
    }

    @Transactional
    public NewsDetailResponse createNews(CreateNewsRequest request) {
        NewsArticle article = NewsArticle.builder()
                .title(request.getTitle())
                .titleKy(request.getTitleKy())
                .coverImageUrl(request.getCoverImageUrl())
                .content(request.getContent())
                .contentKy(request.getContentKy())
                .publishedAt(request.getPublishedAt())
                .active(true)
                .build();
        return toDetailResponse(newsRepository.save(article));
    }

    @Transactional
    public NewsDetailResponse updateNews(Long id, CreateNewsRequest request) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News article not found"));

        article.setTitle(request.getTitle());
        article.setTitleKy(request.getTitleKy());
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setContent(request.getContent());
        article.setContentKy(request.getContentKy());
        article.setPublishedAt(request.getPublishedAt());
        return toDetailResponse(newsRepository.save(article));
    }

    @Transactional
    public void deleteNews(Long id) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News article not found"));
        article.setActive(false);
        newsRepository.save(article);
    }

    private NewsListResponse toListResponse(NewsArticle a, String lang) {
        String content = L10n.pick(a.getContent(), a.getContentKy(), lang);
        String preview = content != null && content.length() > 150
                ? content.substring(0, 150) + "..."
                : content;
        return NewsListResponse.builder()
                .id(a.getId())
                .title(L10n.pick(a.getTitle(), a.getTitleKy(), lang))
                .coverImageUrl(minioService.presign(a.getCoverImageUrl()))
                .preview(preview)
                .publishedAt(a.getPublishedAt())
                .build();
    }

    private NewsDetailResponse toDetailResponse(NewsArticle a) {
        // Admin response — always Russian (full object)
        return NewsDetailResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .coverImageUrl(minioService.presign(a.getCoverImageUrl()))
                .content(a.getContent())
                .publishedAt(a.getPublishedAt())
                .build();
    }
}