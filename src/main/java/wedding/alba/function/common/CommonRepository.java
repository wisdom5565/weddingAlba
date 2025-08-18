package wedding.alba.function.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wedding.alba.function.common.dto.CommonPostResponseDTO;

@Repository
public interface CommonRepository{
    Page<CommonPostResponseDTO> findMyPostingWithPagination(Long userId, Pageable pageable);
}
