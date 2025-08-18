package wedding.alba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 북마크 엔티티
 *
 * 게시글이 삭제되어도 북마크는 유지됩니다.
 * 연관관계는 조회 성능을 위해서만 사용하고, 삭제 시 연쇄 작용은 발생하지 않습니다.
 */
@Entity
@Table(name = "bookmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @Column(name = "posting_id")
    private Long postingId;

    @Column(name = "post_history_id")
    private Long postHistoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "memo")
    private String memo;

    @Column(name = "marc_date", nullable = false)
    private LocalDateTime marcDate;

    // 게시글과의 연관관계 (조회 전용, 삭제 연쇄 없음)
    // optional = true: 게시글이 삭제되어도 북마크는 유지
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "posting_id", insertable = false, updatable = false)
    private Posting posting;

    // 사용자와의 연관관계 (필요시)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // 생성 시 현재 시간 자동 설정
    @PrePersist
    protected void onCreate() {
        if (marcDate == null) {
            marcDate = LocalDateTime.now();
        }
    }

    // 비즈니스 로직 메서드들
    public boolean hasUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public boolean hasPosting(Long postingId) {
        return this.postingId != null && this.postingId.equals(postingId);
    }

    public boolean hasMemo() {
        return memo != null && !memo.trim().isEmpty();
    }

    // 게시글이 삭제되었는지 확인
    public boolean isPostingDeleted() {
        return posting == null;
    }

    // 게시글 정보가 있는지 확인
    public boolean hasValidPosting() {
        return posting != null;
    }

    // toString (순환 참조 방지)
    @Override
    public String toString() {
        return "Bookmark{" +
                "bookmarkId=" + bookmarkId +
                ", postingId=" + postingId +
                ", userId=" + userId +
                ", memo='" + memo + '\'' +
                ", marcDate=" + marcDate +
                '}';
    }

    // equals와 hashCode (ID 기반)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return bookmarkId != null && bookmarkId.equals(bookmark.bookmarkId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}