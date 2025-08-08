// usePosting.ts
// 이 파일은 게시물 작성 폼의 상태 관리 및 관련 로직을 캡슐화하는 커스텀 훅을 제공합니다.
// 폼 데이터, 태그, 임금 계산 등 다양한 상태와 핸들러 함수들을 관리하여 컴포넌트의 복잡성을 줄입니다.

import { useState, useEffect, useCallback } from 'react';
import { PostingRequestDTO } from '../dto';
import { postingApi } from '../api/postingApi';
import { useNavigate, useParams } from 'react-router-dom';
import { useToast } from '../../common/toast/useToast';

// Daum Postcode API에서 반환되는 데이터의 최소한의 인터페이스 정의
// (daum-postcode.d.ts 파일이 없으므로 여기에 포함)
interface DaumPostcodeData {
    address: string;
    zonecode: string;
    roadAddress?: string;
    jibunAddress?: string;
    buildingName?: string; // 건물명 추가
    sido?: string; // 시도 추가
    sigungu?: string; // 시군구 추가
  // 필요한 경우 다른 필드들도 추가할 수 있습니다.
}

/**
 * `usePosting` 커스텀 훅이 반환하는 값들의 인터페이스입니다.
 * 이 인터페이스를 통해 훅의 사용자들은 어떤 상태와 함수들을 사용할 수 있는지 명확히 알 수 있습니다.
 */
interface UsePostingFormResult {
    tags: string[]; // 게시물에 추가된 태그 목록 상태
    setTags: React.Dispatch<React.SetStateAction<string[]>>; // 태그 목록을 업데이트하는 함수
    tagInput: string; // 현재 태그 입력 필드의 값 상태
    setTagInput: React.Dispatch<React.SetStateAction<string>>; // 태그 입력 필드 값을 업데이트하는 함수
    uploadedFile: File | null; // 업로드된 파일 객체 상태
    setUploadedFile: React.Dispatch<React.SetStateAction<File | null>>; // 업로드된 파일 객체를 업데이트하는 함수
    payType: 'hourly' | 'daily'; // 임금 지급 유형 (시급 또는 일급) 상태
    setPayType: React.Dispatch<React.SetStateAction<'hourly' | 'daily'>>; // 임금 지급 유형을 업데이트하는 함수
    payAmount: number; // 임금 금액 상태
    setPayAmount: React.Dispatch<React.SetStateAction<number>>; // 임금 금액을 업데이트하는 함수
    startTime: string; // 근무 시작 시간 상태
    setStartTime: React.Dispatch<React.SetStateAction<string>>; // 근무 시작 시간을 업데이트하는 함수
    endTime: string; // 근무 종료 시간 상태
    setEndTime: React.Dispatch<React.SetStateAction<string>>; // 근무 종료 시간을 업데이트하는 함수
    totalPay: number; // 최종 지급 금액 상태
    setTotalPay: React.Dispatch<React.SetStateAction<number>>; // 최종 지급 금액을 업데이트하는 함수
    formData: Partial<PostingRequestDTO>; // 게시물 폼 데이터 상태
    setFormData: React.Dispatch<React.SetStateAction<Partial<PostingRequestDTO>>>; // 폼 데이터를 업데이트하는 함수
    handleInputChange: (field: keyof PostingRequestDTO, value: any) => void; // 폼 입력 변경을 처리하는 핸들러 함수
    handleTagKeyDown: (e: React.KeyboardEvent<HTMLInputElement>) => void; // 태그 입력 필드에서 키 다운 이벤트를 처리하는 핸들러 함수
    removeTag: (tagToRemove: string) => void; // 태그를 제거하는 함수
    handleFileUpload: (e: React.ChangeEvent<HTMLInputElement>) => void; // 파일 업로드를 처리하는 핸들러 함수
    handleSubmit: () => void; // 폼 제출을 처리하는 핸들러 함수
    calculatePay: () => void; // 임금 계산을 수행하는 함수

