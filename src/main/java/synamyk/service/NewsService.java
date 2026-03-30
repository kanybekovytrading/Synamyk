package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.dto.NewsDetailResponse;
import synamyk.dto.NewsListResponse;
import synamyk.dto.admin.CreateNewsRequest;
import synamyk.entities.NewsArticle;
import synamyk.repo.NewsArticleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsArticleRepository newsRepository;

    public List<NewsListResponse> getNewsList() {
        return newsRepository.findByActiveTrueOrderByPublishedAtDesc().stream()
                .map(this::toListResponse)
                .toList();
    }

    public NewsDetailResponse getNewsDetail(Long id) {
        NewsArticle article = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News article not found"));

        if (!article.getActive()) {
            throw new RuntimeException("News article not found");
        }

        return NewsDetailResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .coverImageUrl(article.getCoverImageUrl())
                .content(article.getContent())
                .publishedAt(article.getPublishedAt())
                .build();
    }

    @Transactional
    public NewsDetailResponse createNews(CreateNewsRequest request) {
        NewsArticle article = NewsArticle.builder()
                .title(request.getTitle())
                .coverImageUrl(request.getCoverImageUrl())
                .content(request.getContent())
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
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setContent(request.getContent());
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

    private NewsListResponse toListResponse(NewsArticle a) {
        String preview = a.getContent() != null && a.getContent().length() > 150
                ? a.getContent().substring(0, 150) + "..."
                : a.getContent();
        return NewsListResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .coverImageUrl(a.getCoverImageUrl())
                .preview(preview)
                .publishedAt(a.getPublishedAt())
                .build();
    }

    private NewsDetailResponse toDetailResponse(NewsArticle a) {
        return NewsDetailResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .coverImageUrl(a.getCoverImageUrl())
                .content(a.getContent())
                .publishedAt(a.getPublishedAt())
                .build();
    }
}