import { useState, useCallback, useRef, useEffect } from "react";
import { PostingResponseDTO } from "../dto/PostingResponseDTO";
import { postingApi } from "../api/postingApi";

export const usePostingList = () => {
    const [postingList, setPostingList] = useState<PostingResponseDTO[]>([]);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [page, setPage] = useState(0);
    const [error, setError] = useState<string | null>(null);
    const [address, setAddress] = useState<string>('');
    const [guestMainRole, setGuestMainRole] = useState<string>('');
    
    // 중복 요청 방지를 위한 ref
    const isLoadingRef = useRef(false);

    const getPostingList = async () => {
        const response = await postingApi.getAllPostingList(0, 10, address, guestMainRole);
        if(response.success) {
            setPostingList(response.data?.content || []);
        }
    }

    const loadMorePostings = useCallback(async () => {
        // 이미 로딩 중이거나 더 이상 데이터가 없으면 중단
        if (isLoadingRef.current || !hasMore) return;
        
        isLoadingRef.current = true;
        setLoading(true);
        setError(null);
        try {
            const response = await postingApi.getAllPostingList(page, 10, address, guestMainRole);
            
            if (response.success && response.data) {
                const { content, last } = response.data;
                
                setPostingList(prevList => [...prevList, ...content]);
                setHasMore(!last);
                setPage(prevPage => prevPage + 1);
            } else {
                setError(response.message || '게시글을 불러오는데 실패했습니다.');
            }
        } catch (err) {
            setError('게시글을 불러오는데 실패했습니다.');
            console.error('Error loading more postings:', err);
        } finally {
            setLoading(false);
            isLoadingRef.current = false;
        }
    }, [page, hasMore, address, guestMainRole]);

    const resetAndLoadFirst = useCallback(async () => {
        setPostingList([]);
        setPage(0);
        setHasMore(true);
        setError(null);
        isLoadingRef.current = false;
        
        // 첫 페이지 로드
        await loadFirstPage();
    }, []);

    const loadFirstPage = useCallback(async () => {
        if (isLoadingRef.current) return;
        
        isLoadingRef.current = true;
        setLoading(true);
        setError(null);
        
        try {
            const response = await postingApi.getAllPostingList(0, 10, address, guestMainRole);
            
            if (response.success && response.data) {
                const { content, last } = response.data;
                
                setPostingList(content);
                setHasMore(!last);
                setPage(1); // 다음 페이지는 1이 됨
            } else {
                setError(response.message || '게시글을 불러오는데 실패했습니다.');
            }
        } catch (err) {
            setError('게시글을 불러오는데 실패했습니다.');
            console.error('Error loading first page:', err);
        } finally {
            setLoading(false);
            isLoadingRef.current = false;
        }
    }, [address, guestMainRole]);

    return {
        postingList,
        loading,
        hasMore,
        error,
        getPostingList,
        loadMorePostings,
        resetAndLoadFirst,
        loadFirstPage,
        setAddress,
        setGuestMainRole
    };
} 