    // 주소 검색 관련 필드 추가
    isAddressSearchOpen: boolean; // 주소 검색 모달의 열림/닫힘 상태
    openAddressSearch: () => void; // 주소 검색 모달을 여는 함수
    closeAddressSearch: () => void; // 주소 검색 모달을 닫는 함수
    handleAddressComplete: (data: DaumPostcodeData) => void; // 주소 검색 완료 시 호출될 콜백 함수
    isEditMode: boolean; // 수정 모드 여부
    
    // Toast 관련 추가
    toastState: any;
    showToast: (message: string, actionText?: string, onAction?: () => void) => void;
    hideToast: () => void;
}

/**
 * 게시물 작성 폼의 상태 관리 및 로직을 제공하는 커스텀 훅입니다.
 * @returns {UsePostingResult} 게시물 폼 관련 상태와 핸들러 함수들.
 */
export const usePostingForm = (): UsePostingFormResult => {
    const navigate = useNavigate();
    const { postingId } = useParams<{ postingId: string }>();
    const isEditMode = !!postingId;
    const { toastState, showToast, hideToast } = useToast();
    
    // 태그 관련 상태 관리
    const [tags, setTags] = useState<string[]>([]);
    const [tagInput, setTagInput] = useState<string>('');
    // 파일 업로드 상태 관리
    const [uploadedFile, setUploadedFile] = useState<File | null>(null);

    // 임금 관련 상태
    const [payType, setPayType] = useState<'hourly' | 'daily'>('hourly'); // 시급 또는 일급
    const [payAmount, setPayAmount] = useState<number>(0); // 임금 금액
    const [startTime, setStartTime] = useState<string>(''); // 근무 시작 시간
    const [endTime, setEndTime] = useState<string>(''); // 근무 종료 시간
    const [totalPay, setTotalPay] = useState<number>(0); // 최종 지급 금액

    // 폼 데이터 상태
    const [formData, setFormData] = useState<Partial<PostingRequestDTO>>({
        title: '',
        appointmentDatetime: '',
        address: '',
        buildingName: '',
        sidoSigungu: '',
        workingHours: '',
        startTime: '',
        endTime: '',
        payAmount: '',
        guestMainRole: '',
        detailContent: '',
        personName: '',
        personPhoneNumber: '',
        hasMobileInvitation: null,
        isSelf: null,
        targetPersonnel: 1,
        tags: []
    });
    // 게시물 데이터 로드 (수정 모드일 때)
    useEffect(() => {
        if (isEditMode && postingId) {
            const loadPosting = async () => {
                try {
                    const response = await postingApi.getPostingDetail(postingId); // getPostingById 대신 getPostingDetail 사용
                    if (response.success && response.data) {
                        // 불러온 데이터를 formData에 설정
                        const loadedData = response.data;
                        setFormData({
                            title: loadedData.title || '',
                            isSelf: loadedData.isSelf !== null && loadedData.isSelf !== undefined ? loadedData.isSelf : null,
                            personName: loadedData.personName || '',
                            personPhoneNumber: loadedData.personPhoneNumber || '',
                            appointmentDatetime: loadedData.appointmentDatetime || '',
                            address: loadedData.address || '',
                            buildingName: loadedData.buildingName || '',
                            sidoSigungu: loadedData.sidoSigungu || '',
                            hasMobileInvitation: loadedData.hasMobileInvitation !== null && loadedData.hasMobileInvitation !== undefined ? loadedData.hasMobileInvitation : null,
                            startTime: loadedData.startTime || '',
                            endTime: loadedData.endTime || '',
                            workingHours: loadedData.workingHours || '',
                            payType: loadedData.payType === 'DAILY' ? 'daily' : 'hourly',
                            payAmount: loadedData.payAmount || '',
                            guestMainRole: loadedData.guestMainRole || '',
                            targetPersonnel: loadedData.targetPersonnel || 1,
                            detailContent: loadedData.detailContent || '',
                            tags: loadedData.tags || [],
                        });
                        setPayType(loadedData.payType === 'DAILY' ? 'daily' : 'hourly');
                        setPayAmount(Number(loadedData.payAmount));
                        setStartTime(loadedData.startTime || '');
                        setEndTime(loadedData.endTime || '');
                        setTags(loadedData.tags || []);
                    } else {
                        showToast('게시물 로드에 실패했습니다.');
                        navigate('/posting/list'); // 실패 시 목록 페이지로 이동
                    }
                } catch (error) {
                    showToast('게시물 로드 중 오류가 발생했습니다.');
                    navigate('/posting/list');
                }
            };
            loadPosting();
        }
    }, [isEditMode, postingId, navigate, showToast]);

    // 주소 검색 관련 상태 추가
    const [isAddressSearchOpen, setIsAddressSearchOpen] = useState<boolean>(false);

    /**
     * 주소 검색 모달을 엽니다.
     */
    const openAddressSearch = useCallback(() => {
        setIsAddressSearchOpen(true);
    }, []);

    /**
     * 주소 검색 모달을 닫습니다.
     */
    const closeAddressSearch = useCallback(() => {
        setIsAddressSearchOpen(false);
    }, []);

    /**
     * 다음 주소 검색이 완료되었을 때 호출되는 콜백 함수입니다.
     * 검색된 주소 정보를 `formData.location`에 업데이트합니다.
     * @param {DaumPostcodeData} data 다음 주소 검색 결과 데이터
     */
    const handleAddressComplete = useCallback((data: DaumPostcodeData) => {
        // const fullAddress = data.roadAddress || data.address; // 도로명 주소 우선 사용
        setFormData(prev => ({
            ...prev,
            address: data.address || '',
            buildingName: data.buildingName || '',
            sidoSigungu: `${data.sido || ''} ${data.sigungu || ''}`.trim(),
        }));
        closeAddressSearch(); // 주소 선택 후 모달 닫기
    }, [closeAddressSearch]);

    /**
     * 근무 시간과 임금 유형, 금액을 기반으로 최종 지급 금액을 계산합니다.
     * `useCallback`을 사용하여 불필요한 함수 재생성을 방지합니다.
     */
    const calculatePay = useCallback(() => {
        if (!startTime || !endTime || payAmount <= 0) {
            setTotalPay(0);
            return;
        }

        const start = new Date(`2000-01-01T${startTime}`);
        const end = new Date(`2000-01-01T${endTime}`);

        if (end <= start) {
            setTotalPay(0);
            return;
        }

        const diffInMs = end.getTime() - start.getTime();
        const diffInHours = diffInMs / (1000 * 60 * 60);

        let finalPay = 0;
        if (payType === 'hourly') {
            finalPay = Math.round(diffInHours * payAmount);
        } else {
            finalPay = payAmount; // 일급의 경우 시간과 관계없이 고정 금액
        }

        setTotalPay(finalPay);
        
        // 계산된 근무 시간과 임금 정보를 formData에 업데이트
        const formatTimeForDisplay = (time: string) => {
            if (!time) return '';
            return time;
        };

        const workingHours = `${diffInHours.toFixed(1)}`;
        
        setFormData(prev => ({
            ...prev,
            startTime: startTime,
            endTime: endTime,
            workingHours: workingHours,
            payType : payType,
            payAmount: payAmount.toString()
        }));
    }, [startTime, endTime, payAmount, payType]);

    /**
     * `startTime`, `endTime`, `payType`, `payAmount` 상태가 변경될 때마다
     * `calculatePay` 함수를 호출하여 최종 지급 금액을 자동으로 재계산합니다.
     */
    useEffect(() => {
        calculatePay();
    }, [startTime, endTime, payType, payAmount, calculatePay]);

    /**
     * 폼 입력 필드의 변경을 처리하는 일반 핸들러 함수입니다.
     * `useCallback`을 사용하여 불필요한 함수 재생성을 방지합니다.
     * @param {keyof PostingRequestDTO} field 변경될 폼 데이터의 키(예: 'title', 'location')
     * @param {any} value 새로운 값
     */
    const handleInputChange = useCallback((field: keyof PostingRequestDTO, value: any) => {
        setFormData(prev => ({
            ...prev,
            [field]: value
        }));
    }, []);

    /**
     * 태그 입력 필드에서 키 다운 이벤트를 처리하는 핸들러 함수입니다.
     * Enter 또는 쉼표(,)를 누르면 새로운 태그를 추가합니다.
     * `useCallback`을 사용하여 불필요한 함수 재생성을 방지합니다.
     * @param {React.KeyboardEvent<HTMLInputElement>} e 키보드 이벤트 객체
     */
    const handleTagKeyDown = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            const newTag = tagInput.trim();
            if (newTag && tags.length < 5 && !tags.includes(newTag)) {
                const updatedTags = [...tags, newTag];
                setTags(updatedTags);
                setFormData(prev => ({ ...prev, tags: updatedTags }));
                setTagInput('');
            }
        }
    }, [tagInput, tags]);

    /**
     * 특정 태그를 태그 목록에서 제거하는 핸들러 함수입니다.
     * `useCallback`을 사용하여 불필요한 함수 재생성을 방지합니다.
     * @param {string} tagToRemove 제거할 태그 문자열
     */
    const removeTag = useCallback((tagToRemove: string) => {
        const updatedTags = tags.filter(tag => tag !== tagToRemove);
        setTags(updatedTags);
        setFormData(prev => ({ ...prev, tags: updatedTags }));
    }, [tags]);

    /**
     * 파일 업로드 변경을 처리하는 핸들러 함수입니다.
     * 사용자가 파일을 선택하면 `uploadedFile` 상태를 업데이트합니다.
     * `useCallback`을 사용하여 불필요한 함수 재생성을 방지합니다.
     * @param {React.ChangeEvent<HTMLInputElement>} e 파일 입력 변경 이벤트 객체
     */
    const handleFileUpload = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            setUploadedFile(file);
        }
    }, []);

    /**
     * 폼 제출을 처리하는 핸들러 함수입니다.
     * 폼 유효성을 검사하고, 유효한 경우 폼 데이터를 콘솔에 기록하고 API를 호출합니다.
     * `useCallback`을 사용하여 불필요한 함수 재생성을 방지합니다.
     */
    const handleSubmit = useCallback(async () => {
        // 필수 항목 유효성 검사
        if (!formData.title || !formData.appointmentDatetime || !startTime || !endTime || payAmount <= 0) {
            showToast('필수 항목을 입력해주세요.');
            return;
        }

        let response;
        if (isEditMode && postingId) {
            // 수정 모드: 게시물 수정 API 호출
            response = await postingApi.updatePosting(postingId, formData);
        } else {
            // 생성 모드: 게시물 추가 API 호출
            response = await postingApi.addPosting(formData);
        }
        const newOrUpdatedPostingId = response.data;
        
        if (response.success) {
            showToast('모집글 등록이 완료되었습니다.');
            navigate(`/posting/${newOrUpdatedPostingId}`);
        } else {
            // 서버에서 응답한 메시지가 있으면 그것을 사용, 없으면 기본 메시지
            const errorMessage = response.message || (isEditMode ? '모집글 수정에 실패했습니다.' : '모집글 생성에 실패했습니다.');
            showToast(errorMessage);
        }

    }, [formData, startTime, endTime, payAmount, uploadedFile, isEditMode, postingId, navigate, showToast]);

    // 훅이 외부로 노출할 상태와 함수들을 반환합니다.
    return {
        tags, setTags,
        tagInput, setTagInput,
        uploadedFile, setUploadedFile,
        payType, setPayType,
        payAmount, setPayAmount,
        startTime, setStartTime,
        endTime, setEndTime,
        totalPay, setTotalPay,
        formData, setFormData,
        handleInputChange,
        handleTagKeyDown,
        removeTag,
        handleFileUpload,
        handleSubmit,
        calculatePay,
        isAddressSearchOpen,
        openAddressSearch,
        closeAddressSearch,
        handleAddressComplete,
        isEditMode,
        toastState,
        showToast,
        hideToast
    };
}; 