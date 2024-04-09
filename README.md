# Notifications for Facebook

Small Android app for getting notified (friends, messages and notifications) while being privacy and battery friendly. It doesn't require the Facebook Platform to be turned on. It plays nicely with [SlimSocial for Facebook](https://f-droid.org/repository/browse/?fdfilter=SlimSOcial&fdid=it.rignanese.leo.slimfacebook).

<a href="https://f-droid.org/packages/org.surrel.facebooknotifications" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="100"/></a>
<a href="https://play.google.com/store/apps/details?id=org.surrel.facebooknotifications.gplay" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="100"/></a>

[Direct link to release .apk file](https://raw.githubusercontent.com/gsurrel/FacebookNotifications/master/FacebookNotifications-release.apk) and [link to debug .apk file](https://raw.githubusercontent.com/gsurrel/FacebookNotifications/master/FacebookNotifications-debug.apk).

![Notifications for Facebook logo image](https://raw.githubusercontent.com/gsurrel/FacebookNotifications/master/app/src/main/ic_launcher-web.png)

## Changelog

* _1.10.1_
  * Upgrade API level for the Play Store to be happy
   * Add link to DontKillMyApp
   * Ask runtime permission to draw over other apps

* _1.10.0_
   * Added shortcut to logout
   * Added quit button
   * Added a "Open in browser" button

* _1.9.2_
   * Fixed a crash on wrong user input

* _1.9.1_
   * Hopefully fixed issues #19 & #21

* _1.9.0_ thanks to @hirvinen
   * Fixed issues #18 & #20
   * Cleaned and refactored code
   * Added possibility to share the current page

* _1.8.0_
   * Fixed custom vibration pattern bug
   * Added custom blinking pattern per category
   * Updated sdk build-tools used

* _1.7.0_
   * Added compatibility to older devices

* _1.6.5_
   * Added vibration control
   * Settings panel disabled irrelevant features

* _1.6.3_
   * Made app Play Store compliant

* _1.6.2_
   * Custom update time added in the settings

* _1.6.1_
   * Simplified preferences page, allowing to disable sound notification by selecting the "None" sound

* _1.6.0_
    * Added sound notification
    * Enable/disable notifications per type
    * Support back button while browsing

* _1.5.x_
    * Removed support library without API level regression
    * F-Droid integration
    * Not pausing current activity when updating
    * Can be used as a browser for Facebook. Not fully featured!
    * Better code construct
    * Unified notification type using "Big View" and direct access buttons
    * Remembers the notification count so it does not bother the user every 5 minutes
    * Using notification LED

* _1.0.0_
    * Initial release
