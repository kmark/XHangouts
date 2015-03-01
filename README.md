# [XHangouts](http://repo.xposed.info/module/com.versobit.kmark.xhangouts)
![XHangouts](https://raw.githubusercontent.com/kmark/XHangouts/master/app/src/main/res/drawable-xhdpi/ic_launcher.png)

Google Hangouts is a great SMS app for those of us who also make considerable use of the Google
Hangouts IM platform. While it does a better job of handling MMS than most Android
alternatives it still has a few key persisting issues:

* Images not being properly rotated
* Low quality resizing making text impossible to read
* *TODO:* Horrific MMS wake locking. If the MMS transaction process doesn't go *perfectly* you can kiss your battery goodbye. Hangouts will send an MMS or literally die trying.

XHangouts is an Xposed module tasked with resolving these issues. Maybe one day we'll get a seamless iMessage-esque solution from Google...

See [this thread on XDA](http://forum.xda-developers.com/xposed/modules/xhangouts-mms-fixes-google-hangouts-t2888102) for more information, support, etc. The GitHub issue tracker is a valid way of reporting bugs as well.

Icon by [Adrian Babilinski](https://twitter.com/mrBabilin)!

## Licensing
Copyright &copy; 2014-2015 Kevin Mark. XHangouts is licensed under the GNU General Public License, Version 3, which can
be found in `LICENSE.md`
