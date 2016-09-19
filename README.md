# QuickOpen
An android app to quickly open your most used apps through a notification.

# TODO
- make NotificationService start at boot
- add better texts and explanations to settings-page
- add empty-view for recyclerview
- change recyclerview-item-layout and/or add divider
- add "about me"
- check sorting of installed applications
- fix slow or not appearing progress dialog 
- check performance on move item
- check customNotification-design on all api-levels (especially api 19)
- check activity leak on api 19 ? (open and close settings)
- add red background on swipe ?
- add feedback ?
- add tutorial ?
- disable MainActivity when notification_enabled = false ?
- check for uninstalled applications every start ? (or BroadcastReceiver?)
- add "Donate a beer" ?
- hide option "VISIBILITY_PRIVATE" because there will never be "sensitive data" to hide on lockscreen ?

# License

```
Copyright 2016 Michael Langer (mchllngr)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
