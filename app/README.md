# English

## Why Brevent

"`Brevent`" hajacks several system API to prevent not-in-use apps in `prevent list` from running or keep running. Furthermore, it applies to system apps too, specially, support google-family apps(`GAPPS`).

Not-in-use packages in `prevent list` can only run:

- some other app call it's activity (share, launcher)
- widget on home, however, it can only run 30 seconds
- sync service if you allow, it can only run 30 seconds too
- system services (excluding normal gapps), or alipay's service

**NOTE**: When Google Play Services(`GMS`) and related apps are in `prevent list`, only `GAPPS` and `GCM`-apps can use it. However, you cannot receive `GCM` message if `GMS` is not running.

**WARNING**: Don't prevent `system` apps nor daily apps, otherwise, you may miss important message.

**WARNING**: Don't prevent "`Xposed Installer`", otherwise the module won't active when you update it.

"`Brevent`" should work from Android 4.4 to 6.0. However, I mainly use 7.1.

## How to use

1. Patch your ROM with "`Brevent`".
2. Open "`Brevent`",  then add/remove application to/from prevent list.
3. Use android normally, press `back` or remove it from recent task to exit, and press `HOME` for pause.

And "`Brevent`" would keep non-"service" processes, of cource it cannot turn to "service".

## Special Search

- `-3` for `third` party apps
- `-a` for `a`ll apps (default show third party apps and gapps)
- `-s` for `s`ystem apps
- `-e` for `e`nabled apps
- `-r` for `r`unning apps
- `-g` for `g`apps, i.e. apps from google
- `-sg` for `s`ystem apps excluding `g`apps

## [Importance for Processes](http://developer.android.com/intl/zh-tw/reference/android/app/ActivityManager.RunningAppProcessInfo.html#constants):

### background

This process contains background code that is expendable.

### empty

This process is empty of any actively running code.

### foreground

This process is running the foreground UI; that is, it is the thing currently at the top of the screen that the user is interacting with.

### foreground service (since Android 6.0)

This process is running a foreground service, for example to perform music playback even while the user is not immediately in the app. This generally indicates that the process is doing something the user actively cares about.

### gone (since Android 5.0)

This process does not exist.

### perceptible

This process is not something the user is directly aware of, but is otherwise perceptable to them to some degree.

### service

This process is contains services that should remain running. These are background services apps have started, not something the user is aware of, so they may be killed by the system relatively freely (though it is generally desired that they stay running as long as they want to).

### top sleeping (since Android 6.0)

This process is running the foreground UI, but the device is asleep so it is not visible to the user. This means the user is not really aware of the process, because they can not see or interact with it, but it is quite important because it what they expect to return to once unlocking the device.

### visible

This process is running something that is actively visible to the user, though not in the immediate foreground. This may be running a window that is behind the current foreground (so paused and with its state saved, not interacting with the user, but visible to them to some degree); it may also be running other services under the system's control that it inconsiders important.

## Project

Project: [Brevent - GitHub](https://github.com/liudongmiao/Brevent). If you like, feel free to donate.

# ??????

## ????????????

???`??????`???????????????????????????API?????????`????????????`????????????????????????????????????????????????????????????????????????

???????????????`????????????`?????????????????????????????????????????????

- ??????????????????????????????????????????(Activity)????????????????????????????????????????????????
- ????????????????????????????????????????????????30??????
- ????????????????????????????????????????????????30??????
- ???`????????????`??????????????????????????????????????????????????????
- ?????????????????????????????????????????????

**??????**??????`????????????`???????????????????????????`??????????????????`???????????????`GCM`????????????????????????????????????????????????`??????????????????`?????????????????????????????????`????????????`??????????????????`GMS`?????????????????????`GCM`?????????????????????????????????

**??????**????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????`??????`????????????????????????????????????????????????????????????????????????????????????????????????

**??????**?????????????????????`Xposed Installer`????????????????????????????????????????????????????????????????????????????????????????????????

**??????**????????????????????????????????????`HOME`???`?????????`??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

???`??????`?????????Android 4.4???7.1??????????????????7.1????????????

## ????????????

1. ???????????????`??????`?????????
2. ?????????`??????`????????????`????????????`(?????????????????????)???
3. ???????????????????????????????????????`HOME`???????????????`?????????`????????????????????????????????????

????????????`??????`????????????`??????`???????????????????????????`??????`?????????????????????`??????`?????????***??????***?????????

## ????????????

- `-3` ??????????????????`???`?????????
- `-a` ??????????????????????????????????????????????????????????????????
- `-s` ?????????????????????
- `-e` ?????????????????????
- `-r` ?????????????????????
- `-g` `???`??????????????????
- `-sg` ?????????????????????????????????

## [????????????](http://developer.android.com/intl/zh-tw/reference/android/app/ActivityManager.RunningAppProcessInfo.html#constants)

### ??????(background)

??????????????????????????????(??????????????????????????????????????????????????????????????????)

### ???(empty)

?????????????????????????????????????????????

### ??????(foreground)

???????????????????????????????????????????????????????????????????????????????????????`??????`?????????????????????????????????`??????`????????????????????????

### ????????????(foreground service, ???Android 6.0)

?????????????????????????????????????????????????????????????????????????????????????????????????????????

### ???(gone, ???Android 5.0)

??????????????????

### ??????(perceptible)

?????????????????????????????????????????????????????????????????????????????????????????????????????????

### ??????(service)

??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

### ????????????(top sleeping??????Android 6.0)

?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

### ??????(visible)

????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

## ??????

???`??????`???<del>???????????????</del>????????????????????????[Brevent - GitHub](https://github.com/liudongmiao/Brevent)????????????????????????????????????
