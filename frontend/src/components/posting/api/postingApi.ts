import { get, put, post, del } from '../../../utils/httpClient';
import { MyPostingResponseDTO, PostingRequestDTO, PostingResponseDTO } from '../dto';

// API 기본 URL
const API_BASE_URL = process.env.REACT_APP_API_URL || '';

// 신청여부, 북마크여부 확인용 응답 타입
export interface PostingStatusResponse {
    bookmarkId: number;
    isBookmarked: boolean;
    isApplied: boolean;
    applyingId: number;
};

export const postingApi = {
    // 모집글 등록
    addPosting: async (postingData: PostingRequestDTO) => {
        try {
            return await post<number>(`${API_BASE_URL}/posting/`, postingData);
        } catch (error) {
            console.error('모집글 등록 에러:', error);
            return {
                success: false,
                data: null,
                message: '모집글 등록에 실패했습니다.'
            };
        }
    },
    
    // 모집글 수정
    updatePosting: async (postingId: string, postingData: PostingRequestDTO) => {
        try {
            return await put<PostingResponseDTO>(`${API_BASE_URL}/posting/${postingId}`, postingData);
        } catch (error) {
            return {
                success: false,
                data: null,
                message: '모집글 수정에 실패했습니다.'
            };
        }
    },

    // 모집 취소
    cancelPosting: async (postingId: number) => {
        try {
            return await del<void>(`${API_BASE_URL}/posting/${postingId}`);
        } catch (error) {
            return {
                success: false,
                data: null,
                message: '모집글 삭제에 실패했습니다.'
            };
        }
    },

    // 모집글 상세조회
    getPostingDetail: async (id: string) => {
        try {
            return await get<PostingResponseDTO>(`${API_BASE_URL}/posting/detail/${id}`);
        } catch(error) {
            return {
                success: false,
                data: null,
                message: '모집글 상세 조회에 실패했습니다.'
            };
        }
    },

    // 모집이력 상세조회
    getPostingHistoryDetail: async (id: string) => {
        try {
            return await get<PostingResponseDTO>(`${API_BASE_URL}/post/history/detail/${id}`);
        } catch(error) {
            return {
                success: false,
                data: null,
                message: '모집이력 상세 조회에 실패했습니다.'
            };
        }
    },

    // 모집글 목록 조회
    getAllPostingList: async (page: number = 0, size: number = 10, address: string = '', guestMainRole: string = '') => {
        try {
            return await get<{
                content: PostingResponseDTO[];
                totalElements: number;
                totalPages: number;
                last: boolean;
                first: boolean;
                number: number;
                size: number;
            }>(`${API_BASE_URL}/posting/page?page=${page}&size=${size}&address=${address}&guestMainRole=${guestMainRole}`);
        } catch(error) {
            return {
                success: false,
                data: null,
                message: '모집글 조회에 실패했습니다.'
            };
        }
    },

    // 내 모집글
    getMyPostingList: async (page: number = 0, size: number = 10) => {
        try {
            return await get<{
                content: MyPostingResponseDTO[]; 
                totalElements: number;
                totalPages: number;
                last: boolean;
                first: boolean;
                number: number;
                size: number;
            }>(`${API_BASE_URL}/posting/my/page?page=${page}&size=${size}`);
        } catch(error) {
            return {
                success: false,
                data: null,
                message: '내 모집글 조회에 실패했습니다.'
            };
        }
    },

    // 모집글 신청 및 북마크 여부 확인
    checkPostingStatus: async (postingId?: number | string) => {
        try {
            return await get<PostingStatusResponse>(`${API_BASE_URL}/posting/check/status/${postingId}`);
        } catch(error) {
            return {
                success: false,
                data: null,
                message: '모집글 상태 확인에 실패했습니다.'
            };
        }
    }, 
    
    // 모집 확정
    confirmationPosting: async (postingId: number) => {
        try {
            return await get<void>(`${API_BASE_URL}/posting/confirmation/${postingId}`);
        } catch(error) {
            return {
                success: false,
                data: null,
                message: '모집글 확정에 실패했습니다.'
            }
        }
    }
}