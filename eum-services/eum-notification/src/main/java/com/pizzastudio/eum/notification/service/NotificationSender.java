package com.pizzastudio.eum.notification.service;

/** 문자·메일 발송 창구. 실제 발송은 외부 사업자 연계다. */
public interface NotificationSender {

    void send(String channelId, String receiver, String title, String content);
}
