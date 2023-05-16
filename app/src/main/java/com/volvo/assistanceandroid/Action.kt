package com.volvo.assistanceandroid

enum class Action(val label: Int, val answer: String) {
    MACHINE_FRONT_WORKLIGHT_ON(0, "Yes, I'll turn on the front work light"),
    MACHINE_FRONT_WORKLIGHT_OFF(1, "Yes, I'll turn off the front work light"),
    MACHINE_BOOM_WORKLIGHT_ON(2, "Yes, I'll turn on the boom work light"),
    MACHINE_BOOM_WORKLIGHT_OFF(3, "Yes, I'll turn ff the boom work light"),
    MACHINE_SIDE_WORKLIGHT_ON(4, "Yes, I'll turn on the side work light"),
    MACHINE_SIDE_WORKLIGHT_OFF(5, "Yes, I'll turn off the side work light"),
    MACHINE_REAR_WORKLIGHT_ON(6, "Yes, I'll turn on the rear work light"),
    MACHINE_REAR_WORKLIGHT_OFF(7, "Yes, I'll turn on the rear work light"),
    MACHINE_WORKMODE_ECO(8, "Yes, I will change to eco work mode"),
    MACHINE_WORKMODE_POWER(9, "Yes, I will change to power work mode"),
    MACHINE_WORKMODE_LIFTING(10, "Yes, I will change to lifting work mode"),
    MACHINE_WIPER_ON(11, "Yes, I'll run the wipers."),
    MACHINE_WIPER_OFF(12, "Yes, I will stop the wipers."),
    MACHINE_LEVEL_STATUS(13, "Yes, I'll show you the level status"),
    MACHINE_SERVICE_STATUS(14, "Yes, I'll show you the service status"),
    MACHINE_OPERATING_INFO_STATUS(15, "Yes, I'll show you the operating info status"),
    CLIMATE_TEMPERATURE_UP(16, "Yes, I'll turn the room temperature up."),
    CLIMATE_TEMPERATURE_DOWN(17, "Yes, I'll turn the room temperature down."),
    CLIMATE_MODE_AUTO(18, "Yes, I'll set your climate control mode to Auto."),
    CLIMATE_MODE_MANUAL(19, "Yes, I'll set your climate control mode to Manual."),
    MEDIA_MUSIC_ON(20, "Yes, I'll turn on the music"),
    MEDIA_MUSIC_OFF(21, "Yes, I'll turn off the music"),
    MEDIA_RADIO_ON(22, "Yes, I'll turn on the radio."),
    MEDIA_RADIO_OFF(23, "Yes, I'll turn off the radio."),
    NONE(100, "Sorry, I don't understand well. could you say it again?")
}