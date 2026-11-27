package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.PostPageResponse;
import com.ssangtteongnip.application.service.dto.PostResponse;
import com.ssangtteongnip.application.service.dto.PostSummaryResponse;
import com.ssangtteongnip.application.service.request.PostRequest;
import com.ssangtteongnip.domain.post.Post;
import com.ssangtteongnip.domain.post.PostImage;
import com.ssangtteongnip.domain.post.PostRepository;
import com.ssangtteongnip.domain.profile.FileStorage;
import com.ssangtteongnip.domain.profile.PresignedUrl;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.global.transaction.AfterCommit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    static final int PAGE_SIZE = 20;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Transactional
    public PostResponse create(Long userId, PostRequest request) {
        User author = findUserOrThrow(userId);
        author.validateHasNickname();
        Post post = Post.create(userId, request.title(), request.content(), request.images(), fileStorage);
        saveOrDeleteUploaded(post);
        return PostResponse.convert(post, author, userId);
    }

    // 이미지 개수 한도를 동시 수정이 함께 넘지 못하도록 글 행을 잠근 뒤 고친다.
    @Transactional
    public PostResponse rewrite(Long userId, Long id, PostRequest request) {
        Post post = findForUpdateOrThrow(id);
        post.validateWrittenBy(userId);
        List<PostImage> removed = post.rewrite(request.title(), request.content(),
                request.keepImageIds(), request.images(), fileStorage);
        removed.forEach(this::deleteObjectAfterCommit);
        return PostResponse.convert(post, findUserOrThrow(userId), userId);
    }

    public PostResponse find(Long userId, Long id) {
        Post post = findOrThrow(id);
        return PostResponse.convert(post, findUserOrThrow(post.getUserId()), userId);
    }

    public PostPageResponse findPage(int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<Post> posts = postRepository.findNewest(page, PAGE_SIZE);
        Map<Long, User> authors = findAuthors(posts);
        List<PostSummaryResponse> summaries = posts.stream()
                .map(post -> PostSummaryResponse.convert(post, authors.get(post.getUserId())))
                .toList();
        return new PostPageResponse(summaries, page, PAGE_SIZE, postRepository.count());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Post post = findOrThrow(id);
        post.validateWrittenBy(userId);
        postRepository.delete(post);
        post.getImages().forEach(this::deleteObjectAfterCommit);
    }

    public PresignedUrl findImageViewUrl(UUID publicId) {
        return postRepository.findImage(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_IMAGE_NOT_FOUND))
                .createViewUrl(fileStorage);
    }

    // 저장소에는 이미 올라갔으므로 DB 저장이 실패하면 올린 이미지가 고아로 남지 않게 지우고 예외를 다시 던진다.
    private void saveOrDeleteUploaded(Post post) {
        try {
            postRepository.save(post);
        } catch (RuntimeException e) {
            post.deleteImageObjects(fileStorage);
            throw e;
        }
    }

    // DB 행은 이 트랜잭션에서 지우고, S3 객체는 커밋된 뒤에 지운다. 객체 삭제가 실패해도 글 삭제·수정은 성공시킨다.
    private void deleteObjectAfterCommit(PostImage image) {
        AfterCommit.runQuietly("게시글 이미지 객체 삭제", () -> image.deleteObject(fileStorage));
    }

    private Map<Long, User> findAuthors(List<Post> posts) {
        Set<Long> authorIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Post findOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Post findForUpdateOrThrow(Long id) {
        return postRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
