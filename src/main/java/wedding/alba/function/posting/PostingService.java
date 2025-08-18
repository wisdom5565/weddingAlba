package wedding.alba.function.posting;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import wedding.alba.entity.*;
import wedding.alba.function.applyHistory.ApplyHistoryService;
import wedding.alba.function.applyHistory.dto.ApplyHistoryDTO;
import wedding.alba.function.applyHistory.mapper.ApplyHistoryMapper;
import wedding.alba.function.applying.ApplyingService;
import wedding.alba.function.applying.dto.ApplyingResponseDTO;
import wedding.alba.function.bookMark.BookmarkService;
import wedding.alba.function.postHistory.mapper.PostHistoryMapper;
import wedding.alba.function.postHistory.dto.PostHistoryDTO;
import wedding.alba.function.postHistory.PostHistoryService;
import wedding.alba.function.posting.dto.PostingRequestDTO;
import wedding.alba.function.posting.dto.PostingResponseDTO;
import wedding.alba.function.posting.mapper.PostingMapper;
import wedding.alba.function.profile.ProfileRepository;

@Service
@Slf4j
public class PostingService {

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ApplyingService applyingService;

    @Autowired
    private PostHistoryService postHistoryService;

    @Autowired
    private ApplyHistoryService applyHistoryService;

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private PostingMapper postingMapper;

    @Autowired
    private PostHistoryMapper postHistoryMapper;

    @Autowired
    private ApplyHistoryMapper applyHistoryMapper;

    // 모집글 작성
    public Long createPosting(PostingRequestDTO postingDto) {
        // dto -> Posting 엔터티로 변경
        Posting posting = postingMapper.toPosting(postingDto);

        Long createdPostingId = postingRepository.save(posting).getPostingId();

        return createdPostingId;
    }

    // 모집글 수정
    @Transactional
    public Long updatePosting(Long userId, Long postingId, PostingRequestDTO postingDto) {
        Posting existPosting = checkPostingStatus(postingId, userId, "update");

        postingDto.setUserId(userId);
        postingDto.setPostingId(postingId);

        postingMapper.updatePostingFromDto(postingDto, existPosting);
        Long updatePostingId = postingRepository.save(existPosting).getPostingId();
        log.info("사용자 {}가  모집글 {} 수정완료", userId, updatePostingId);
        return updatePostingId;
    }

    @Transactional
    public void deletePosting(Long userId, Long postingId) {

        // 삭제전에 신청글 확인
        Posting existPosting = checkPostingStatus(postingId, userId, "delete");

        // 모집글 히스토리로 데이터 이동 (상태 : -1)
        // 실패시 삭제 시도안하고 전체 롤백되게
        PostHistoryDTO historyDTO = postHistoryMapper.toPostHistoryDTOFromPosting(existPosting, true);
        Long postHistoryId = postHistoryService.movePostingToHistory(historyDTO);

        if(postHistoryId == null || postHistoryId == 0L) {
            log.error("모집글 {}을 모집글 이력으로 데이터 이동 실패", postingId);
            throw new RuntimeException("모집글 이력 저장에 실패하여 삭제를 중단합니다.");
        }

        // 확정된 신청글이있는경우, 신청이력테이블로 데이터 이동
        // 없는경우, 신청글 모두 신청이력테이블로 이동 모집글 이력으로 이동
        List<ApplyingResponseDTO> applyingList = applyingService.getApplyingListByPostingId(postingId);

        if(!applyingList.isEmpty()) {
            for(ApplyingResponseDTO applying : applyingList) {
                ApplyHistoryDTO applyHistoryDTO = applyHistoryMapper.toApplyHistoryDTO(applying, true, postHistoryId);
                Long applyHistoryId = applyHistoryService.moveApplyingToHistory(applyHistoryDTO);

                if(applyHistoryId == null || applyHistoryId == 0L) {
                    log.error("신청글 {}을 신청글 이력으로 데이터 이동 실패", applying.getApplyingId());
                    throw new RuntimeException("신청글 이력 저장에 실패하여 삭제를 중단합니다.");
                }

                applyingService.deleteApplyingById(applying.getApplyingId());
            }
        }

        // 모집글 삭제
        postingRepository.deleteById(postingId);
    }

