package wedding.alba.function.common.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class CommonPostQueryResult {
    private Long id;
    private Long userId;
    private String nickname;
    private String title;
    private String detailContent;
    private LocalDateTime appointmentDatetime;
    private Integer isSelf;
    private String personName;
    private String personPhoneNumber;
    private String address;
    private String buildingName;
    private String sidoSigungu;
    private Integer hasMobileInvitation;
    private String workingHours;
    private String startTime;
    private String endTime;
    private String payAmount;
    private Integer targetPersonnel;
    private String guestMainRole;
    private String tags;
    private LocalDateTime registrationDatetime;
    private LocalDateTime updateDatetime;
    private String payType;
    private String dataType;
    private Integer applyCount;
    private Integer confirmationCount;
    private Integer status;
}
