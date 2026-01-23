package com.expense.tracking.dto;

import java.util.List;
import java.util.Map;

import com.expense.tracking.service.PushNotificationService;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for push notification payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPayloadDto {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private String icon;
    private String badge;
    private String tag;
    
    @Builder.Default
    private Boolean requireInteraction = false;
    
    private Map<String, Object> data;
    private List<NotificationActionDto> actions;
    
    /**
     * Converts this DTO to the service layer NotificationPayload.
     */
    public PushNotificationService.NotificationPayload toServicePayload() {
        PushNotificationService.NotificationPayload payload = new PushNotificationService.NotificationPayload();
        payload.setTitle(this.title);
        payload.setBody(this.body);
        payload.setIcon(this.icon);
        payload.setBadge(this.badge);
        payload.setTag(this.tag);
        payload.setRequireInteraction(this.requireInteraction != null ? this.requireInteraction : false);
        payload.setData(this.data);
        
        if (this.actions != null && !this.actions.isEmpty()) {
            PushNotificationService.NotificationAction[] serviceActions = 
                this.actions.stream()
                    .map(NotificationActionDto::toServiceAction)
                    .toArray(PushNotificationService.NotificationAction[]::new);
            payload.setActions(serviceActions);
        }
        
        return payload;
    }
    
    /**
     * DTO for notification action buttons.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationActionDto {
        
        @NotBlank(message = "Action is required")
        private String action;
        
        @NotBlank(message = "Title is required")
        private String title;
        
        private String icon;
        
        /**
         * Converts this DTO to the service layer NotificationAction.
         */
        public PushNotificationService.NotificationAction toServiceAction() {
            return new PushNotificationService.NotificationAction(this.action, this.title, this.icon);
        }
    }
}