    @Transactional
    public void confirmationPosting(Long postingId, Long userId) {
        Posting existPosting = checkPostingStatus(postingId, userId, "confirm");

        // 모집글 이력으로 이동
        PostHistoryDTO historyDTO = postHistoryMapper.toPostHistoryDTOFromPosting(existPosting, false);
        Long postHistoryId = postHistoryService.movePostingToHistory(historyDTO);

        if(postHistoryId == null || postHistoryId == 0L) {
            log.error("모집글 {}을 모집글 이력으로 데이터 이동 실패", postingId);
            throw new RuntimeException("모집글 이력 저장에 실패하여 확정을 중단합니다.");
        }
        // 신청글 확인
        List<ApplyingResponseDTO> applyingList = applyingService.getApplyingListByPostingId(postingId);

        if(!applyingList.isEmpty()) {
            for (ApplyingResponseDTO applying : applyingList) {
                if(applying.getStatus() == 1) {
                    ApplyHistoryDTO applyHistoryDTO = applyHistoryMapper.toApplyHistoryDTO(applying, false, postHistoryId);
                    Long applyHistoryId = applyHistoryService.moveApplyingToHistory(applyHistoryDTO);

                    if(applyHistoryId == null || applyHistoryId == 0L) {
                        log.error("신청글 {}을 신청글 이력으로 데이터 이동 실패", applying.getApplyingId());
                        throw new RuntimeException("신청글 이력 저장에 실패하여 모집확정을 중단합니다.");
                    }
                } else {
                    ApplyHistoryDTO applyHistoryDTO = applyHistoryMapper.toApplyHistoryDTO(applying, true, postHistoryId);
                    Long applyHistoryId = applyHistoryService.moveApplyingToHistory(applyHistoryDTO);

                    if(applyHistoryId == null || applyHistoryId == 0L) {
                        log.error("신청글 {}을 신청글 이력으로 데이터 이동 실패", applying.getApplyingId());
                        throw new RuntimeException("신청글 이력 저장에 실패하여 모집확정을 중단합니다.");
                    }
                }
                applyingService.deleteApplyingById(applying.getApplyingId());
            }
        }

        postingRepository.deleteById(postingId);
    }

    // 전체 모집글 리스트 조회
    public Page<PostingResponseDTO> getAllPostingList(int page, int size, String address, String guestMainRole) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDatetime"));
            // 페이징으로 Posting 엔터티 조회
            Page<Posting> postingPage = postingRepository.findPostingPageByKeyword(pageable, LocalDateTime.now(), address,guestMainRole);
            
            // 각 Posting에  DTO로 변환
            List<PostingResponseDTO> dtoList = new ArrayList<>();
            for(Posting posting : postingPage.getContent()) {
                PostingResponseDTO dto = postingMapper.toDetailDTO(posting);
                dtoList.add(dto);
            }
            
            // Page<PostingResponseDTO>로 변환하여 반환
            return new PageImpl<>(dtoList, pageable, postingPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("페이징 모집글 조회 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("페이징 모집글 조회에 실패했습니다.",    e);
        }
    }

    // 상세페이지
    public PostingResponseDTO getPostingDetail(Long postingId) {
        Posting posting = postingRepository.findById(postingId).orElseThrow(() -> new NoSuchElementException("모집글을 찾을 수 없습니다. ID: " + postingId));
        Profile profile = profileRepository.findByUserId(posting.getUserId()).orElseThrow(() -> new NoSuchElementException("사용자 프로필을 찾을 수 없습니다. ID: " + postingId));
        PostingResponseDTO dto = postingMapper.toDetailDTO(posting);
        return dto;
    }

    public List<PostingResponseDTO> getPostingListByUserId(Long userId) {
        List<PostingResponseDTO> postingResponseDTOList =  postingRepository.findPostingPageByUserId(userId).stream().map(posting -> postingMapper.toDetailDTO(posting)).toList();
        return postingResponseDTOList;
    }


    private Posting checkPostingStatus (Long postingId, Long userId, String check) {
        String checkText = "";
        switch (check) {
            case "update":
                checkText = "수정";
                break;
            case "delete":
                checkText = "삭제";
                break;
            case "confirm":
                checkText = "확정";
                break;
        }

        String finalCheckText = checkText;
        Posting existPosting = postingRepository.findById(postingId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 모집글 {}  {} 시도", postingId, finalCheckText);
                    return new IllegalArgumentException("존재하지 않는 모집글입니다.");
                });

        // 본인 게시글인지, 수정할려는 게시글이 맞는지 확인
        if(!existPosting.getUserId().equals(userId)) {
            log.warn("사용자 {}가 다른 사용자의 모집글 {} {} 시도", userId, postingId, finalCheckText);
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        return existPosting;
    }

}
