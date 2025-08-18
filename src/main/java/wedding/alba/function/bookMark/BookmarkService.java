package wedding.alba.function.bookMark;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wedding.alba.entity.Bookmark;
import wedding.alba.entity.Posting;
import wedding.alba.function.posting.PostingRepository;
import wedding.alba.repository.BookmarkRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 북마크 비즈니스 로직 서비스
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostingRepository postingRepository;

    /**
     * 사용자의 북마크 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크 목록 페이지
     */
    public Page<BookmarkResponseDto> getBookmarksByUserId(Long userId, Pageable pageable) {
        log.debug("사용자 {}의 북마크 목록 조회 시작", userId);

        try {
            // JPA를 사용한 페이징 조회 (게시글 정보 포함)
            Page<Bookmark> bookmarkPage = bookmarkRepository.findBookmarksWithPostingByUserId(userId, pageable);

            // Entity를 DTO로 변환
            Page<BookmarkResponseDto> responsePage = bookmarkPage.map(this::convertToBookmarkResponseDto);

            log.debug("사용자 {}의 북마크 {}개 조회 완료", userId, responsePage.getContent().size());

            return responsePage;

        } catch (Exception e) {
            log.error("사용자 {}의 북마크 목록 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("북마크 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 새로운 북마크 추가
     *
     * @param userId 사용자 ID
     * @param requestDto 북마크 추가 요청 정보
     * @return 생성된 북마크 정보
     * @throws IllegalArgumentException 이미 북마크한 게시글이거나 존재하지 않는 게시글인 경우
     */
    @Transactional
    public BookmarkResponseDto addBookmark(Long userId, BookmarkRequestDto requestDto) {
        log.debug("사용자 {}가 게시글 {} 북마크 추가 시도", userId, requestDto.getPostingId());

        try {
            // 이미 북마크한 게시글인지 확인
            if (bookmarkRepository.existsByUserIdAndPostingId(userId, requestDto.getPostingId())) {
                log.warn("사용자 {}가 이미 북마크한 게시글 {} 중복 추가 시도", userId, requestDto.getPostingId());
                throw new IllegalArgumentException("이미 북마크한 게시글입니다.");
            }

            // 게시글 존재 여부 확인
            Posting posting = postingRepository.findById(requestDto.getPostingId())
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 게시글 {} 북마크 시도", requestDto.getPostingId());
                        return new IllegalArgumentException("존재하지 않는 게시글입니다.");
                    });

            // 자신의 게시글은 북마크할 수 없도록 제한 (선택사항)
            if (posting.getUserId().equals(userId)) {
                log.warn("사용자 {}가 자신의 게시글 {} 북마크 시도", userId, requestDto.getPostingId());
                throw new IllegalArgumentException("자신의 게시글은 북마크할 수 없습니다.");
            }

            // 북마크 생성
            Bookmark bookmark = Bookmark.builder()
                    .postingId(requestDto.getPostingId())
                    .userId(userId)
                    .memo(requestDto.getMemo())
                    .marcDate(LocalDateTime.now())
                    .build();

            Bookmark savedBookmark = bookmarkRepository.save(bookmark);

            log.info("사용자 {}가 게시글 {} 북마크 추가 완료 (북마크 ID: {})",
                    userId, requestDto.getPostingId(), savedBookmark.getBookmarkId());

            // 저장된 북마크와 게시글 정보로 응답 DTO 생성
            return createBookmarkResponseDto(savedBookmark, posting);

        } catch (IllegalArgumentException e) {
            throw e; // 비즈니스 로직 예외는 그대로 전파
        } catch (Exception e) {
            log.error("사용자 {}가 게시글 {} 북마크 추가 중 오류 발생: {}", userId, requestDto.getPostingId(), e.getMessage(), e);
            throw new RuntimeException("북마크 추가 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 북마크 삭제
     *
     * @param bookmarkId 삭제할 북마크 ID
     * @param userId 요청한 사용자 ID
     * @throws IllegalArgumentException 존재하지 않는 북마크이거나 삭제 권한이 없는 경우
     */
    @Transactional
    public void deleteBookmark(Long bookmarkId, Long userId) {
        log.debug("사용자 {}가 북마크 {} 삭제 시도", userId, bookmarkId);

        try {
            Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 북마크 {} 삭제 시도", bookmarkId);
                        return new IllegalArgumentException("존재하지 않는 북마크입니다.");
                    });

            // 본인의 북마크인지 확인
            if (!bookmark.getUserId().equals(userId)) {
                log.warn("사용자 {}가 다른 사용자의 북마크 {} 삭제 시도", userId, bookmarkId);
                throw new IllegalArgumentException("삭제 권한이 없습니다.");
            }

            bookmarkRepository.delete(bookmark);

            log.info("사용자 {}가 북마크 {} 삭제 완료", userId, bookmarkId);

        } catch (IllegalArgumentException e) {
            throw e; // 비즈니스 로직 예외는 그대로 전파
        } catch (Exception e) {
            log.error("사용자 {}가 북마크 {} 삭제 중 오류 발생: {}", userId, bookmarkId, e.getMessage(), e);
            throw new RuntimeException("북마크 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 북마크 메모 수정
     *
     * @param bookmarkId 수정할 북마크 ID
     * @param userId 요청한 사용자 ID
     * @param memo 새로운 메모 내용
     * @return 수정된 북마크 정보
     * @throws IllegalArgumentException 존재하지 않는 북마크이거나 수정 권한이 없는 경우
     */
    @Transactional
    public BookmarkResponseDto updateMemo(Long bookmarkId, Long userId, String memo) {
        log.debug("사용자 {}가 북마크 {} 메모 수정 시도", userId, bookmarkId);

        try {
            // JPA를 사용한 북마크 조회 (게시글 정보 포함)
            Bookmark bookmark = bookmarkRepository.findBookmarkWithPostingById(bookmarkId)
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 북마크 {} 메모 수정 시도", bookmarkId);
                        return new IllegalArgumentException("존재하지 않는 북마크입니다.");
                    });

            // 본인의 북마크인지 확인
            if (!bookmark.getUserId().equals(userId)) {
                log.warn("사용자 {}가 다른 사용자의 북마크 {} 메모 수정 시도", userId, bookmarkId);
                throw new IllegalArgumentException("수정 권한이 없습니다.");
            }

            // 메모 수정
            bookmark.setMemo(memo);
            Bookmark updatedBookmark = bookmarkRepository.save(bookmark);

            log.info("사용자 {}가 북마크 {} 메모 수정 완료", userId, bookmarkId);

            // 수정된 북마크를 DTO로 변환하여 반환
            return convertToBookmarkResponseDto(updatedBookmark);

        } catch (IllegalArgumentException e) {
            throw e; // 비즈니스 로직 예외는 그대로 전파
        } catch (Exception e) {
            log.error("사용자 {}가 북마크 {} 메모 수정 중 오류 발생: {}", userId, bookmarkId, e.getMessage(), e);
            throw new RuntimeException("북마크 메모 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자가 특정 게시글을 북마크했는지 확인
     *
     * @param userId 사용자 ID
     * @param postingId 게시글 ID
     * @return 북마크 정보 (없으면 null)
     */
    public BookmarkResponseDto findBookmarkByUserAndPosting(Long userId, Long postingId) {
        log.debug("사용자 {}의 게시글 {} 북마크 여부 확인", userId, postingId);

        try {
            Optional<Bookmark> bookmarkOpt = bookmarkRepository
                    .findBookmarkWithPostingByUserAndPosting(userId, postingId);

            if (bookmarkOpt.isPresent()) {
                log.debug("사용자 {}가 게시글 {} 북마크함", userId, postingId);
                return convertToBookmarkResponseDto(bookmarkOpt.get());
            } else {
                log.debug("사용자 {}가 게시글 {} 북마크하지 않음", userId, postingId);
                return null;
            }

        } catch (Exception e) {
            log.error("사용자 {}의 게시글 {} 북마크 확인 중 오류 발생: {}", userId, postingId, e.getMessage(), e);
            return null; // 조회 실패 시 null 반환
        }
    }

    /**
     * 사용자의 총 북마크 개수 조회
     *
     * @param userId 사용자 ID
     * @return 북마크 총 개수
     */
    public long getBookmarkCountByUserId(Long userId) {
        log.debug("사용자 {}의 총 북마크 개수 조회", userId);

        try {
            long count = bookmarkRepository.countByUserId(userId);
            log.debug("사용자 {}의 총 북마크 개수: {}개", userId, count);
            return count;

        } catch (Exception e) {
            log.error("사용자 {}의 북마크 개수 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            return 0; // 오류 시 0 반환
        }
    }

    /**
     * 게시글의 총 북마크 개수 조회
     *
     * @param postingId 게시글 ID
     * @return 해당 게시글의 북마크 총 개수
     */
    public long getBookmarkCountByPostingId(Long postingId) {
        log.debug("게시글 {}의 총 북마크 개수 조회", postingId);

        try {
            long count = bookmarkRepository.countByPostingId(postingId);
            log.debug("게시글 {}의 총 북마크 개수: {}개", postingId, count);
            return count;

        } catch (Exception e) {
            log.error("게시글 {}의 북마크 개수 조회 중 오류 발생: {}", postingId, e.getMessage(), e);
            return 0; // 오류 시 0 반환
        }
    }

    /**
     * 사용자의 최근 북마크 N개 조회
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @return 최근 북마크 목록
     */
    public List<BookmarkResponseDto> getRecentBookmarks(Long userId, int limit) {
        log.debug("사용자 {}의 최근 북마크 {}개 조회", userId, limit);

        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Bookmark> bookmarks = bookmarkRepository.findRecentBookmarksWithPostingByUserId(userId, pageable);

            return bookmarks.stream()
                    .map(this::convertToBookmarkResponseDto)
                    .toList();

        } catch (Exception e) {
            log.error("사용자 {}의 최근 북마크 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            return List.of(); // 오류 시 빈 리스트 반환
        }
    }

    /**
     * 북마크 검색
     *
     * @param userId 사용자 ID
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 북마크 페이지
     */
    public Page<BookmarkResponseDto> searchBookmarks(Long userId, String keyword, Pageable pageable) {
        log.debug("사용자 {}의 북마크 검색: {}", userId, keyword);

        try {
            Page<Bookmark> bookmarkPage = bookmarkRepository.searchBookmarksWithPosting(userId, keyword, pageable);

            return bookmarkPage.map(this::convertToBookmarkResponseDto);

        } catch (Exception e) {
            log.error("사용자 {}의 북마크 검색 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("북마크 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 삭제된 게시글의 북마크 목록 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 삭제된 게시글의 북마크 페이지
     */
    public Page<BookmarkResponseDto> getBookmarksWithDeletedPostings(Long userId, Pageable pageable) {
        log.debug("사용자 {}의 삭제된 게시글 북마크 목록 조회", userId);

        try {
            Page<Bookmark> bookmarkPage = bookmarkRepository.findBookmarksWithDeletedPostings(userId, pageable);

            return bookmarkPage.map(this::convertToBookmarkResponseDto);

        } catch (Exception e) {
            log.error("사용자 {}의 삭제된 게시글 북마크 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            throw new RuntimeException("삭제된 게시글 북마크 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Bookmark 엔티티를 BookmarkResponseDto로 변환
     * 삭제된 게시글의 경우 기본 정보를 표시
     *
     * @param bookmark 북마크 엔티티 (게시글 정보 포함 또는 null)
     * @return BookmarkResponseDto 객체
     */
    private BookmarkResponseDto convertToBookmarkResponseDto(Bookmark bookmark) {
        // 게시글 정보 DTO 생성
        BookmarkResponseDto.PostingDto postingDto = null;

        
        if (bookmark.getPosting() != null) {
            // 게시글이 존재하는 경우
            Posting posting = bookmark.getPosting();
            postingDto = BookmarkResponseDto.PostingDto.builder()
                    .postingId(posting.getPostingId())
                    .title(posting.getTitle())
                    .appointmentDatetime(posting.getAppointmentDatetime())
                    .location(posting.getAddress())
                    .isSelf(posting.getIsSelf())
                    .personName(posting.getPersonName())
                    .personPhoneNumber(posting.getPersonPhoneNumber())
                    .hasMobileInvitation(posting.getHasMobileInvitation())
                    .registrationDatetime(posting.getRegistrationDatetime())
                    .userId(posting.getUserId())
                    .build();
        } else {
            // 게시글이 삭제된 경우 기본 정보 표시
            postingDto = BookmarkResponseDto.PostingDto.builder()
                    .postingId(bookmark.getPostingId())
                    .title("[삭제된 게시글]")
                    .appointmentDatetime(null)
                    .location("[정보 없음]")
                    .isSelf(null)
                    .personName("[정보 없음]")
                    .personPhoneNumber(null)
                    .hasMobileInvitation(null)
                    .registrationDatetime(null)
                    .userId(null)
                    .build();
        }

        // 북마크 응답 DTO 생성
        return BookmarkResponseDto.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .postingId(bookmark.getPostingId())
                .userId(bookmark.getUserId())
                .memo(bookmark.getMemo())
                .marcDate(bookmark.getMarcDate())
                .posting(postingDto)
                .isPostingDeleted(bookmark.getPosting() == null) // 삭제 여부 표시
                .build();
    }

    /**
     * Bookmark와 Posting 엔티티로 BookmarkResponseDto 생성
     *
     * @param bookmark 북마크 엔티티
     * @param posting 게시글 엔티티
     * @return BookmarkResponseDto 객체
     */
    private BookmarkResponseDto createBookmarkResponseDto(Bookmark bookmark, Posting posting) {
        BookmarkResponseDto.PostingDto postingDto = BookmarkResponseDto.PostingDto.builder()
                .postingId(posting.getPostingId())
                .title(posting.getTitle())
                .appointmentDatetime(posting.getAppointmentDatetime())
                .location(posting.getAddress())
                .isSelf(posting.getIsSelf())
                .personName(posting.getPersonName())
                .personPhoneNumber(posting.getPersonPhoneNumber())
                .hasMobileInvitation(posting.getHasMobileInvitation())
                .registrationDatetime(posting.getRegistrationDatetime())
                .userId(posting.getUserId())
                .build();

        return BookmarkResponseDto.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .postingId(bookmark.getPostingId())
                .userId(bookmark.getUserId())
                .memo(bookmark.getMemo())
                .marcDate(bookmark.getMarcDate())
                .posting(postingDto)
                .build();
    }
}