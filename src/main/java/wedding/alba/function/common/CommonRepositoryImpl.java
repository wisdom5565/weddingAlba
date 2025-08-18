package wedding.alba.function.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import wedding.alba.function.common.dto.CommonPostQueryResult;
import wedding.alba.function.common.dto.CommonPostResponseDTO;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class CommonRepositoryImpl implements CommonRepository {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private CommonMapper commonMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Page<CommonPostResponseDTO> findMyPostingWithPagination(Long userId, Pageable pageable) {
        String sql = """
            SELECT 
                combined.id, 
                combined.userId,
                prof.nickname,
                combined.title, 
                combined.detailContent,
                combined.appointmentDatetime,
                combined.isSelf,
                combined.personName,
                combined.personPhoneNumber,
                combined.address,
                combined.buildingName,
                combined.sidoSigungu,
                combined.hasMobileInvitation,
                combined.workingHours,
                combined.startTime,
                combined.endTime,
                combined.payAmount,
                combined.targetPersonnel,
                combined.guestMainRole,
                combined.tags,
                combined.registration_datetime,
                combined.updateDatetime,
                combined.payType,
                combined.dataType,
                combined.applyCount,
                combined.confirmationCount,
                combined.status
            FROM (
                SELECT 
                    p.posting_id as id,
                    p.user_id as userId,
                    p.title as title,
                    p.detail_content as detailContent,
                    p.is_self as isSelf,
                    p.person_name as personName,
                    p.person_phone_number as personPhoneNumber,
                    p.appointment_datetime as appointmentDatetime,
                    p.address as address,
                    p.building_name as buildingName,
                    p.sido_sigungu as sidoSigungu, 
                    p.has_mobile_invitation as hasMobileInvitation,
                    p.start_time as startTime,
                    p.end_time as endTime,
                    p.working_hours as workingHours,
                    p.pay_type as payType,
                    p.pay_amount as payAmount,
                    p.target_personnel as targetPersonnel,
                    p.guest_main_role as guestMainRole,
                    p.tags as tags,
                    p.registration_datetime as registration_datetime,
                    p.update_datetime as updateDatetime,
                    COUNT(a.applying_id) as applyCount,
                    SUM(CASE WHEN a.status = 1 THEN 1 ELSE 0 END) as confirmationCount,
                    'ACTIVE' as dataType, 
                    NULL as status
                FROM postings p 
                LEFT JOIN applying a ON p.posting_id = a.posting_id 
                WHERE p.user_id = :userId
                GROUP BY p.posting_id
                
                UNION ALL
                
                SELECT 
                    ph.post_history_id as id,
                    ph.user_id as userId,
                    ph.title as title,
                    ph.detail_content as detailContent,
                    ph.is_self as isSelf,
                    ph.person_name as personName,
                    ph.person_phone_number as personPhoneNumber,
                    ph.appointment_datetime as appointmentDatetime,
                    ph.address as address,
                    ph.building_name as buildingName,
                    ph.sido_sigungu as sidoSigungu, 
                    ph.has_mobile_invitation as hasMobileInvitation,
                    ph.start_time as startTime,
                    ph.end_time as endTime,
                    ph.working_hours as workingHours,
                    ph.pay_type as payType,
                    ph.pay_amount as payAmount,
                    ph.target_personnel as targetPersonnel,
                    ph.guest_main_role as guestMainRole,
                    ph.tags as tags,
                    ph.registration_datetime as registration_datetime,
                    ph.update_datetime as updateDatetime,
                    COUNT(ah.apply_history_id) as applyCount,
                    SUM(CASE WHEN ah.status = 1 THEN 1 ELSE 0 END) as confirmationCount,
                    'HISTORY' as dataType,
                    ph.status as status
                FROM post_histories ph 
                LEFT JOIN apply_histories ah ON ph.post_history_id = ah.post_history_id 
                WHERE ph.user_id = :userId
                GROUP BY ph.post_history_id
            ) combined
            LEFT JOIN profiles prof ON combined.userId = prof.user_id 
            ORDER BY registration_datetime DESC
            LIMIT :limit OFFSET :offset
            """;

        String countSql = """
            SELECT COUNT(*) FROM (
                SELECT p.posting_id FROM postings p WHERE p.user_id = :userId
                UNION ALL
                SELECT ph.post_history_id FROM post_histories ph WHERE ph.user_id = :userId
            ) totalCount
            """;

        // 메인 쿼리 실행
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("limit", pageable.getPageSize());
        query.setParameter("offset", pageable.getOffset());
        
        List<Object[]> rawResults = query.getResultList();
        List<CommonPostResponseDTO> results = new ArrayList<>();
        
        // 중간 DTO를 사용하여 결과 매핑
        for (Object[] row : rawResults) {
            // 중간 DTO 생성 및 값 설정
            CommonPostQueryResult queryResult = new CommonPostQueryResult();
            int i = 0;
            
            queryResult.setId(convertToLong(row[i++]));
            queryResult.setUserId(convertToLong(row[i++]));
            queryResult.setNickname((String) row[i++]);
            queryResult.setTitle((String) row[i++]);
            queryResult.setDetailContent((String) row[i++]);
            queryResult.setAppointmentDatetime(convertToLocalDateTime(row[i++]));
            queryResult.setIsSelf(convertToInteger(row[i++]));
            queryResult.setPersonName((String) row[i++]);
            queryResult.setPersonPhoneNumber((String) row[i++]);
            queryResult.setAddress((String) row[i++]);
            queryResult.setBuildingName((String) row[i++]);
            queryResult.setSidoSigungu((String) row[i++]);
            queryResult.setHasMobileInvitation(convertToInteger(row[i++]));
            queryResult.setWorkingHours((String) row[i++]);
            queryResult.setStartTime(convertTimeToString(row[i++]));
            queryResult.setEndTime(convertTimeToString(row[i++]));
            queryResult.setPayAmount((String) row[i++]);
            queryResult.setTargetPersonnel(convertToInteger(row[i++]));
            queryResult.setGuestMainRole((String) row[i++]);
            queryResult.setTags((String) row[i++]);
            queryResult.setRegistrationDatetime(convertToLocalDateTime(row[i++]));
            queryResult.setUpdateDatetime(convertToLocalDateTime(row[i++]));
            queryResult.setPayType((String) row[i++]);
            queryResult.setDataType((String) row[i++]);
            queryResult.setApplyCount(convertToInteger(row[i++]));
            queryResult.setConfirmationCount(convertToInteger(row[i++]));
            queryResult.setStatus(convertToInteger(row[i++]));
            
            // MapStruct를 사용하여 최종 DTO로 변환
            CommonPostResponseDTO dto = commonMapper.toCommonPostResponseDTO(queryResult);
            results.add(dto);
        }
        
        // 카운트 쿼리 실행
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("userId", userId);
        Object totalResult = countQuery.getSingleResult();
        long total;
        
        // 데이터베이스 종류에 따라 반환 타입이 다를 수 있으므로 안전하게 처리
        if (totalResult instanceof BigInteger) {
            total = ((BigInteger) totalResult).longValue();
        } else if (totalResult instanceof Long) {
            total = (Long) totalResult;
        } else if (totalResult instanceof Number) {
            total = ((Number) totalResult).longValue();
        } else {
            total = Long.parseLong(totalResult.toString());
        }

        return new PageImpl<>(results, pageable, total);
    }
    
    // 유틸리티 메소드들
    private Long convertToLong(Object value) {
        return value != null ? ((Number) value).longValue() : null;
    }
    
    private Integer convertToInteger(Object value) {
        return value != null ? ((Number) value).intValue() : null;
    }
    
    private LocalDateTime convertToLocalDateTime(Object value) {
        return value != null ? ((Timestamp) value).toLocalDateTime() : null;
    }
    
    private String convertTimeToString(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Time) {
            return ((java.sql.Time) value).toString();
        }
        return value.toString();
    }
    
    private List<String> convertToStringList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        return Arrays.asList(value.split(","));
    }
}
