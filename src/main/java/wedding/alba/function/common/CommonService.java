package wedding.alba.function.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import wedding.alba.enums.EnumType;
import wedding.alba.function.applyHistory.ApplyHistoryService;
import wedding.alba.function.applyHistory.dto.ApplyHistoryDTO;
import wedding.alba.function.applying.ApplyingService;
import wedding.alba.function.applying.dto.ApplyingResponseDTO;
import wedding.alba.function.common.dto.CommonApplyResponseDTO;
import wedding.alba.function.common.dto.CommonPostResponseDTO;
import wedding.alba.function.postHistory.PostHistoryService;
import wedding.alba.function.postHistory.dto.PostHistoryDTO;
import wedding.alba.function.posting.PostingService;
import wedding.alba.function.posting.dto.PostingResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommonService {
    @Autowired
    private CommonMapper commonMapper;

    @Autowired
    private CommonRepository commonRepository;

    @Autowired
    private PostingService postingService;

    @Autowired
    private PostHistoryService postHistoryService;

    @Autowired
    private ApplyingService applyingService;

    @Autowired
    private ApplyHistoryService applyHistoryService;

    // 내 모집글 리스트 - 무한스크롤 최적화 버전
    public Page<CommonPostResponseDTO> getMyPostingPage(int page, int size, Long userId) {
        try {
            // 페이지네이션 설정
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDatetime"));
            
            // CommonRepository를 통해 최적화된 단일 쿼리로 데이터 조회
            return commonRepository.findMyPostingWithPagination(userId, pageable);
        } catch (Exception e) {
            log.error("내 모집글 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("내 모집글 조회에 실패했습니다.", e);
        }
    }

    public List<CommonApplyResponseDTO> getApplyListByPostId(Long id, String dataType) {
        List<CommonApplyResponseDTO> commonApplyResponseDTOList =  new ArrayList<>();
        
        try {
            EnumType.PostingStatusType statusType = EnumType.PostingStatusType.valueOf(dataType);
            
            if(statusType == EnumType.PostingStatusType.ACTIVE) {
                List<CommonApplyResponseDTO> list = applyingService.getApplyingListByPostingId(id)
                        .stream()
                        .map(commonMapper::toCommonApplyResponseDTO)
                        .collect(Collectors.toList());
                commonApplyResponseDTOList.addAll(list);
            } else if(statusType == EnumType.PostingStatusType.HISTORY) {
                List<CommonApplyResponseDTO> list = applyHistoryService.getApplyHistoryListByPostId(id)
                        .stream()
                        .map(commonMapper::toCommonApplyResponseDTO)
                        .collect(Collectors.toList());
                commonApplyResponseDTOList.addAll(list);
            }
        } catch (IllegalArgumentException e) {
            // 잘못된 dataType인 경우 기본적으로 ACTIVE로 처리
            log.warn("잘못된 dataType: {}, ACTIVE로 처리합니다.", dataType);
            List<CommonApplyResponseDTO> list = applyingService.getApplyingListByPostingId(id)
                    .stream()
                    .map(commonMapper::toCommonApplyResponseDTO)
                    .collect(Collectors.toList());
            commonApplyResponseDTOList.addAll(list);
        }
        
        return commonApplyResponseDTOList;
    }
}
