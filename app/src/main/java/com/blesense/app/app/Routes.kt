package com.blesense.app.app

object Routes {

    const val SPLASH = "splash_screen"
    const val FIRST = "first_screen"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val INTERMEDIATE = "intermediate_screen"
    const val HOME = "home_screen"
    const val SETTINGS = "settings_screen"
    const val ROBOT = "robot_screen"
    const val LED_REMOTE = "led_remote_screen"

    const val ADVERTISING =
        "advertising/{deviceName}/{deviceAddress}/{sensorType}/{deviceId}"

    const val DATA_LOGGER =
        "data_logger/{deviceName}/{deviceAddress}/{deviceId}"

    const val CHART =
        "chart_screen/{deviceAddress}"

    const val CHART_2 =
        "chart_screen_2/{title}/{value}"


    const val ip="172.26.78.34:8080"
}
